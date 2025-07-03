package com.may21.trobl._global.enums;

public enum ImageSize {
    THUMBNAIL(150, 150, "thumb"),
    SMALL(300, 300, "sm"),
    MEDIUM(600, 600, "md"),
    LARGE(1200, 1200, "lg"),
    ORIGINAL(0, 0, "orig");

    private final int width;
    private final int height;
    private final String prefix;

    ImageSize(int width, int height, String prefix) {
        this.width = width;
        this.height = height;
        this.prefix = prefix;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getPrefix() { return prefix; }
}