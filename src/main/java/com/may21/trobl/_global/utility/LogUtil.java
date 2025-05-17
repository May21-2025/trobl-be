package com.may21.trobl._global.utility;

import static com.may21.trobl._global.component.AnsiColorCode.*;
import com.may21.trobl._global.aop.Threshold;

public class LogUtil {

  private LogUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String color(String text, String color) {
    return color + text + RESET;
  }

  public static String formatWarning(String method, String type, Threshold t) {
    return color(
            "[SENTENCE] [" + method + "] : " + t.label() + " [" + type + "] " + t.message(),
            t.bgColor())
        + "\n";
  }

  public static String formatTable(
      String uri,
      String method,
      long time,
      int queryCount,
      Threshold qt,
      Threshold tt,
      String warning) {
    return "\n+----------------------+-----------------------------------+\n"
        + String.format("| %-20s | %-33s |%n", "Request URI", uri)
        + String.format("| %-20s | %-33s |%n", "Method", method)
        + String.format(
            "| %-20s | %-33s |%n",
            "Execution Time (ms)", color(String.valueOf(time), tt != null ? tt.color() : GREEN))
        + String.format(
            "| %-20s | %-33s |%n",
            "Query Count", color(String.valueOf(queryCount), qt != null ? qt.color() : GREEN))
        + "+----------------------+-----------------------------------+\n"
        + warning;
  }
}
