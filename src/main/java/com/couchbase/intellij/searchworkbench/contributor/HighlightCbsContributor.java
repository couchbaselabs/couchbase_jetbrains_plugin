package com.couchbase.intellij.searchworkbench.contributor;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.json.psi.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HighlightCbsContributor implements CBSContributor {

    public static final List<String> keys = Arrays.asList("style", "fields");

    @Override
    public boolean accept(String parentKey) {
        return "highlight".equals(parentKey);
    }

    @Override
    public void contributeKey(String parentKey, JsonObject jsonObject, List<String> contributors, CompletionResultSet result) {
        ContributorUtil.suggestMissing(jsonObject, keys, contributors);
    }

    @Override
    public void contributeValue(JsonObject jsonObject, String attributeKey, List<String> contributors, Map<String, String> fields) {
        if ("style".equals(attributeKey)) {
            contributors.add("ansi");
            contributors.add("html");
        }
    }

}
