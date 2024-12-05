package com.ishland.c2me.base.mixin.access;

import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkLoadingManager.TicketManager.class)
public interface IThreadedAnvilChunkStorageTicketManager {

    @Accessor("field_17443")
    ServerChunkLoadingManager c2me$getSuperClass();

}
