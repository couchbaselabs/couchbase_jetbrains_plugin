package com.couchbase.intellij.tools;

import java.util.HashMap;
import java.util.Map;

public class CBTools {

    public static Map<Type, CBTool> tools = new HashMap<>() {{
        for (Type type : Type.values()) {
            put(type, new CBTool());
        }
    }};

    public static CBTool getTool(Type type) {
        return tools.get(type);
    }

    public enum Type {
        SHELL,
        CB_IMPORT,
        CB_EXPORT,
        CBC_PILLOW_FIGHT,
        CBSTATS,
        MCTIMINGS,
    }
}
