package com.ishland.c2me.base.mixin.access;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.IntSupplier;

@Mixin(ServerLightingProvider.class)
public interface IServerLightingProvider {

    @Invoker
    void invokeUpdateChunkStatus(ChunkPos pos);

}
