package com.ishland.c2me.opts.dfc.common.ast.dfvisitor;

import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

public class StripBlending implements DensityFunction.DensityFunctionVisitor {

    public static final StripBlending INSTANCE = new StripBlending();

    private StripBlending() {
    }

    @Override
    public DensityFunction apply(DensityFunction densityFunction) {
        return switch (densityFunction) {
            case ChunkNoiseSampler.BlendAlphaDensityFunction f -> DensityFunctionTypes.constant(1.0);
            case ChunkNoiseSampler.BlendOffsetDensityFunction f -> DensityFunctionTypes.constant(0.0);
            case DensityFunctionTypes.BlendAlpha f -> DensityFunctionTypes.constant(1.0);
            case DensityFunctionTypes.BlendOffset f -> DensityFunctionTypes.constant(0.0);
            default -> densityFunction;
        };
    }

}
