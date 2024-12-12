package com.ishland.c2me.opts.dfc.common.gen;

import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;

public interface CompiledEntry extends ISingleMethod, IMultiMethod {

    double evalSingle(int x, int y, int z, EvalType type);

    void evalMulti(double[] res, int[] x, int[] y, int[] z, EvalType type, ArrayCache arrayCache);

    CompiledEntry newInstance(Object[] args);

    Object[] getArgs();

}
