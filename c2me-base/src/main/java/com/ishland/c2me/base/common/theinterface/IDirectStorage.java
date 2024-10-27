package com.ishland.c2me.base.common.theinterface;

import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface IDirectStorage {

    public CompletableFuture<Void> setRawChunkData(ChunkPos pos, byte[] data);

    public CompletableFuture<Void> setRawChunkData(ChunkPos pos, CompletableFuture<byte[]> data);

}
