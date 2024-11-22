package com.ishland.c2me.notickvd.common.iterators;

import net.minecraft.util.math.ChunkPos;

import java.util.Iterator;

public interface ChunkIterator extends Iterator<ChunkPos> {

    long remaining();

    long total();

    int originX();

    int originZ();

    int radius();

    default boolean isInRange(int x, int z) {
        int originX = this.originX();
        int originZ = this.originZ();
        int radius = this.radius();
        return x >= originX - radius && x <= originX + radius &&
                z >= originZ - radius && z <= originZ + radius;
    }

}
