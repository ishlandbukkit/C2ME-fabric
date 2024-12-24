package com.ishland.c2me.rewrites.chunksystem.mixin;

import com.ishland.c2me.base.common.threadstate.ThreadInstrumentation;
import com.ishland.c2me.base.common.threadstate.ThreadState;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IChunkSystemAccess;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "shutdown", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;runTasksTillTickEnd()V"))
    private void onTaskWait(CallbackInfo ci, @Share("c2me:shutdownLastPrint") LocalLongRef lastPrint, @Share("c2me:shutdownFirstPrint") LocalLongRef firstPrint) {
        long now = System.nanoTime();
        if (firstPrint.get() == 0) {
            firstPrint.set(now);
        }
        if (now - lastPrint.get() > 2_000_000_000L) {
            if (now - firstPrint.get() > 10_000_000_000L) {
                StringBuilder builder = new StringBuilder();
                builder.append('\n');
                boolean haveState = false;
                for (Map.Entry<Thread, ThreadState> entry : ThreadInstrumentation.entrySet()) {
                    String msg = ThreadInstrumentation.printState(entry.getKey().getName(), entry.getKey().threadId(), entry.getValue());
                    if (msg != null) {
                        builder.append(msg);
                        haveState = true;
                    }
                }
                if (haveState) {
                    LOGGER.info(builder.toString());
                }
            }

            for (ServerWorld world : this.getWorlds()) {
                final int itemCount = ((IChunkSystemAccess) world.getChunkManager().chunkLoadingManager).c2me$getTheChunkSystem().itemCount();
                if (itemCount > 0) {
                    LOGGER.info("{}/{}: waiting for {} chunks to unload", world, world.getRegistryKey().getValue(), itemCount);
                }
            }
            lastPrint.set(now);
        }
    }

}
