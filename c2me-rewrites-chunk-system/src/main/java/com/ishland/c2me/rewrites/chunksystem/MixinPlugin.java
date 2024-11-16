package com.ishland.c2me.rewrites.chunksystem;

import com.ishland.c2me.base.common.ModuleMixinPlugin;
import com.ishland.c2me.rewrites.chunksystem.common.Config;

import java.lang.reflect.Field;

public class MixinPlugin extends ModuleMixinPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName))
            return false;

        boolean gcFreeChunkSerializerDetected = tryDetectGcFreeSerializer();

        if (mixinClassName.startsWith("com.ishland.c2me.rewrites.chunksystem.mixin.serialization_sync."))
            return !gcFreeChunkSerializerDetected;

        if (mixinClassName.startsWith("com.ishland.c2me.rewrites.chunksystem.mixin.fluid_postprocessing"))
            return Config.fluidPostProcessingToScheduledTick;

        return true;
    }

    private static boolean tryDetectGcFreeSerializer() {
        try {
            Class<?> entryPoint = Class.forName("com.ishland.c2me.rewrites.chunk_serializer.ModuleEntryPoint");
            Field enabled = entryPoint.getField("enabled");
            return (boolean) enabled.get(null);
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}
