package com.ishland.c2me.fixes.worldgen.threading_issues.mixin.threading;

import com.ishland.c2me.fixes.worldgen.threading_issues.asm.MakeVolatile;
import net.minecraft.structure.SwampHutGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SwampHutGenerator.class)
public class MixinSwampHutGenerator {

    @MakeVolatile
    @Shadow private boolean hasWitch;

    @MakeVolatile
    @Shadow private boolean hasCat;

}