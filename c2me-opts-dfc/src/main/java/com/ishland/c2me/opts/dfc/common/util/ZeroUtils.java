package com.ishland.c2me.opts.dfc.common.util;

public class ZeroUtils {

    public static boolean isPositiveZero(double x) {
        if (x != 0.0) {
            throw new IllegalArgumentException("x isn't zero");
        }
        return 1.0 / x > 0.0;
    }

    private ZeroUtils() {
    }

}
