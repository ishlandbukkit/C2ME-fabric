package com.ishland.c2me.opts.dfc.mixin;

import com.ishland.c2me.opts.dfc.common.ducks.IArrayCacheCapable;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DensityFunctionTypes.BinaryOperation.class)
public class MixinDFTBinaryOperation {

    @Shadow @Final private DensityFunctionTypes.BinaryOperationLike.Type type;

    @Shadow @Final private DensityFunction argument1;

    @Shadow @Final private DensityFunction argument2;

    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void wrapFill(double[] densities, DensityFunction.EachApplier applier, CallbackInfo ci) {
        if (this.type == DensityFunctionTypes.BinaryOperationLike.Type.ADD) {
            this.argument1.fill(densities, applier);
            double[] ds;

            ArrayCache arrayCache = applier instanceof IArrayCacheCapable arrayCacheCapable ? arrayCacheCapable.c2me$getArrayCache() : null;

            if (arrayCache != null) {
                ds = arrayCache.getDoubleArray(densities.length, false);
            } else {
                ds = new double[densities.length];
            }

            this.argument2.fill(ds, applier);

            for (int i = 0; i < densities.length; i++) {
                densities[i] += ds[i];
            }

            if (arrayCache != null) {
                arrayCache.recycle(ds);
            }

            ci.cancel();
        }
    }

}
