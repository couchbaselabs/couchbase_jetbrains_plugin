package com.couchbase.intellij.tree.overview.apis;

import com.couchbase.client.core.deps.com.google.common.reflect.TypeToken;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.intellij.database.ActiveCluster;
import com.couchbase.intellij.database.Permissions;
import com.couchbase.intellij.tree.overview.range.Range;
import com.couchbase.intellij.tree.overview.range.RangeData;
import com.couchbase.intellij.workbench.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.lang.reflect.Type;

public class CouchbaseRestAPI {

    public static String getMetaDocument(String bucket, String scope, String collection, String id ) throws Exception {
        String result = callSingleEndpoint((ActiveCluster.getInstance().isSSLEnabled() ? "18091" : "8091") + "/pools/default/buckets/"
                        +bucket+"/scopes/"+scope+"/collections/"+collection+"/docs/"+id, ActiveCluster.getInstance().getClusterURL());

        JsonObject object = JsonObject.fromJson(result);
        object.removeKey("json");
        return object.toString();
    }
    public static List<String> listKVDocuments(String bucket, String scope, String collection, int skip, int limit ) throws Exception {
        String result = callSingleEndpoint((ActiveCluster.getInstance().isSSLEnabled() ? "18091" : "8091") + "/pools/default/buckets/"
                        +bucket+"/scopes/"+scope+"/collections/"+collection+"/docs?skip="+skip+"&limit="+limit+"&include_doc=false",
                ActiveCluster.getInstance().getClusterURL());

        Gson gson = new Gson();
        KVDocsList response = gson.fromJson(result, KVDocsList.class);

        return response.getRows().stream()
                .map(KVRow::getId)
                .collect(Collectors.toList());
    }
    public static Map<String, Integer> getCollectionCounts(String bucket, String scope) throws Exception {

        String payload = "[\n" +
                "  {\n" +
                "    \"step\": 3,\n" +
                "    \"timeWindow\": 360,\n" +
                "    \"start\": -3,\n" +
                "    \"metric\": [\n" +
                "      {\n" +
                "        \"label\": \"name\",\n" +
                "        \"value\": \"kv_collection_item_count\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"bucket\",\n" +
                "        \"value\": \""+bucket+"\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"scope\",\n" +
                "        \"value\": \""+scope+"\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"nodesAggregation\": \"sum\"\n" +
                "  }\n" +
                "]";

        String content = callPostSingleEndpoint((ActiveCluster.getInstance().isSSLEnabled() ? "18091" : "8091") + "/pools/default/stats/range/",
                ActiveCluster.getInstance().getClusterURL(), payload);

        Gson gson = new GsonBuilder().create();

        Type listType = new TypeToken<List<Range>>() {
        }.getType();
        List<Range> ranges = gson.fromJson(content, listType);

        Map<String, Integer> result = new HashMap<>();
        for (Range range : ranges) {
            for (RangeData data : range.getData()) {
                if ("kv_collection_item_count".equals(data.getMetric().getName())) {
                    result.put(data.getMetric().getBucket() + "." + data.getMetric().getScope() + "." + data.getMetric().getCollection(),
                            Integer.valueOf(data.getValues().get(0).get(1).toString()));
                }
            }
        }
        return result;
    }

    public static Map<String, IndexStats> getIndexStats(String bucket, String scope, String collection, String indexName) throws Exception {

        String key = bucket;
        if ("_default".equals(scope) && "_default".equals(collection)) {
            key += ":" + indexName;
        } else {
            key += ":" + scope + ":" + collection + ":" + indexName;
        }

        List<String> calls = callAllEndpoints((ActiveCluster.getInstance().isSSLEnabled() ? "19102" : "9102") + "/api/v1/stats/" + bucket + "." + scope + "." + collection, ActiveCluster.getInstance().getClusterURL());

        Log.debug("Endpoint results " + calls.size());
        Gson gson = new GsonBuilder().create();

        Map<String, IndexStats> result = new HashMap<>();
        for (String jsonString : calls) {

            if (jsonString != null) {
                Map<String, IndexStats> map = gson.fromJson(jsonString, new TypeToken<Map<String, IndexStats>>() {
                }.getType());

                for (Map.Entry<String, IndexStats> entry : map.entrySet()) {
                    //the index might contain replicas which will look like "travel-sample:def_sourceairport (replica 1)"
                    if (key.equals(entry.getKey()) || entry.getKey().contains(key + " ")) {
                        Log.debug(entry.getKey() + ":" + entry.getValue());
                        result.put(entry.getKey(), entry.getValue());
                    } else {
                        Log.debug("Excluding:" + entry.getKey());
                    }
                }
            }
        }

        return result;
    }

    public static ServerOverview getOverview() throws Exception {

        String content = callSingleEndpoint((ActiveCluster.getInstance().isSSLEnabled() ? "18091" : "8091") + "/pools/nodes", ActiveCluster.getInstance().getClusterURL());
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.fromJson(content, ServerOverview.class);
    }

    public static BucketOverview getBucketOverview(String bucket) throws Exception {

        String content = callSingleEndpoint((ActiveCluster.getInstance().isSSLEnabled() ? "18091" : "8091") + "/pools/default/buckets/" + bucket, ActiveCluster.getInstance().getClusterURL());
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.fromJson(content, BucketOverview.class);
    }

    public static Permissions callWhoAmIEndpoint() throws Exception {
        String content = callSingleEndpoint((ActiveCluster.getInstance().isSSLEnabled() ? "18091" : "8091") + "/whoami", ActiveCluster.getInstance().getClusterURL());
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.fromJson(content, Permissions.class);
    }

    private static String callSingleEndpoint(String endpoint, String cbURL) throws Exception {
        List<String> servers = NSLookup.getServerURL(cbURL);
        return callGetEndpoint(endpoint, servers.get(0));
    }

    private static List<String> callAllEndpoints(String endpoint, String cbURL) throws Exception {
        List<String> servers = NSLookup.getServerURL(cbURL);
        Log.debug("Servers Found:" + servers.toString());
        List<CompletableFuture<String>> futureList = servers.parallelStream()
                .map(key -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return callGetEndpoint(endpoint, key);
                    } catch (Exception e) {
                        return null;
                    }
                }))
                .collect(Collectors.toList());

        return futureList.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

    }

    public static String callPostSingleEndpoint(String endpoint, String serverURL, String data) throws Exception {
        List<String> servers = NSLookup.getServerURL(serverURL);
        return callEndpoint(false, endpoint, servers.get(0), data);
    }

    private static String callGetEndpoint(String endpoint, String serverURL) throws Exception {
        return callEndpoint(true, endpoint, serverURL, null);
    }

    private static String callEndpoint(boolean isGet, String endpoint, String serverURL, String data) throws Exception {

        String userpass = ActiveCluster.getInstance().getUsername() + ":" + ActiveCluster.getInstance().getPassword();
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));

        String urlContent = (ActiveCluster.getInstance().isSSLEnabled() ? "https://" : "http://") + serverURL + ":" + endpoint;
        URL url = new URL(urlContent);
        InputStream stream;

        HttpURLConnection conn =  null;
        if (ActiveCluster.getInstance().isSSLEnabled()) {
            conn  = (HttpsURLConnection) url.openConnection();
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod(isGet?"GET":"POST");
        conn.setRequestProperty("Authorization", basicAuth);
        if(!isGet) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        stream = conn.getInputStream();


        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }
}
