package com.couchbase.intellij.tree.node;

import com.intellij.openapi.util.IconLoader;

public class MissingIndexNodeDescriptor extends NodeDescriptor {

    private String bucket;
    private String scope;
    private String collection;

    public MissingIndexNodeDescriptor(String bucket, String scope, String collection) {
        super("<html>Index not found. <small style='font-size: 80%'>Double-click to create one.</small</html>",
                IconLoader.getIcon("/assets/icons/warning-circle-simple.svg", MissingIndexNodeDescriptor.class));
        this.bucket = bucket;
        this.scope = scope;
        this.collection = collection;
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

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }
}

