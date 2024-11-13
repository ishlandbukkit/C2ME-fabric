package com.ishland.c2me.base.mixin.report;

import com.ishland.c2me.base.common.threadstate.ThreadInstrumentation;
import com.ishland.c2me.base.common.threadstate.ThreadState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.dedicated.DedicatedServerWatchdog;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

@Mixin(DedicatedServerWatchdog.class)
public class MixinDedicatedServerWatchdog {

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "createCrashReport", at = @At(value = "INVOKE", target = "Ljava/lang/management/ThreadInfo;getThreadId()J"))
    private static void prependThreadInstrumentation(String message, long threadId, CallbackInfoReturnable<CrashReport> cir, @Local StringBuilder stringBuilder, @Local ThreadInfo threadInfo) {
        String state = null;
        try {
            state = ThreadInstrumentation.printState(threadInfo);
        } catch (Throwable t) {
            LOGGER.error("Failed to fetch state for thread {}", threadInfo);
        }
        if (state != null) {
            stringBuilder.append(state);
        }
    }

    @Inject(method = "createCrashReport", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/crash/CrashReport;addElement(Ljava/lang/String;)Lnet/minecraft/util/crash/CrashReportSection;", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void addInstrumentationData(String message, long threadId, CallbackInfoReturnable<CrashReport> cir, ThreadMXBean threadMXBean, ThreadInfo[] threadInfos, StringBuilder stringBuilder, Error error, CrashReport crashReport) {
        CrashReportSection section = crashReport.addElement("Thread trace dump (obtained on a best-effort basis)");
        try {
            for (Map.Entry<Thread, ThreadState> entry : ThreadInstrumentation.entrySet()) {
                try {
                    Thread thread = entry.getKey();
                    String state = ThreadInstrumentation.printState(thread.getName(), thread.threadId(), entry.getValue());
                    if (state != null) {
                        section.add(thread.getName(), state);
                    }
                } catch (Throwable t) {
                    LOGGER.error("Failed to dumping state for thread {}", entry.getKey(), t);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to dump all known thread states", t);
        }
    }

}
