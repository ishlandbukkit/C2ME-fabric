package com.ishland.c2me.opts.natives_math.common;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

public class Bindings {

    private static MethodHandle bind(MethodHandle template, String prefix) {
        return template.bindTo(NativeLoader.lookup.find(prefix + NativeLoader.currentMachineTarget.getSuffix()).get());
    }

    private static final MethodHandle MH_c2me_natives_noise_perlin_double = bind(BindingsTemplate.c2me_natives_noise_perlin_double, "c2me_natives_noise_perlin_double");
    private static final MethodHandle MH_c2me_natives_noise_perlin_double_ptr = bind(BindingsTemplate.c2me_natives_noise_perlin_double_ptr, "c2me_natives_noise_perlin_double");

    public static double c2me_natives_noise_perlin_double(MemorySegment data, double x, double y, double z) {
        try {
            return (double) MH_c2me_natives_noise_perlin_double.invokeExact(data, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static double c2me_natives_noise_perlin_double(long data_ptr, double x, double y, double z) {
        try {
            return (double) MH_c2me_natives_noise_perlin_double_ptr.invokeExact(data_ptr, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle MH_c2me_natives_noise_perlin_double_batch = bind(BindingsTemplate.c2me_natives_noise_perlin_double_batch, "c2me_natives_noise_perlin_double_batch");
    private static final MethodHandle MH_c2me_natives_noise_perlin_double_batch_partial_ptr = bind(BindingsTemplate.c2me_natives_noise_perlin_double_batch_ptr, "c2me_natives_noise_perlin_double_batch");

    public static void c2me_natives_noise_perlin_double_batch(MemorySegment data, MemorySegment res, MemorySegment x, MemorySegment y, MemorySegment z, int length) {
        try {
            MH_c2me_natives_noise_perlin_double_batch.invokeExact(data, res, x, y, z, length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void c2me_natives_noise_perlin_double_batch(long data_ptr, MemorySegment res, MemorySegment x, MemorySegment y, MemorySegment z, int length) {
        try {
            MH_c2me_natives_noise_perlin_double_batch_partial_ptr.invokeExact(data_ptr, res, x, y, z, length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle MH_c2me_natives_noise_interpolated = bind(BindingsTemplate.c2me_natives_noise_interpolated, "c2me_natives_noise_interpolated");
    private static final MethodHandle MH_c2me_natives_noise_interpolated_ptr = bind(BindingsTemplate.c2me_natives_noise_interpolated_ptr, "c2me_natives_noise_interpolated");

    public static double c2me_natives_noise_interpolated(MemorySegment data, double x, double y, double z) {
        try {
            return (double) MH_c2me_natives_noise_interpolated.invokeExact(data, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static double c2me_natives_noise_interpolated(long data_ptr, double x, double y, double z) {
        try {
            return (double) MH_c2me_natives_noise_interpolated_ptr.invokeExact(data_ptr, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle MH_c2me_natives_end_islands_sample = bind(BindingsTemplate.c2me_natives_end_islands_sample, "c2me_natives_end_islands_sample");
    private static final MethodHandle MH_c2me_natives_end_islands_sample_ptr = bind(BindingsTemplate.c2me_natives_end_islands_sample_ptr, "c2me_natives_end_islands_sample");

    public static float c2me_natives_end_islands_sample(MemorySegment data, int x, int z) {
        try {
            return (float) MH_c2me_natives_end_islands_sample.invokeExact(data, x, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static float c2me_natives_end_islands_sample(long data_ptr, int x, int z) {
        if ((int) (x * x + z * z) < 0) { // workaround some compiler bugs
            return Float.NaN;
        }
        try {
            return (float) MH_c2me_natives_end_islands_sample_ptr.invokeExact(data_ptr, x, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle MH_c2me_natives_biome_access_sample = bind(BindingsTemplate.c2me_natives_biome_access_sample, "c2me_natives_biome_access_sample");

    public static int c2me_natives_biome_access_sample(long seed, int x, int y, int z) {
        try {
            return (int) MH_c2me_natives_biome_access_sample.invokeExact(seed, x, y, z);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
