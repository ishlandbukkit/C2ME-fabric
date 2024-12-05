package com.ishland.c2me.rewrites.chunksystem.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk  extends Chunk {

    @Shadow @Final private World world;

    public MixinWorldChunk(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable ChunkSection[] sectionArray, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biomeRegistry, inhabitedTime, sectionArray, blendingData);
    }

    @WrapOperation(method = "runPostProcessing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;"))
    private BlockEntity syncBlockEntities(WorldChunk instance, BlockPos pos, Operation<BlockEntity> original) {
        if (this.world instanceof ServerWorld serverWorld) {
            BlockEntity blockEntity = original.call(instance, pos);
            if (blockEntity == null) return null;

            List<ServerPlayerEntity> playersWatchingChunk = serverWorld.getChunkManager().chunkLoadingManager.getPlayersWatchingChunk(this.getPos(), false);
            BlockEntityUpdateS2CPacket packet = BlockEntityUpdateS2CPacket.create(blockEntity);
            for (ServerPlayerEntity player : playersWatchingChunk) {
                player.networkHandler.sendPacket(packet);
            }

            return blockEntity;
        } else {
            return original.call(instance, pos);
        }
    }

}
