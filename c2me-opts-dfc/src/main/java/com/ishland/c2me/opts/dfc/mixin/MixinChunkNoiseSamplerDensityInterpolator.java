package com.ishland.c2me.opts.dfc.mixin;

import com.ishland.c2me.base.mixin.access.IChunkNoiseSampler;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.vif.NoisePosVanillaInterface;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

@Mixin(ChunkNoiseSampler.DensityInterpolator.class)
public abstract class MixinChunkNoiseSamplerDensityInterpolator implements IFastCacheLike {

    @Shadow @Final private ChunkNoiseSampler field_34622;

    @Shadow private double x0y0z0;

    @Shadow private double x1y0z0;

    @Shadow private double x0y1z0;

    @Shadow private double x1y1z0;

    @Shadow private double x0y0z1;

    @Shadow private double x1y0z1;

    @Shadow private double x0y1z1;

    @Shadow private double x1y1z1;

    @Shadow private double result;

    @Mutable
    @Shadow @Final private DensityFunction delegate;

    @WrapMethod(method = "sample")
    private double wrapSample(DensityFunction.NoisePos pos, Operation<Double> original) {
        if (pos instanceof ChunkNoiseSampler) {
            return original.call(pos);
        }
        if (pos instanceof NoisePosVanillaInterface vif && vif.getType() == EvalType.INTERPOLATION) {
            boolean isInInterpolationLoop = ((IChunkNoiseSampler) this.field_34622).getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                int startBlockX = ((IChunkNoiseSampler) this.field_34622).getStartBlockX();
                int startBlockY = ((IChunkNoiseSampler) this.field_34622).getStartBlockY();
                int startBlockZ = ((IChunkNoiseSampler) this.field_34622).getStartBlockZ();
                int horizontalCellBlockCount = ((IChunkNoiseSampler) this.field_34622).getHorizontalCellBlockCount();
                int verticalCellBlockCount = ((IChunkNoiseSampler) this.field_34622).getVerticalCellBlockCount();
                int cellBlockX = pos.blockX() - startBlockX;
                int cellBlockY = pos.blockY() - startBlockY;
                int cellBlockZ = pos.blockZ() - startBlockZ;
                return ((IChunkNoiseSampler) this.field_34622).getIsSamplingForCaches()
                        ? MathHelper.lerp3(
                        (double)cellBlockX / (double)horizontalCellBlockCount,
                        (double)cellBlockY / (double)verticalCellBlockCount,
                        (double)cellBlockZ / (double)horizontalCellBlockCount,
                        this.x0y0z0,
                        this.x1y0z0,
                        this.x0y1z0,
                        this.x1y1z0,
                        this.x0y0z1,
                        this.x1y0z1,
                        this.x0y1z1,
                        this.x1y1z1
                )
                        : this.result;
            }
        }
        return original.call(pos);
    }

    @Override
    public double c2me$getCached(int x, int y, int z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            boolean isInInterpolationLoop = ((IChunkNoiseSampler) this.field_34622).getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                if (((IChunkNoiseSampler) this.field_34622).getIsSamplingForCaches()) {
                    int startBlockX = ((IChunkNoiseSampler) this.field_34622).getStartBlockX();
                    int startBlockY = ((IChunkNoiseSampler) this.field_34622).getStartBlockY();
                    int startBlockZ = ((IChunkNoiseSampler) this.field_34622).getStartBlockZ();
                    int horizontalCellBlockCount = ((IChunkNoiseSampler) this.field_34622).getHorizontalCellBlockCount();
                    int verticalCellBlockCount = ((IChunkNoiseSampler) this.field_34622).getVerticalCellBlockCount();
                    int cellBlockX = x - startBlockX;
                    int cellBlockY = y - startBlockY;
                    int cellBlockZ = z - startBlockZ;
                    return MathHelper.lerp3(
                            (double) cellBlockX / (double) horizontalCellBlockCount,
                            (double) cellBlockY / (double) verticalCellBlockCount,
                            (double) cellBlockZ / (double) horizontalCellBlockCount,
                            this.x0y0z0,
                            this.x1y0z0,
                            this.x0y1z0,
                            this.x1y1z0,
                            this.x0y0z1,
                            this.x1y0z1,
                            this.x0y1z1,
                            this.x1y1z1
                    );
                } else {
                    return this.result;
                }
            }
        }

        return CACHE_MISS_NAN_BITS;
    }

    @Override
    public boolean c2me$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (evalType == EvalType.INTERPOLATION) {
            boolean isInInterpolationLoop = ((IChunkNoiseSampler) this.field_34622).getIsInInterpolationLoop();
            if (isInInterpolationLoop) {
                if (((IChunkNoiseSampler) this.field_34622).getIsSamplingForCaches()) {
                    int startBlockX = ((IChunkNoiseSampler) this.field_34622).getStartBlockX();
                    int startBlockY = ((IChunkNoiseSampler) this.field_34622).getStartBlockY();
                    int startBlockZ = ((IChunkNoiseSampler) this.field_34622).getStartBlockZ();
                    double horizontalCellBlockCount = ((IChunkNoiseSampler) this.field_34622).getHorizontalCellBlockCount();
                    double verticalCellBlockCount = ((IChunkNoiseSampler) this.field_34622).getVerticalCellBlockCount();
                    for (int i = 0; i < res.length; i ++) {
                        int cellBlockX = x[i] - startBlockX;
                        int cellBlockY = y[i] - startBlockY;
                        int cellBlockZ = z[i] - startBlockZ;
                        res[i] = MathHelper.lerp3(
                                (double)cellBlockX / horizontalCellBlockCount,
                                (double)cellBlockY / verticalCellBlockCount,
                                (double)cellBlockZ / horizontalCellBlockCount,
                                this.x0y0z0,
                                this.x1y0z0,
                                this.x0y1z0,
                                this.x1y1z0,
                                this.x0y0z1,
                                this.x1y0z1,
                                this.x0y1z1,
                                this.x1y1z1
                        );
                    }
                    return true;
                } else {
                    Arrays.fill(res, this.result);
                    return true;
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
        return false;
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
