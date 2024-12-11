package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.base.mixin.access.IServerChunkManager;
import com.ishland.c2me.notickvd.common.ChunkTicketManagerExtension;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Inject(method = "shutdown", at = @At(value = "INVOKE_STRING", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V", args = "ldc=Saving worlds"))
    private void stopNoTickVD(CallbackInfo ci) {
        for (ServerWorld world : this.getWorlds()) {
            ((ChunkTicketManagerExtension) ((IServerChunkManager) world.getChunkManager()).getTicketManager()).c2me$closeNoTickVD();
        }
    }

}
