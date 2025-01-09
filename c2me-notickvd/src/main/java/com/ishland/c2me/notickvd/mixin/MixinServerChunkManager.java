package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.base.common.theinterface.IFastChunkHolder;
import com.ishland.c2me.base.common.util.FilteringIterable;
import com.ishland.c2me.base.mixin.access.IChunkTicketManager;
import com.ishland.c2me.base.mixin.access.ISimulationDistanceLevelPropagator;
import com.ishland.c2me.notickvd.common.ChunkTicketManagerExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkManager.class)
public class MixinServerChunkManager {

    @Shadow @Final private ChunkTicketManager ticketManager;

    @WrapOperation(method = "tickChunks(Lnet/minecraft/util/profiler/Profiler;JLjava/util/List;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;iterateEntities()Ljava/lang/Iterable;"))
    private Iterable<Entity> redirectIterateEntities(ServerWorld serverWorld, Operation<Iterable<Entity>> op) {
        Long2ByteMap trackedChunks = ((ISimulationDistanceLevelPropagator) ((IChunkTicketManager) this.ticketManager).getSimulationDistanceTracker()).getLevels();
        return new FilteringIterable<>(op.call(serverWorld), entity -> trackedChunks.containsKey(entity.getChunkPos().toLong()));
    }

    @Redirect(method = "broadcastUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkHolder;getWorldChunk()Lnet/minecraft/world/chunk/WorldChunk;"))
    private WorldChunk broadcastBorderChunks(ChunkHolder instance) {
        if (instance instanceof IFastChunkHolder fastChunkHolder) {
            return fastChunkHolder.c2me$immediateWorldChunk();
        } else {
            return instance.getAccessibleFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).orElse(null);
        }
    }

}
