package com.ishland.c2me.opts.worldgen.vanilla.mixin.aquifer;

import com.ishland.c2me.opts.worldgen.general.common.random_instances.RandomUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AquiferSampler.Impl.class)
public abstract class MixinAquiferSamplerImpl {

    @Shadow
    @Final
    private int startX;

    @Shadow
    @Final
    private int startY;

    @Shadow
    @Final
    private int startZ;

    @Shadow
    @Final
    private int sizeZ;

    @Shadow @Final private int sizeX;

    @Shadow @Final private long[] blockPositions;

    @Shadow @Final private RandomSplitter randomDeriver;

    @Shadow
    @Final
    private AquiferSampler.FluidLevel[] waterLevels;

    @Shadow
    @Final
    private static int[][] CHUNK_POS_OFFSETS;

    @Shadow
    @Final
    private ChunkNoiseSampler chunkNoiseSampler;

    @Shadow
    @Final
    private DensityFunction barrierNoise;

    @Shadow
    @Final
    private DensityFunction fluidLevelFloodednessNoise;

    @Shadow
    @Final
    private DensityFunction fluidLevelSpreadNoise;

    @Shadow
    @Final
    private DensityFunction fluidTypeNoise;

    @Shadow
    @Final
    private static double NEEDS_FLUID_TICK_DISTANCE_THRESHOLD;

    @Shadow
    private boolean needsFluidTick;

    @Shadow
    @Final
    private AquiferSampler.FluidLevelSampler fluidLevelSampler;

    @Shadow protected abstract int index(int x, int y, int z);

    @Shadow
    protected static double maxDistance(int i, int a) {
        throw new AbstractMethodError();
    }

    @Shadow protected abstract int getNoiseBasedFluidLevel(int blockX, int blockY, int blockZ, int surfaceHeightEstimate);

    @Shadow protected abstract AquiferSampler.FluidLevel getFluidLevel(int blockX, int blockY, int blockZ);

    @Shadow @Final private DensityFunction erosionDensityFunction;

    @Shadow @Final private DensityFunction depthDensityFunction;

    @Unique
    private int c2me$packed1;
    @Unique
    private int c2me$packed2;
    @Unique
    private int c2me$packed3;

    @Unique
    private double c2me$mutableDoubleThingy;

    @Unique
    private short[] c2me$packedBlockPositions;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        // preload position cache
        if (this.blockPositions.length % (this.sizeX * this.sizeZ) != 0) {
            throw new AssertionError("Array length");
        }

        int sizeY = this.blockPositions.length / (this.sizeX * this.sizeZ);

        this.c2me$packedBlockPositions = new short[this.blockPositions.length];

        final Random random = RandomUtils.getRandom(this.randomDeriver);
        // index: y, z, x
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    final int x1 = x + this.startX;
                    final int y1 = y + this.startY;
                    final int z1 = z + this.startZ;
                    RandomUtils.derive(this.randomDeriver, random, x1, y1, z1);
                    int r0 = random.nextInt(10);
                    int r1 = random.nextInt(9);
                    int r2 = random.nextInt(10);
                    int x2 = x1 * 16 + r0;
                    int y2 = y1 * 12 + r1;
                    int z2 = z1 * 16 + r2;
                    int index = this.index(x1, y1, z1);
                    this.blockPositions[index] = BlockPos.asLong(x2, y2, z2);
                    this.c2me$packedBlockPositions[index] = (short) ((r0 << 8) | (r1 << 4) | r2);
                }
            }
        }
        for (long blockPosition : this.blockPositions) {
            if (blockPosition == Long.MAX_VALUE) {
                throw new AssertionError("Array initialization");
            }
        }
    }

    @Unique
    private static int c2me$unpackPackedX(int packed) {
        return packed >> 8;
    }

    @Unique
    private static int c2me$unpackPackedY(int packed) {
        return (packed >> 4) & 0b1111;
    }

    @Unique
    private static int c2me$unpackPackedZ(int packed) {
        return packed & 0b1111;
    }

    @Unique
    private static int c2me$unpackPackedDist(int packed) {
        return packed >> 20;
    }

    private static int c2me$unpackPackedPosIdx(int packed) {
        return packed & 0xffff;
    }

    /**
     * @author ishland
     * @reason make C2 happier by splitting method into many
     */
    @Overwrite
    public BlockState apply(DensityFunction.NoisePos pos, double density) {
        int i = pos.blockX();
        int j = pos.blockY();
        int k = pos.blockZ();
        if (density > 0.0) {
            this.needsFluidTick = false;
            return null;
        } else {
            AquiferSampler.FluidLevel fluidLevel = this.fluidLevelSampler.getFluidLevel(i, j, k);
            if (fluidLevel.getBlockState(j).isOf(Blocks.LAVA)) {
                this.needsFluidTick = false;
                return Blocks.LAVA.getDefaultState();
            } else {
                aquiferExtracted$refreshDistPosIdx(i, j, k);
                return aquiferExtracted$applyPost(pos, density, j, i, k);
            }
        }
    }

    @Unique
    private @Nullable BlockState aquiferExtracted$applyPost(DensityFunction.NoisePos pos, double density, int j, int i, int k) {
        AquiferSampler.FluidLevel fluidLevel2 = this.c2me$getWaterLevelIndexed(c2me$unpackPackedPosIdx(this.c2me$packed1));
        double d = maxDistance(c2me$unpackPackedDist(this.c2me$packed1), c2me$unpackPackedDist(this.c2me$packed2));
        BlockState blockState = fluidLevel2.getBlockState(j);
        if (d <= 0.0) {
            this.needsFluidTick = d >= NEEDS_FLUID_TICK_DISTANCE_THRESHOLD;
            return blockState;
        } else if (blockState.isOf(Blocks.WATER) && this.fluidLevelSampler.getFluidLevel(i, j - 1, k).getBlockState(j - 1).isOf(Blocks.LAVA)) {
            this.needsFluidTick = true;
            return blockState;
        } else {
//            MutableDouble mutableDouble = new MutableDouble(Double.NaN); // 234MB/s alloc rate at 480 cps
            this.c2me$mutableDoubleThingy = Double.NaN;
            AquiferSampler.FluidLevel fluidLevel3 = this.c2me$getWaterLevelIndexed(c2me$unpackPackedPosIdx(this.c2me$packed2));
            double e = d * this.c2me$calculateDensityModified(pos, fluidLevel2, fluidLevel3);
            if (density + e > 0.0) {
                this.needsFluidTick = false;
                return null;
            } else {
                return aquiferExtracted$getFinalBlockState(pos, density, d, fluidLevel2, fluidLevel3, blockState);
            }
        }
    }

    @Unique
    private BlockState aquiferExtracted$getFinalBlockState(DensityFunction.NoisePos pos, double density, double d, AquiferSampler.FluidLevel fluidLevel2, AquiferSampler.FluidLevel fluidLevel3, BlockState blockState) {
        AquiferSampler.FluidLevel fluidLevel4 = this.c2me$getWaterLevelIndexed(c2me$unpackPackedPosIdx(this.c2me$packed3));
        int dist3 = c2me$unpackPackedDist(this.c2me$packed3);
        double f = maxDistance(c2me$unpackPackedDist(this.c2me$packed1), dist3);
        if (aquiferExtracted$extractedCheckFG(pos, density, d, fluidLevel2, f, fluidLevel4)) return null;

        double g = maxDistance(c2me$unpackPackedDist(this.c2me$packed2), dist3);
        if (aquiferExtracted$extractedCheckFG(pos, density, d, fluidLevel3, g, fluidLevel4)) return null;

        this.needsFluidTick = true;
        return blockState;
    }

    @Unique
    private boolean aquiferExtracted$extractedCheckFG(DensityFunction.NoisePos pos, double density, double d, AquiferSampler.FluidLevel fluidLevel2, double f, AquiferSampler.FluidLevel fluidLevel4) {
        if (f > 0.0) {
            double g = d * f * this.c2me$calculateDensityModified(pos, fluidLevel2, fluidLevel4);
            if (density + g > 0.0) {
                this.needsFluidTick = false;
                return true;
            }
        }
        return false;
    }

    @Unique
    @NotNull
    private void aquiferExtracted$refreshDistPosIdx(int x, int y, int z) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;
        int A = Integer.MAX_VALUE;
        int B = Integer.MAX_VALUE;
        int C = Integer.MAX_VALUE;

        int index = 12; // 12 max
        for (int offY = -1; offY <= 1; ++offY) {
            int gymul = (gy + offY) * 12;
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int gzmul = (gz + offZ) << 4;

                int index0 = index - 1;
                int posIdx0 = this.index(gx, gy + offY, gz + offZ);
                int position0 = this.c2me$packedBlockPositions[posIdx0];
                int dx0 = (gx << 4) + c2me$unpackPackedX(position0) - x;
                int dy0 = gymul + c2me$unpackPackedY(position0) - y;
                int dz0 = gzmul + c2me$unpackPackedZ(position0) - z;
                int dist_0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

                int index1 = index - 2;
                int posIdx1 = posIdx0 + 1;
                int position1 = this.c2me$packedBlockPositions[posIdx1];
                int dx1 = ((gx + 1) << 4) + c2me$unpackPackedX(position1) - x;
                int dy1 = gymul + c2me$unpackPackedY(position1) - y;
                int dz1 = gzmul + c2me$unpackPackedZ(position1) - z;
                int dist_1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

                int p0 = (dist_0 << 20) | (index0 << 16) | posIdx0;
                if (p0 <= C) {
                    int n01 = Math.max(A, p0);
                    A = Math.min(A, p0);

                    int n02 = Math.max(B, n01);
                    B = Math.min(B, n01);

                    C = Math.min(C, n02);
                }

                int p1 = (dist_1 << 20) | (index1 << 16) | posIdx1;
                if (p1 <= C) {
                    int n11 = Math.max(A, p1);
                    A = Math.min(A, p1);

                    int n12 = Math.max(B, n11);
                    B = Math.min(B, n11);

                    C = Math.min(C, n12);
                }

                index -= 2;
            }
        }

//        this.c2me$dist1 = A >> 20;
//        this.c2me$dist2 = B >> 20;
//        this.c2me$dist3 = C >> 20;
//        this.c2me$pos1 = this.blockPositions[A & 0xffff];
//        this.c2me$pos2 = this.blockPositions[B & 0xffff];
//        this.c2me$pos3 = this.blockPositions[C & 0xffff];
        this.c2me$packed1 = A;
        this.c2me$packed2 = B;
        this.c2me$packed3 = C;
    }

    /**
     * @author ishland
     * @reason optimize
     */
    @Overwrite
    private AquiferSampler.FluidLevel getWaterLevel(long pos) {
        int i = BlockPos.unpackLongX(pos);
        int j = BlockPos.unpackLongY(pos);
        int k = BlockPos.unpackLongZ(pos);
        int l = i >> 4; // C2ME - inline: floorDiv(i, 16)
        int m = Math.floorDiv(j, 12); // C2ME - inline
        int n = k >> 4; // C2ME - inline: floorDiv(k, 16)
        int o = this.index(l, m, n);
        AquiferSampler.FluidLevel fluidLevel = this.waterLevels[o];
        if (fluidLevel != null) {
            return fluidLevel;
        } else {
            AquiferSampler.FluidLevel fluidLevel2 = this.getFluidLevel(i, j, k);
            this.waterLevels[o] = fluidLevel2;
            return fluidLevel2;
        }
    }

    @Unique
    private AquiferSampler.FluidLevel c2me$getWaterLevelIndexed(int index) {
        AquiferSampler.FluidLevel fluidLevel = this.waterLevels[index];
        if (fluidLevel != null) {
            return fluidLevel;
        } else {
            long blockPosition = this.blockPositions[index];
            int x = BlockPos.unpackLongX(blockPosition);
            int y = BlockPos.unpackLongY(blockPosition);
            int z = BlockPos.unpackLongZ(blockPosition);
            AquiferSampler.FluidLevel fluidLevel2 = this.getFluidLevel(x, y, z);
            this.waterLevels[index] = fluidLevel2;
            return fluidLevel2;
        }
    }


    /**
     * @author ishland
     * @reason optimize
     */
    @Overwrite
    private int getFluidBlockY(int blockX, int blockY, int blockZ, AquiferSampler.FluidLevel defaultFluidLevel, int surfaceHeightEstimate, boolean bl) {
        DensityFunction.UnblendedNoisePos unblendedNoisePos = new DensityFunction.UnblendedNoisePos(blockX, blockY, blockZ);
        double d;
        double e;
        if (VanillaBiomeParameters.inDeepDarkParameters(this.erosionDensityFunction, this.depthDensityFunction, unblendedNoisePos)) {
            d = -1.0;
            e = -1.0;
        } else {
            int i = surfaceHeightEstimate + 8 - blockY;
            double f = bl ? MathHelper.clampedLerp(1.0, 0.0, ((double) i) / 64.0) : 0.0; // inline
            double g = MathHelper.clamp(this.fluidLevelFloodednessNoise.sample(unblendedNoisePos), -1.0, 1.0);
            d = g + 0.8 + (f - 1.0) * 1.2; // inline
            e = g + 0.3 + (f - 1.0) * 1.1; // inline
        }

        int i;
        if (e > 0.0) {
            i = defaultFluidLevel.y;
        } else if (d > 0.0) {
            i = this.getNoiseBasedFluidLevel(blockX, blockY, blockZ, surfaceHeightEstimate);
        } else {
            i = DimensionType.field_35479;
        }

        return i;
    }

    /**
     * @author ishland
     * @reason optimize, split method into many
     */
    @Overwrite
    private double calculateDensity(
            DensityFunction.NoisePos pos, MutableDouble mutableDouble, AquiferSampler.FluidLevel fluidLevel, AquiferSampler.FluidLevel fluidLevel2
    ) {
        int i = pos.blockY();
        BlockState blockState = fluidLevel.getBlockState(i);
        BlockState blockState2 = fluidLevel2.getBlockState(i);
        if ((!blockState.isOf(Blocks.LAVA) || !blockState2.isOf(Blocks.WATER)) && (!blockState.isOf(Blocks.WATER) || !blockState2.isOf(Blocks.LAVA))) {
            int j = Math.abs(fluidLevel.y - fluidLevel2.y);
            if (j == 0) {
                return 0.0;
            } else {
                double d = 0.5 * (double)(fluidLevel.y + fluidLevel2.y);
                final double q = aquiferExtracted$getQ(i, d, j);

                return aquiferExtracted$postCalculateDensity(pos, mutableDouble, q);
            }
        } else {
            return 2.0;
        }
    }

    private double c2me$calculateDensityModified(
            DensityFunction.NoisePos pos, AquiferSampler.FluidLevel fluidLevel, AquiferSampler.FluidLevel fluidLevel2
    ) {
        int i = pos.blockY();
        BlockState blockState = fluidLevel.getBlockState(i);
        BlockState blockState2 = fluidLevel2.getBlockState(i);
        if ((!blockState.isOf(Blocks.LAVA) || !blockState2.isOf(Blocks.WATER)) && (!blockState.isOf(Blocks.WATER) || !blockState2.isOf(Blocks.LAVA))) {
            int j = Math.abs(fluidLevel.y - fluidLevel2.y);
            if (j == 0) {
                return 0.0;
            } else {
                double d = 0.5 * (double)(fluidLevel.y + fluidLevel2.y);
                final double q = aquiferExtracted$getQ(i, d, j);

                return aquiferExtracted$postCalculateDensityModified(pos, q);
            }
        } else {
            return 2.0;
        }
    }

    @Unique
    private double aquiferExtracted$postCalculateDensity(DensityFunction.NoisePos pos, MutableDouble mutableDouble, double q) {
        double r;
        if (!(q < -2.0) && !(q > 2.0)) {
            double s = mutableDouble.getValue();
            if (Double.isNaN(s)) {
                double t = this.barrierNoise.sample(pos);
                mutableDouble.setValue(t);
                r = t;
            } else {
                r = s;
            }
        } else {
            r = 0.0;
        }

        return 2.0 * (r + q);
    }

    @Unique
    private double aquiferExtracted$postCalculateDensityModified(DensityFunction.NoisePos pos, double q) {
        double r;
        if (!(q < -2.0) && !(q > 2.0)) {
            double s = this.c2me$mutableDoubleThingy;
            if (Double.isNaN(s)) {
                double t = this.barrierNoise.sample(pos);
                this.c2me$mutableDoubleThingy = t;
                r = t;
            } else {
                r = s;
            }
        } else {
            r = 0.0;
        }

        return 2.0 * (r + q);
    }

    @Unique
    private static double aquiferExtracted$getQ(double i, double d, double j) {
        double e = i + 0.5 - d;
        double f = j / 2.0;
        double o = f - Math.abs(e);
        double q;
        if (e > 0.0) {
            if (o > 0.0) {
                q = o / 1.5;
            } else {
                q = o / 2.5;
            }
        } else {
            double p = 3.0 + o;
            if (p > 0.0) {
                q = p / 3.0;
            } else {
                q = p / 10.0;
            }
        }
        return q;
    }

    /**
     * @author ishland
     * @reason optimize
     */
    @Overwrite
    private BlockState getFluidBlockState(int blockX, int blockY, int blockZ, AquiferSampler.FluidLevel defaultFluidLevel, int fluidLevel) {
        BlockState blockState = defaultFluidLevel.state;
        if (fluidLevel <= -10 && fluidLevel != DimensionType.field_35479 && defaultFluidLevel.state != Blocks.LAVA.getDefaultState()) {
            int k = blockX >> 6; // floorDiv(blockX, 64)
            int l = Math.floorDiv(blockY, 40);
            int m = blockZ >> 6; // floorDiv(blockZ, 64)
            double d = this.fluidTypeNoise.sample(new DensityFunction.UnblendedNoisePos(k, l, m));
            if (Math.abs(d) > 0.3) {
                blockState = Blocks.LAVA.getDefaultState();
            }
        }

        return blockState;
    }

}
