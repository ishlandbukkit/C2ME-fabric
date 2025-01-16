package com.ishland.c2me.server.utils.common;

import com.ishland.c2me.base.mixin.access.IThreadedAnvilChunkStorage;
import com.ishland.c2me.notickvd.common.ChunkLevelManagerExtension;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.text.Text;

public class C2MECommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("c2me")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.literal("notick")
                                        .requires(unused -> com.ishland.c2me.notickvd.ModuleEntryPoint.enabled)
                                        .executes(C2MECommands::noTickCommand)
                        )
                        .then(
                                CommandManager.literal("debug")
                                        .requires(unused -> FabricLoader.getInstance().isDevelopmentEnvironment())
//                                        .then(
//                                                CommandManager.literal("mobcaps")
//                                                        .requires(unused -> com.ishland.c2me.notickvd.ModuleEntryPoint.enabled)
//                                                        .executes(C2MECommands::mobcapsCommand)
//                                        )
                        )
        );
    }

    private static int noTickCommand(CommandContext<ServerCommandSource> ctx) {
        final ServerChunkManager chunkManager = ctx.getSource().getWorld().toServerWorld().getChunkManager();
        final ChunkLevelManager ticketManager = ((IThreadedAnvilChunkStorage) chunkManager.chunkLoadingManager).getLevelManager();
        final long noTickPendingTicketUpdates = ((ChunkLevelManagerExtension) ticketManager).c2me$getPendingLoadsCount();
        ctx.getSource().sendFeedback(() -> Text.of(String.format("No-tick chunk pending chunk loads: %d", noTickPendingTicketUpdates)), true);

        return 0;
    }

//    private static int mobcapsCommand(CommandContext<ServerCommandSource> ctx) {
//        final ServerWorld serverWorld = ctx.getSource().getWorld().toServerWorld();
//        final ServerChunkManager chunkManager = serverWorld.getChunkManager();
//        final ChunkTicketManager ticketManager = ((IServerChunkManager) chunkManager).getTicketManager();
//        final LongSet noTickOnlyChunks = ((ChunkLevelManagerExtension) ticketManager).getNoTickOnlyChunks();
//        final Iterable<Entity> iterable;
//        if (noTickOnlyChunks == null) {
//            iterable = serverWorld.iterateEntities();
//        } else {
//            iterable = new FilteringIterable<>(serverWorld.iterateEntities(), entity -> !noTickOnlyChunks.contains(entity.getChunkPos().toLong()));
//        }
//
//        ctx.getSource().sendFeedback(Text.of("Mobcap details"), true);
//        for (Entity entity : iterable) {
//            if (entity instanceof MobEntity mobEntity) {
//                ctx.getSource().sendFeedback(Text.of(String.format("%s: ", mobEntity.getType().getSpawnGroup().asString())).(mobEntity.getDisplayName()).append(String.format(" in %s", mobEntity.getChunkPos())), true);
//            }
//        }
//        return 0;
//    }

}
