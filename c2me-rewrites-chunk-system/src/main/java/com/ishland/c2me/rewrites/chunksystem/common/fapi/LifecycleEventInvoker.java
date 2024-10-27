package com.ishland.c2me.rewrites.chunksystem.common.fapi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

public class LifecycleEventInvoker {

    public static void invokeChunkLoaded(ServerWorld world, WorldChunk chunk, boolean newChunk) {
        try {
            ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(world, chunk);
            if (newChunk) {
                ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(world, chunk);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void invokeChunkUnload(ServerWorld world, WorldChunk chunk) {
        try {
            ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(world, chunk);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
