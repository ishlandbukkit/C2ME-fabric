package com.ishland.c2me.opts.allocs.mixin.predicates;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.blockpredicate.OffsetPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OffsetPredicate.class)
public abstract class MixinOffsetPredicate {

    @Shadow protected abstract boolean test(BlockState state);

    @Shadow @Final protected Vec3i offset;

    /**
     * @author ishland
     * @reason reduce allocs
     */
    @Overwrite
    public final boolean test(StructureWorldAccess structureWorldAccess, BlockPos blockPos) {
        if (blockPos instanceof BlockPos.Mutable mutable) {
            int savedX = mutable.getX();
            int savedY = mutable.getY();
            int savedZ = mutable.getZ();
            boolean res = this.test(structureWorldAccess.getBlockState(mutable.set(savedX + this.offset.getX(), savedY + this.offset.getY(), savedZ + this.offset.getZ())));
            mutable.set(savedX, savedY, savedZ);
            return res;
        } else {
            return this.test(structureWorldAccess.getBlockState(blockPos.add(this.offset)));
        }
    }

}
