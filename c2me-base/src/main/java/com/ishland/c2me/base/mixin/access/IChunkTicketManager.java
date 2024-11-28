package com.ishland.c2me.base.mixin.access;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.world.SimulationDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkTicketManager.class)
public interface IChunkTicketManager {

    @Invoker
    void invokeSetWatchDistance(int viewDistance);

    @Accessor
    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersByChunkPos();

    @Accessor
    Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> getTicketsByPosition();

    @Accessor
    ChunkTicketManager.NearbyChunkTicketUpdater getNearbyChunkTicketUpdater();

    @Accessor
    SimulationDistanceLevelPropagator getSimulationDistanceTracker();

}
