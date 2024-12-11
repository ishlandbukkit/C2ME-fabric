package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.notickvd.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerBlockTicking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ServerBlockTicking.class, remap = false)
public class MixinServerBlockTicking {

    @Inject(method = "sendChunkToPlayer(Lcom/ishland/c2me/rewrites/chunksystem/common/ChunkLoadingContext;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private static void preventChunkSending(CallbackInfo ci) {
        if (!Config.ensureChunkCorrectness) {
            ci.cancel();
        }
    }

}
