package com.ishland.c2me.opts.dfc;

import com.ishland.c2me.base.common.config.ConfigSystem;

public class ModuleEntryPoint {

    private static final boolean enabled = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.useDensityFunctionCompiler")
            .comment("""
                    Whether to use density function compiler to accelerate world generation
                    
                    Density function: https://minecraft.wiki/w/Density_function
                    
                    This functionality compiles density functions from world generation
                    datapacks (including vanilla generation) to JVM bytecode to increase
                    performance by allowing JVM JIT to better optimize the code
                    
                    Currently, all functions provided by vanilla are implemented.
                    Chunk upgrades from pre-1.18 versions are not implemented and will
                    fall back to the unoptimized version of density functions.
                    """)
            .getBoolean(false, false);

}
