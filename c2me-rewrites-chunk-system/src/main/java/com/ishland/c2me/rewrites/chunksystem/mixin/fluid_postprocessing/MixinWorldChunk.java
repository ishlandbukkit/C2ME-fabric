package com.ishland.c2me.rewrites.chunksystem.mixin.fluid_postprocessing;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldChunk.class)
public class MixinWorldChunk {

    @Redirect(method = "runPostProcessing", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;onScheduledTick(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    private void redirectFluidScheduledTick(FluidState instance, ServerWorld world, BlockPos pos, BlockState state) {
        world.scheduleFluidTick(pos, instance.getFluid(), 1);
    }

}
