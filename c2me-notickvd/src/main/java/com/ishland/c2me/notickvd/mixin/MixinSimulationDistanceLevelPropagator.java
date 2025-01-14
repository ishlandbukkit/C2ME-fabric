package com.ishland.c2me.notickvd.mixin;

import net.minecraft.world.SimulationDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SimulationDistanceLevelPropagator.class)
public class MixinSimulationDistanceLevelPropagator {

    @ModifyConstant(method = "<init>", constant = {@Constant(intValue = 34), @Constant(intValue = 33)}, require = 2)
    private static int modifyMax(int constant) {
        return constant + 1;
    }

    @ModifyConstant(method = "getLevel(Lnet/minecraft/util/collection/SortedArraySet;)I", constant = @Constant(intValue = 34))
    private int modifyMax1(int constant) {
        return constant + 1;
    }

    @ModifyConstant(method = "setLevel", constant = @Constant(intValue = 33))
    private int modifyMax2(int constant) {
        return constant + 1;
    }

}
