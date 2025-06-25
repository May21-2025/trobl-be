package com.may21.trobl._global.utility;

import org.apache.commons.text.StringEscapeUtils;

public class SecurityUtils {
    public static String escapeHtml(String input) {
        return StringEscapeUtils.escapeHtml4(input);
    }

    public static String decodeHtml(String input) {
        return StringEscapeUtils.unescapeHtml4(input);
    }
}
