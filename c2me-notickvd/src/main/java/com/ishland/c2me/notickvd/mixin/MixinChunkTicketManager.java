package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorageTicketManager;
import com.ishland.c2me.notickvd.common.ChunkTicketManagerExtension;
import com.ishland.c2me.notickvd.common.NoTickSystem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.SimulationDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkTicketManager.class)
public class MixinChunkTicketManager implements ChunkTicketManagerExtension {

    @Shadow private long age;
    @Mutable
    @Shadow @Final private SimulationDistanceLevelPropagator simulationDistanceTracker;
    @Shadow @Final private ChunkTicketManager.NearbyChunkTicketUpdater nearbyChunkTicketUpdater;

    @Unique
    private NoTickSystem noTickSystem;

    @Unique
    private long lastNoTickSystemTick = -1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.noTickSystem = new NoTickSystem(((IThreadedAnvilChunkStorageTicketManager) this).c2me$getSuperClass());
    }

    @Inject(method = "handleChunkEnter", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$DistanceFromNearestPlayerTracker;updateLevel(JIZ)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onHandleChunkEnter(ChunkSectionPos pos, ServerPlayerEntity player, CallbackInfo ci) {
        this.noTickSystem.addPlayerSource(pos.toChunkPos());
    }

    @Inject(method = "handleChunkLeave", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkTicketManager$DistanceFromNearestPlayerTracker;updateLevel(JIZ)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onHandleChunkLeave(ChunkSectionPos pos, ServerPlayerEntity player, CallbackInfo ci) {
        this.noTickSystem.removePlayerSource(pos.toChunkPos());
    }

    @Inject(method = "purgeExpiredTickets", at = @At("RETURN"))
    private void onPurge(CallbackInfo ci) {
        this.noTickSystem.runPurge(this.age);
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void beforeTick(ServerChunkLoadingManager chunkStorage, CallbackInfoReturnable<Boolean> cir) {
        this.noTickSystem.beforeTicketTicks();
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void onTick(ServerChunkLoadingManager chunkStorage, CallbackInfoReturnable<Boolean> cir) {
        this.noTickSystem.afterTicketTicks();
        this.noTickSystem.tick();
    }

    /**
     * @author ishland
     * @reason remap setSimulationDistance to the normal one
     */
    @Overwrite
    public void setSimulationDistance(int i) {
        this.nearbyChunkTicketUpdater.setWatchDistance(i);
    }

    /**
     * @author ishland
     * @reason remap setWatchDistance to no-tick one
     */
    @Overwrite
    public void setWatchDistance(int viewDistance) {
        this.noTickSystem.setNoTickViewDistance(viewDistance + 1);
    }

    @Override
    @Unique
    public long c2me$getPendingLoadsCount() {
        return this.noTickSystem.getPendingLoadsCount();
    }

    @Override
    public void c2me$closeNoTickVD() {
        this.noTickSystem.close();
    }
}
