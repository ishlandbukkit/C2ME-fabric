#include <ext_math.h>
#include <target_macros.h>

TARGET_IMPL(c2me_natives_noise_perlin_sample, double, (const aligned_uint32_ptr permutations, const double originX,
                                                       const double originY, const double originZ, const double x,
                                                       const double y, const double z, const double yScale,
                                                       const double yMax) {
    return math_noise_perlin_sample(permutations, originX, originY, originZ, x, y, z, yScale, yMax);
})

TARGET_IMPL(c2me_natives_noise_perlin_double, double, (const double_octave_sampler_data_t *const data,
                                                       const double x, const double y, const double z) {
    return math_noise_perlin_double_octave_sample(data, x, y, z);
})

TARGET_IMPL(c2me_natives_noise_perlin_double_batch, void, (const double_octave_sampler_data_t *const data,
                                                           double *const res, const double *const x,
                                                           const double *const y, const double *const z,
                                                           const uint32_t length) {
    math_noise_perlin_double_octave_sample_batch(data, res, x, y, z, length);
})

TARGET_IMPL(c2me_natives_noise_interpolated, double, (const interpolated_noise_sampler_t *const data,
                                                      const double x, const double y, const double z) {
    return math_noise_perlin_interpolated_sample(data, x, y, z);
})

TARGET_IMPL(c2me_natives_end_islands_sample, float, (const aligned_uint32_ptr simplex_permutations, const int32_t x, const int32_t z) {
    return math_end_islands_sample(simplex_permutations, x, z);
})

TARGET_IMPL(c2me_natives_biome_access_sample, uint32_t, (const int64_t theSeed, const int32_t x, const int32_t y, const int32_t z) {
    return math_biome_access_sample(theSeed, x, y, z);
})

TARGET_IMPL(c2me_natives_aquifer_refreshDistPosIdx, void, (const uint16_t *restrict const packedBlockPositions,
                                                           uint32_t *restrict const res,
                                                           const aquifer_data_t *restrict const aquiferData,
                                                           const int32_t x, const int32_t y, const int32_t z) {
    math_aquifer_refreshDistPosIdx(packedBlockPositions, res, aquiferData, x, y, z);
})
