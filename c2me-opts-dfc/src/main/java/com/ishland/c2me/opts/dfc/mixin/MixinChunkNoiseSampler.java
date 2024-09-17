package com.ishland.c2me.opts.dfc.mixin;

import com.ishland.c2me.opts.dfc.common.ducks.IArrayCacheCapable;
import com.ishland.c2me.opts.dfc.common.ducks.ICoordinatesFilling;
import com.ishland.c2me.opts.dfc.common.util.ArrayCache;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkNoiseSampler.class)
public class MixinChunkNoiseSampler implements IArrayCacheCapable, ICoordinatesFilling {

    @Shadow @Final private int verticalCellBlockCount;
    @Shadow @Final private int horizontalCellBlockCount;
    @Shadow private int startBlockY;
    @Shadow private int startBlockX;
    @Shadow private int startBlockZ;
    private final ArrayCache c2me$arrayCache = new ArrayCache();

    @Override
    public ArrayCache c2me$getArrayCache() {
        return this.c2me$arrayCache != null ? this.c2me$arrayCache : new ArrayCache();
    }

    @Override
    public void c2me$fillCoordinates(int[] x, int[] y, int[] z) {
        int index = 0;
        for (int i = this.verticalCellBlockCount - 1; i >= 0; i--) {
            int blockY = this.startBlockY + i;
            for (int j = 0; j < this.horizontalCellBlockCount; j++) {
                int blockX = this.startBlockX + j;
                for (int k = 0; k < this.horizontalCellBlockCount; k++) {
                    int blockZ = this.startBlockZ + k;

                    x[index] = blockX;
                    y[index] = blockY;
                    z[index] = blockZ;

                    index++;
                }
            }
        }
    }
}