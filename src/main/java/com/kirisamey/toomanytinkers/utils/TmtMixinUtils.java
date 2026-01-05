package com.kirisamey.toomanytinkers.utils;

public class TmtMixinUtils {
    public static class ToolModel{
        public static int animForNormalQuads = -1;
        public static boolean excludedForNormalQuads = false;

        public static void resetForNormalQuads() {
            animForNormalQuads = -1;
            excludedForNormalQuads = false;
        }
    }

    public static class PartModel{
        public static int animForQuads = -1;
        public static boolean excludedForQuads = false;

        public static void resetForQuads() {
            animForQuads = -1;
            excludedForQuads = false;
        }
    }
}
