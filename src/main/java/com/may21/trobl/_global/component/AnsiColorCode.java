package com.may21.trobl._global.component;

public class AnsiColorCode {
    public static final String RESET = "\u001B[0m"; // 색상 초기화

    // 글자색
    public static final String WHITE = "\u001B[97m";
    public static final String PINK = "\u001B[95m";
    public static final String RED = "\u001B[91m";
    public static final String YELLOW = "\u001B[93m";
    public static final String GREEN = "\u001B[92m";
    public static final String LIGHT_GREEN = "\u001B[38;5;118m";
    public static final String BLUE = "\u001B[94m";
    public static final String LIGHT_BLUE = "\u001B[38;5;39m";
    public static final String CYAN = "\u001B[96m";
    public static final String LIGHT_CYAN = "\u001B[38;5;159m";
    public static final String PURPLE = "\u001B[35m";
    public static final String LIGHT_PURPLE = "\u001B[38;5;183m";
    public static final String ORANGE = "\u001B[38;5;208m";
    public static final String BROWN = "\u001B[38;5;94m";
    public static final String TAN = "\u001B[38;5;180m";
    public static final String GRAY = "\u001B[90m";
    public static final String LIGHT_GREY = "\u001B[37m";
    public static final String BLACK = "\u001B[30m";
    public static final String TROBL_PINK = "\u001B[38;2;209;157;152m";
    public static final String TROBL_BLUE = "\u001B[38;2;84;130;246m";
    public static final String TROBL_WHITE = "\u001B[38;2;240;232;224m";
    public static final String TROBL_RED = "\u001B[38;2;246;60;60m";

    // 배경색
    public static final String RED_BG = "\u001B[48;5;88m";
    public static final String YELLOW_BG = "\u001B[48;5;58m";
    public static final String ORANGE_BG = "\u001B[48;5;130m";
    public static final String GREEN_BG = "\u001B[42";
    public static final String BLUE_BG = "\u001B[44m";
    public static final String PURPLE_BG = "\u001B[45m";
    public static final String PINK_BG = "\u001B[48;5;125m";
    public static final String GREY_BG = "\u001B[48;5;236m";

    public static String colorLog(String color, String message) {
        return colorLog(color, message, null);
    }

    public static String colorLog(String color, String message, Integer length) {
        String coloredLog = color + message + RESET;
        if (length != null) {
            int padding = length - message.length();
            coloredLog += " ".repeat(Math.max(0, padding));
        }
        return coloredLog;
    }

    public static String colorLogWithBG(
            String textColor, String bgColor, String message, Integer length) {
        String coloredLog = textColor + bgColor + message;
        if (length != null) {
            int padding = length - message.length();
            coloredLog += " ".repeat(Math.max(0, padding));
        }
        return coloredLog + RESET;
    }

    public static String colorLogWithBG(String textColor, String bgColor, String message) {
        return colorLogWithBG(textColor, bgColor, message, null);
    }
}
