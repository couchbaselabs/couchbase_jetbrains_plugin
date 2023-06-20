package com.couchbase.intellij.tree.node;

import com.couchbase.intellij.tree.NodeDescriptor;
import com.intellij.openapi.util.IconLoader;

public class CollectionsNodeDescriptor extends NodeDescriptor {

    private String connectionId;
    private String bucket;
    private String scope;
    public CollectionsNodeDescriptor(String connectionId, String bucket, String scope) {
        super("Collections", IconLoader.findIcon("./assets/icons/collections.svg", ScopeNodeDescriptor.class,
                false, true));
        this.connectionId = connectionId;
        this.bucket = bucket;
        this.scope = scope;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}