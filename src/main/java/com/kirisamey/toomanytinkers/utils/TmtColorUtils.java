package com.kirisamey.toomanytinkers.utils;

import org.joml.Vector4f;

public class TmtColorUtils {
    public static int Argb2Abgr(int argb) {
        int b = (argb & 0xff) << 16;
        int r = (argb & 0xff0000) >> 16;
        return (argb & 0xff00ff00) | b | r;
    }

    public static Vector4f Argb2RgbaF(int argb) {
        var b = (argb & 0xff) / 255f;
        var g = ((argb >> 8) & 0xff) / 255f;
        var r = ((argb >> 16) & 0xff) / 255f;
        var a = ((argb >> 24) & 0xff) / 255f;
        return new Vector4f(r, g, b, a);
    }
}
