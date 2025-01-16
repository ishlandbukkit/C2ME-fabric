package com.ishland.c2me.rewrites.chunksystem.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ThrottledChunkTaskScheduler;
import net.minecraft.util.thread.TaskExecutor;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executor;

@Mixin(value = ChunkLevelManager.class, priority = 1051)
public class MixinChunkLevelManager {

    @Dynamic
    @TargetHandler(
            mixin = "com.ishland.vmp.mixins.ticketsystem.ticketpropagator.MixinChunkLevelManager",
            name = "tickTickets"
    )
    @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkHolder;getLevel()I"), require = 0)
    private int fakeLevel(ChunkHolder instance) {
        return Integer.MAX_VALUE;
    }

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/util/thread/TaskExecutor;Ljava/util/concurrent/Executor;I)Lnet/minecraft/server/world/ThrottledChunkTaskScheduler;"))
    private ThrottledChunkTaskScheduler syncPlayerTickets(TaskExecutor<Runnable> executor, Executor dispatchExecutor, int maxConcurrentChunks, Operation<ThrottledChunkTaskScheduler> original) {
        if (Config.syncPlayerTickets) {
            return original.call(executor, (Executor) Runnable::run, maxConcurrentChunks); // improve player ticket consistency
        } else {
            return original.call(executor, dispatchExecutor, maxConcurrentChunks);
        }
    }

}
