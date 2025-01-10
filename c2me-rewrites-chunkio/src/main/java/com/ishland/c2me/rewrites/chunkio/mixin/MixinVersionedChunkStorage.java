package com.ishland.c2me.rewrites.chunkio.mixin;

import com.ishland.c2me.base.common.scheduler.IVanillaChunkManager;
import com.ishland.c2me.rewrites.chunkio.common.C2MEStorageVanillaInterface;
import net.minecraft.world.storage.StorageIoWorker;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Mixin(VersionedChunkStorage.class)
public class MixinVersionedChunkStorage {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/world/storage/StorageKey;Ljava/nio/file/Path;Z)Lnet/minecraft/world/storage/StorageIoWorker;"))
    private StorageIoWorker redirectStorageIoWorker(StorageKey arg, Path path, boolean bl) {
        if (this instanceof IVanillaChunkManager vanillaChunkManager) {
            return new C2MEStorageVanillaInterface(arg, path, bl, pos -> vanillaChunkManager.c2me$getSchedulingManager().positionedExecutor(pos));
        } else {
            return new C2MEStorageVanillaInterface(arg, path, bl, null);
        }
    }

}
