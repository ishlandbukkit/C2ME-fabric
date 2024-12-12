package com.ishland.c2me.opts.dfc.common.gen;

import com.google.common.base.Suppliers;
import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IArrayCacheCapable;
import com.ishland.c2me.opts.dfc.common.ducks.IBlendingAwareVisitor;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import com.ishland.c2me.opts.dfc.common.vif.EachApplierVanillaInterface;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Supplier;

public class CompiledDensityFunction extends SubCompiledDensityFunction {

    private final CompiledEntry compiledEntry;

    public CompiledDensityFunction(CompiledEntry compiledEntry, DensityFunction blendingFallback) {
        super(compiledEntry, compiledEntry, blendingFallback);
        this.compiledEntry = Objects.requireNonNull(compiledEntry);
    }

    private CompiledDensityFunction(CompiledEntry compiledEntry, Supplier<DensityFunction> blendingFallback) {
        super(compiledEntry, compiledEntry, blendingFallback);
        this.compiledEntry = Objects.requireNonNull(compiledEntry);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        if (visitor instanceof IBlendingAwareVisitor blendingAwareVisitor && blendingAwareVisitor.c2me$isBlendingEnabled()) {
            DensityFunction fallback1 = this.getFallback();
            if (fallback1 == null) {
                throw new IllegalStateException("blendingFallback is no more");
            }
            return fallback1.apply(visitor);
        }
        boolean modified = false;
        Object[] args = this.compiledEntry.getArgs();
        for (int i = 0; i < args.length; i ++) {
            Object next = args[i];
            if (next instanceof DensityFunction df) {
                if (!(df instanceof IFastCacheLike)) {
                    DensityFunction applied = df.apply(visitor);
                    if (df != applied) {
                        args[i] = applied;
                        modified = true;
                    }
                }
            }
            if (next instanceof Noise noise) {
                Noise applied = visitor.apply(noise);
                if (noise != applied) {
                    args[i] = applied;
                    modified = true;
                }
            }
        }

        for (int i = 0; i < args.length; i ++) {
            Object next = args[i];
            if (next instanceof IFastCacheLike cacheLike) {
                DensityFunction applied = visitor.apply(cacheLike);
                if (applied == cacheLike.c2me$getDelegate()) {
                    args[i] = null; // cache removed
                    modified = true;
                } else if (applied instanceof IFastCacheLike newCacheLike) {
                    args[i] = newCacheLike;
                    modified = true;
                } else {
                    throw new UnsupportedOperationException("Unsupported transformation on Wrapping node");
                }
            }
        }

        Supplier<DensityFunction> fallback = this.blendingFallback != null ? Suppliers.memoize(() -> {
            DensityFunction densityFunction = this.blendingFallback.get();
            return densityFunction != null ? densityFunction.apply(visitor) : null;
        }) : null;
        if (fallback != this.blendingFallback) {
            modified = true;
        }
        if (modified) {
            return new CompiledDensityFunction(this.compiledEntry.newInstance(args), fallback);
        } else {
            return this;
        }
    }

}
