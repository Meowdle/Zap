package com.zap.config;

import java.util.Objects;

public final class ClasspathEntries {
    private ClasspathEntries() {}
    private static final String WILD_CARD = "/*";

    // just trim it with forward slashes
    public static String portable(String entry) {
        Objects.requireNonNull(entry, "entry");
        String newEntry = entry.trim().replace("\\", "/");
        return Objects.requireNonNull(newEntry, "newEntry");
    }

    // return true for entries such as "lib/*" (every jar inside the folder)
    public static boolean isWildCard(String entry) {
        Objects.requireNonNull(entry, "entry");
        return portable(entry).endsWith(WILD_CARD);
    }

    // the path part of the entry, without the trailing wild card
    public static String pathPart(String entry) {
        Objects.requireNonNull(entry, "entry");
        String portable = portable(entry);
        return isWildCard(portable)
                ? portable.substring(0, portable.length() - WILD_CARD.length())
                : portable;
    }
}
