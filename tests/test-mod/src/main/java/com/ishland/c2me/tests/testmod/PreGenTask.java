package com.ishland.c2me.tests.testmod;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ibm.asyncutil.locks.AsyncSemaphore;
import com.ibm.asyncutil.locks.FairAsyncSemaphore;
import com.ishland.c2me.tests.testmod.mixin.IServerChunkManager;
import com.ishland.c2me.tests.testmod.mixin.IThreadedAnvilChunkStorage;
import com.mojang.datafixers.util.Pair;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.structure.Structure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PreGenTask {

    private static final Logger LOGGER = LogManager.getLogger("C2ME Test");
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.max(1,Runtime.getRuntime().availableProcessors() - 1),
            new ThreadFactoryBuilder().setDaemon(true).setPriority(Thread.NORM_PRIORITY - 2).setNameFormat("locator-%d").build()
    );
    private static final ChunkTicketType TICKET = new ChunkTicketType(0, false, ChunkTicketType.class_10558.LOADING);

    private static final int SEARCH_RADIUS = 512 * 16;

    public static CompletableFuture<Void> runPreGen(ServerWorld world, Consumer<ChunkGeneratedEventInfo> eventListener) {
        Preconditions.checkNotNull(eventListener);
        System.err.printf("Starting pre-generation in %s;%s\n", world.toString(), world.getRegistryKey().getValue().toString());
        final BlockPos spawnPos = world.getSpawnPos();
        final Set<ChunkPos> chunksHashed = Sets.newConcurrentHashSet();
        final List<ChunkPos> chunks = Collections.synchronizedList(new ArrayList<>());
        chunks.addAll(createPreGenChunks33(new ChunkPos(spawnPos), chunksHashed::add));
        final AtomicInteger locatedBiomes = new AtomicInteger();
        final AtomicInteger locatedStructures = new AtomicInteger();
        System.err.printf("Fetching structure and biome list\n");
        final Registry<Biome> biomeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
        final Set<RegistryEntry<Biome>> biomes =
                world.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes()
                        .stream()
                        .filter(biomeclass_6880 -> biomeRegistry.getKey(biomeclass_6880.value()).isPresent())
                        .collect(Collectors.toCollection(HashSet::new));
        final Registry<Structure> structureFeatureRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        final Set<RegistryEntryList<Structure>> structureFeatures;
//        if (false) {
//            structureFeatures = structureFeatureRegistry.getEntrySet().stream()
//                    .filter(entry -> world.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes().stream().anyMatch(entry.getValue().getValidBiomes()::contains))
//                    .flatMap(entry -> structureFeatureRegistry.getEntry(entry.getKey()).map(RegistryEntryList::of).stream())
//                    .collect(Collectors.toSet());
//        } else {
            LOGGER.warn("locateStructure() is too slow, disabling structure locating");
            structureFeatures = Collections.emptySet();
//        }
        System.err.printf("Submitting tasks\n");

//        final CompletableFuture<Void> biomeFuture = CompletableFuture.allOf(biomes.stream()
//                .map(biome -> CompletableFuture.runAsync(() -> {
//                    //noinspection OptionalGetWithoutIsPresent
//                    final Pair<BlockPos, RegistryEntry<Biome>> pair = world.locateBiome(entry -> biomeRegistry.getKey(biome.value()).get() == entry.getKey().get(), spawnPos, SEARCH_RADIUS, 8, 64);
//                    locatedBiomes.incrementAndGet();
//                    if (pair != null) {
//                        final ChunkPos chunkPos = new ChunkPos(pair.getFirst());
//                        chunks.addAll(createPreGenChunks25(chunkPos, chunksHashed::add));
//                        LOGGER.info("Located biome {}", biomeRegistry.getId(biome.value()));
//                        return;
//                    }
//                    LOGGER.info("Unable to locate biome {}", biomeRegistry.getId(biome.value()));
//                }, EXECUTOR)).distinct().toArray(CompletableFuture[]::new));
        final CompletableFuture<Void> biomeFuture = CompletableFuture.runAsync(() -> {
            Set<RegistryEntry<Biome>> biomesToLocate = Sets.newConcurrentHashSet(biomes);
            locateAllTheBiomes(world.getChunkManager().getChunkGenerator().getBiomeSource(), spawnPos, SEARCH_RADIUS, 8, 64, new LocateCallback() {
                @Override
                public void consume(RegistryEntry<Biome> biome, int x, int y, int z) {
                    if (biomesToLocate.remove(biome)) {
                        final ChunkPos chunkPos = new ChunkPos(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
                        chunks.addAll(createPreGenChunks25(chunkPos, chunksHashed::add));
                        LOGGER.info("Located biome {}", biomeRegistry.getId(biome.value()));
                        locatedBiomes.incrementAndGet();
                    }
                }

                @Override
                public boolean shouldStop() {
                    return biomesToLocate.isEmpty();
                }
            }, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler(), world, EXECUTOR);
        }, new ThreadPerTaskExecutor(Thread::new));
        final CompletableFuture<Void> structureFuture = CompletableFuture.allOf(structureFeatures.stream()
                .map(structureFeature -> CompletableFuture.runAsync(() -> {
                    final Pair<BlockPos, RegistryEntry<Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(world, structureFeature, spawnPos, SEARCH_RADIUS / 16, false);
                    locatedStructures.incrementAndGet();
                    if (pair != null) {
                        final ChunkPos chunkPos = new ChunkPos(pair.getFirst());
                        chunks.addAll(createPreGenChunks25(chunkPos, chunksHashed::add));
                        return;
                    }
                    LOGGER.info("Unable to locate structure {}", Arrays.toString(structureFeature.stream()
                            .flatMap(entry -> entry.getKey().stream())
                            .map(RegistryKey::getValue)
                            .map(Identifier::toString)
                            .toArray(String[]::new))
                    );
                }, EXECUTOR)).distinct().toArray(CompletableFuture[]::new));
        final CompletableFuture<Void> locateFuture = CompletableFuture.allOf(biomeFuture, structureFuture);
        long lastProgress = System.currentTimeMillis();
        int printCounter = 0;
        System.err.printf("Waiting for tasks to finish\n");
        while (!locateFuture.isDone() && world.getServer().isRunning()) {
            while (System.currentTimeMillis() - lastProgress > 40) {
                lastProgress += 40;
                printCounter++;
                final String formatted = String.format("Locating: Biomes: %d / %d, Structures: %d / %d\n", locatedBiomes.get(), biomes.size(), locatedStructures.get(), structureFeatures.size());
                System.out.print("[noprint]" + formatted);
                if (printCounter > 128) {
                    LOGGER.info("[noprogress]" + formatted.substring(0, formatted.length() - 1));
                    printCounter = 0;
                }
            }
            if (!((IMinecraftServer) world.getServer()).c2metest$runAsyncTask())
                LockSupport.parkNanos("waiting for tasks", 100000L);
        }
        if (!world.getServer().isRunning()) return CompletableFuture.completedFuture(null);
        chunksHashed.clear();
        final int total = chunks.size();
        LOGGER.info("Total chunks: {}", total);
        AsyncSemaphore working = new FairAsyncSemaphore(Runtime.getRuntime().maxMemory() / 1024 / 1024 / 1024 * 24);
        AtomicLong generatedCount = new AtomicLong();
        final Set<CompletableFuture<Void>> futures = chunks.stream()
                .map(pos -> working.acquire()
                        .toCompletableFuture()
                        .thenComposeAsync(unused -> getChunkAtAsync(world, pos).thenAccept(unused1 -> {
                                    generatedCount.incrementAndGet();
                                    working.release();
                                    eventListener.accept(new ChunkGeneratedEventInfo(generatedCount.get(), total, world));
                                }),
                                runnable -> {
                                    if (world.getServer().isOnThread()) runnable.run();
                                    else
                                        ((IThreadedAnvilChunkStorage) world.getChunkManager().chunkLoadingManager).getMainThreadExecutor().execute(runnable);
                                }
                        )
                )
                .collect(Collectors.toSet());
        System.err.printf("Task for %s;%s started\n", world, world.getRegistryKey().getValue().toString());
        final CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        future.thenRun(() -> {
            eventListener.accept(new ChunkGeneratedEventInfo(total + 1, total, world));
            System.err.printf("Task for %s;%s completed\n", world, world.getRegistryKey().getValue().toString());
        });
        return future;
    }

    /**
     * Add a generation range of 33x33 (hardcoded) with 16 pregen regions
     */
    private static ArrayList<ChunkPos> createPreGenChunks33(ChunkPos center, Predicate<ChunkPos> shouldAdd) {
        final ArrayList<ChunkPos> chunks = new ArrayList<>(33 * 33);
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x - 8, center.z - 8), shouldAdd));
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x - 8, center.z + 8), shouldAdd));
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x + 8, center.z - 8), shouldAdd));
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x + 8, center.z + 8), shouldAdd));
        return chunks;
    }

    /**
     * Add a generation range of 25x25 (hardcoded) with 16 pregen regions
     */
    private static ArrayList<ChunkPos> createPreGenChunks25(ChunkPos center, Predicate<ChunkPos> shouldAdd) {
        final ArrayList<ChunkPos> chunks = new ArrayList<>(25 * 25);
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x - 4, center.z - 4), shouldAdd));
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x - 4, center.z + 4), shouldAdd));
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x + 4, center.z - 4), shouldAdd));
        chunks.addAll(createPreGenChunks17(new ChunkPos(center.x + 4, center.z + 4), shouldAdd));
        return chunks;
    }

    /**
     * Add a generation range of 17x17 (hardcoded) with 4 pregen regions
     */
    private static ArrayList<ChunkPos> createPreGenChunks17(ChunkPos center, Predicate<ChunkPos> shouldAdd) {
        final ArrayList<ChunkPos> chunks = new ArrayList<>(17 * 17);
        chunks.addAll(createPreGenChunks0(new ChunkPos(center.x - 4, center.z - 4), 4, shouldAdd));
        chunks.addAll(createPreGenChunks0(new ChunkPos(center.x - 4, center.z + 4), 4, shouldAdd));
        chunks.addAll(createPreGenChunks0(new ChunkPos(center.x + 4, center.z - 4), 4, shouldAdd));
        chunks.addAll(createPreGenChunks0(new ChunkPos(center.x + 4, center.z + 4), 4, shouldAdd));
        return chunks;
    }

    @NotNull
    private static ArrayList<ChunkPos> createPreGenChunks0(ChunkPos center, int radius, Predicate<ChunkPos> shouldAdd) {
        final ArrayList<ChunkPos> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int x = center.x - radius; x <= center.x + radius; x++)
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                final ChunkPos chunkPos = new ChunkPos(x, z);
                if (shouldAdd.test(chunkPos)) {
                    chunks.add(chunkPos);
                }
            }
        chunks.sort(Comparator.comparingInt(one -> one.getChebyshevDistance(center)));
        return chunks;
    }

    private static CompletableFuture<Void> getChunkAtAsync(ServerWorld world, ChunkPos pos) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        world.getChunkManager().method_66009(TICKET, pos, 0);
        ((IServerChunkManager) world.getChunkManager()).invokeUpdateChunks();
        final ChunkHolder chunkHolder = ((IThreadedAnvilChunkStorage) world.getChunkManager().chunkLoadingManager).invokeGetChunkHolder(pos.toLong());
        Preconditions.checkNotNull(chunkHolder, "chunkHolder is null");
        chunkHolder.getAccessibleFuture().thenAcceptAsync(either -> {
            world.getChunkManager().method_66010(TICKET, pos, 0);
            if (either.isPresent())
                future.complete(null);
            else
                future.completeExceptionally(new RuntimeException(either.getError()));
        }, ((IThreadedAnvilChunkStorage) world.getChunkManager().chunkLoadingManager).getMainThreadExecutor()).exceptionally(throwable -> {
            future.completeExceptionally(throwable);
            return null;
        });
        return future;
    }

    private static void locateAllTheBiomes(
            BiomeSource source,
            BlockPos origin,
            int radius,
            int horizontalBlockCheckInterval,
            int verticalBlockCheckInterval,
            LocateCallback locateCallback,
            MultiNoiseUtil.MultiNoiseSampler noiseSampler,
            WorldView world,
            Executor executor
    ) {
        int i = Math.floorDiv(radius, horizontalBlockCheckInterval);
        int[] is = MathHelper.stream(origin.getY(), world.getBottomY() + 1, world.getTopYInclusive(), verticalBlockCheckInterval).toArray();

        final int permits = Runtime.getRuntime().availableProcessors() * 4;
        Semaphore semaphore = new Semaphore(permits);

        for(BlockPos.Mutable mutable : BlockPos.iterateInSquare(BlockPos.ORIGIN, i, Direction.EAST, Direction.SOUTH)) {
            int j = origin.getX() + mutable.getX() * horizontalBlockCheckInterval;
            int k = origin.getZ() + mutable.getZ() * horizontalBlockCheckInterval;
            int l = BiomeCoords.fromBlock(j);
            int m = BiomeCoords.fromBlock(k);

            semaphore.acquireUninterruptibly();
            executor.execute(() -> {
                try {
                    for(int n : is) {
                        int o = BiomeCoords.fromBlock(n);
                        RegistryEntry<Biome> registryEntry = source.getBiome(l, o, m, noiseSampler);
                        locateCallback.consume(registryEntry, j, n, k);
                    }
                } finally {
                    semaphore.release();
                }
            });

            if (locateCallback.shouldStop()) {
                semaphore.acquireUninterruptibly(permits);
                return;
            }
        }
    }

    private interface LocateCallback {
        void consume(RegistryEntry<Biome> biome, int x, int y, int z);

        boolean shouldStop();
    }

    public record ChunkGeneratedEventInfo(long generatedCount, long totalCount, ServerWorld world) {
    }

    public static class PreGenEventListener implements Consumer<PreGenTask.ChunkGeneratedEventInfo> {

        private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00%");
        private final HashMap<ServerWorld, ChunkGeneratedEventInfo> inProgressWorlds = new HashMap<>();
        public boolean fullyStarted = false;
        private long lastLog = System.currentTimeMillis();
        private long lastPrint = System.currentTimeMillis();

        @Override
        public synchronized void accept(PreGenTask.ChunkGeneratedEventInfo chunkGeneratedEventInfo) {
            if (chunkGeneratedEventInfo.generatedCount >= chunkGeneratedEventInfo.totalCount) {
                inProgressWorlds.remove(chunkGeneratedEventInfo.world);
            } else {
                inProgressWorlds.put(chunkGeneratedEventInfo.world, chunkGeneratedEventInfo);
            }

            Supplier<String> resultSupplier = Suppliers.memoize(() -> {
                StringBuilder result = new StringBuilder();
                for (ChunkGeneratedEventInfo value : inProgressWorlds.values()) {
                    result
                            .append(value.world.getRegistryKey().getValue())
                            .append(":")
                            .append(value.generatedCount)
                            .append("/")
                            .append(value.totalCount)
                            .append(",")
                            .append(DECIMAL_FORMAT.format(value.generatedCount / (double) value.totalCount))
                            .append(" ");
                }
                return result.toString();
            });

            final long timeMillis = System.currentTimeMillis();
            if (timeMillis >= lastLog + 5000L) {
                lastLog += 5000L;
                LOGGER.info("[noprogress] " + resultSupplier.get());
            }
            if (fullyStarted && timeMillis >= lastPrint + 100L) {
                lastPrint += 100L;
                System.out.print("[noprint]" + resultSupplier.get() + "\n");
            }
        }

    }

}
