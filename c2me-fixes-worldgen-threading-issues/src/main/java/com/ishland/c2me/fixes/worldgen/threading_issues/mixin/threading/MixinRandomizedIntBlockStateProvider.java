package com.ishland.c2me.fixes.worldgen.threading_issues.mixin.threading;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.RandomizedIntBlockStateProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RandomizedIntBlockStateProvider.class)
public abstract class MixinRandomizedIntBlockStateProvider {

    @Shadow @Nullable private IntProperty property;

    @Shadow @Final private BlockStateProvider source;

    @Shadow
    @Nullable
    private static IntProperty getIntPropertyByName(BlockState state, String propertyName) {
        throw new AbstractMethodError();
    }

    @Shadow @Final private String propertyName;

    @Shadow @Final private IntProvider values;

    /**
     * @author ishland
     * @reason ensure proper behavior
     */
    @Overwrite
    public BlockState get(Random random, BlockPos pos) {
        BlockState blockState = this.source.get(random, pos);
        IntProperty propertyLocal = this.property; // used as cache only
        if (propertyLocal == null || !blockState.contains(propertyLocal)) {
            IntProperty intProperty = getIntPropertyByName(blockState, this.propertyName);
            if (intProperty == null) {
                return blockState;
            }

            this.property = propertyLocal = intProperty;
        }

        return blockState.with(propertyLocal, Integer.valueOf(this.values.get(random)));
    }

}
