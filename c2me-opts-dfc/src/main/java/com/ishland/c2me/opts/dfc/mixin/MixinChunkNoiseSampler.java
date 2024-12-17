package com.ishland.c2me.opts.dfc.mixin;

import com.ishland.c2me.base.mixin.access.IChunkNoiseSamplerDensityInterpolator;
import com.ishland.c2me.opts.dfc.common.ducks.IArrayCacheCapable;
import com.ishland.c2me.opts.dfc.common.ducks.ICoordinatesFilling;
import com.ishland.c2me.opts.dfc.common.ducks.IPreloadedCoordinates;
import com.ishland.c2me.opts.dfc.common.gen.DelegatingBlendingAwareVisitor;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChunkNoiseSampler.class)
public abstract class MixinChunkNoiseSampler implements IArrayCacheCapable, ICoordinatesFilling, IPreloadedCoordinates {

    @Shadow @Final private int verticalCellBlockCount;
    @Shadow @Final private int horizontalCellBlockCount;
    @Shadow private int startBlockY;
    @Shadow private int startBlockX;
    @Shadow private int startBlockZ;
    @Shadow private boolean isInInterpolationLoop;

    @Unique
    private int c2me$oldStartBlockX = Integer.MAX_VALUE;
    @Unique
    private int c2me$oldStartBlockY = Integer.MAX_VALUE;
    @Unique
    private int c2me$oldStartBlockZ = Integer.MAX_VALUE;
    @Unique
    private int[] c2me$cachedXArray;
    @Unique
    private int[] c2me$cachedYArray;
    @Unique
    private int[] c2me$cachedZArray;

    @Unique
    ChunkNoiseSampler.DensityInterpolator[] c2me$interpolatorsArray;

    @Shadow public abstract Blender getBlender();

    @Shadow @Final private List<ChunkNoiseSampler.DensityInterpolator> interpolators;
    @Shadow private int cellBlockY;
    @Shadow private int cellBlockX;
    @Shadow private int cellBlockZ;
    @Shadow private long sampleUniqueIndex;
    private final ArrayCache c2me$arrayCache = new ArrayCache();

    @Override
    public ArrayCache c2me$getArrayCache() {
        return this.c2me$arrayCache != null ? this.c2me$arrayCache : new ArrayCache();
    }

    @Override
    public void c2me$fillCoordinates(int[] x, int[] y, int[] z) {
        int index = 0;
        for (int i = this.verticalCellBlockCount - 1; i >= 0; i--) {
            int blockY = this.startBlockY + i;
            for (int j = 0; j < this.horizontalCellBlockCount; j++) {
                int blockX = this.startBlockX + j;
                for (int k = 0; k < this.horizontalCellBlockCount; k++) {
                    int blockZ = this.startBlockZ + k;

                    x[index] = blockX;
                    y[index] = blockY;
                    z[index] = blockZ;

                    index++;
                }
            }
        }
    }

    @Unique
    private void c2me$reloadCachedArrays() {
        if (this.c2me$cachedXArray == null || this.c2me$cachedYArray == null || this.c2me$cachedZArray == null) {
            int length = this.horizontalCellBlockCount * this.horizontalCellBlockCount * this.verticalCellBlockCount;
            ArrayCache arrayCache = this.c2me$getArrayCache();
            this.c2me$cachedXArray = arrayCache.getIntArray(length, false);
            this.c2me$cachedYArray = arrayCache.getIntArray(length, false);
            this.c2me$cachedZArray = arrayCache.getIntArray(length, false);
        }
        if (this.c2me$oldStartBlockX != this.startBlockX || this.c2me$oldStartBlockY != this.startBlockY || this.c2me$oldStartBlockZ != this.startBlockZ) {
            this.c2me$fillCoordinates(this.c2me$cachedXArray, this.c2me$cachedYArray, this.c2me$cachedZArray);
            this.c2me$oldStartBlockX = this.startBlockX;
            this.c2me$oldStartBlockY = this.startBlockY;
            this.c2me$oldStartBlockZ = this.startBlockZ;
        }
    }

    @Override
    public int[] c2me$getXArray() {
        this.c2me$reloadCachedArrays();
        return this.c2me$cachedXArray;
    }

    @Override
    public int[] c2me$getYArray() {
        this.c2me$reloadCachedArrays();
        return this.c2me$cachedYArray;
    }

    @Override
    public int[] c2me$getZArray() {
        this.c2me$reloadCachedArrays();
        return this.c2me$cachedZArray;
    }

    @Inject(method = "getActualDensityFunctionImpl", at = @At("HEAD"))
    private void protectInterpolationLoop(DensityFunction function, CallbackInfoReturnable<DensityFunction> cir) {
        if (this.isInInterpolationLoop && function instanceof DensityFunctionTypes.Wrapping) {
            throw new IllegalStateException("Cannot create more wrapping during interpolation loop");
        }
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/noise/NoiseRouter;apply(Lnet/minecraft/world/gen/densityfunction/DensityFunction$DensityFunctionVisitor;)Lnet/minecraft/world/gen/noise/NoiseRouter;"))
    private DensityFunction.DensityFunctionVisitor modifyVisitor1(DensityFunction.DensityFunctionVisitor visitor) {
        return c2me$getDelegatingBlendingAwareVisitor(visitor);
    }

    @Unique
    private @NotNull DelegatingBlendingAwareVisitor c2me$getDelegatingBlendingAwareVisitor(DensityFunction.DensityFunctionVisitor visitor) {
        return new DelegatingBlendingAwareVisitor(visitor, this.getBlender() != Blender.getNoBlending());
    }

    @ModifyArg(method = {"<init>", "createMultiNoiseSampler"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/densityfunction/DensityFunction;apply(Lnet/minecraft/world/gen/densityfunction/DensityFunction$DensityFunctionVisitor;)Lnet/minecraft/world/gen/densityfunction/DensityFunction;"), require = 7, expect = 7)
    private DensityFunction.DensityFunctionVisitor modifyVisitor2(DensityFunction.DensityFunctionVisitor visitor) {
        return c2me$getDelegatingBlendingAwareVisitor(visitor);
    }

    private ChunkNoiseSampler.DensityInterpolator[] c2me$initInterpolatorsArray() {
        ChunkNoiseSampler.DensityInterpolator[] interpolatorsArray = this.c2me$interpolatorsArray;
        if (interpolatorsArray == null) {
            interpolatorsArray = this.c2me$interpolatorsArray = this.interpolators.toArray(ChunkNoiseSampler.DensityInterpolator[]::new);
        }
        return interpolatorsArray;
    }

    /**
     * @author ishland
     * @reason array iteration
     */
    @Overwrite
    public void interpolateY(int blockY, double deltaY) {
        this.cellBlockY = blockY - this.startBlockY;

        for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.c2me$initInterpolatorsArray()) {
            ((IChunkNoiseSamplerDensityInterpolator) densityInterpolator).invokeInterpolateY(deltaY);
        }
    }

    /**
     * @author ishland
     * @reason array iteration
     */
    @Overwrite
    public void interpolateX(int blockX, double deltaX) {
        this.cellBlockX = blockX - this.startBlockX;

        for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.c2me$initInterpolatorsArray()) {
            ((IChunkNoiseSamplerDensityInterpolator) densityInterpolator).invokeInterpolateX(deltaX);
        }
    }

    /**
     * @author ishland
     * @reason array iteration
     */
    @Overwrite
    public void interpolateZ(int blockZ, double deltaZ) {
        this.cellBlockZ = blockZ - this.startBlockZ;
        this.sampleUniqueIndex++;

        for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.c2me$initInterpolatorsArray()) {
            ((IChunkNoiseSamplerDensityInterpolator) densityInterpolator).invokeInterpolateZ(deltaZ);
        }
    }

    /**
     * @author ishland
     * @reason array iteration
     */
    @Overwrite
    public void swapBuffers() {
        for (ChunkNoiseSampler.DensityInterpolator densityInterpolator : this.c2me$initInterpolatorsArray()) {
            ((IChunkNoiseSamplerDensityInterpolator) densityInterpolator).invokeSwapBuffers();
        }
    }

}
