package com.ishland.c2me.opts.natives_math.common;

import com.ishland.c2me.base.mixin.access.IInterpolatedNoiseSampler;
import com.ishland.c2me.base.mixin.access.IOctavePerlinNoiseSampler;
import com.ishland.c2me.base.mixin.access.IPerlinNoiseSampler;
import com.ishland.c2me.opts.natives_math.common.util.MemoryUtil;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.stream.IntStream;

public class BindingsTemplate {

    // double c2me_natives_noise_perlin_sample (const uint8_t *permutations, double originX, double originY, double originZ, double x, double y, double z, double yScale, double yMax)
    public static final MethodHandle c2me_natives_noise_perlin_sample = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE
            ),
            Linker.Option.critical(true)
    );

    // c2me_natives_noise_perlin_double, double, (const double_octave_sampler_data_t *data, double x, double y, double z)
    public static final MethodHandle c2me_natives_noise_perlin_double = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE
            ),
            Linker.Option.critical(false)
    );
    public static final MethodHandle c2me_natives_noise_perlin_double_ptr = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE
            ),
            Linker.Option.critical(false)
    );

    // c2me_natives_noise_perlin_double_batch, void, (const double_octave_sampler_data_t *const data,
    //                                                                           double *const res, const double *const x,
    //                                                                           const double *const y, const double *const z,
    //                                                                           const uint32_t length)
    public static final MethodHandle c2me_natives_noise_perlin_double_batch = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(true)
    );
    public static final MethodHandle c2me_natives_noise_perlin_double_batch_ptr = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(true)
    );


    public static final StructLayout double_octave_sampler_data = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("length"),
            ValueLayout.JAVA_DOUBLE.withName("amplitude"),
            ValueLayout.ADDRESS.withName("need_shift"),
            ValueLayout.ADDRESS.withName("lacunarity_powd"),
            ValueLayout.ADDRESS.withName("persistence_powd"),
            ValueLayout.ADDRESS.withName("sampler_permutations"),
            ValueLayout.ADDRESS.withName("sampler_originX"),
            ValueLayout.ADDRESS.withName("sampler_originY"),
            ValueLayout.ADDRESS.withName("sampler_originZ"),
            ValueLayout.ADDRESS.withName("amplitudes")
    ).withByteAlignment(32).withName("double_double_octave_sampler_data");
    public static final VarHandle double_octave_sampler_data$length = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("length"));
    public static final VarHandle double_octave_sampler_data$amplitude = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("amplitude"));
    public static final VarHandle double_octave_sampler_data$need_shift = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("need_shift"));
    public static final VarHandle double_octave_sampler_data$lacunarity_powd = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("lacunarity_powd"));
    public static final VarHandle double_octave_sampler_data$persistence_powd = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("persistence_powd"));
    public static final VarHandle double_octave_sampler_data$sampler_permutations = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("sampler_permutations"));
    public static final VarHandle double_octave_sampler_data$sampler_originX = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("sampler_originX"));
    public static final VarHandle double_octave_sampler_data$sampler_originY = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("sampler_originY"));
    public static final VarHandle double_octave_sampler_data$sampler_originZ = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("sampler_originZ"));
    public static final VarHandle double_octave_sampler_data$amplitudes = double_octave_sampler_data.varHandle(MemoryLayout.PathElement.groupElement("amplitudes"));

    public static MemorySegment double_octave_sampler_data$create(Arena arena, OctavePerlinNoiseSampler firstSampler, OctavePerlinNoiseSampler secondSampler, double amplitude) {
        long nonNullSamplerCount = 0;
        for (PerlinNoiseSampler sampler : ((IOctavePerlinNoiseSampler) firstSampler).getOctaveSamplers()) {
            if (sampler != null) {
                nonNullSamplerCount++;
            }
        }
        for (PerlinNoiseSampler sampler : ((IOctavePerlinNoiseSampler) secondSampler).getOctaveSamplers()) {
            if (sampler != null) {
                nonNullSamplerCount++;
            }
        }
        final MemorySegment data = arena.allocate(double_octave_sampler_data.byteSize(), 64);
        final MemorySegment need_shift = arena.allocate(nonNullSamplerCount, 64);
        final MemorySegment lacunarity_powd = arena.allocate(nonNullSamplerCount * 8, 64);
        final MemorySegment persistence_powd = arena.allocate(nonNullSamplerCount * 8, 64);
        final MemorySegment sampler_permutations = arena.allocate(nonNullSamplerCount * 256 * 4, 64);
        final MemorySegment sampler_originX = arena.allocate(nonNullSamplerCount * 8, 64);
        final MemorySegment sampler_originY = arena.allocate(nonNullSamplerCount * 8, 64);
        final MemorySegment sampler_originZ = arena.allocate(nonNullSamplerCount * 8, 64);
        final MemorySegment amplitudes = arena.allocate(nonNullSamplerCount * 8, 64);
        double_octave_sampler_data$length.set(data, 0L, nonNullSamplerCount);
        double_octave_sampler_data$amplitude.set(data, 0L, amplitude);
        double_octave_sampler_data$need_shift.set(data, 0L, need_shift);
        double_octave_sampler_data$lacunarity_powd.set(data, 0L, lacunarity_powd);
        double_octave_sampler_data$persistence_powd.set(data, 0L, persistence_powd);
        double_octave_sampler_data$sampler_permutations.set(data, 0L, sampler_permutations);
        double_octave_sampler_data$sampler_originX.set(data, 0L, sampler_originX);
        double_octave_sampler_data$sampler_originY.set(data, 0L, sampler_originY);
        double_octave_sampler_data$sampler_originZ.set(data, 0L, sampler_originZ);
        double_octave_sampler_data$amplitudes.set(data, 0L, amplitudes);
        long index = 0;
        {
            PerlinNoiseSampler[] octaveSamplers = ((IOctavePerlinNoiseSampler) firstSampler).getOctaveSamplers();
            for (int i = 0, octaveSamplersLength = octaveSamplers.length; i < octaveSamplersLength; i++) {
                PerlinNoiseSampler sampler = octaveSamplers[i];
                if (sampler != null) {
                    need_shift.set(ValueLayout.JAVA_BOOLEAN, index, false);
                    lacunarity_powd.set(ValueLayout.JAVA_DOUBLE, index * 8, ((IOctavePerlinNoiseSampler) firstSampler).getLacunarity() * Math.pow(2.0, i));
                    persistence_powd.set(ValueLayout.JAVA_DOUBLE, index * 8, ((IOctavePerlinNoiseSampler) firstSampler).getPersistence() * Math.pow(2.0, -i));
                    MemorySegment.copy(MemorySegment.ofArray(MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getPermutation())), 0, sampler_permutations, index * 256L * 4L, 256 * 4);
                    sampler_originX.set(ValueLayout.JAVA_DOUBLE, index * 8, sampler.originX);
                    sampler_originY.set(ValueLayout.JAVA_DOUBLE, index * 8, sampler.originY);
                    sampler_originZ.set(ValueLayout.JAVA_DOUBLE, index * 8, sampler.originZ);
                    amplitudes.set(ValueLayout.JAVA_DOUBLE, index * 8, ((IOctavePerlinNoiseSampler) firstSampler).getAmplitudes().getDouble(i));
                    index++;
                }
            }
        }
        {
            PerlinNoiseSampler[] octaveSamplers = ((IOctavePerlinNoiseSampler) secondSampler).getOctaveSamplers();
            for (int i = 0, octaveSamplersLength = octaveSamplers.length; i < octaveSamplersLength; i++) {
                PerlinNoiseSampler sampler = octaveSamplers[i];
                if (sampler != null) {
                    need_shift.set(ValueLayout.JAVA_BOOLEAN, index, true);
                    lacunarity_powd.set(ValueLayout.JAVA_DOUBLE, index * 8, ((IOctavePerlinNoiseSampler) secondSampler).getLacunarity() * Math.pow(2.0, i));
                    persistence_powd.set(ValueLayout.JAVA_DOUBLE, index * 8, ((IOctavePerlinNoiseSampler) secondSampler).getPersistence() * Math.pow(2.0, -i));
                    MemorySegment.copy(MemorySegment.ofArray(MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getPermutation())), 0, sampler_permutations, index * 256L * 4L, 256 * 4);
                    sampler_originX.set(ValueLayout.JAVA_DOUBLE, index * 8, sampler.originX);
                    sampler_originY.set(ValueLayout.JAVA_DOUBLE, index * 8, sampler.originY);
                    sampler_originZ.set(ValueLayout.JAVA_DOUBLE, index * 8, sampler.originZ);
                    amplitudes.set(ValueLayout.JAVA_DOUBLE, index * 8, ((IOctavePerlinNoiseSampler) secondSampler).getAmplitudes().getDouble(i));
                    index++;
                }
            }
        }

        VarHandle.fullFence();

        return data;
    }

    // c2me_natives_noise_interpolated, double, (const interpolated_noise_sampler_t *const data, const double x, const double y, const double z)
    public static final MethodHandle c2me_natives_noise_interpolated = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE
            ),
            Linker.Option.critical(false)
    );
    public static final MethodHandle c2me_natives_noise_interpolated_ptr = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE
            ),
            Linker.Option.critical(false)
    );

    // typedef const struct interpolated_noise_sub_sampler {
    //     const aligned_uint8_ptr sampler_permutations;
    //     const aligned_double_ptr sampler_originX;
    //     const aligned_double_ptr sampler_originY;
    //     const aligned_double_ptr sampler_originZ;
    //     const aligned_double_ptr sampler_mulFactor;
    //     const uint32_t length;
    // } interpolated_noise_sub_sampler_t;
    public static final StructLayout interpolated_noise_sub_sampler = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("sampler_permutations"),
            ValueLayout.ADDRESS.withName("sampler_originX"),
            ValueLayout.ADDRESS.withName("sampler_originY"),
            ValueLayout.ADDRESS.withName("sampler_originZ"),
            ValueLayout.ADDRESS.withName("sampler_mulFactor"),
            ValueLayout.JAVA_INT.withName("length"),
            MemoryLayout.paddingLayout(4)
    ).withName("interpolated_noise_sub_sampler_t");

    // typedef const struct interpolated_noise_sampler {
    //     const double scaledXzScale;
    //     const double scaledYScale;
    //     const double xzFactor;
    //     const double yFactor;
    //     const double smearScaleMultiplier;
    //     const double xzScale;
    //     const double yScale;
    //
    //     const interpolated_noise_sub_sampler_t lower;
    //     const interpolated_noise_sub_sampler_t upper;
    //     const interpolated_noise_sub_sampler_t normal;
    // } interpolated_noise_sampler_t;
    public static final StructLayout interpolated_noise_sampler = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("scaledXzScale"),
            ValueLayout.JAVA_DOUBLE.withName("scaledYScale"),
            ValueLayout.JAVA_DOUBLE.withName("xzFactor"),
            ValueLayout.JAVA_DOUBLE.withName("yFactor"),
            ValueLayout.JAVA_DOUBLE.withName("smearScaleMultiplier"),
            ValueLayout.JAVA_DOUBLE.withName("xzScale"),
            ValueLayout.JAVA_DOUBLE.withName("yScale"),

            interpolated_noise_sub_sampler.withName("lower"),
            interpolated_noise_sub_sampler.withName("upper"),
            interpolated_noise_sub_sampler.withName("normal")
    ).withByteAlignment(32).withName("interpolated_noise_sampler_t");

    public static final VarHandle interpolated_noise_sampler$scaledXzScale = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("scaledXzScale"));
    public static final VarHandle interpolated_noise_sampler$scaledYScale = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("scaledYScale"));
    public static final VarHandle interpolated_noise_sampler$xzFactor = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("xzFactor"));
    public static final VarHandle interpolated_noise_sampler$yFactor = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("yFactor"));
    public static final VarHandle interpolated_noise_sampler$smearScaleMultiplier = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("smearScaleMultiplier"));
    public static final VarHandle interpolated_noise_sampler$xzScale = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("xzScale"));
    public static final VarHandle interpolated_noise_sampler$yScale = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("yScale"));
    public static final VarHandle interpolated_noise_sampler$lower$sampler_permutations = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("lower"), MemoryLayout.PathElement.groupElement("sampler_permutations"));
    public static final VarHandle interpolated_noise_sampler$lower$sampler_originX = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("lower"), MemoryLayout.PathElement.groupElement("sampler_originX"));
    public static final VarHandle interpolated_noise_sampler$lower$sampler_originY = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("lower"), MemoryLayout.PathElement.groupElement("sampler_originY"));
    public static final VarHandle interpolated_noise_sampler$lower$sampler_originZ = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("lower"), MemoryLayout.PathElement.groupElement("sampler_originZ"));
    public static final VarHandle interpolated_noise_sampler$lower$sampler_mulFactor = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("lower"), MemoryLayout.PathElement.groupElement("sampler_mulFactor"));
    public static final VarHandle interpolated_noise_sampler$lower$length = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("lower"), MemoryLayout.PathElement.groupElement("length"));
    public static final VarHandle interpolated_noise_sampler$upper$sampler_permutations = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("upper"), MemoryLayout.PathElement.groupElement("sampler_permutations"));
    public static final VarHandle interpolated_noise_sampler$upper$sampler_originX = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("upper"), MemoryLayout.PathElement.groupElement("sampler_originX"));
    public static final VarHandle interpolated_noise_sampler$upper$sampler_originY = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("upper"), MemoryLayout.PathElement.groupElement("sampler_originY"));
    public static final VarHandle interpolated_noise_sampler$upper$sampler_originZ = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("upper"), MemoryLayout.PathElement.groupElement("sampler_originZ"));
    public static final VarHandle interpolated_noise_sampler$upper$sampler_mulFactor = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("upper"), MemoryLayout.PathElement.groupElement("sampler_mulFactor"));
    public static final VarHandle interpolated_noise_sampler$upper$length = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("upper"), MemoryLayout.PathElement.groupElement("length"));
    public static final VarHandle interpolated_noise_sampler$normal$sampler_permutations = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("normal"), MemoryLayout.PathElement.groupElement("sampler_permutations"));
    public static final VarHandle interpolated_noise_sampler$normal$sampler_originX = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("normal"), MemoryLayout.PathElement.groupElement("sampler_originX"));
    public static final VarHandle interpolated_noise_sampler$normal$sampler_originY = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("normal"), MemoryLayout.PathElement.groupElement("sampler_originY"));
    public static final VarHandle interpolated_noise_sampler$normal$sampler_originZ = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("normal"), MemoryLayout.PathElement.groupElement("sampler_originZ"));
    public static final VarHandle interpolated_noise_sampler$normal$sampler_mulFactor = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("normal"), MemoryLayout.PathElement.groupElement("sampler_mulFactor"));
    public static final VarHandle interpolated_noise_sampler$normal$length = interpolated_noise_sampler.varHandle(MemoryLayout.PathElement.groupElement("normal"), MemoryLayout.PathElement.groupElement("length"));

    public static boolean interpolated_noise_sampler$isSpecializedBase3dNoiseFunction(InterpolatedNoiseSampler interpolated) {
        return IntStream.range(0, 16).mapToObj(((IInterpolatedNoiseSampler) interpolated).getLowerInterpolatedNoise()::getOctave).filter(Objects::nonNull).count() == 16 &&
                IntStream.range(0, 16).mapToObj(((IInterpolatedNoiseSampler) interpolated).getUpperInterpolatedNoise()::getOctave).filter(Objects::nonNull).count() == 16 &&
                IntStream.range(0, 8).mapToObj(((IInterpolatedNoiseSampler) interpolated).getInterpolationNoise()::getOctave).filter(Objects::nonNull).count() == 8;
    }

    public static MemorySegment interpolated_noise_sampler$create(Arena arena, InterpolatedNoiseSampler interpolated) {
        final MemorySegment data = arena.allocate(interpolated_noise_sampler.byteSize(), 64);
        interpolated_noise_sampler$scaledXzScale.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getScaledXzScale());
        interpolated_noise_sampler$scaledYScale.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getScaledYScale());
        interpolated_noise_sampler$xzFactor.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getXzFactor());
        interpolated_noise_sampler$yFactor.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getYFactor());
        interpolated_noise_sampler$smearScaleMultiplier.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getSmearScaleMultiplier());
        interpolated_noise_sampler$xzScale.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getXzScale());
        interpolated_noise_sampler$yScale.set(data, 0L, ((IInterpolatedNoiseSampler) interpolated).getYScale());

//        if (true) {
//            System.out.println(String.format("Interpolated total: %d", countNonNull));
//            System.out.println(String.format("lower: %d", IntStream.range(0, 16).mapToObj(((IInterpolatedNoiseSampler) interpolated).getLowerInterpolatedNoise()::getOctave).filter(Objects::nonNull).count()));
//            System.out.println(String.format("upper: %d", IntStream.range(0, 16).mapToObj(((IInterpolatedNoiseSampler) interpolated).getUpperInterpolatedNoise()::getOctave).filter(Objects::nonNull).count()));
//            System.out.println(String.format("normal: %d", IntStream.range(0, 8).mapToObj(((IInterpolatedNoiseSampler) interpolated).getInterpolationNoise()::getOctave).filter(Objects::nonNull).count()));
//        }

        final MemorySegment sampler_permutations = arena.allocate(40 * 256L * 4L, 64);
        final MemorySegment sampler_originX = arena.allocate(40 * 8L, 64);
        final MemorySegment sampler_originY = arena.allocate(40 * 8L, 64);
        final MemorySegment sampler_originZ = arena.allocate(40 * 8L, 64);
        final MemorySegment sampler_mulFactor = arena.allocate(40 * 8L, 64);

        int index = 0;

        {
            int startIndex = index;

            for (int i = 0; i < 8; i++) {
                PerlinNoiseSampler sampler = ((IInterpolatedNoiseSampler) interpolated).getInterpolationNoise().getOctave(i);
                if (sampler != null) {
                    MemorySegment.copy(MemorySegment.ofArray(MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getPermutation())), 0, sampler_permutations, index * 256L * 4L, 256 * 4);
                    sampler_originX.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originX);
                    sampler_originY.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originY);
                    sampler_originZ.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originZ);
                    sampler_mulFactor.set(ValueLayout.JAVA_DOUBLE, index * 8L, Math.pow(2, -i));
                    index ++;
                }
            }

            BindingsTemplate.interpolated_noise_sampler$normal$sampler_permutations.set(data, 0L, sampler_permutations.asSlice(startIndex * 256L * 4L));
            BindingsTemplate.interpolated_noise_sampler$normal$sampler_originX.set(data, 0L, sampler_originX.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$normal$sampler_originY.set(data, 0L, sampler_originY.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$normal$sampler_originZ.set(data, 0L, sampler_originZ.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$normal$sampler_mulFactor.set(data, 0L, sampler_mulFactor.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$normal$length.set(data, 0L, index - startIndex);
        }

        {
            int startIndex = index = 8;

            for (int i = 0; i < 16; i++) {
                PerlinNoiseSampler sampler = ((IInterpolatedNoiseSampler) interpolated).getLowerInterpolatedNoise().getOctave(i);
                if (sampler != null) {
                    MemorySegment.copy(MemorySegment.ofArray(MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getPermutation())), 0, sampler_permutations, index * 256L * 4L, 256 * 4);
                    sampler_originX.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originX);
                    sampler_originY.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originY);
                    sampler_originZ.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originZ);
                    sampler_mulFactor.set(ValueLayout.JAVA_DOUBLE, index * 8L, Math.pow(2, -i));
                    index ++;
                }
            }

            BindingsTemplate.interpolated_noise_sampler$lower$sampler_permutations.set(data, 0L, sampler_permutations.asSlice(startIndex * 256L * 4L));
            BindingsTemplate.interpolated_noise_sampler$lower$sampler_originX.set(data, 0L, sampler_originX.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$lower$sampler_originY.set(data, 0L, sampler_originY.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$lower$sampler_originZ.set(data, 0L, sampler_originZ.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$lower$sampler_mulFactor.set(data, 0L, sampler_mulFactor.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$lower$length.set(data, 0L, index - startIndex);
        }

        {
            int startIndex = index = 8 + 16;

            for (int i = 0; i < 16; i++) {
                PerlinNoiseSampler sampler = ((IInterpolatedNoiseSampler) interpolated).getUpperInterpolatedNoise().getOctave(i);
                if (sampler != null) {
                    MemorySegment.copy(MemorySegment.ofArray(MemoryUtil.byte2int(((IPerlinNoiseSampler) (Object) sampler).getPermutation())), 0, sampler_permutations, index * 256L * 4L, 256 * 4);
                    sampler_originX.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originX);
                    sampler_originY.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originY);
                    sampler_originZ.set(ValueLayout.JAVA_DOUBLE, index * 8L, sampler.originZ);
                    sampler_mulFactor.set(ValueLayout.JAVA_DOUBLE, index * 8L, Math.pow(2, -i));
                    index ++;
                }
            }

            BindingsTemplate.interpolated_noise_sampler$upper$sampler_permutations.set(data, 0L, sampler_permutations.asSlice(startIndex * 256L * 4L));
            BindingsTemplate.interpolated_noise_sampler$upper$sampler_originX.set(data, 0L, sampler_originX.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$upper$sampler_originY.set(data, 0L, sampler_originY.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$upper$sampler_originZ.set(data, 0L, sampler_originZ.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$upper$sampler_mulFactor.set(data, 0L, sampler_mulFactor.asSlice(startIndex * 8L));
            BindingsTemplate.interpolated_noise_sampler$upper$length.set(data, 0L, index - startIndex);
        }

        VarHandle.fullFence();

        return data;
    }

    // c2me_natives_end_islands_sample, float, (const int32_t *const simplex_permutations, const int32_t x, const int32_t z)
    public static final MethodHandle c2me_natives_end_islands_sample = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_FLOAT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(true)
    );
    public static final MethodHandle c2me_natives_end_islands_sample_ptr = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_FLOAT,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(false)
    );

    // c2me_natives_biome_access_sample, uint32_t, (const int64_t theSeed, const int32_t x, const int32_t y, const int32_t z)
    public static final MethodHandle c2me_natives_biome_access_sample = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(false)
    );

    // (const uint16_t *restrict const packedBlockPositions, uint32_t *restrict const res,
    //  const aquifer_data_t *restrict const aquiferData,
    //  const int32_t x, const int32_t y, const int32_t z)
    public static final MethodHandle c2me_natives_aquifer_refreshDistPosIdx = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(true)
    );

    public static final MethodHandle c2me_natives_aquifer_refreshDistPosIdx_ptr = NativeLoader.linker.downcallHandle(
            FunctionDescriptor.ofVoid(
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT
            ),
            Linker.Option.critical(true)
    );

}
