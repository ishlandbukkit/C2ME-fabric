package com.ishland.c2me.opts.worldgen.vanilla.mixin.tlcache;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Block.class)
public class MixinBlock {

    @Unique
    private static final ThreadLocal<Object2BooleanLinkedOpenHashMap<VoxelShape>> c2me$full_cube_shape_cache_tl =
            ThreadLocal.withInitial(() -> new Object2BooleanLinkedOpenHashMap<>(256, 0.25F) {
                @Override
                protected void rehash(int newN) {
                    if (newN > n) {
                        super.rehash(newN);
                    }
                }
            });

    /**
     * @author ishland
     * @reason use ThreadLocal cache
     */
    @Overwrite
    public static boolean isShapeFullCube(VoxelShape shape) {
        Object2BooleanLinkedOpenHashMap<VoxelShape> map = c2me$full_cube_shape_cache_tl.get();
        if (map.containsKey(shape)) {
            return map.getAndMoveToFirst(shape);
        } else {
            boolean uncached = !VoxelShapes.matchesAnywhere(VoxelShapes.fullCube(), shape, BooleanBiFunction.NOT_SAME);
            if (map.size() >= 256) {
                map.removeLastBoolean();
            }
            map.putAndMoveToFirst(shape, uncached);
            return uncached;
        }
    }

}
