package com.ishland.c2me.opts.dfc.common.ducks;

import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public interface IFastCacheLike extends DensityFunction {

    public static final long CACHE_MISS_NAN_BITS = 0x7ffddb972d486a4fL;

    double c2me$getCached(int x, int y, int z, EvalType evalType);

    boolean c2me$getCached(double[] res, int[] x, int[] y, int[] z, EvalType evalType);

    void c2me$cache(int x, int y, int z, EvalType evalType, double cached);

    void c2me$cache(double[] res, int[] x, int[] y, int[] z, EvalType evalType);

    boolean c2me$isActualCache();

    DensityFunction c2me$getDelegate();

    // called by generated code
    DensityFunction c2me$withDelegate(DensityFunction delegate);

}
