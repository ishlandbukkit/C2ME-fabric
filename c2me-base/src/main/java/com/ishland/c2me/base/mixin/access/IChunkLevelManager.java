package com.ishland.c2me.base.mixin.access;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.SimulationDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkLevelManager.class)
public interface IChunkLevelManager {

    @Invoker
    void invokeSetWatchDistance(int viewDistance);

    @Accessor
    Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersByChunkPos();

    @Accessor
    ChunkLevelManager.NearbyChunkTicketUpdater getNearbyChunkTicketUpdater();

    @Accessor
    SimulationDistanceLevelPropagator getSimulationDistanceLevelPropagator();

    @Accessor
    ChunkTicketManager getTicketManager();

}
