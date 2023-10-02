package com.couchbase.intellij.workbench;

import com.couchbase.client.core.api.query.CoreQueryResult;
import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.*;
import com.couchbase.client.java.transactions.TransactionQueryResult;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.persistence.storage.QueryHistoryStorage;
import com.couchbase.intellij.workbench.error.CouchbaseQueryError;
import com.couchbase.intellij.workbench.error.CouchbaseQueryErrorUtil;
import com.couchbase.intellij.workbench.error.CouchbaseQueryResultError;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class QueryExecutor {

    private static final DecimalFormat df = new DecimalFormat("#.00");
    private static ToolWindow toolWindow;
    private static QueryResultToolWindowFactory resultWindow;
    private static boolean isQueryScript = false;

    private static QueryResultToolWindowFactory getOutputWindow(Project project) {
        if (toolWindow == null) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            toolWindow = toolWindowManager.getToolWindow("Couchbase Output");
        }
        SwingUtilities.invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> toolWindow.show()));

        if (resultWindow == null) {
            resultWindow = QueryResultToolWindowFactory.instance;
        }
        return resultWindow;
    }

    public static boolean executeScript(QueryType type, List<String> statements, String bucket, String scope, int historyIndex, Project project) {
        Cluster cluster = ActiveCluster.getInstance().getCluster();
        if (statements == null || statements.isEmpty()) {
            return false;
        }
        if (ActiveCluster.getInstance().get() == null) {
            Messages.showMessageDialog("There is no active connection to run this query", "Couchbase Plugin Error", Messages.getErrorIcon());
            return false;
        }
        getOutputWindow(project).setStatusAsLoading();

        List<JsonObject> result = new ArrayList<>();
        List<QueryMetaData> metas = new ArrayList<>();
        Long start = System.currentTimeMillis();
        AtomicLong mutationCount = new AtomicLong();
        AtomicInteger resultCount = new AtomicInteger();
        AtomicLong resultSize = new AtomicLong();
        CouchbaseQueryResultError error = new CouchbaseQueryResultError();
        try {
            cluster.transactions().run(tx -> {
                for (int i = 0; i < statements.size(); i++) {
                    String query = statements.get(i);

                    if (type != QueryType.NORMAL) {
                        query = String.format("%s %s", type.toString(), query);
                    }

                    JsonObject queryResult = JsonObject.create();
                    result.add(queryResult);
                    queryResult.put("_sequence_num", i);
                    queryResult.put("_sequence_query", query);

                    try {
                        TransactionQueryResult r;
                        if (bucket == null) {
                            r = tx.query(query);
                        } else {
                            r = tx.query(cluster.bucket(bucket).scope(scope), query);
                        }

                        QueryMetaData meta = r.metaData();
                        metas.add(meta);
                        if (meta.status() == QueryStatus.SUCCESS) {
                            queryResult.put("_sequence_query_status", "success");
                            JsonArray rows = JsonArray.from(r.rowsAsObject());
                            if (rows.size() > 0) {
                                queryResult.put("_sequence_result", rows);
                                resultCount.addAndGet(rows.size());
                                meta.metrics().ifPresent(queryMetrics -> {
                                    resultSize.addAndGet(queryMetrics.resultSize());
                                    mutationCount.addAndGet(queryMetrics.mutationCount());
                                });
                            } else {
                                meta.metrics().ifPresent(queryMetrics -> {
                                    mutationCount.addAndGet(queryMetrics.mutationCount());
                                });
                            }
                        } else {
                            queryResult.put("_sequence_query_status", meta.status().toString().toLowerCase());
                        }
                    } catch (Throwable e) {
                        queryResult.put("_sequence_query_status", "error");
                        queryResult.put("_sequence_query_error", e.getMessage());
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            error.getErrors().add(new CouchbaseQueryError(0, e.getMessage(), false));
        }

        List<String> metricsList = new ArrayList<>();
        metricsList.add(System.currentTimeMillis() - start + " MS");
        metricsList.add("-");
        metricsList.add("-");
        metricsList.add(String.valueOf(mutationCount.get()));
        metricsList.add(String.valueOf(resultCount.get()));
        metricsList.add(getSizeText(resultSize.get()));
        List<String> timings;
        if (type == QueryType.EXPLAIN) {
            timings = result.stream()
                    .map(o -> o.getArray("_sequence_result"))
                    .filter(Objects::nonNull)
                    .map(a -> a.getObject(0))
                    .filter(Objects::nonNull)
                    .map(o -> o.get("plan"))
                    .filter(Objects::nonNull)
                    .map(o -> o.toString())
                    .collect(Collectors.toList());
        } else if (type == QueryType.ADVISE) {
            timings = metas.stream()
                    .filter(meta -> meta.profile().isPresent())
                    .map(meta -> meta.profile().get().get("executionTimings").toString())
                    .collect(Collectors.toList());
        } else {
            timings = null;
        }
        getOutputWindow(project).updateQueryStats(metricsList, result, error, timings,true);

        return error.getErrors().isEmpty();
    }

    public static boolean executeQuery(QueryType type, String query, String bucket, String scope, int historyIndex, Project project) {

        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        if (ActiveCluster.getInstance().get() == null) {
            Messages.showMessageDialog("There is no active connection to run this query", "Couchbase Plugin Error", Messages.getErrorIcon());
            return false;
        }
        getOutputWindow(project).setStatusAsLoading();

        if (QueryType.EXPLAIN == type) {
            query = "EXPLAIN " + query;
        } else if (QueryType.ADVISE == type) {
            query = "ADVISE " + query;
        } else {
            if (ActiveCluster.getInstance().isReadOnlyMode() && SQLPPAnalyzer.isMutation(query)) {
                CouchbaseQueryError err = new CouchbaseQueryError();
                err.setMessage("Hey! You can not run mutations when your cluster is on read-only mode");
                CouchbaseQueryResultError error = new CouchbaseQueryResultError();
                error.setErrors(List.of(err));

                getOutputWindow(project).updateQueryStats(Arrays.asList("0 MS", "-", "-", "-", "-", "-"),
                        null, error, null,false);
                return false;
            }
        }

        final String adjustedQuery = query;

        SwingUtilities.invokeLater(() -> {
            long start = 0;
            try {
                start = System.currentTimeMillis();
                QueryResult result;

                if (bucket != null) {
                    result = ActiveCluster.getInstance().get().bucket(bucket).scope(scope).query(adjustedQuery,
                            QueryOptions.queryOptions().profile(QueryProfile.TIMINGS).metrics(true));
                } else {
                    result = ActiveCluster.getInstance().get().query(adjustedQuery, QueryOptions.queryOptions().profile(QueryProfile.TIMINGS).metrics(true));
                }
                long end = System.currentTimeMillis();

                Optional<QueryMetrics> metrics = result.metaData().metrics();
                List<String> metricsList = new ArrayList<>();
                if (metrics.isPresent()) {
                    metricsList.add(end - start + " MS");
                    metricsList.add(metrics.get().elapsedTime().toMillis() + " MS");
                    metricsList.add(metrics.get().executionTime().toMillis() + " MS");
                    metricsList.add(String.valueOf(metrics.get().mutationCount()));
                    metricsList.add(String.valueOf(metrics.get().resultCount()));
                    metricsList.add(getSizeText(metrics.get().resultSize()));
                } else {
                    metricsList.add("-");
                    metricsList.add("-");
                    metricsList.add("-");
                    metricsList.add("-");
                    metricsList.add("-");
                    metricsList.add("-");
                }

                List<JsonObject> resultList;

                try {
                    resultList = result.rowsAsObject();
                } catch (Exception ex) {
                    if (ex.getMessage().startsWith("Deserialization of content into target class com.couchbase.client.java.json.JsonObject failed")) {
                        Field field = QueryResult.class.getDeclaredField("internal");
                        field.setAccessible(true);
                        CoreQueryResult internal = (CoreQueryResult) field.get(result);
                        List<JsonObject> objList = new ArrayList<>();
                        internal.rows().forEach(e -> {
                            JsonObject obj = JsonObject.create();
                            obj.put("content", JsonArray.fromJson(e.data()));
                            objList.add(obj);
                        });
                        resultList = objList;
                    } else {
                        Log.error(ex);
                        throw ex;
                    }
                }


                String timings;
                if (QueryType.EXPLAIN == type) {
                    timings = result.rowsAsObject().get(0).get("plan").toString();
                } else {
                    timings = result.metaData().profile().isPresent() ? result.metaData().profile().get().get("executionTimings").toString() : null;
                }

                getOutputWindow(project).updateQueryStats(metricsList, resultList, null, Collections.singletonList(timings),false);
            } catch (CouchbaseException e) {
                long end = System.currentTimeMillis();
                getOutputWindow(project).updateQueryStats(Arrays.asList((end - start) + " MS", "-", "-", "-", "-", "-"),
                        null, CouchbaseQueryErrorUtil.parseQueryError(e), null,false);
            } catch (Exception e) {
                Log.error(e);
                e.printStackTrace();
            }
        });

        //if historyIndex is negative, doesn't add it to the history
        if (historyIndex >= 0) {
            return updateQueryHistory(query, historyIndex);
        } else {
            return false;
        }


    }

    private static String getSizeText(long size) {
        if (size < 1024) {
            return size + " Bytes";
        } else {
            return df.format(size / 1024.0) + " KB";
        }
    }

    private static boolean updateQueryHistory(String query, int currentIndex) {
        List<String> hist = QueryHistoryStorage.getInstance().getValue().getHistory();
        if (query == null) {
            return false;
        }
        //running am old query
        if (hist.size() >= currentIndex + 1) {
            String histQuery = hist.get(currentIndex);
            if (histQuery.equals(query.trim())) {
                return false;
            } else {
                addQueryToHistory(query, hist);
                return true;
            }
        } else {
            addQueryToHistory(query, hist);
            return true;
        }
    }

    private static void addQueryToHistory(String query, List<String> hist) {
        if (hist.size() < 100) {
            hist.add(query.trim());
        } else {
            hist.remove(0);
            hist.add(query.trim());
        }
    }

    public static void setIsQueryScript(boolean isQueryScript) {
        QueryExecutor.isQueryScript = isQueryScript;
    }

    public static boolean getIsQueryScript() {
        return isQueryScript;
    }

    public enum QueryType {
        NORMAL,
        EXPLAIN,
        ADVISE
    }
}
