#pragma once

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>
#include <float.h>

__attribute__((aligned(64))) static const double FLAT_SIMPLEX_GRAD[] = {
        1, 1, 0, 0,
        -1, 1, 0, 0,
        1, -1, 0, 0,
        -1, -1, 0, 0,
        1, 0, 1, 0,
        -1, 0, 1, 0,
        1, 0, -1, 0,
        -1, 0, -1, 0,
        0, 1, 1, 0,
        0, -1, 1, 0,
        0, 1, -1, 0,
        0, -1, -1, 0,
        1, 1, 0, 0,
        0, -1, 1, 0,
        -1, 1, 0, 0,
        0, -1, -1, 0,
};

static const double SQRT_3 = 1.7320508075688772;
// 0.5 * (SQRT_3 - 1.0)
static const double SKEW_FACTOR_2D = 0.3660254037844386;
// (3.0 - SQRT_3) / 6.0
static const double UNSKEW_FACTOR_2D = 0.21132486540518713;

typedef const double *aligned_double_ptr __attribute__((align_value(64)));
typedef const uint8_t *aligned_uint8_ptr __attribute__((align_value(64)));
typedef const uint32_t *aligned_uint32_ptr __attribute__((align_value(64)));

#define max(a, b) \
   ({ __typeof__ (a) _a = (a); \
       __typeof__ (b) _b = (b); \
     _a >= _b ? _a : _b; })
#define min(a, b) \
   ({ __typeof__ (a) _a = (a); \
       __typeof__ (b) _b = (b); \
     _a <= _b ? _a : _b; })

#pragma clang attribute push (__attribute__((always_inline)), apply_to = function)

static inline __attribute__((const)) float fminf(const float x, const float y) {
    return __builtin_fminf(x, y);
}

static inline __attribute__((const)) float fmaxf(const float x, const float y) {
    return __builtin_fmaxf(x, y);
}

static inline __attribute__((const)) float fabsf(const float x) {
    union {
        float f;
        uint32_t i;
    } u = {x};
    u.i &= 0x7fffffff;
    return u.f;
}

static inline __attribute__((const)) int64_t labs(const int64_t x) {
    return __builtin_labs(x);
}

static inline __attribute__((const)) double floor(double x) {
    return __builtin_floor(x);
}

static inline __attribute__((const)) float sqrtf(float x) {
    return __builtin_sqrtf(x);
}

static inline __attribute__((const)) float fmodf(float x, float y) {
    return __builtin_fmodf(x, y);
}

static inline __attribute__((const)) int32_t math_floorDiv(const int32_t x, const int32_t y) {
    int r = x / y;
    // if the signs are different and modulo not zero, round down
    if ((x ^ y) < 0 && (r * y != x)) {
        r--;
    }
    return r;
}

static inline __attribute__((const)) float clampf(const float value, const float min, const float max) {
    return fminf(fmaxf(value, min), max);
}

static inline __attribute__((const)) double math_octave_maintainPrecision(const double value) {
    return value - floor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
}

static inline __attribute__((const)) double math_simplex_grad(const int32_t hash, const double x, const double y,
                                                              const double z, const double distance) {
    double d = distance - x * x - y * y - z * z;
    if (d < 0.0) {
        return 0.0;
    } else {
        int32_t i = hash << 2;
        double var0 = FLAT_SIMPLEX_GRAD[i | 0] * x;
        double var1 = FLAT_SIMPLEX_GRAD[i | 1] * y;
        double var2 = FLAT_SIMPLEX_GRAD[i | 2] * z;
        return d * d * d * d * (var0 + var1 + var2);
    }
}

static inline __attribute__((const)) double math_lerp(const double delta, const double start, const double end) {
    return start + delta * (end - start);
}

static inline __attribute__((const)) float math_lerpf(const float delta, const float start, const float end) {
    return start + delta * (end - start);
}

static inline __attribute__((const)) double math_clampedLerp(const double start, const double end, const double delta) {
    if (delta < 0.0) {
        return start;
    } else {
        return delta > 1.0 ? end : math_lerp(delta, start, end);
    }
}

static inline __attribute__((const)) double math_square(const double operand) {
    return operand * operand;
}

static inline __attribute__((const)) double math_lerp2(const double deltaX, const double deltaY, const double x0y0,
                                                       const double x1y0, const double x0y1, const double x1y1) {
    return math_lerp(deltaY, math_lerp(deltaX, x0y0, x1y0), math_lerp(deltaX, x0y1, x1y1));
}

static inline __attribute__((const)) double math_lerp3(
        const double deltaX,
        const double deltaY,
        const double deltaZ,
        const double x0y0z0,
        const double x1y0z0,
        const double x0y1z0,
        const double x1y1z0,
        const double x0y0z1,
        const double x1y0z1,
        const double x0y1z1,
        const double x1y1z1
) {
    return math_lerp(deltaZ, math_lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0),
                     math_lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1));
}

static inline __attribute__((const)) double math_getLerpProgress(const double value, const double start,
                                                                 const double end) {
    return (value - start) / (end - start);
}

static inline __attribute__((const)) double
math_clampedLerpFromProgress(const double lerpValue, const double lerpStart, const double lerpEnd, const double start,
                             const double end) {
    return math_clampedLerp(start, end, math_getLerpProgress(lerpValue, lerpStart, lerpEnd));
}

static inline __attribute__((const)) int32_t math_floorMod(const int32_t x, const int32_t y) {
    int32_t mod = x % y;
    // if the signs are different and modulo not zero, adjust result
    if ((mod ^ y) < 0 && mod != 0) {
        mod += y;
    }
    return mod;
}

static inline __attribute__((const)) int32_t math_biome2block(const int32_t biomeCoord) {
    return biomeCoord << 2;
}

static inline __attribute__((const)) int32_t math_block2biome(const int32_t blockCoord) {
    return blockCoord >> 2;
}

static inline __attribute__((const)) uint32_t
__math_simplex_map(const aligned_uint32_ptr permutations, const int32_t input) {
    return permutations[input & 0xFF];
}

static inline __attribute__((const)) double math_simplex_dot(const int32_t hash, const double x, const double y,
                                                             const double z) {
    const int32_t loc = hash << 2;
    return FLAT_SIMPLEX_GRAD[loc + 0] * x + FLAT_SIMPLEX_GRAD[loc + 1] * y + FLAT_SIMPLEX_GRAD[loc + 2] * z;
}

static inline __attribute__((const)) double __math_simplex_grad(const int32_t hash, const double x, const double y,
                                                                const double z, const double distance) {
    double d = distance - x * x - y * y - z * z;
    double e;
    if (d < 0.0) {
        e = 0.0;
    } else {
        d *= d;
        e = d * d * math_simplex_dot(hash, x, y, z);
    }
    return e;
    // double tmp = d * d; // speculative execution

    // return d < 0.0 ? 0.0 : tmp * tmp * math_simplex_dot(hash, x, y, z);
}

static inline double __attribute__((const))
math_noise_simplex_sample2d(const aligned_uint32_ptr permutations, const double x, const double y) {
    const double d = (x + y) * SKEW_FACTOR_2D;
    const double i = floor(x + d);
    const double j = floor(y + d);
    const double e = (i + j) * UNSKEW_FACTOR_2D;
    const double f = i - e;
    const double g = j - e;
    const double h = x - f;
    const double k = y - g;
    double l;
    int32_t li;
    double m;
    int32_t mi;
    if (h > k) {
        l = 1;
        li = 1;
        m = 0;
        mi = 0;
    } else {
        l = 0;
        li = 1;
        m = 1;
        mi = 1;
    }

    const double n = h - (double) l + UNSKEW_FACTOR_2D;
    const double o = k - (double) m + UNSKEW_FACTOR_2D;
    const double p = h - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
    const double q = k - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
    const int32_t r = (int32_t) i & 0xFF;
    const int32_t s = (int32_t) j & 0xFF;
    const int32_t t = __math_simplex_map(permutations, r + __math_simplex_map(permutations, s)) % 12;
    const int32_t u = __math_simplex_map(permutations, r + li + __math_simplex_map(permutations, s + mi)) % 12;
    const int32_t v = __math_simplex_map(permutations, r + 1 + __math_simplex_map(permutations, s + 1)) % 12;
    const double w = __math_simplex_grad(t, h, k, 0.0, 0.5);
    const double z = __math_simplex_grad(u, n, o, 0.0, 0.5);
    const double aa = __math_simplex_grad(v, p, q, 0.0, 0.5);
    return 70.0 * (w + z + aa);
}

static inline __attribute__((const)) double math_perlinFade(const double value) {
    return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
}

static inline __attribute__((const)) double __math_perlin_grad(const aligned_uint32_ptr permutations, const int32_t px,
                                                               const int32_t py, const int32_t pz, const double fx,
                                                               const double fy, const double fz) {
    const double f[3] = {fx, fy, fz};
    const int32_t p[3] = {px, py, pz};
    const uint32_t q[3] = {p[0] & 0xFF, p[1] & 0xFF, p[2] & 0xFF};
    const uint32_t hash = permutations[(permutations[(permutations[q[0]] + q[1]) & 0xFF] + q[2]) & 0xFF] & 0xF;
    const double *const grad = FLAT_SIMPLEX_GRAD + (hash << 2);
    return grad[0] * f[0] + grad[1] * f[1] + grad[2] * f[2];
}

static inline __attribute__((const)) double
math_noise_perlin_sampleScalar(const aligned_uint32_ptr permutations,
                               const int32_t px0, const int32_t py0, const int32_t pz0,
                               const double fx0, const double fy0, const double fz0, const double fadeLocalY) {
    const int32_t px1 = px0 + 1;
    const int32_t py1 = py0 + 1;
    const int32_t pz1 = pz0 + 1;
    const double fx1 = fx0 - 1;
    const double fy1 = fy0 - 1;
    const double fz1 = fz0 - 1;

    const double f000 = __math_perlin_grad(permutations, px0, py0, pz0, fx0, fy0, fz0);
    const double f100 = __math_perlin_grad(permutations, px1, py0, pz0, fx1, fy0, fz0);
    const double f010 = __math_perlin_grad(permutations, px0, py1, pz0, fx0, fy1, fz0);
    const double f110 = __math_perlin_grad(permutations, px1, py1, pz0, fx1, fy1, fz0);
    const double f001 = __math_perlin_grad(permutations, px0, py0, pz1, fx0, fy0, fz1);
    const double f101 = __math_perlin_grad(permutations, px1, py0, pz1, fx1, fy0, fz1);
    const double f011 = __math_perlin_grad(permutations, px0, py1, pz1, fx0, fy1, fz1);
    const double f111 = __math_perlin_grad(permutations, px1, py1, pz1, fx1, fy1, fz1);

    const double dx = math_perlinFade(fx0);
    const double dy = math_perlinFade(fadeLocalY);
    const double dz = math_perlinFade(fz0);
    return math_lerp3(dx, dy, dz, f000, f100, f010, f110, f001, f101, f011, f111);
}


static inline __attribute__((const)) double
math_noise_perlin_sample(const aligned_uint32_ptr permutations,
                         const double originX, const double originY, const double originZ,
                         const double x, const double y, const double z,
                         const double yScale, const double yMax) {
    const double d = x + originX;
    const double e = y + originY;
    const double f = z + originZ;
    const double i = floor(d);
    const double j = floor(e);
    const double k = floor(f);
    const double g = d - i;
    const double h = e - j;
    const double l = f - k;
    const double o = yScale != 0 ? floor(((yMax >= 0.0 && yMax < h) ? yMax : h) / yScale + 1.0E-7) * yScale : 0;

    return math_noise_perlin_sampleScalar(permutations, (int32_t) i, (int32_t) j, (int32_t) k, g, h - o, l, h);
}


typedef const struct double_octave_sampler_data {
    const uint64_t length;
    const double amplitude;
    const bool *const need_shift;
    const aligned_double_ptr lacunarity_powd;
    const aligned_double_ptr persistence_powd;
    const aligned_uint32_ptr sampler_permutations;
    const aligned_double_ptr sampler_originX;
    const aligned_double_ptr sampler_originY;
    const aligned_double_ptr sampler_originZ;
    const aligned_double_ptr amplitudes;
} double_octave_sampler_data_t;

static inline __attribute__((const)) double
math_noise_perlin_double_octave_sample_impl(const double_octave_sampler_data_t *const data,
                                            const double x, const double y, const double z,
                                            const double yScale, const double yMax, const uint8_t useOrigin) {
    double ds[data->length];

#pragma clang loop vectorize(enable) interleave(enable) interleave_count(2)
    for (uint32_t i = 0; i < data->length; i++) {
        const double e = data->lacunarity_powd[i];
        const double f = data->persistence_powd[i];
        const aligned_uint32_ptr permutations = data->sampler_permutations + 256 * i;
        const double sampleX = data->need_shift[i] ? x * 1.0181268882175227 : x;
        const double sampleY = data->need_shift[i] ? y * 1.0181268882175227 : y;
        const double sampleZ = data->need_shift[i] ? z * 1.0181268882175227 : z;
        const double g = math_noise_perlin_sample(
                permutations,
                data->sampler_originX[i],
                data->sampler_originY[i],
                data->sampler_originZ[i],
                math_octave_maintainPrecision(sampleX * e),
                useOrigin ? -(data->sampler_originY[i]) : math_octave_maintainPrecision(sampleY * e),
                math_octave_maintainPrecision(sampleZ * e),
                yScale * e,
                yMax * e);
        ds[i] = data->amplitudes[i] * g * f;
    }

    double d1 = 0.0;
    double d2 = 0.0;
    for (uint32_t i = 0; i < data->length; i++) {
        if (!data->need_shift[i]) {
            d1 += ds[i];
        } else {
            d2 += ds[i];
        }
    }

    return (d1 + d2) * data->amplitude;
}

//static inline void
//math_noise_perlin_double_octave_sample_impl_batch(const double_octave_sampler_data_t *const data, double *const res,
//                                                  const double *const x, const double *const y, const double *const z,
//                                                  const uint32_t length) {
//    double ds[data->length][length];
//
//    for (uint32_t si = 0; si < data->length; si ++) {
//#pragma clang loop vectorize(enable) interleave(enable) interleave_count(2)
//        for (uint32_t bi = 0; bi < length; bi++) {
//            const double e = data->lacunarity_powd[si];
//            const double f = data->persistence_powd[si];
//            const aligned_uint32_ptr permutations = data->sampler_permutations + 256 * si;
//            const double sampleX = data->need_shift[si] ? x[bi] * 1.0181268882175227 : x[bi];
//            const double sampleY = data->need_shift[si] ? y[bi] * 1.0181268882175227 : y[bi];
//            const double sampleZ = data->need_shift[si] ? z[bi] * 1.0181268882175227 : z[bi];
//            const double g = math_noise_perlin_sample(
//                    permutations,
//                    data->sampler_originX[si],
//                    data->sampler_originY[si],
//                    data->sampler_originZ[si],
//                    math_octave_maintainPrecision(sampleX * e),
//                    math_octave_maintainPrecision(sampleY * e),
//                    math_octave_maintainPrecision(sampleZ * e),
//                    0.0,
//                    0.0);
//            ds[si][bi] = data->amplitudes[si] * g * f;
//        }
//    }
//
//    double d1[length];
//    double d2[length];
//    for (uint32_t i = 0; i < length; i ++) {
//        d1[i] = 0.0;
//        d2[i] = 0.0;
//    }
//    for (uint32_t bi = 0; bi < length; bi++) {
//        for (uint32_t si = 0; si < data->length; si ++) {
//            if (!data->need_shift[si]) {
//                d1[bi] += ds[si][bi];
//            } else {
//                d2[bi] += ds[si][bi];
//            }
//        }
//    }
//    for (uint32_t bi = 0; bi < length; bi++) {
//        res[bi] = (d1[bi] + d2[bi]) * data->amplitude;
//    }
//}

//static inline void
//math_noise_perlin_double_octave_sample_impl_batch(const double_octave_sampler_data_t *restrict const data,
//                                                  double *restrict const res, const double *restrict const x,
//                                                  const double *restrict const y, const double *restrict const z,
//                                                  const uint32_t length) {
//    const uint32_t total_len = data->length * length;
//
//    double ds[total_len];
//    uint32_t sia[total_len]; // sampler index array
//    uint32_t bia[total_len]; // batch index array
//    double xa[total_len]; // x array
//    double ya[total_len]; // y array
//    double za[total_len]; // z array
//
//    double lacunarity_powd[total_len];
//    double persistence_powd[total_len];
//    bool need_shift[total_len];
//    double sampler_originX[total_len];
//    double sampler_originY[total_len];
//    double sampler_originZ[total_len];
//    double amplitudes[total_len];
//
//    {
//        uint32_t idx = 0;
//        for (uint32_t si = 0; si < data->length; si++) {
//            for (uint32_t bi = 0; bi < length; bi++) {
//                sia[idx] = si;
//                bia[idx] = bi;
//                xa[idx] = x[bi];
//                ya[idx] = y[bi];
//                za[idx] = z[bi];
//                lacunarity_powd[idx] = data->lacunarity_powd[si];
//                persistence_powd[idx] = data->persistence_powd[si];
//                need_shift[idx] = data->need_shift[si];
//                sampler_originX[idx] = data->sampler_originX[si];
//                sampler_originY[idx] = data->sampler_originY[si];
//                sampler_originZ[idx] = data->sampler_originZ[si];
//                amplitudes[idx] = data->amplitudes[si];
//                idx++;
//            }
//        }
//    }
//
//#pragma clang loop vectorize(enable) interleave(enable) interleave_count(2)
//    for (uint32_t idx = 0; idx < total_len; idx++) {
//        const uint32_t si = sia[idx];
//        const double xi = xa[idx];
//        const double yi = ya[idx];
//        const double zi = za[idx];
//        const double e = lacunarity_powd[idx];
//        const double f = persistence_powd[idx];
//        const aligned_uint32_ptr permutations = data->sampler_permutations + 256 * si;
//        const double sampleX = need_shift[idx] ? xi * 1.0181268882175227 : xi;
//        const double sampleY = need_shift[idx] ? yi * 1.0181268882175227 : yi;
//        const double sampleZ = need_shift[idx] ? zi * 1.0181268882175227 : zi;
//        const double g = math_noise_perlin_sample(
//                permutations,
//                sampler_originX[idx],
//                sampler_originY[idx],
//                sampler_originZ[idx],
//                math_octave_maintainPrecision(sampleX * e),
//                math_octave_maintainPrecision(sampleY * e),
//                math_octave_maintainPrecision(sampleZ * e),
//                0.0,
//                0.0);
//        ds[idx] = amplitudes[idx] * g * f;
//    }
//
//    double d1[length];
//    double d2[length];
//    for (uint32_t i = 0; i < length; i++) {
//        d1[i] = 0.0;
//        d2[i] = 0.0;
//    }
//    for (uint32_t idx = 0; idx < total_len; idx++) {
//        const uint32_t si = sia[idx];
//        const uint32_t bi = bia[idx];
//        if (!data->need_shift[si]) {
//            d1[bi] += ds[idx];
//        } else {
//            d2[bi] += ds[idx];
//        }
//    }
//    for (uint32_t bi = 0; bi < length; bi++) {
//        res[bi] = (d1[bi] + d2[bi]) * data->amplitude;
//    }
//}

static inline __attribute__((const)) double
math_noise_perlin_double_octave_sample(const double_octave_sampler_data_t *const data,
                                       const double x, const double y, const double z) {
    return math_noise_perlin_double_octave_sample_impl(data, x, y, z, 0.0, 0.0, 0);
}

static inline void
math_noise_perlin_double_octave_sample_batch(const double_octave_sampler_data_t *const data, double *const res,
                                             const double *const x, const double *const y, const double *const z,
                                             const uint32_t length) {
//    math_noise_perlin_double_octave_sample_impl_batch(data, res, x, y, z, length);
    for (uint32_t i = 0; i < length; i ++) {
        res[i] = math_noise_perlin_double_octave_sample_impl(data, x[i], y[i], z[i], 0.0, 0.0, 0);
    }
}

typedef const struct interpolated_noise_sub_sampler {
    const aligned_uint32_ptr sampler_permutations;
    const aligned_double_ptr sampler_originX;
    const aligned_double_ptr sampler_originY;
    const aligned_double_ptr sampler_originZ;
    const aligned_double_ptr sampler_mulFactor;
    const uint32_t length;
} interpolated_noise_sub_sampler_t;

typedef const struct interpolated_noise_sampler {
    const double scaledXzScale;
    const double scaledYScale;
    const double xzFactor;
    const double yFactor;
    const double smearScaleMultiplier;
    const double xzScale;
    const double yScale;

    const interpolated_noise_sub_sampler_t lower;
    const interpolated_noise_sub_sampler_t upper;
    const interpolated_noise_sub_sampler_t normal;
} interpolated_noise_sampler_t;


static inline __attribute__((const)) double
math_noise_perlin_interpolated_sample(const interpolated_noise_sampler_t *const data,
                                      const double x, const double y, const double z) {
    const double d = x * data->scaledXzScale;
    const double e = y * data->scaledYScale;
    const double f = z * data->scaledXzScale;
    const double g = d / data->xzFactor;
    const double h = e / data->yFactor;
    const double i = f / data->xzFactor;
    const double j = data->scaledYScale * data->smearScaleMultiplier;
    const double k = j / data->yFactor;
    double l = 0.0;
    double m = 0.0;
    double n = 0.0;

    double ns[data->normal.length];
#pragma clang loop vectorize(enable)
    for (uint32_t offset = 0; offset < data->normal.length; offset++) {
        ns[offset] = math_noise_perlin_sample(
                data->normal.sampler_permutations + 256 * offset,
                data->normal.sampler_originX[offset],
                data->normal.sampler_originY[offset],
                data->normal.sampler_originZ[offset],
                math_octave_maintainPrecision(g * data->normal.sampler_mulFactor[offset]),
                math_octave_maintainPrecision(h * data->normal.sampler_mulFactor[offset]),
                math_octave_maintainPrecision(i * data->normal.sampler_mulFactor[offset]),
                k * data->normal.sampler_mulFactor[offset],
                h * data->normal.sampler_mulFactor[offset]
        ) / data->normal.sampler_mulFactor[offset];
    }

    for (uint32_t offset = 0; offset < data->normal.length; offset++) {
        n += ns[offset];
    }

    const double q = (n / 10.0 + 1.0) / 2.0;
    const uint8_t bl2 = q >= 1.0;
    const uint8_t bl3 = q <= 0.0;

    if (!bl2) {
        double ls[data->lower.length];
#pragma clang loop vectorize(enable) interleave_count(2)
        for (uint32_t offset = 0; offset < data->lower.length; offset++) {
            ls[offset] = math_noise_perlin_sample(
                    data->lower.sampler_permutations + 256 * offset,
                    data->lower.sampler_originX[offset],
                    data->lower.sampler_originY[offset],
                    data->lower.sampler_originZ[offset],
                    math_octave_maintainPrecision(d * data->lower.sampler_mulFactor[offset]),
                    math_octave_maintainPrecision(e * data->lower.sampler_mulFactor[offset]),
                    math_octave_maintainPrecision(f * data->lower.sampler_mulFactor[offset]),
                    j * data->lower.sampler_mulFactor[offset],
                    e * data->lower.sampler_mulFactor[offset]
            ) / data->lower.sampler_mulFactor[offset];
        }

        for (uint32_t offset = 0; offset < data->lower.length; offset++) {
            l += ls[offset];
        }
    }

    if (!bl3) {
        double ms[data->upper.length];
#pragma clang loop vectorize(enable) interleave_count(2)
        for (uint32_t offset = 0; offset < data->upper.length; offset++) {
            ms[offset] = math_noise_perlin_sample(
                    data->upper.sampler_permutations + 256 * offset,
                    data->upper.sampler_originX[offset],
                    data->upper.sampler_originY[offset],
                    data->upper.sampler_originZ[offset],
                    math_octave_maintainPrecision(d * data->upper.sampler_mulFactor[offset]),
                    math_octave_maintainPrecision(e * data->upper.sampler_mulFactor[offset]),
                    math_octave_maintainPrecision(f * data->upper.sampler_mulFactor[offset]),
                    j * data->upper.sampler_mulFactor[offset],
                    e * data->upper.sampler_mulFactor[offset]
            ) / data->upper.sampler_mulFactor[offset];
        }
        for (uint32_t offset = 0; offset < data->upper.length; offset++) {
            m += ms[offset];
        }
    }

    return math_clampedLerp(l / 512.0, m / 512.0, q) / 128.0;
}

static inline __attribute__((const)) float
math_end_islands_sample(const aligned_uint32_ptr simplex_permutations, const int32_t x, const int32_t z) {
    const int32_t i = x / 2;
    const int32_t j = z / 2;
    const int32_t k = x % 2;
    const int32_t l = z % 2;
    const int32_t muld = x * x + z * z; // int32_t intentionally
    if (muld < 0) {
        return __builtin_nanf("");
    }
    float f = 100.0F - sqrtf((float) muld) * 8.0F;
    f = clampf(f, -100.0F, 80.0F);

    int8_t ms[25 * 25], ns[25 * 25], hit[25 * 25];
    const int64_t omin = labs(i) - 12LL;
    const int64_t pmin = labs(j) - 12LL;
    const int64_t omax = labs(i) + 12LL;
    const int64_t pmax = labs(j) + 12LL;

    {
        uint32_t idx = 0;
#pragma clang loop vectorize(enable)
        for (int8_t m = -12; m < 13; m++) {
            for (int8_t n = -12; n < 13; n++) {
                ms[idx] = m;
                ns[idx] = n;
                idx++;
            }
        }
        if (idx != 25 * 25) {
            __builtin_trap();
        }
    }

    if (omin * omin + pmin * pmin > 4096LL) {
        for (uint32_t idx = 0; idx < 25 * 25; idx++) {
            const int64_t o = (int64_t) i + (int64_t) ms[idx];
            const int64_t p = (int64_t) j + (int64_t) ns[idx];
            hit[idx] = math_noise_simplex_sample2d(simplex_permutations, (double) o, (double) p) < -0.9F;
        }
    } else {
        for (uint32_t idx = 0; idx < 25 * 25; idx++) {
            const int64_t o = (int64_t) i + (int64_t) ms[idx];
            const int64_t p = (int64_t) j + (int64_t) ns[idx];
            hit[idx] = (o * o + p * p > 4096LL) && math_noise_simplex_sample2d(
                    simplex_permutations, (double) o, (double) p) < -0.9F;
        }
    }

#pragma clang loop vectorize(enable) interleave(enable)
    for (uint32_t idx = 0; idx < 25 * 25; idx++) {
        if (hit[idx]) {
            const int32_t m = ms[idx];
            const int32_t n = ns[idx];
            const int64_t o = (int64_t) i + (int64_t) m;
            const int64_t p = (int64_t) j + (int64_t) n;
            const float g1 = fabsf((float) o) * 3439.0F;
            const float g2 = fabsf((float) p) * 147.0F;
            const float g = fmodf((g1 + g2), 13.0F) + 9.0F;
            const float h = (float) (k - m * 2);
            const float q = (float) (l - n * 2);
            float r = 100.0F - sqrtf(h * h + q * q) * g;
            r = clampf(r, -100.0F, 80.0F);
            f = fmaxf(f, r);
        }
    }

    return f;
}

static inline __attribute__((const)) uint32_t
math_biome_access_sample(const int64_t theSeed, const int32_t x, const int32_t y, const int32_t z) {
    const int32_t var0 = x - 2;
    const int32_t var1 = y - 2;
    const int32_t var2 = z - 2;
    const int32_t var3 = var0 >> 2;
    const int32_t var4 = var1 >> 2;
    const int32_t var5 = var2 >> 2;
    const double var6 = (double) (var0 & 3) / 4.0;
    const double var7 = (double) (var1 & 3) / 4.0;
    const double var8 = (double) (var2 & 3) / 4.0;
    uint32_t var9 = 0;
    double var10 = DBL_MAX;

    double var28s[8];

#pragma clang loop interleave_count(2)
    for (uint32_t var11 = 0; var11 < 8; ++var11) {
        uint32_t var12 = var11 & 4;
        uint32_t var13 = var11 & 2;
        uint32_t var14 = var11 & 1;
        int64_t var15 = var12 ? var3 + 1 : var3;
        int64_t var16 = var13 ? var4 + 1 : var4;
        int64_t var17 = var14 ? var5 + 1 : var5;
        double var18 = var12 ? var6 - 1.0 : var6;
        double var19 = var13 ? var7 - 1.0 : var7;
        double var20 = var14 ? var8 - 1.0 : var8;
        int64_t var21 = theSeed * (theSeed * 6364136223846793005L + 1442695040888963407L) + var15;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var15;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var16;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + var17;
        double var22 = (double) ((var21 >> 24) & 1023) / 1024.0;
        double var23 = (var22 - 0.5) * 0.9;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + theSeed;
        double var24 = (double) ((var21 >> 24) & 1023) / 1024.0;
        double var25 = (var24 - 0.5) * 0.9;
        var21 = var21 * (var21 * 6364136223846793005L + 1442695040888963407L) + theSeed;
        double var26 = (double) ((var21 >> 24) & 1023) / 1024.0;
        double var27 = (var26 - 0.5) * 0.9;
        double var28 = math_square(var20 + var27) + math_square(var19 + var25) + math_square(var18 + var23);
        var28s[var11] = var28;
    }

    for (int i = 0; i < 8; ++i) {
        if (var10 > var28s[i]) {
            var9 = i;
            var10 = var28s[i];
        }
    }

    return var9;
}

typedef const struct aquifer_data {
    int32_t startX;
    int32_t startY;
    int32_t startZ;
    int32_t sizeX;
    int32_t sizeZ;
} aquifer_data_t;

static inline __attribute__((const)) uint32_t
math_aquifer_index(const aquifer_data_t *restrict const aquiferData, const int32_t x, const int32_t y,
                   const int32_t z) {
    int i = x - aquiferData->startX;
    int j = y - aquiferData->startY;
    int k = z - aquiferData->startZ;
    return (j * aquiferData->sizeZ + k) * aquiferData->sizeX + i;
}

static inline __attribute__((const)) int32_t
math_aquifer_unpackPackedX(uint32_t packed) {
    return packed >> 8;
}

static inline __attribute__((const)) int32_t
math_aquifer_unpackPackedY(uint32_t packed) {
    return (packed >> 4) & 0b1111;
}

static inline __attribute__((const)) int32_t
math_aquifer_unpackPackedZ(uint32_t packed) {
    return packed & 0b1111;
}

static inline void
math_aquifer_refreshDistPosIdx(const uint16_t *restrict const packedBlockPositions, uint32_t *restrict const res,
                               const aquifer_data_t *restrict const aquiferData,
                               const int32_t x, const int32_t y, const int32_t z) {
    int32_t gx = (x - 5) >> 4;
    int32_t gy = math_floorDiv(y + 1, 12) - 1;
    int32_t gz = (z - 5) >> 4;
    uint32_t A = UINT32_MAX;
    uint32_t B = UINT32_MAX;
    uint32_t C = UINT32_MAX;

    uint32_t ps[12];

    uint32_t index = 12; // 12 max
    for (int32_t offY = 0; offY <= 2; ++offY) {
        int32_t gymul = gy * 12 + offY * 12;
        for (int32_t offZ = 0; offZ <= 1; ++offZ) {
            int32_t gzmul = (gz + offZ) << 4;

            uint32_t index0 = index - 1;
            uint32_t posIdx0 = math_aquifer_index(aquiferData, gx, gy + offY, gz + offZ);
            uint32_t position0 = packedBlockPositions[posIdx0];
            int32_t dx0 = (gx << 4) + math_aquifer_unpackPackedX(position0) - x;
            int32_t dy0 = gymul + math_aquifer_unpackPackedY(position0) - y;
            int32_t dz0 = gzmul + math_aquifer_unpackPackedZ(position0) - z;
            uint32_t dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

            uint32_t index1 = index - 2;
            uint32_t posIdx1 = posIdx0 + 1;
            uint32_t position1 = packedBlockPositions[posIdx1];
            int32_t dx1 = ((gx + 1) << 4) + math_aquifer_unpackPackedX(position1) - x;
            int32_t dy1 = gymul + math_aquifer_unpackPackedY(position1) - y;
            int32_t dz1 = gzmul + math_aquifer_unpackPackedZ(position1) - z;
            uint32_t dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

            ps[12 - index] = (dist_0 << 20) | (index0 << 16) | posIdx0;
            ps[13 - index] = (dist_1 << 20) | (index1 << 16) | posIdx1;

            index -= 2;
        }
    }

    A = ps[0];
    {
        uint32_t p1 = ps[1];
        uint32_t n11 = max(A, p1);
        A = min(A, p1);
        B = n11;
    }
    {
        uint32_t p1 = ps[2];
        uint32_t n11 = max(A, p1);
        A = min(A, p1);

        uint32_t n12 = max(B, n11);
        B = min(B, n11);

        C = n12;
    }

    for (uint32_t i = 3; i < 12; i ++) {
        uint32_t p1 = ps[i];
        if (p1 <= C) {
            uint32_t n11 = max(A, p1);
            A = min(A, p1);

            uint32_t n12 = max(B, n11);
            B = min(B, n11);

            C = min(C, n12);
        }
    }

    res[0] = A;
    res[1] = B;
    res[2] = C;
}

#pragma clang attribute pop

