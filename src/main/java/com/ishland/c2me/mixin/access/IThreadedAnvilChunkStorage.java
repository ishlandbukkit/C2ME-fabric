package com.ishland.c2me.mixin.access;

import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface IThreadedAnvilChunkStorage {

    @Accessor
    int getWatchDistance();

}