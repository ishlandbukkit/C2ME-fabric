package com.ishland.c2me.base.mixin.access;

import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkNoiseSampler.DensityInterpolator.class)
public interface IChunkNoiseSamplerDensityInterpolator {

    @Invoker
    void invokeInterpolateX(double deltaX);

    @Invoker
    void invokeInterpolateY(double deltaY);

    @Invoker
    void invokeInterpolateZ(double deltaZ);

    @Invoker
    void invokeSwapBuffers();

}
