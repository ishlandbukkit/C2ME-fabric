package com.ishland.c2me.opts.scheduling.mixin.shutdown;

import com.ishland.c2me.opts.scheduling.common.ITryFlushable;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.LockSupport;

@Mixin(ServerWorld.class)
public class MixinServerWorld {

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private ServerChunkManager chunkManager;

    @Shadow @Final private ServerEntityManager<Entity> entityManager;

    @Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerEntityManager;flush()V", shift = At.Shift.BEFORE))
    private void replaceEntityFlushLogic(ProgressListener progressListener, boolean flush, boolean savingDisabled, CallbackInfo ci) {
        while (!((ITryFlushable) this.entityManager).c2me$tryFlush()) {
            this.server.runTask();
            while (this.chunkManager.executeQueuedTasks());
            LockSupport.parkNanos("waiting for completion", 1_000_000);
        }
    }

}
