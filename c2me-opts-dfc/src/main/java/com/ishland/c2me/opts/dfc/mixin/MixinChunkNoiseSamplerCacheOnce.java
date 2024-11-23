package com.ishland.c2me.opts.dfc.mixin;

import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.vif.EachApplierVanillaInterface;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

@Mixin(ChunkNoiseSampler.CacheOnce.class)
public abstract class MixinChunkNoiseSamplerCacheOnce implements IFastCacheLike {

    @Mutable
    @Shadow @Final private DensityFunction delegate;
    private double c2me$lastValue = Double.NaN;
    private int c2me$lastX = Integer.MIN_VALUE;
    private int c2me$lastY = Integer.MIN_VALUE;
    private int c2me$lastZ = Integer.MIN_VALUE;

    private int[] c2me$lastXa;
    private int[] c2me$lastYa;
    private int[] c2me$lastZa;
    private double[] c2me$lastValuea;

    @WrapMethod(method = "sample")
    private double wrapSample(DensityFunction.NoisePos pos, Operation<Double> original) {
        if (pos instanceof ChunkNoiseSampler) {
            return original.call(pos);
        }
        int blockX = pos.blockX();
        int blockY = pos.blockY();
        int blockZ = pos.blockZ();
        if (c2me$lastValuea != null) {
            for (int i = 0; i < this.c2me$lastValuea.length; i ++) {
                if (c2me$lastXa[i] == blockX && c2me$lastYa[i] == blockY && c2me$lastZa[i] == blockZ) {
                    return c2me$lastValuea[i];
                }
            }
        }
        if (!Double.isNaN(c2me$lastValue) && c2me$lastX == blockX && c2me$lastY == blockY && c2me$lastZ == blockZ) {
            return c2me$lastValue;
        }
        double sample = this.delegate.sample(pos);
        c2me$lastValue = sample;
        c2me$lastX = blockX;
        c2me$lastY = blockY;
        c2me$lastZ = blockZ;
        return sample;
    }

    @WrapMethod(method = "fill")
    private void wrapFill(double[] densities, DensityFunction.EachApplier applier, Operation<Void> original) {
        if (applier instanceof ChunkNoiseSampler) {
            original.call(densities, applier);
            return;
        }
        if (applier instanceof EachApplierVanillaInterface ap) {
            if (c2me$lastValuea != null && Arrays.equals(ap.getY(), c2me$lastYa) && Arrays.equals(ap.getX(), c2me$lastXa) && Arrays.equals(ap.getZ(), c2me$lastZa)) {
                System.arraycopy(c2me$lastValuea, 0, densities, 0, c2me$lastValuea.length);
            } else {
                this.delegate.fill(densities, applier);
                this.c2me$lastValuea = Arrays.copyOf(densities, densities.length);
                this.c2me$lastXa = ap.getX();
                this.c2me$lastYa = ap.getY();
                this.c2me$lastZa = ap.getZ();
            }
            return;
        }
        this.delegate.fill(densities, applier);
    }

    @Override
    public double c2me$getCached(int x, int y, int z, EvalType evalType) {
        if (c2me$lastValuea != null) {
            for (int i = 0; i < this.c2me$lastValuea.length; i ++) {
                if (c2me$lastXa[i] == x && c2me$lastYa[i] == y && c2me$lastZa[i] == z) {
                    return c2me$lastValuea[i];
                }
            }
        }
        if (!Double.isNaN(c2me$lastValue) && c2me$lastX == x && c2me$lastY == y && c2me$lastZ == z) {
            return c2me$lastValue;
        }

        return Double.longBitsToDouble(CACHE_MISS_NAN_BITS);
    }

    @Override
    public boolean c2me$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (c2me$lastValuea != null && Arrays.equals(y, c2me$lastYa) && Arrays.equals(x, c2me$lastXa) && Arrays.equals(z, c2me$lastZa)) {
            System.arraycopy(c2me$lastValuea, 0, res, 0, c2me$lastValuea.length);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void c2me$cache(int x, int y, int z, EvalType evalType, double cached) {
        c2me$lastValue = cached;
        c2me$lastX = x;
        c2me$lastY = y;
        c2me$lastZ = z;
    }

    @Override
    public void c2me$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType) {
        if (c2me$lastValuea != null && this.c2me$lastValuea.length == res.length) {
            System.arraycopy(res, 0, this.c2me$lastValuea, 0, this.c2me$lastValuea.length);
            System.arraycopy(x, 0, this.c2me$lastXa, 0, this.c2me$lastValuea.length);
            System.arraycopy(y, 0, this.c2me$lastYa, 0, this.c2me$lastValuea.length);
            System.arraycopy(z, 0, this.c2me$lastZa, 0, this.c2me$lastValuea.length);
        } else {
            this.c2me$lastValuea = Arrays.copyOf(res, res.length);
            this.c2me$lastXa = Arrays.copyOf(x, x.length);
            this.c2me$lastYa = Arrays.copyOf(y, y.length);
            this.c2me$lastZa = Arrays.copyOf(z, z.length);
        }
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
