package com.couchbase.intellij.searchworkbench.validator;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.json.psi.JsonObject;

public interface SearchObjectValidator {


    boolean accept(String key);

    void validate(String key, JsonObject jsonObject, ProblemsHolder holder);
}
