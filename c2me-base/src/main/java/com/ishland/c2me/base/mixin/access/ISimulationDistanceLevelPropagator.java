package com.ishland.c2me.base.mixin.access;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import net.minecraft.world.SimulationDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimulationDistanceLevelPropagator.class)
public interface ISimulationDistanceLevelPropagator {

    @Accessor
    Long2ByteMap getLevels();

}
