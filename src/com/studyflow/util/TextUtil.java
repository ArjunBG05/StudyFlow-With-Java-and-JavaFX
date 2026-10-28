package com.studyflow.util;

/** Escaping helpers so free text can be stored safely in pipe-delimited files. */
public final class TextUtil {

    private TextUtil() { }

    public static String escape(String s) {
        return s.replace("%", "%25").replace("|", "%7C").replace("\n", "%0A").replace("\r", "%0D");
    }

    public static String unescape(String s) {
        return s.replace("%0D", "\r").replace("%0A", "\n").replace("%7C", "|").replace("%25", "%");
    }

    public static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "~";
    }
}
