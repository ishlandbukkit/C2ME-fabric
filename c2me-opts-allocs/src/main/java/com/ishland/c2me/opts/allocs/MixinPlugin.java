package com.ishland.c2me.opts.allocs;

import com.ishland.c2me.base.common.ModuleMixinPlugin;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) return false;

        if (mixinClassName.equals("com.ishland.c2me.opts.allocs.mixin.MixinNbtCompound") ||
                mixinClassName.equals("com.ishland.c2me.opts.allocs.mixin.MixinNbtCompound1"))
            return !FabricLoader.getInstance().isModLoaded("lithium");

        if (mixinClassName.startsWith("com.ishland.c2me.opts.allocs.mixin.surfacebuilder.")) {
            return !FabricLoader.getInstance().isModLoaded("quilted_fabric_api") &&
                   !FabricLoader.getInstance().isModLoaded("frozenlib");
        }

        return true;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        super.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
    }

}
