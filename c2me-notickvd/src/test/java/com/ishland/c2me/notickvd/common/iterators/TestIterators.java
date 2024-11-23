package com.ishland.c2me.notickvd.common.iterators;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestIterators {

    private static final int RADIUS = 256;

    @BeforeAll
    public static void setup() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    public void testSpiral() {
        SpiralIterator iterator = new SpiralIterator(0, 0, RADIUS);
        ObjectSet<ChunkPos> chunks = new ObjectLinkedOpenHashSet<>((int) iterator.total());
        while (iterator.hasNext()) {
            chunks.add(iterator.next());
        }
        if (iterator.total() != chunks.size()) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (!chunks.contains(new ChunkPos(x, z))) {
                        System.out.println(String.format("%d %d", x, z));
                    }
                }
            }
            Assertions.fail();
        }
        for (ChunkPos chunk : chunks) {
            Assertions.assertTrue(iterator.isInRange(chunk.x, chunk.z));
        }
    }

    @AfterAll
    public static void teardown() {
        Util.shutdownExecutors();
        SkullBlockEntity.clearServices();
    }

}
