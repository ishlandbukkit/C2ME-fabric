package com.ishland.c2me.rewrites.chunksystem.mixin;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.TicketDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TicketDistanceLevelPropagator.class)
public class MixinChunkTicketManagerTicketDistanceLevelPropagator {

    @Shadow @Final private static int UNLOADED;

    private final Long2IntMap levels = new Long2IntOpenHashMap();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(CallbackInfo ci) {
        this.levels.defaultReturnValue(UNLOADED + 1);
    }

    /**
     * @author ishland
     * @reason use internal levels
     */
    @Overwrite
    public int getLevel(long id) {
        return this.levels.get(id);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void preSetLevel(long id, int level, CallbackInfo ci) {
        if (level >= UNLOADED) {
            this.levels.remove(id);
        } else {
            this.levels.put(id, level);
        }
    }

    @Redirect(method = "setLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkHolder;getLevel()I"))
    private int fakeLevel(ChunkHolder instance) {
        return Integer.MAX_VALUE;
    }

}
