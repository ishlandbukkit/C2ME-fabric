package com.ishland.c2me.opts.allocs.mixin.predicates;

import com.ishland.c2me.opts.allocs.common.ducks.CombinedBlockPredicateExtension;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.blockpredicate.CombinedBlockPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(CombinedBlockPredicate.class)
public class MixinCombinedBlockPredicate implements CombinedBlockPredicateExtension {

    @Shadow @Final protected List<BlockPredicate> predicates;

    @Unique
    private BlockPredicate[] c2me$predicatesArray;

    @Override
    public BlockPredicate[] c2me$getPredicatesArray() {
        BlockPredicate[] predicateArray = this.c2me$predicatesArray;
        if (predicateArray == null) {
            this.c2me$predicatesArray = predicateArray = this.predicates.toArray(BlockPredicate[]::new);
        }
        return predicateArray;
    }

}
