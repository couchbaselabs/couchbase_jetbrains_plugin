package com.couchbase.intellij.workbench;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.ProjectManager;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Log {

    //1 - errors, 2 - errors and info, 3-debug,infos,errors
    public static int logLevel = 2;
    private static ConsoleView console;

    public static void setLevel(int level) {
        logLevel = level;
    }

    public static boolean isDebug() {
        return logLevel >= 3;
    }

    public static ConsoleView getLogger() {
        if (console == null) {
            console = TextConsoleBuilderFactory.getInstance().createBuilder(
                    ProjectManager.getInstance().getDefaultProject()).getConsole();
        }
        return console;
    }

    public static void info(String message) {
        if (logLevel >= 2) {
            getLogger().print("\n" + message, ConsoleViewContentType.LOG_INFO_OUTPUT);
        }
    }

    public static void debug(String message) {
        if (logLevel >= 3) {
            getLogger().print("\n" + message, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        }
    }

    public static void error(Class c, String message, Exception e) {

        String exception = convertToString(e);
        getLogger().print("\n" + c.getSimpleName() + ":" + message + " error: " + exception, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    public static void error(Exception e) {

        String exception = convertToString(e);
        getLogger().print("\n" + "error: " + exception, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    public static void error(String message, Exception e) {
        getLogger().print("\n" + message + " error: " + convertToString(e), ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    public static void error(String message) {
        getLogger().print("\n" + message, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    private static String convertToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        pw.close();
        return stackTrace;
    }
}
