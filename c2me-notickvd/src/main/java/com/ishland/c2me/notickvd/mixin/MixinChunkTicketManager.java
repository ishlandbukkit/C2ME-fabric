package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.notickvd.common.IChunkTicketManager;
import com.ishland.c2me.notickvd.common.NoOPTickingMap;
import com.ishland.c2me.notickvd.common.NoTickSystem;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
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
public class MixinChunkTicketManager implements IChunkTicketManager {

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
        this.noTickSystem = new NoTickSystem((ChunkTicketManager) (Object) this);
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
        this.noTickSystem.tick(chunkStorage);
    }

    @Inject(method = "addTicket(JLnet/minecraft/server/world/ChunkTicket;)V", at = @At("RETURN"))
    private void onAddTicket(long position, ChunkTicket<?> ticket, CallbackInfo ci) {
//        if (ticket.getType() != ChunkTicketType.UNKNOWN) System.err.printf("Added ticket (%s) at %s\n", ticket, new ChunkPos(position));
        this.noTickSystem.onTicketAdded(position, ticket);
    }

    @Inject(method = "removeTicket(JLnet/minecraft/server/world/ChunkTicket;)V", at = @At("RETURN"))
    private void onRemoveTicket(long pos, ChunkTicket<?> ticket, CallbackInfo ci) {
//        if (ticket.getType() != ChunkTicketType.UNKNOWN) System.err.printf("Removed ticket (%s) at %s\n", ticket, new ChunkPos(pos));
        this.noTickSystem.onTicketRemoved(pos, ticket);
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
        this.noTickSystem.setNoTickViewDistance(viewDistance);
    }

    @Override
    @Unique
    public LongSet getNoTickOnlyChunks() {
        return this.noTickSystem.getNoTickOnlyChunksSnapshot();
    }

    @Override
    @Unique
    public int getNoTickPendingTicketUpdates() {
        return this.noTickSystem.getPendingNoTickTicketUpdatesCount();
    }
}
