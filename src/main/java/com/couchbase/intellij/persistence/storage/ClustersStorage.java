package com.couchbase.intellij.persistence.storage;

import com.couchbase.intellij.persistence.Clusters;
import com.couchbase.intellij.persistence.SavedCluster;
import com.couchbase.intellij.workbench.Log;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@State(
        name = "ClustersStorage",
        storages = {
                @Storage(
                        value = "clusters.xml"
                )
        }
)
public class ClustersStorage implements PersistentStateComponent<ClustersStorage.State> {

    private final State myState = new State();

    public static ClustersStorage getInstance() {
        return ApplicationManager.getApplication().getService(ClustersStorage.class);
    }

    @Nullable
    @Override
    public ClustersStorage.State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull ClustersStorage.State state) {
        XmlSerializerUtil.copyBean(state, myState);

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Cleaning up stale cache entries...", true) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // remove stale cache
                myState.clusters.setInferCacheUpdateTimes(
                        myState.clusters.getInferCacheUpdateTimes().entrySet().stream()
                                .filter(entry -> {

                                    if (myState.clusters.getMap() == null) {
                                        return false;
                                    }
                                    if (!myState.clusters.getMap().containsKey(entry.getKey())) {
                                        Log.debug("removing caches for stale cluster " + entry.getKey());
                                        myState.clusters.getInferCache().remove(entry.getKey());
                                        return false;
                                    }
                                    return true;
                                })
                                .filter(e -> e.getValue() != null)
                                .peek(utimes -> {
                                    utimes.setValue(utimes.getValue().entrySet().stream()
                                            .filter(Objects::nonNull)
                                            .filter(utime -> utime.getValue() != null)
                                            .filter(utime -> {
                                                SavedCluster savedCluster = myState.clusters.getMap().get(utimes.getKey());
                                                if (savedCluster != null) {
                                                    if (System.currentTimeMillis() - utime.getValue() < savedCluster.getInferCachePeriod()) {
                                                        return true;
                                                    }
                                                }
                                                savedCluster.getInferCacheValues().remove(utime.getKey());
                                                Log.debug("Removing stale cache for " + utime.getKey() + " on cluster " + utimes.getKey());
                                                return false;
                                            })
                                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                    );
                                })
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );
            }
        });
    }

    public Clusters getValue() {
        if (myState.clusters == null) {
            myState.clusters = new Clusters();
        }
        return myState.clusters;
    }

    public void setValue(Clusters newValue) {
        myState.clusters = newValue;
    }

    public static class State {
        public Clusters clusters = null;

        public Clusters getClusters() {
            return clusters;
        }

        public void setClusters(Clusters clusters) {
            this.clusters = clusters;
        }
    }
}