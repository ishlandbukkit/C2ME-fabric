package com.ishland.c2me.rewrites.chunksystem.common;

import com.ishland.c2me.base.common.config.ConfigSystem;

public class Config {

    public static final boolean asyncSerialization = new ConfigSystem.ConfigAccessor()
            .key("chunkSystem.asyncSerialization")
            .comment("""
                    Whether to enable async serialization
                    """)
            .getBoolean(true, false);

    public static final boolean recoverFromErrors = new ConfigSystem.ConfigAccessor()
            .key("chunkSystem.recoverFromErrors")
            .comment("""
                    Whether to recover from errors when loading chunks\s
                     This will cause errored chunk to be regenerated entirely, which may cause data loss\s
                     Only applies when async chunk loading is enabled
                     """)
            .getBoolean(false, false);

    public static final boolean allowPOIUnloading = new ConfigSystem.ConfigAccessor()
            .key("chunkSystem.allowPOIUnloading")
            .comment("""
                    Whether to allow POIs (Point of Interest) to be unloaded
                    Unloaded POIs are reloaded on-demand or when the corresponding chunks are loaded again,
                    which should not cause any behavior change
                    \s
                    Note:
                    Vanilla never unloads POIs when chunks unload, causing small memory leaks
                    These leaks adds up and eventually cause issues after generating millions of chunks
                    in a single world instance
                    """)
            .getBoolean(true, false);

    public static final boolean suppressGhostMushrooms = new ConfigSystem.ConfigAccessor()
            .key("chunkSystem.suppressGhostMushrooms")
            .comment("""
                    This option workarounds MC-276863, a bug that makes mushrooms appear in non-postprocessed chunks
                    This bug is amplified with notickvd as it exposes non-postprocessed chunks to players
                    
                    This should not affect other worldgen behavior and game mechanics in general
                    """)
            .getBoolean(true, false);

    public static void init() {
        // intentionally empty
    }

}
