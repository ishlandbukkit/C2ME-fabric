package com.ishland.c2me.opts.dfc.mixin;

import com.ishland.c2me.base.mixin.access.IChunkNoiseSampler;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.vif.NoisePosVanillaInterface;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkNoiseSampler.CellCache.class)
public abstract class MixinChunkNoiseSamplerCellCache implements IFastCacheLike {

    @Shadow
    @Final
    private ChunkNoiseSampler field_36602;

    @Shadow
    @Final
    private double[] cache;

    @Mutable
    @Shadow
    @Final
    private DensityFunction delegate;

    @WrapMethod(method = "sample")
    private double wrapSample(DensityFunction.NoisePos pos, Operation<Double> original) {
        if (pos instanceof ChunkNoiseSampler) {
            return original.call(pos);
        }
        if (pos instanceof NoisePosVanillaInterface vif && vif.getType() == EvalType.INTERPOLATION) {
            boolean isInInterpolationLoop = ((IChunkNoiseSampler) this.field_36602).getIsInInterpolationLoop();
            if (!isInInterpolationLoop) {
                return original.call(pos);
            }
            int startBlockX = ((IChunkNoiseSampler) this.field_36602).getStartBlockX();
            int startBlockY = ((IChunkNoiseSampler) this.field_36602).getStartBlockY();
            int startBlockZ = ((IChunkNoiseSampler) this.field_36602).getStartBlockZ();
            int horizontalCellBlockCount = ((IChunkNoiseSampler) this.field_36602).getHorizontalCellBlockCount();
            int verticalCellBlockCount = ((IChunkNoiseSampler) this.field_36602).getVerticalCellBlockCount();
            int cellBlockX = pos.blockX() - startBlockX;
            int cellBlockY = pos.blockY() - startBlockY;
            int cellBlockZ = pos.blockZ() - startBlockZ;
            return cellBlockX >= 0
                    && cellBlockY >= 0
                    && cellBlockZ >= 0
                    && cellBlockX < horizontalCellBlockCount
                    && cellBlockY < verticalCellBlockCount
                    && cellBlockZ < horizontalCellBlockCount
                    ? this.cache[((verticalCellBlockCount - 1 - cellBlockY) * horizontalCellBlockCount + cellBlockX)
                    * horizontalCellBlockCount
                    + cellBlockZ]
                    : this.delegate.sample(pos);
        }
        return original.call(pos);
    }

    @Override
    public double c2me$getCached(int x, int y, int z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            boolean isInInterpolationLoop = ((IChunkNoiseSampler) this.field_36602).getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                int startBlockX = ((IChunkNoiseSampler) this.field_36602).getStartBlockX();
                int startBlockY = ((IChunkNoiseSampler) this.field_36602).getStartBlockY();
                int startBlockZ = ((IChunkNoiseSampler) this.field_36602).getStartBlockZ();
                int horizontalCellBlockCount = ((IChunkNoiseSampler) this.field_36602).getHorizontalCellBlockCount();
                int verticalCellBlockCount = ((IChunkNoiseSampler) this.field_36602).getVerticalCellBlockCount();
                int cellBlockX = x - startBlockX;
                int cellBlockY = y - startBlockY;
                int cellBlockZ = z - startBlockZ;
                if (cellBlockX >= 0 &&
                        cellBlockY >= 0 &&
                        cellBlockZ >= 0 &&
                        cellBlockX < horizontalCellBlockCount &&
                        cellBlockY < verticalCellBlockCount &&
                        cellBlockZ < horizontalCellBlockCount) {
                    return this.cache[((verticalCellBlockCount - 1 - cellBlockY) * horizontalCellBlockCount + cellBlockX)
                            * horizontalCellBlockCount
                            + cellBlockZ];
                }
            }
        }

        return CACHE_MISS_NAN_BITS;
    }

    @Override
    public boolean c2me$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            boolean isInInterpolationLoop = ((IChunkNoiseSampler) this.field_36602).getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                int startBlockX = ((IChunkNoiseSampler) this.field_36602).getStartBlockX();
                int startBlockY = ((IChunkNoiseSampler) this.field_36602).getStartBlockY();
                int startBlockZ = ((IChunkNoiseSampler) this.field_36602).getStartBlockZ();
                int horizontalCellBlockCount = ((IChunkNoiseSampler) this.field_36602).getHorizontalCellBlockCount();
                int verticalCellBlockCount = ((IChunkNoiseSampler) this.field_36602).getVerticalCellBlockCount();
                for (int i = 0; i < res.length; i++) {
                    int cellBlockX = x[i] - startBlockX;
                    int cellBlockY = y[i] - startBlockY;
                    int cellBlockZ = z[i] - startBlockZ;
                    if (cellBlockX >= 0 &&
                            cellBlockY >= 0 &&
                            cellBlockZ >= 0 &&
                            cellBlockX < horizontalCellBlockCount &&
                            cellBlockY < verticalCellBlockCount &&
                            cellBlockZ < horizontalCellBlockCount) {
                        res[i] = this.cache[((verticalCellBlockCount - 1 - cellBlockY) * horizontalCellBlockCount + cellBlockX) * horizontalCellBlockCount + cellBlockZ];
                    } else {
                        System.out.println("partial cell cache hit");
                        return false; // partial hit possible
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void c2me$cache(int x, int y, int z, EvalType evalType, double cached) {
        // nop
    }

    @Override
    public void c2me$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        // nop
    }

    @Override
    public boolean c2me$isActualCache() {
        return true;
    }

    @Override
    public DensityFunction c2me$getDelegate() {
        return this.delegate;
    }

    @Override
    public DensityFunction c2me$withDelegate(DensityFunction delegate) {
        this.delegate = delegate;
        return this;
    }
}
