package com.ishland.c2me.notickvd.mixin;

import com.ishland.c2me.base.mixin.access.IServerChunkManager;
import com.ishland.c2me.notickvd.common.Config;
import com.ishland.c2me.notickvd.common.ChunkTicketManagerExtension;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkLoadingManager.class)
public abstract class MixinThreadedAnvilChunkStorage {

    @Shadow public abstract List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge);

    @Shadow @Final private ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;

    @Shadow protected abstract void sendToPlayers(ChunkHolder holder, WorldChunk chunk);

    @ModifyArg(method = "setViewDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"), index = 2)
    private int modifyMaxVD(int max) {
        return Config.maxViewDistance;
    }

    @Redirect(method = "getPostProcessedChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkHolder;getPostProcessedChunk()Lnet/minecraft/world/chunk/WorldChunk;"))
    private WorldChunk redirectSendWatchPacketsGetWorldChunk(ChunkHolder chunkHolder) {
        return chunkHolder.getAccessibleFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).orElse(null);
    }

    // TODO ensureChunkCorrectness
    @Inject(method = "makeChunkAccessible", at = @At("RETURN"))
    private void onMakeChunkAccessible(ChunkHolder chunkHolder, CallbackInfoReturnable<CompletableFuture<OptionalChunk<WorldChunk>>> cir) {
        cir.getReturnValue().thenAccept(either -> either.ifPresent(worldChunk -> {
            if (Config.compatibilityMode) {
                this.mainThreadExecutor.send(() -> this.sendToPlayers(chunkHolder, worldChunk));
            } else {
                this.sendToPlayers(chunkHolder, worldChunk);
            }
        }));
    }

    // private synthetic method_17243(Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/server/network/ServerPlayerEntity;)V
//    /**
//     * @author ishland
//     * @reason dont send chunks twice
//     */
//    @Overwrite
//    private void method_17243(MutableObject<ChunkDataS2CPacket> mutableObject, WorldChunk worldChunk, ServerPlayerEntity player) {
//        if (Config.ensureChunkCorrectness && NoTickChunkSendingInterceptor.onChunkSending(player, worldChunk.getPos().toLong()))
//            this.sendChunkDataPackets(player, mutableObject, worldChunk);
//    }

    @WrapWithCondition(method = "method_61257", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;sendToPlayers(Lnet/minecraft/server/world/ChunkHolder;Lnet/minecraft/world/chunk/WorldChunk;)V"))
    private boolean controlDuplicateChunkSending(ServerChunkLoadingManager instance, ChunkHolder chunkHolder, WorldChunk chunk) {
        return Config.ensureChunkCorrectness;
    }

    @WrapWithCondition(method = "method_53687", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;sendToPlayers(Lnet/minecraft/server/world/ChunkHolder;Lnet/minecraft/world/chunk/WorldChunk;)V"))
    private boolean controlDuplicateChunkSending1(ServerChunkLoadingManager instance, ChunkHolder chunkHolder, WorldChunk chunk) {
        return Config.ensureChunkCorrectness; // TODO config set to false unfixes MC-264947
    }

}
