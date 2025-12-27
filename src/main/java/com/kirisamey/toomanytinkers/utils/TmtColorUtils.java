package com.kirisamey.toomanytinkers.utils;

public class TmtColorUtils {
    public static int Argb2Abgr(int argb) {
        int b = (argb & 0xff) << 16;
        int r = (argb & 0xff0000) >> 16;
        return (argb & 0xff00ff00) | b | r;
    }
}
