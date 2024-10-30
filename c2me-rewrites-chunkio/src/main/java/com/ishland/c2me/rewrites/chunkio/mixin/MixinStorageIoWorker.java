package com.ishland.c2me.rewrites.chunkio.mixin;

import com.ishland.c2me.rewrites.chunkio.common.C2MEStorageVanillaInterface;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.PrioritizedConsecutiveExecutor;
import net.minecraft.world.storage.RegionBasedStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(StorageIoWorker.class)
public class MixinStorageIoWorker {

    @Mutable
    @Shadow @Final private SequencedMap<ChunkPos, StorageIoWorker.Result> results;

    @Mutable
    @Shadow @Final private RegionBasedStorage storage;

    @Mutable
    @Shadow @Final private AtomicBoolean closed;

    @Mutable
    @Shadow @Final private PrioritizedConsecutiveExecutor executor;

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if ((Object) this instanceof C2MEStorageVanillaInterface) {
            // fail-fast incompatibility
            this.results = null;
            this.storage = null;
            this.closed = null;
            this.executor = null;
        }
    }

}
