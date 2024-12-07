package com.ishland.c2me.rewrites.chunksystem.mixin;

import com.ishland.c2me.rewrites.chunksystem.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.ducks.IPOIUnloading;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(SerializingRegionBasedStorage.class)
public abstract class MixinSerializingRegionBasedStorage<R> implements IPOIUnloading {

    @Shadow @Final protected HeightLimitView world;

    @Shadow @Final private Long2ObjectMap<Optional<R>> loadedElements;

    @Shadow public abstract void saveChunk(ChunkPos pos);

    @Override
    public void c2me$unloadPoi(ChunkPos pos) {
        if (!Config.allowPOIUnloading) return;

        if (!this.c2me$shouldUnloadPoi(pos)) {
            return;
        }

        this.saveChunk(pos);
        for (int i = this.world.getBottomSectionCoord(); i <= this.world.getTopSectionCoord(); i++) {
            this.loadedElements.remove(ChunkSectionPos.asLong(pos.x, i, pos.z));
        }
    }

}
