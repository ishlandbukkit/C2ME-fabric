package com.ishland.c2me.base;

import com.ishland.c2me.base.common.config.ConfigSystem;
import io.netty.util.internal.PlatformDependent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

public class ModuleEntryPoint {

    private static final boolean enabled = true;

    private static final String DEFAULT_EXPRESSION =
            """
                                    
                    max(
                        1,
                        min(
                            if( is_windows,
                                (cpus / 1.6 - 2),
                                (cpus / 1.2 - 2)
                            )  - if(is_client, 2, 0),
                            if( is_j9vm,
                                ( ( mem_gb - (if(is_client, 0.6, 0.2)) ) / 0.4 ),
                                ( ( mem_gb - (if(is_client, 1.2, 0.6)) ) / 0.6 )
                            )
                        )
                    )
                \040""";

    public static final String defaultGlobalExecutorParallelismExpression = new ConfigSystem.ConfigAccessor()
            .key("defaultGlobalExecutorParallelismExpression")
            .comment("""

                    The expression for the default value of global executor parallelism.\s
                    This is used when the parallelism isn't overridden.
                    Available variables: is_windows, is_j9vm, is_client, cpus, mem_gb
                    """.indent(1))
            .getString(DEFAULT_EXPRESSION, DEFAULT_EXPRESSION);

    public static final boolean disableLoggingShutdownHook = new ConfigSystem.ConfigAccessor()
            .key("fixes.disableLoggingShutdownHook")
            .comment("""
                    
                    Whether to disable the shutdown hook of log4j2 on dedicated servers.
                    Enabling this also makes the JVM exit when the dedicated server is considered fully shut down.
                    This option have no effect on client-side.
                    We has historically been doing this, and this config option allows you to disable this behavior.
                    """.indent(1))
            .incompatibleMod("textile_backup", "*")
            .getBoolean(true, false);

    public static final int defaultParallelism;

    private static int tryEvaluateExpression(String expression) {
        return (int) Math.max(1,
                new ExpressionBuilder(expression)
                        .variables("is_windows", "is_j9vm", "is_client", "cpus", "mem_gb")
                        .function(new Function("max", 2) {
                            @Override
                            public double apply(double... args) {
                                return Math.max(args[0], args[1]);
                            }
                        })
                        .function(new Function("min", 2) {
                            @Override
                            public double apply(double... args) {
                                return Math.min(args[0], args[1]);
                            }
                        })
                        .function(new Function("if", 3) {
                            @Override
                            public double apply(double... args) {
                                return args[0] != 0 ? args[1] : args[2];
                            }
                        })
                        .build()
                        .setVariable("is_windows", PlatformDependent.isWindows() ? 1 : 0)
                        .setVariable("is_j9vm", PlatformDependent.isJ9Jvm() ? 1 : 0)
                        .setVariable("is_client", FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? 1 : 0)
                        .setVariable("cpus", Runtime.getRuntime().availableProcessors())
                        .setVariable("mem_gb", Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0)
                        .evaluate()
        );
    }

    public static final long globalExecutorParallelism;

    static {
        final int defaultEval = tryEvaluateExpression(DEFAULT_EXPRESSION);
        int value;
        try {
            value = tryEvaluateExpression(defaultGlobalExecutorParallelismExpression);
        } catch (Throwable t) {
            ConfigSystem.LOGGER.error("Failed to evaluate defaultGlobalExecutorParallelismExpression, falling back to default value", t);
            value = defaultEval;
        }

        defaultParallelism = value;
        globalExecutorParallelism = new ConfigSystem.ConfigAccessor()
                .key("globalExecutorParallelism")
                .comment("Configures the parallelism of global executor")
                .getLong(value, value, ConfigSystem.LongChecks.THREAD_COUNT);

        ConfigSystem.LOGGER.info("Global Executor Parallelism: {} configured, {} evaluated, {} default evaluated", globalExecutorParallelism, defaultParallelism, defaultEval);
    }

//    public static int getDefaultGlobalExecutorParallelism() {
//        return Math.max(1, Math.min(getDefaultParallelismCPU(), getDefaultParallelismHeap()));
//    }
//
//    private static int getDefaultParallelismCPU() {
//        if (PlatformDependent.isWindows()) {
//            return Math.max(1, (int) (Runtime.getRuntime().availableProcessors() / 1.6 - 2)) + defaultParallelismEnvTypeOffset();
//        } else {
//            return Math.max(1, (int) (Runtime.getRuntime().availableProcessors() / 1.2 - 2)) + defaultParallelismEnvTypeOffset();
//        }
//    }
//
//    private static int defaultParallelismEnvTypeOffset() {
//        return isClientSide() ? -2 : 0;
//    }
//
//    private static boolean isClientSide() {
//        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
//    }
//
//    private static int getDefaultParallelismHeap() {
//        if (PlatformDependent.isJ9Jvm()) {
//            return (int) ((memoryInGiB() + (isClientSide() ? -0.6 : -0.2)) / 0.5) + defaultParallelismEnvTypeOffset();
//        } else {
//            return (int) ((memoryInGiB() + (isClientSide() ? -1.2 : -0.6)) / 1.2) + defaultParallelismEnvTypeOffset();
//        }
//    }
//
//    private static double memoryInGiB() {
//        return Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0;
//    }


}
