package com.ishland.c2me.rewrites.chunkio.mixin;

import com.ishland.c2me.rewrites.chunkio.common.C2MEStorageVanillaInterface;
import net.minecraft.world.storage.RecreatedChunkStorage;
import net.minecraft.world.storage.StorageIoWorker;
import net.minecraft.world.storage.StorageKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Mixin(RecreatedChunkStorage.class)
public class MixinRecreatedChunkStorage {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/world/storage/StorageKey;Ljava/nio/file/Path;Z)Lnet/minecraft/world/storage/StorageIoWorker;"))
    private StorageIoWorker redirectStorageIoWorker(StorageKey arg, Path path, boolean bl) {
        return new C2MEStorageVanillaInterface(arg, path, bl, null);
    }

}
