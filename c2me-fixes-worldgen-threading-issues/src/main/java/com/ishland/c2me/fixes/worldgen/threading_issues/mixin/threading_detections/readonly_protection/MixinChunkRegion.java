package com.ishland.c2me.fixes.worldgen.threading_issues.mixin.threading_detections.readonly_protection;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.EmptyTickSchedulers;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRegion.class)
public abstract class MixinChunkRegion {

    @Shadow public abstract boolean isValidForSetBlock(BlockPos pos);

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ChunkGenerationStep generationStep;

    @Shadow @Final private Chunk centerPos;

    @WrapMethod(method = "method_14340")
    private BasicTickScheduler<BlockPos> wrapBlockScheduledTick(BlockPos pos, Operation<BasicTickScheduler<BlockPos>> original) {
        if (this.isValidForSetBlock(pos)) {
            return original.call(pos);
        } else {
            LOGGER.warn("Detected block scheduled tick access in a far chunk {}, pos: {}, status: {}, currently generating: {}", new ChunkPos(pos), pos, this.generationStep.targetStatus(), this.centerPos.getPos());
            return EmptyTickSchedulers.getReadOnlyTickScheduler();
        }
    }

    @WrapMethod(method = "method_14337")
    private BasicTickScheduler<Fluid> wrapFluidScheduledTick(BlockPos pos, Operation<BasicTickScheduler<Fluid>> original) {
        if (this.isValidForSetBlock(pos)) {
            return original.call(pos);
        } else {
            LOGGER.warn("Detected fluid scheduled tick access in a far chunk {}, pos: {}, status: {}, currently generating: {}", new ChunkPos(pos), pos, this.generationStep.targetStatus(), this.centerPos.getPos());
            return EmptyTickSchedulers.getReadOnlyTickScheduler();
        }
    }

}
