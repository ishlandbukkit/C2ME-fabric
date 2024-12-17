package com.ishland.c2me.opts.dfc.common.gen;

import com.google.common.base.Suppliers;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IArrayCacheCapable;
import com.ishland.c2me.opts.dfc.common.ducks.IBlendingAwareVisitor;
import com.ishland.c2me.opts.dfc.common.ducks.ICoordinatesFilling;
import com.ishland.c2me.opts.dfc.common.ducks.IPreloadedCoordinates;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import com.ishland.c2me.opts.dfc.common.vif.EachApplierVanillaInterface;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

public class SubCompiledDensityFunction implements DensityFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubCompiledDensityFunction.class);

    private final ISingleMethod singleMethod;
    private final IMultiMethod multiMethod;
    protected final Supplier<DensityFunction> blendingFallback;

    // also called from generated code
    public SubCompiledDensityFunction(ISingleMethod singleMethod, IMultiMethod multiMethod, DensityFunction blendingFallback) {
        this(singleMethod, multiMethod, unwrap(blendingFallback));
    }

    protected SubCompiledDensityFunction(ISingleMethod singleMethod, IMultiMethod multiMethod, Supplier<DensityFunction> blendingFallback) {
        this.singleMethod = Objects.requireNonNull(singleMethod);
        this.multiMethod = Objects.requireNonNull(multiMethod);
        this.blendingFallback = blendingFallback;
    }

    private static Supplier<DensityFunction> unwrap(DensityFunction densityFunction) {
        if (densityFunction instanceof SubCompiledDensityFunction scdf) {
            return scdf.blendingFallback;
        } else {
            return densityFunction != null ? Suppliers.ofInstance(densityFunction) : null;
        }
    }

    @Override
    public double sample(NoisePos pos) {
        if (pos.getBlender() != Blender.getNoBlending()) {
            DensityFunction fallback = this.getFallback();
            if (fallback == null) {
                throw new IllegalStateException("blendingFallback is no more");
            }
            return fallback.sample(pos);
        } else {
            return this.singleMethod.evalSingle(pos.blockX(), pos.blockY(), pos.blockZ(), EvalType.from(pos));
        }
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        if (applier instanceof ChunkNoiseSampler sampler) {
            if (sampler.getBlender() != Blender.getNoBlending()) {
                DensityFunction fallback = this.getFallback();
                if (fallback == null) {
                    throw new IllegalStateException("blendingFallback is no more");
                }
                fallback.fill(densities, applier);
                return;
            }
        }
        if (applier instanceof EachApplierVanillaInterface vanillaInterface) {
            this.multiMethod.evalMulti(densities, vanillaInterface.getX(), vanillaInterface.getY(), vanillaInterface.getZ(), EvalType.from(applier), vanillaInterface.c2me$getArrayCache());
            return;
        }

        ArrayCache cache = applier instanceof IArrayCacheCapable cacheCapable ? cacheCapable.c2me$getArrayCache() : new ArrayCache();
        int[] x;
        int[] y;
        int[] z;
        if (applier instanceof IPreloadedCoordinates preloadedCoordinates) {
            x = preloadedCoordinates.c2me$getXArray();
            y = preloadedCoordinates.c2me$getYArray();
            z = preloadedCoordinates.c2me$getZArray();
        } else {
            x = cache.getIntArray(densities.length, false);
            y = cache.getIntArray(densities.length, false);
            z = cache.getIntArray(densities.length, false);
            if (applier instanceof ICoordinatesFilling coordinatesFilling) {
                coordinatesFilling.c2me$fillCoordinates(x, y, z);
            } else {
                for (int i = 0; i < densities.length; i ++) {
                    NoisePos pos = applier.at(i);
                    x[i] = pos.blockX();
                    y[i] = pos.blockY();
                    z[i] = pos.blockZ();
                }
            }
        }
        this.multiMethod.evalMulti(densities, x, y, z, EvalType.from(applier), cache);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        if (this.getClass() != SubCompiledDensityFunction.class) {
            throw new AbstractMethodError();
        }
        if (visitor instanceof IBlendingAwareVisitor blendingAwareVisitor && blendingAwareVisitor.c2me$isBlendingEnabled()) {
            DensityFunction fallback1 = this.getFallback();
            if (fallback1 == null) {
                throw new IllegalStateException("blendingFallback is no more");
            }
            return fallback1.apply(visitor);
        }
        boolean modified = false;
        Supplier<DensityFunction> fallback = this.blendingFallback != null ? Suppliers.memoize(() -> {
            DensityFunction densityFunction = this.blendingFallback.get();
            return densityFunction != null ? densityFunction.apply(visitor) : null;
        }) : null;
        if (fallback != this.blendingFallback) {
            modified = true;
        }
        if (modified) {
            return new SubCompiledDensityFunction(this.singleMethod, this.multiMethod, fallback);
        } else {
            return this;
        }
    }

    @Override
    public double minValue() {
//        DensityFunction fallback = this.getFallback();
//        return fallback != null ? fallback.minValue() : Double.MIN_VALUE;
        return Double.MIN_VALUE;
    }

    @Override
    public double maxValue() {
//        DensityFunction fallback = this.getFallback();
//        return fallback != null ? fallback.maxValue() : Double.MAX_VALUE;
        return Double.MAX_VALUE;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        throw new UnsupportedOperationException();
    }

    protected DensityFunction getFallback() {
        return this.blendingFallback != null ? this.blendingFallback.get() : null;
    }
}
