package com.ishland.c2me.rewrites.chunksystem.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.ishland.c2me.base.mixin.access.ITACSTicketManager;
import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(value = ChunkTicketManager.class, priority = 1051)
public class MixinChunkTicketManager {

    @Dynamic
    @TargetHandler(
            mixin = "com.ishland.vmp.mixins.ticketsystem.ticketpropagator.MixinChunkTicketManager",
            name = "tickTickets"
    )
    @Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkHolder;getLevel()I"), require = 0)
    private int fakeLevel(ChunkHolder instance) {
        return Integer.MAX_VALUE;
    }

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Ljava/util/List;Ljava/util/concurrent/Executor;I)Lnet/minecraft/server/world/ChunkTaskPrioritySystem;"))
    private ChunkTaskPrioritySystem syncPlayerTickets(List actors, Executor executor, int maxQueues, Operation<ChunkTaskPrioritySystem> original) {
        if (Config.syncPlayerTickets) {
            return original.call(actors, (Executor) Runnable::run, maxQueues); // improve player ticket consistency
        } else {
            return original.call(actors, executor, maxQueues);
        }
    }

}
