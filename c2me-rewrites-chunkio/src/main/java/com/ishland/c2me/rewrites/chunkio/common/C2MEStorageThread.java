package com.ishland.c2me.rewrites.chunkio.common;

import com.ibm.asyncutil.util.Either;
import com.ishland.c2me.base.common.GlobalExecutors;
import com.ishland.c2me.base.common.structs.RawByteArrayOutputStream;
import com.ishland.c2me.base.common.util.SneakyThrow;
import com.ishland.c2me.base.mixin.access.IRegionBasedStorage;
import com.ishland.c2me.base.mixin.access.IRegionFile;
import io.netty.util.internal.PlatformDependent;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.ChunkCompressionFormat;
import net.minecraft.world.storage.RegionBasedStorage;
import net.minecraft.world.storage.RegionFile;
import net.minecraft.world.storage.StorageKey;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.LongFunction;

public class C2MEStorageThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger("C2ME Storage");

    private static final AtomicLong SERIAL = new AtomicLong(0);

    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private final RegionBasedStorage storage;
    private final AtomicInteger taskSize = new AtomicInteger();
    private final Long2ReferenceLinkedOpenHashMap<CompletionStage<Either<NbtCompound, byte[]>>> writeBacklog = new Long2ReferenceLinkedOpenHashMap<>();
    private final Long2ReferenceLinkedOpenHashMap<CompletionStage<Either<NbtCompound, byte[]>>> cache = new Long2ReferenceLinkedOpenHashMap<>();
    private final Queue<Runnable> pendingTasks = PlatformDependent.newMpscQueue();
    private final LongFunction<Executor> backgroundExecutorSupplier;
    private final Executor executor = command -> {
        if (Thread.currentThread() == this) {
            command.run();
        } else {
            final boolean empty = this.taskSize.getAndIncrement() == 0;
            pendingTasks.add(command);
            if (empty) this.wakeUp();
        }
    };
    private final ObjectArraySet<CompletableFuture<Void>> writeFutures = new ObjectArraySet<>();
    private final Object sync = new Object();

    public C2MEStorageThread(StorageKey arg, Path path, boolean dsync, LongFunction<Executor> backgroundExecutorSupplier) {
        this.storage = new RegionBasedStorage(arg, path, dsync);
        if (backgroundExecutorSupplier != null) {
            this.backgroundExecutorSupplier = backgroundExecutorSupplier;
        } else {
            Executor executor1 = GlobalExecutors.prioritizedScheduler.executor(16);
            this.backgroundExecutorSupplier = unused -> executor1;
        }
        this.setName("C2ME Storage #%d".formatted(SERIAL.incrementAndGet()));
        this.setDaemon(true);
        this.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Thread %s died".formatted(t), e));
        this.start();
    }

    @Override
    public void run() {
        main_loop:
        while (true) {
            boolean hasWork = false;
            hasWork |= pollTasks();

            runWriteFutureGC();

            if (!hasWork) {
                if (this.closing.get()) {
                    flush0(true);
                    try {
                        this.storage.close();
                    } catch (Throwable t) {
                        LOGGER.error("Error closing storage", t);
                    }
                    this.closeFuture.complete(null);
                    break;
                } else {
//                    // attempt to spin-wait before sleeping
//                    if (!pollTasks()) {
//                        Thread.interrupted(); // clear interrupt flag
//                        for (int i = 0; i < 5000; i ++) {
//                            if (pollTasks() || this.closing.get()) continue main_loop;
//                            LockSupport.parkNanos("Spin-waiting for tasks", 10_000); // 100us
//                        }
//                    }
                    synchronized (sync) {
                        if (this.taskSize.get() != 0 || this.closing.get()) continue main_loop;
                        try {
                            sync.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }
        LOGGER.info("Storage thread {} stopped", this);
    }

    private boolean pollTasks() {
        boolean hasWork = false;
        hasWork = handleTasks() || hasWork;
        hasWork = writeBacklog() || hasWork;
        return hasWork;
    }

    private boolean hasPendingTasks() {
        return !this.pendingTasks.isEmpty() || !this.writeBacklog.isEmpty();
    }

    private void wakeUp() {
        synchronized (sync) {
            sync.notifyAll();
        }
    }

    /**
     * Read chunk data from storage
     * @param pos target pos
     * @param scanner if null then ignored, if non-null then used and produce null future
     * @return future
     */
    public CompletableFuture<NbtCompound> getChunkData(long pos, NbtScanner scanner) {
        final CompletableFuture<NbtCompound> future = new CompletableFuture<>();
        if (this.closing.get()) {
            future.completeExceptionally(new CancellationException());
            return future.thenApply(Function.identity());
        }
        this.executor.execute(() -> this.read0(pos, future, scanner));
//        future.thenApply(Function.identity()).orTimeout(60, TimeUnit.SECONDS).exceptionally(throwable -> {
//            if (throwable instanceof TimeoutException) {
//                LOGGER.warn("Chunk read at pos {} took too long (> 1min)", new ChunkPos(pos).toLong());
//            }
//            return null;
//        });
        return future
                .thenApply(Function.identity());
    }

    public CompletableFuture<Void> setChunkData(long pos, @Nullable NbtCompound nbt) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.executor.execute(() -> {
            this.write0(pos, CompletableFuture.completedFuture(nbt != null ? Either.left(nbt) : null));
            future.complete(null);
        });
        return future;
    }

    public CompletableFuture<Void> setChunkData(long pos, @Nullable byte[] data) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.executor.execute(() -> {
            this.write0(pos, CompletableFuture.completedFuture(data != null ? Either.right(data) : null));
            future.complete(null);
        });
        return future;
    }

    public CompletableFuture<Void> setChunkData(long pos, CompletionStage<NbtCompound> nbtFuture) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.executor.execute(() -> {
            this.write0(pos, nbtFuture.thenApply(nbt -> nbt != null ? Either.left(nbt) : null));
            future.complete(null);
        });
        return future;
    }

    public CompletableFuture<Void> setChunkDataRaw(long pos, CompletionStage<byte[]> dataFuture) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.executor.execute(() -> {
            this.write0(pos, dataFuture.thenApply(data -> data != null ? Either.right(data) : null));
            future.complete(null);
        });
        return future;
    }

    public CompletableFuture<Void> flush(boolean sync) {
        return CompletableFuture.runAsync(() -> flush0(sync), this.executor);
    }

    private void flush0(boolean sync) {
        try {
            while (true) {
                runWriteFutureGC();
                if (handleTasks()) continue;
                if (writeBacklog()) continue;

                break;
            }
            flushBacklog();
            if (sync) this.storage.sync();
        } catch (Throwable t) {
            LOGGER.error("Error flushing storage", t);
        }
    }

    public StorageKey getStorageKey() {
        return this.storage.getStorageKey();
    }

    public CompletableFuture<Void> close() {
        this.closing.set(true);
        this.wakeUp();
        return this.closeFuture.thenApply(Function.identity());
    }

    private boolean handleTasks() {
        boolean hasWork = false;
        Runnable runnable;
        while ((runnable = this.pendingTasks.poll()) != null) {
            hasWork = true;
            this.taskSize.decrementAndGet();
            try {
                runnable.run();
            } catch (Throwable t) {
                LOGGER.error("Error while executing task", t);
            }
        }
        return hasWork;
    }

    private void write0(long pos, CompletionStage<Either<NbtCompound, byte[]>> nbt) {
        this.cache.put(pos, nbt);
        this.writeBacklog.put(pos, nbt);
    }

    private void read0(long pos, CompletableFuture<NbtCompound> future, NbtScanner scanner) {
        if (this.cache.containsKey(pos)) {
            final CompletionStage<Either<NbtCompound, byte[]>> cachedFuture = this.cache.get(pos);
            if (cachedFuture == null) {
                future.complete(null);
            } else {
                cachedFuture.whenComplete((cached, throwable) -> { // mirror vanilla behavior: get the immediate result rather than latest
                    if (throwable != null) {
                        this.executor.execute(() -> {
                            LOGGER.warn("Retrying read of chunk {} because previous write to chunk threw an exception", new ChunkPos(pos));
                            this.read0(pos, future, scanner);
                        }); // retry
                        return;
                    }
                    if (cached == null) {
                        future.complete(null);
                    } else if (cached.left().isPresent()) {
                        if (scanner != null) {
                            backgroundExecutorSupplier.apply(pos).execute(() -> {
                                try {
                                    cached.left().get().accept(scanner);
                                    future.complete(null);
                                } catch (Throwable t) {
                                    future.completeExceptionally(t);
                                }
                            });
                        } else {
                            future.complete(cached.left().get());
                        }
                    } else {
                        CompletableFuture.supplyAsync(() -> {
                                    try {
                                        final DataInputStream input = new DataInputStream(new ByteArrayInputStream(cached.right().get()));
                                        if (scanner != null) {
                                            NbtIo.scan(input, scanner, NbtSizeTracker.ofUnlimitedBytes());
                                            return null;
                                        } else {
                                            final NbtCompound compound = NbtIo.readCompound(input);
                                            return compound;
                                        }
                                    } catch (IOException e) {
                                        SneakyThrow.sneaky(e);
                                        return null; // unreachable
                                    }
                                }, backgroundExecutorSupplier.apply(pos))
                                .thenAccept(future::complete)
                                .exceptionally(throwable1 -> {
                                    future.completeExceptionally(throwable1);
                                    return null;
                                });
                    }
                });
            }
        } else {
            scheduleChunkRead(pos, future, scanner);
        }
    }

    private boolean writeBacklog() {
        if (!this.writeBacklog.isEmpty()) {
            final long pos = this.writeBacklog.firstLongKey();
            final CompletionStage<Either<NbtCompound, byte[]>> nbtFuture = this.writeBacklog.removeFirst();
            writeChunk(pos, nbtFuture);
            return true;
        }
        return false;
    }

    private void runWriteFutureGC() {
        this.writeFutures.removeIf(CompletableFuture::isDone);
    }

    private void flushBacklog() {
        while (!this.writeFutures.isEmpty()) {
            while (writeBacklog()) ;
            runWriteFutureGC();
            final CompletableFuture<Void> allFuture = CompletableFuture.allOf(this.writeFutures.stream()
                    .map(future -> future.exceptionally(unused -> null))
                    .distinct()
                    .toArray(CompletableFuture[]::new));
            while (!allFuture.isDone()) {
                handleTasks();
            }
            runWriteFutureGC();
        }
    }

    private void scheduleChunkRead(long pos, CompletableFuture<NbtCompound> future, NbtScanner scanner) {
        try {
            final ChunkPos pos1 = new ChunkPos(pos);
            final RegionFile regionFile = ((IRegionBasedStorage) this.storage).invokeGetRegionFile(pos1);
            final DataInputStream chunkInputStream = regionFile.getChunkInputStream(pos1);
            if (chunkInputStream == null) {
                future.complete(null);
                return;
            }
            CompletableFuture.supplyAsync(() -> {
                try {
                    try (DataInputStream inputStream = chunkInputStream) {
                        if (scanner != null) {
                            NbtIo.scan(inputStream, scanner, NbtSizeTracker.ofUnlimitedBytes());
                            return null;
                        } else {
                            return NbtIo.readCompound(inputStream);
                        }
                    }
                } catch (Throwable t) {
                    SneakyThrow.sneaky(t);
                    return null; // Unreachable anyway
                }
            }, backgroundExecutorSupplier.apply(pos)).handle((compound, throwable) -> {
                if (throwable != null) future.completeExceptionally(throwable);
                else future.complete(compound);
                return null;
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
    }

    private void writeChunk(long pos, CompletionStage<Either<NbtCompound, byte[]>> nbtFuture) {
        CompletionStage<Void> writeFuture1 = nbtFuture.thenAcceptAsync(nbt -> {
            if (nbt == null) {
                if (this.cache.get(pos) == nbtFuture) {
                    try {
                        final ChunkPos pos1 = new ChunkPos(pos);
                        final RegionFile regionFile = ((IRegionBasedStorage) this.storage).invokeGetRegionFile(pos1);
                        regionFile.delete(pos1);
                    } catch (Throwable t) {
                        LOGGER.error("Error writing chunk %s".formatted(new ChunkPos(pos)), t);
                    }
                    this.cache.remove(pos);
                } // discard old data
            } else {
                ChunkCompressionFormat compressionFormat;
                {
                    final ChunkPos pos1 = new ChunkPos(pos);
                    try {
                        final RegionFile regionFile = ((IRegionBasedStorage) this.storage).invokeGetRegionFile(pos1);
                        compressionFormat = ((IRegionFile) regionFile).getCompressionFormat();
                    } catch (Throwable t) {
                        LOGGER.warn("Failed to get compression format for chunk %s".formatted(pos1), t);
                        compressionFormat = ChunkCompressionFormat.getCurrentFormat();
                    }
                }
                ChunkCompressionFormat finalCompressionFormat = compressionFormat;
                final CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        final RawByteArrayOutputStream out = new RawByteArrayOutputStream(8096);
                        // TODO [VanillaCopy] RegionFile.ChunkBuffer
                        out.write(0);
                        out.write(0);
                        out.write(0);
                        out.write(0);
                        out.write(finalCompressionFormat.getId());
                        try (DataOutputStream dataOutputStream = new DataOutputStream(finalCompressionFormat.wrap(out))) {
                            if (nbt.left().isPresent()) {
                                NbtIo.writeCompound(nbt.left().get(), dataOutputStream);
                            } else {
                                dataOutputStream.write(nbt.right().get());
                            }
                        }
                        return out;
                    } catch (Throwable t) {
                        SneakyThrow.sneaky(t);
                        return null; // Unreachable anyway
                    }
                }, GlobalExecutors.prioritizedScheduler.executor(16)).thenAcceptAsync(bytes -> { // still use priority 16 for writes
                    if (this.cache.remove(pos, nbtFuture)) { // only write if match to avoid overwrites
                        try {
                            final ChunkPos pos1 = new ChunkPos(pos);
                            final RegionFile regionFile = ((IRegionBasedStorage) this.storage).invokeGetRegionFile(pos1);
                            ByteBuffer byteBuffer = bytes.asByteBuffer();
                            // TODO [VanillaCopy] RegionFile.ChunkBuffer
                            byteBuffer.putInt(0, bytes.size() - 5 + 1);
                            ((IRegionFile) regionFile).invokeWriteChunk(pos1, byteBuffer);
                        } catch (Throwable t) {
                            SneakyThrow.sneaky(t);
                        }
                    }
                }, this.executor).handleAsync((unused, throwable) -> {
                    if (throwable != null)
                        LOGGER.error("Error writing chunk %s".formatted(new ChunkPos(pos)), throwable);
                    // TODO error retry

                    return null;
                }, this.executor);
                this.writeFutures.add(future);
            }
        }, this.executor);
        this.writeFutures.add(writeFuture1.toCompletableFuture());
    }

    private record ReadRequest(long pos, CompletableFuture<NbtCompound> future, @Nullable NbtScanner scanner) {
    }

    private record WriteRequest(long pos, Either<NbtCompound, byte[]> nbt) {
    }

}
