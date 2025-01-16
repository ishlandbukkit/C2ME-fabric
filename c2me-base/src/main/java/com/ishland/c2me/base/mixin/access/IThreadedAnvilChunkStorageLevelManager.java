package com.ishland.c2me.base.mixin.access;

import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkLoadingManager.LevelManager.class)
public interface IThreadedAnvilChunkStorageLevelManager {

    @Accessor("field_17443")
    ServerChunkLoadingManager c2me$getSuperClass();

}
