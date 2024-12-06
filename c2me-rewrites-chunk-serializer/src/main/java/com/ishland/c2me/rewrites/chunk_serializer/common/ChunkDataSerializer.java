package com.ishland.c2me.rewrites.chunk_serializer.common;

import com.ishland.c2me.base.mixin.access.IBelowZeroRetrogen;
import com.ishland.c2me.base.mixin.access.IChunkTickScheduler;
import com.ishland.c2me.base.mixin.access.ISimpleTickScheduler;
import com.ishland.c2me.base.mixin.access.IState;
import com.ishland.c2me.base.mixin.access.IStructurePiece;
import com.ishland.c2me.base.mixin.access.IStructureStart;
import com.ishland.c2me.base.mixin.access.IUpgradeData;
import com.ishland.c2me.rewrites.chunk_serializer.common.utils.LithiumUtil;
import com.ishland.c2me.rewrites.chunk_serializer.common.utils.StarLightUtil;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtShort;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EightWayDirection;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkType;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.chunk.ReadableContainer.Serialized;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.SimpleTickScheduler;
import net.minecraft.world.tick.Tick;
import net.minecraft.world.tick.TickPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.LongStream;

@SuppressWarnings("JavadocReference")
public final class ChunkDataSerializer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final byte[] STRING_DATA_VERSION = NbtWriter.getAsciiStringBytes("DataVersion");
    private static final byte[] STRING_X_POS = NbtWriter.getAsciiStringBytes("xPos");
    private static final byte[] STRING_Y_POS = NbtWriter.getAsciiStringBytes("yPos");
    private static final byte[] STRING_Z_POS = NbtWriter.getAsciiStringBytes("zPos");
    private static final byte[] STRING_LAST_UPDATE = NbtWriter.getAsciiStringBytes("LastUpdate");
    private static final byte[] STRING_INHABITED_TIME = NbtWriter.getAsciiStringBytes("InhabitedTime");
    private static final byte[] STRING_STATUS = NbtWriter.getAsciiStringBytes("Status");
    private static final byte[] STRING_BLENDING_DATA = NbtWriter.getAsciiStringBytes("blending_data");
    private static final byte[] STRING_BELOW_ZERO_RETROGEN = NbtWriter.getAsciiStringBytes("below_zero_retrogen");
    private static final byte[] STRING_UPGRADE_DATA = NbtWriter.getAsciiStringBytes("upgrade_data");
    private static final byte[] STRING_IS_LIGHT_ON = NbtWriter.getAsciiStringBytes("isLightOn");
    private static final byte[] STRING_BLOCK_ENTITIES = NbtWriter.getAsciiStringBytes("block_entities");
    private static final byte[] STRING_PALETTE = NbtWriter.getAsciiStringBytes("palette");
    private static final byte[] STRING_DATA = NbtWriter.getAsciiStringBytes("data");
    private static final byte[] STRING_SECTIONS = NbtWriter.getAsciiStringBytes("sections");
    private static final byte[] STRING_BLOCK_STATES = NbtWriter.getAsciiStringBytes("block_states");
    private static final byte[] STRING_BIOMES = NbtWriter.getAsciiStringBytes("biomes");
    private static final byte[] STRING_BLOCK_LIGHT = NbtWriter.getAsciiStringBytes("BlockLight");
    private static final byte[] STRING_SKY_LIGHT = NbtWriter.getAsciiStringBytes("SkyLight");
    private static final byte[] STRING_OLD_NOISE = NbtWriter.getAsciiStringBytes("old_noise");
    private static final byte[] STRING_HEIGHTS = NbtWriter.getAsciiStringBytes("heights");
    private static final byte[] STRING_MIN_SECTION = NbtWriter.getAsciiStringBytes("min_section");
    private static final byte[] STRING_MAX_SECTION = NbtWriter.getAsciiStringBytes("max_section");
    private static final byte[] STRING_TARGET_STATUS = NbtWriter.getAsciiStringBytes("target_status");
    private static final byte[] STRING_MISSING_BEDROCK = NbtWriter.getAsciiStringBytes("missing_bedrock");
    private static final byte[] STRING_INDICES = NbtWriter.getAsciiStringBytes("Indices");
    private static final byte[] STRING_SIDES = NbtWriter.getAsciiStringBytes("Sides");
    private static final byte[] STRING_ENTITIES = NbtWriter.getAsciiStringBytes("entities");
    private static final byte[] STRING_LIGHTS = NbtWriter.getAsciiStringBytes("Lights");
    private static final byte[] STRING_CARVING_MASK = NbtWriter.getAsciiStringBytes("carving_mask");
    private static final byte[] STRING_HEIGHTMAPS = NbtWriter.getAsciiStringBytes("Heightmaps");
    private static final byte[] STRING_POST_PROCESSING = NbtWriter.getAsciiStringBytes("PostProcessing");
    private static final byte[] STRING_BLOCK_TICKS = NbtWriter.getAsciiStringBytes("block_ticks");
    private static final byte[] STRING_FLUID_TICKS = NbtWriter.getAsciiStringBytes("fluid_ticks");
    private static final byte[] STRING_STRUCTURES = NbtWriter.getAsciiStringBytes("structures");
    private static final byte[] STRING_STARTS = NbtWriter.getAsciiStringBytes("starts");
    private static final byte[] STRING_BIG_REFERENCES = NbtWriter.getAsciiStringBytes("References");
    private static final byte[] STRING_ID = NbtWriter.getAsciiStringBytes("id");
    private static final byte[] STRING_CHUNK_X = NbtWriter.getAsciiStringBytes("ChunkX");
    private static final byte[] STRING_CHUNK_Z = NbtWriter.getAsciiStringBytes("ChunkZ");
    private static final byte[] STRING_SMALL_REFERENCES = NbtWriter.getAsciiStringBytes("references");
    private static final byte[] STRING_CHILDREN = NbtWriter.getAsciiStringBytes("Children");
    private static final byte[] STRING_INVALID = NbtWriter.getAsciiStringBytes("INVALID");
    private static final byte[] STRING_BB = NbtWriter.getAsciiStringBytes("BB");
    private static final byte[] STRING_O = NbtWriter.getAsciiStringBytes("O");
    private static final byte[] STRING_GD = NbtWriter.getAsciiStringBytes("GD");
    private static final byte[] STRING_NAME = NbtWriter.getAsciiStringBytes("Name");
    private static final byte[] STRING_PROPERTIES = NbtWriter.getAsciiStringBytes("Properties");

    private static final byte[] STRING_CHAR_BIG_Y = NbtWriter.getAsciiStringBytes("Y");
    private static final byte[] STRING_CHAR_SMALL_I = NbtWriter.getAsciiStringBytes("i");
    private static final byte[] STRING_CHAR_SMALL_P = NbtWriter.getAsciiStringBytes("p");
    private static final byte[] STRING_CHAR_SMALL_T = NbtWriter.getAsciiStringBytes("t");
    private static final byte[] STRING_CHAR_SMALL_X = NbtWriter.getAsciiStringBytes("x");
    private static final byte[] STRING_CHAR_SMALL_Y = NbtWriter.getAsciiStringBytes("y");
    private static final byte[] STRING_CHAR_SMALL_Z = NbtWriter.getAsciiStringBytes("z");

    private static final byte[] STRING_C2ME = NbtWriter.getAsciiStringBytes("C2ME");
    private static final byte[] STRING_KROPPEB = NbtWriter.getAsciiStringBytes("Kroppeb was here :); Version: 0.3.0");

    private static final byte[] STRING_C2ME_MARK_A = NbtWriter.getAsciiStringBytes("C2ME::MarkA");
    private static final byte[] STRING_MARKER_FLUID_PROTO = NbtWriter.getAsciiStringBytes("fluid:proto");
    private static final byte[] STRING_MARKER_FLUID_FULL = NbtWriter.getAsciiStringBytes("fluid:full");
    private static final byte[] STRING_MARKER_FLUID_FALLBACK = NbtWriter.getAsciiStringBytes("fluid:fallback");

    // STARLIGHT
    private static final byte[] STRING_BLOCKLIGHT_STATE_TAG = NbtWriter.getAsciiStringBytes("starlight.blocklight_state");
    private static final byte[] STRING_SKYLIGHT_STATE_TAG = NbtWriter.getAsciiStringBytes("starlight.skylight_state");
    private static final byte[] STRING_STARLIGHT_VERSION_TAG = NbtWriter.getAsciiStringBytes("starlight.light_version");
    private static final int STARLIGHT_LIGHT_VERSION = 8;

    // TODO: validating starlight compatibility?
    private static final boolean STARLIGHT = FabricLoader.getInstance().isModLoaded("starlight");

    /**
     * Mirror of {@link SerializedChunk#serialize(ServerWorld, Chunk)}
     */
    public static void write(SerializedChunk serializable, NbtWriter writer) {
        ChunkPos chunkPos = serializable.chunkPos();

//        System.out.printf("Serializing chunk at: %d %d%n", chunkPos.x, chunkPos.z);

        writer.putString(STRING_C2ME, STRING_KROPPEB);
        writer.putInt(STRING_DATA_VERSION, SharedConstants.getGameVersion().getSaveVersion().getId());
        writer.putInt(STRING_X_POS, chunkPos.x);
        writer.putInt(STRING_Y_POS, serializable.minSectionY());
        writer.putInt(STRING_Z_POS, chunkPos.z);
        writer.putLong(STRING_LAST_UPDATE, serializable.lastUpdateTime());
        writer.putLong(STRING_INHABITED_TIME, serializable.inhabitedTime());
        writer.putString(STRING_STATUS, ((ChunkStatusAccessor) serializable.chunkStatus()).getIdBytes());

        BlendingData.Serialized blendingData = serializable.blendingData();
        if (blendingData != null) {
            // Inline codec
            writer.startCompound(STRING_BLENDING_DATA);
            writeBlendingData(writer, blendingData);
            writer.finishCompound();
        }

        BelowZeroRetrogen belowZeroRetrogen = serializable.belowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            // Inline codec
            writer.startCompound(STRING_BELOW_ZERO_RETROGEN);
            writeBelowZeroRetrogen(writer, (IBelowZeroRetrogen) (Object) belowZeroRetrogen);
            writer.finishCompound();
        }

        UpgradeData upgradeData = serializable.upgradeData();
        if (!upgradeData.isDone()) {
            // Inline serialization
            writer.startCompound(STRING_UPGRADE_DATA);
            writeUpgradeData(writer, (IUpgradeData) upgradeData);
            writer.finishCompound();
        }

        List<SerializedChunk.SectionData> sectionData = serializable.sectionData();

        Registry<Biome> biomeRegistry = serializable.biomeRegistry();

        checkLightFlag(serializable.lightCorrect(), writer);

        writeSectionData(writer, chunkPos, sectionData, biomeRegistry);


        writer.startFixedList(STRING_BLOCK_ENTITIES, serializable.blockEntities().size(), NbtElement.COMPOUND_TYPE);

        for (NbtCompound blockEntity : serializable.blockEntities()) {
            writer.putElementEntry(blockEntity);
        }


        if (serializable.chunkStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            final List<NbtCompound> entities = serializable.entities();
            writer.startFixedList(STRING_ENTITIES, entities.size(), NbtElement.COMPOUND_TYPE);
            for (NbtCompound entity : entities) {
                //noinspection deprecation
                writer.putElementEntry(entity);
            }

//            putShortListArray(j.getLightSourcesBySection(), writer, STRING_LIGHTS); // no longer exists after lighting update

            if (serializable.carvingMask() != null) {
                writer.putLongArray(STRING_CARVING_MASK, serializable.carvingMask());
            }
        }

        serializeTicks(writer, serializable.packedTicks());
        ShortList[] postProcessingLists = serializable.postProcessingSections();
        putShortListArray(postProcessingLists, writer, STRING_POST_PROCESSING);


        writer.startCompound(STRING_HEIGHTMAPS);
        for (Map.Entry<Heightmap.Type, long[]> entry : serializable.heightmaps().entrySet()) {
            writer.putLongArray(
                    ((HeightMapTypeAccessor) (Object) entry.getKey()).getNameBytes(),
                    entry.getValue()
            );
        }
        writer.finishCompound();

        writer.getVisitor().visit(STRING_STRUCTURES, serializable.structureData());
    }

    private static void checkLightFlag(boolean lightCorrect, NbtWriter writer) {
        if (STARLIGHT) {
            // starlight also has a check to see if the "level" isn't a "serverlevel"???
            if (lightCorrect) {
                writer.putBoolean(STRING_IS_LIGHT_ON, false);
            }
        } else {
            if (lightCorrect) {
                writer.putBoolean(STRING_IS_LIGHT_ON, true);
            }
        }
    }

    private static void putShortListArray(ShortList[] data, NbtWriter writer, byte[] name) {
        long start = writer.startList(name, NbtElement.LIST_TYPE);
        int count = 0;

        for (ShortList shortList : data) {
            if (shortList != null) {
                writer.startFixedListEntry(shortList.size(), NbtElement.SHORT_TYPE);
                for (Short short_ : shortList) {
                    writer.putShortEntry(short_);
                }
                count ++;
            }
        }

        writer.finishList(start, count);
    }

    private static void writeSectionData(
            NbtWriter writer,
            ChunkPos chunkPos,
            List<SerializedChunk.SectionData> sectionData,
            Registry<Biome> biomeRegistry
    ) {
//        if (STARLIGHT) {
//            writeSectionDataStarlight(writer, chunkPos, sectionData, biomeRegistry);
//        } else {
            writeSectionDataVanilla(writer, chunkPos, sectionData, biomeRegistry);
//        }
    }

    /**
     * Mirror section of {@link SerializedChunk#serialize(ServerWorld, Chunk)}
     */
    private static void writeSectionDataVanilla(
            NbtWriter writer,
            ChunkPos chunkPos,
            List<SerializedChunk.SectionData> sectionData,
            Registry<Biome> biomeRegistry
    ) {
        long sectionsStart = writer.startList(STRING_SECTIONS, NbtElement.COMPOUND_TYPE);
        int sectionCount = 0;

        for (int __i = 0, sectionDataSize = sectionData.size(); __i < sectionDataSize; __i++) {
            SerializedChunk.SectionData sectionDatum = sectionData.get(__i);
            boolean hasInner = false;
            if (sectionDatum.chunkSection() != null) {
                if (!hasInner) {
                    hasInner = true;
                    writer.compoundEntryStart();
                }

                writeBlockStates(writer, sectionDatum.chunkSection().getBlockStateContainer());
                writeBiomes(writer, sectionDatum.chunkSection().getBiomeContainer(), biomeRegistry);
            }
            
            if (sectionDatum.blockLight() != null) {
                if (!hasInner) {
                    hasInner = true;
                    writer.compoundEntryStart();
                }
                
                writer.putByteArray(STRING_BLOCK_LIGHT, sectionDatum.blockLight().asByteArray());
            }
            
            if (sectionDatum.skyLight() != null) {
                if (!hasInner) {
                    hasInner = true;
                    writer.compoundEntryStart();
                }
                
                writer.putByteArray(STRING_SKY_LIGHT, sectionDatum.skyLight().asByteArray());
            }
            
            if (hasInner) {
                writer.putByte(STRING_CHAR_BIG_Y, (byte) sectionDatum.y());
                writer.finishCompound();
                sectionCount ++;
            }
        }

        writer.finishList(sectionsStart, sectionCount);
    }

//    /**
//     * Mirror section of {@link ChunkSerializer#serialize(ServerWorld, Chunk)}
//     * with the changes by StarLight applied inline
//     */
//    private static void writeSectionDataStarlight(
//            NbtWriter writer,
//            ChunkPos chunkPos,
//            List<ChunkSerializer.SectionData> sectionData,
//            Registry<Biome> biomeRegistry
//    ) {
//        // START DIFF
//        boolean lit = chunk.isLightOn();
//        ChunkStatus status = chunk.getStatus();
//        boolean shouldWrite = lit && status.isAtLeast(ChunkStatus.LIGHT);
//        var blockNibbles = StarLightUtil.getBlockNibbles(chunk);
//        var skyNibbles = StarLightUtil.getSkyNibbles(chunk);
//        int minSection;
//        // END DIFF
//
//        long sectionsStart = writer.startList(STRING_SECTIONS, NbtElement.COMPOUND_TYPE);
//        int sectionCount = 0;
//
//        for (int i = minSection = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); ++i) {
//            int index = chunk.sectionCoordToIndex(i);
//            boolean bl2 = index >= 0 && index < chunkSections.length;
//
//            // START DIFF
////
////            ChunkNibbleArray blockLight = lightingProvider.get(LightType.BLOCK)
////                    .getLightSection(ChunkSectionPos.from(chunkPos, i));
////            ChunkNibbleArray skyLight = lightingProvider.get(LightType.SKY)
////                    .getLightSection(ChunkSectionPos.from(chunkPos, i));
//
//            var blockNibble = shouldWrite ? StarLightUtil.getSaveState(blockNibbles[i - minSection]) : null;
//            var skyNibble = shouldWrite ? StarLightUtil.getSaveState(skyNibbles[i - minSection]) : null;
//
//            if (bl2 || blockNibble != null || skyNibble != null) {
//                // END DIFF
//                boolean hasInner = false;
//                if (bl2) {
//                    hasInner = true;
//                    writer.compoundEntryStart();
//                    IChunkSection chunkSection = chunkSections[index];
//
//                    writeBlockStates(writer, chunkSection.getBlockStateContainer());
//                    writeBiomes(writer, chunkSection.getBiomeContainer(), biomeRegistry);
//                }
//
//                // START DIFF
////                if (blockLight != null && !blockLight.isUninitialized()) {
////                    if (!hasInner) {
////                        writer.compoundEntryStart();
////                        hasInner = true;
////                    }
////                    writer.putByteArray(STRING_BLOCK_LIGHT, blockLight.asByteArray());
////                }
//
////                if (skyLight != null && !skyLight.isUninitialized()) {
////                    if (!hasInner) {
////                        writer.compoundEntryStart();
////                        hasInner = true;
////                    }
////                    writer.putByteArray(STRING_SKY_LIGHT, skyLight.asByteArray());
////                }
//
//                if (blockNibble != null) {
//                    if (blockNibble.getData() != null) {
//                        writer.putByteArray(STRING_BLOCK_LIGHT, blockNibble.getData());
//                    }
//                    writer.putInt(STRING_BLOCKLIGHT_STATE_TAG, blockNibble.getState());
//                }
//
//                if (skyNibble != null) {
//                    if (skyNibble.getData() != null) {
//                        writer.putByteArray(STRING_SKY_LIGHT, skyNibble.getData());
//                    }
//                    writer.putInt(STRING_SKYLIGHT_STATE_TAG, skyNibble.getState());
//                }
//
//                // END DIFF
//
//
//                if (hasInner) {
//                    writer.putByte(STRING_CHAR_BIG_Y, (byte) i);
//                    writer.finishCompound();
//                    sectionCount++;
//                }
//            }
//        }
//
//        writer.finishList(sectionsStart, sectionCount);
//
//        if (lit) {
//            writer.putInt(STRING_STARLIGHT_VERSION_TAG, STARLIGHT_LIGHT_VERSION);
//        }
//    }

    /**
     * mirror of {@link SerializedChunk#CODEC}
     * created by {@link PalettedContainer#createPalettedContainerCodec(IndexedIterable, Codec, PalettedContainer.PaletteProvider, Object)}
     * with: {@link Block#STATE_IDS} as idList,
     * {@link BlockState#CODEC} as entryCodec,
     * {@link PalettedContainer.PaletteProvider#BLOCK_STATE} as paletteProvider,
     * {@link Blocks#AIR}{@code .getDefaultState()} as defaultValue
     */
    @SuppressWarnings("unchecked")
    private static void writeBlockStates(NbtWriter writer, PalettedContainer<BlockState> blockStateContainer) {
        writer.startCompound(STRING_BLOCK_STATES);
        // todo can this be optimized?
        // todo: does this conflict with lithium by any chance?
        var data = blockStateContainer.serialize(
                Block.STATE_IDS,
                PalettedContainer.PaletteProvider.BLOCK_STATE);

        List<BlockState> paletteEntries = data.paletteEntries();
        writer.startFixedList(STRING_PALETTE, paletteEntries.size(), NbtElement.COMPOUND_TYPE);

        for (BlockState paletteEntry : paletteEntries) {
            writer.compoundEntryStart();
            writer.putRegistry(STRING_NAME, Registries.BLOCK, paletteEntry.getBlock());
            if (!paletteEntry.getEntries().isEmpty()) {
                // TODO: optimize this
                writer.putElement(STRING_PROPERTIES, ((IState<BlockState>) paletteEntry).getCodec().codec()
                        .encodeStart(NbtOps.INSTANCE, paletteEntry)
                        .getOrThrow(SerializedChunk.ChunkLoadingException::new));
            }
            writer.finishCompound();
        }

        Optional<LongStream> storage = data.storage();
        //noinspection OptionalIsPresent
        if (storage.isPresent()) {
            writer.putLongArray(STRING_DATA, storage.get().toArray());
        }
        writer.finishCompound();
    }

    /**
     * mirror of local codec
     * created by {@link PalettedContainer#createReadableContainerCodec(IndexedIterable, Codec, PalettedContainer.PaletteProvider, Object)}
     * with: {@link Registry#getIndexedEntries()} as idList,
     * {@link Registry#getEntryCodec()} as entryCodec,
     * {@link PalettedContainer.PaletteProvider#BIOME} as paletteProvider,
     * {@link BiomeKeys#PLAINS} as defaultValue
     */
    private static void writeBiomes(NbtWriter writer, ReadableContainer<RegistryEntry<Biome>> biomeContainer, Registry<Biome> biomeRegistry) {
        writer.startCompound(STRING_BIOMES);
        // todo can this be optimized?
        // todo: does this conflict with lithium by any chance?
        var data = biomeContainer.serialize(
                biomeRegistry.getIndexedEntries(),
                PalettedContainer.PaletteProvider.BIOME);

        List<RegistryEntry<Biome>> paletteEntries = data.paletteEntries();
        writer.startFixedList(STRING_PALETTE, paletteEntries.size(), NbtElement.STRING_TYPE);

        for (RegistryEntry<Biome> paletteEntry : paletteEntries) {
            writer.putRegistryEntry(paletteEntry);
        }

        Optional<LongStream> storage = data.storage();
        //noinspection OptionalIsPresent
        if (storage.isPresent()) {
            writer.putLongArray(STRING_DATA, storage.get().toArray());
        }
        writer.finishCompound();
    }

    /**
     * mirror of {@link BlendingData#CODEC}
     */
    private static void writeBlendingData(NbtWriter writer, BlendingData.Serialized blendingData) {
        writer.putInt(STRING_MIN_SECTION, blendingData.minSection());
        writer.putInt(STRING_MAX_SECTION, blendingData.maxSection());

        double[] heights = blendingData.heights().orElse(null);
        if (heights != null) {
            writer.putDoubles(STRING_HEIGHTS, heights);
        }
    }

    /**
     * mirror of {@link BelowZeroRetrogen#CODEC}
     */
    private static void writeBelowZeroRetrogen(NbtWriter writer, IBelowZeroRetrogen belowZeroRetrogen) {
        writer.putRegistry(STRING_TARGET_STATUS, Registries.CHUNK_STATUS, belowZeroRetrogen.invokeGetTargetStatus());

        BitSet missingBedrock = belowZeroRetrogen.getMissingBedrock();
        if (!missingBedrock.isEmpty()) {
            writer.putLongArray(STRING_MISSING_BEDROCK, missingBedrock.toLongArray());
        }
    }


    private static void writeUpgradeData(NbtWriter writer, IUpgradeData upgradeData) {
        long indicesStart = -1;
        int indicesCount = 0;

        int[][] centerIndicesToUpgrade = upgradeData.getCenterIndicesToUpgrade();

        for (int i = 0; i < centerIndicesToUpgrade.length; ++i) {
            if (centerIndicesToUpgrade[i] != null && centerIndicesToUpgrade[i].length != 0) {
                String string = String.valueOf(i);
                if (indicesStart == -1) {
                    indicesStart = writer.startList(STRING_INDICES, NbtElement.INT_ARRAY_TYPE);
                }
                indicesCount++;
                // TODO: cache this
                writer.putIntArray(NbtWriter.getAsciiStringBytes(string), centerIndicesToUpgrade[i]);
            }
        }

        if (indicesStart != -1) {
            writer.finishList(indicesStart, indicesCount);
        }

        int i = 0;

        for (EightWayDirection eightWayDirection : upgradeData.getSidesToUpgrade()) {
            i |= 1 << eightWayDirection.ordinal();
        }

        writer.putByte(STRING_SIDES, (byte) i);
    }

    @Deprecated
    public static NbtList toNbt(ShortList[] lists) {
        NbtList nbtList = new NbtList();

        for (ShortList shortList : lists) {
            NbtList nbtList2 = new NbtList();
            if (shortList != null) {
                for (Short short_ : shortList) {
                    nbtList2.add(NbtShort.of(short_));
                }
            }

            nbtList.add(nbtList2);
        }

        return nbtList;
    }


    /**
     * mirror of {@link SerializedChunk#serializeTicks(ServerWorld, NbtCompound, Chunk.TickSchedulers)}
     */
    private static void serializeTicks(NbtWriter writer, Chunk.TickSchedulers tickSchedulers) {
        writeTicks(writer, tickSchedulers.blocks(), Registries.BLOCK, STRING_BLOCK_TICKS);
        writeTicks(writer, tickSchedulers.fluids(), Registries.FLUID, STRING_FLUID_TICKS);
    }


    /**
     * mirrors of {@link SimpleTickScheduler#toNbt(long, Function)},
     * {@link ChunkTickScheduler#toNbt(long, Function)} and
     * {@link Tick#toNbt(Function)}
     */
    private static <T> void writeTicks(
            NbtWriter writer,
            List<Tick<T>> scheduledTicks,
            DefaultedRegistry<T> reg,
            byte[] key
    ) {
        writer.startFixedList(key, scheduledTicks.size(), NbtElement.COMPOUND_TYPE);
        for (Tick<T> scheduledTick : scheduledTicks) {
            writeTick(writer, scheduledTick, reg);
        }
    }

    private static <T> void writeOrderedTick(NbtWriter writer, OrderedTick<T> orderedTick, long time, Registry<T> reg) {
        writer.compoundEntryStart();
        writer.putRegistry(STRING_CHAR_SMALL_I, reg, orderedTick.type());
        writeGenericTickData(writer, orderedTick.pos(), (int) (orderedTick.triggerTick() - time), orderedTick.priority());
        writer.finishCompound();
    }

    private static <T> void writeTick(NbtWriter writer, Tick<T> scheduledTick, Registry<T> reg) {
        writer.compoundEntryStart();
        writer.putRegistry(STRING_CHAR_SMALL_I, reg, scheduledTick.type());
        writeGenericTickData(writer, scheduledTick);
        writer.finishCompound();
    }

    private static void writeGenericTickData(
            NbtWriter writer,
            Tick<?> scheduledTick) {
        writeGenericTickData(writer, scheduledTick.pos(), scheduledTick.delay(), scheduledTick.priority());
    }

    private static void writeGenericTickData(
            NbtWriter writer,
            BlockPos pos,
            int delay,
            TickPriority priority) {
        writer.putInt(STRING_CHAR_SMALL_X, pos.getX());
        writer.putInt(STRING_CHAR_SMALL_Y, pos.getY());
        writer.putInt(STRING_CHAR_SMALL_Z, pos.getZ());
        writer.putInt(STRING_CHAR_SMALL_T, delay);
        writer.putInt(STRING_CHAR_SMALL_P, priority.getIndex());
    }

    /**
     * mirror of {@link SerializedChunk#writeStructures(StructureContext, ChunkPos, Map, Map)}
     */
    private static void writeStructures(
            NbtWriter writer,
            StructureContext context,
            ChunkPos pos,
            Map<Structure, StructureStart> starts,
            Map<Structure, LongSet> references
    ) {
        writer.startCompound(STRING_STRUCTURES);
        writer.startCompound(STRING_STARTS);

        Registry<Structure> configuredStructureFeatureRegistry = context.registryManager().getOrThrow(RegistryKeys.STRUCTURE);

        for (var entry : starts.entrySet()) {
            writer.startCompound(NbtWriter.getNameBytesFromRegistry(configuredStructureFeatureRegistry, entry.getKey()));
            IStructureStart value = cast(entry.getValue());
            writeStructureStart(writer, value, context, pos);
            writer.finishCompound();
        }
        writer.finishCompound();

        writer.startCompound(STRING_BIG_REFERENCES);
        for (var entry : references.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            writer.putLongArray(NbtWriter.getNameBytesFromRegistry(configuredStructureFeatureRegistry, entry.getKey()), entry.getValue());
        }
        writer.finishCompound();

        writer.finishCompound();
    }


    /**
     * mirror of {@link StructureStart#toNbt(StructureContext, ChunkPos)}
     * <p>
     * section mirror of {@link StructurePiecesList#toNbt(StructureContext)}
     */
    private static void writeStructureStart(NbtWriter writer, IStructureStart structureStart, StructureContext context, ChunkPos pos) {
        final StructurePiecesList children = structureStart.getChildren();
        if (children.isEmpty()) {
            writer.putString(STRING_ID, STRING_INVALID);
            return;
        }

        writer.putRegistry(STRING_ID, context.registryManager().getOrThrow(RegistryKeys.STRUCTURE), structureStart.getStructure());
        writer.putInt(STRING_CHUNK_X, pos.x);
        writer.putInt(STRING_CHUNK_Z, pos.z);
        writer.putInt(STRING_SMALL_REFERENCES, structureStart.getReferences());

        // section: StructurePiecesList#toNbt(StructureContext)
        writer.startFixedList(STRING_CHILDREN, children.pieces().size(), NbtElement.COMPOUND_TYPE);
        for (StructurePiece piece : children.pieces()) {
            writer.putElementEntry(piece.toNbt(context));
            // TODO: writeStructurePiece(writer,(StructurePieceAccessor) piece, context);
        }
    }

    @SuppressWarnings("unused")
    private static void writeStructurePiece(NbtWriter writer, IStructurePiece structurePiece, StructureContext context) {
        writer.compoundEntryStart();
        writer.putRegistry(STRING_ID, Registries.STRUCTURE_PIECE, structurePiece.getType());

        final Optional<NbtElement> optional = BlockBox.CODEC.encodeStart(NbtOps.INSTANCE, structurePiece.getBoundingBox()).resultOrPartial(LOGGER::error);

        //noinspection OptionalIsPresent
        if (optional.isPresent()) {
            writer.putElement(STRING_BB, optional.get());
        }

        Direction direction = structurePiece.getFacing();
        writer.putInt(STRING_O, direction == null ? -1 : direction.getHorizontal());
        writer.putInt(STRING_GD, structurePiece.getChainLength());
        // FML, didn't think about this one
        // this.writeNbt(context, nbtCompound);
        writer.finishCompound();
    }


    @SuppressWarnings("unchecked")
    @Contract("null -> null; !null -> !null")
    private static <T> T cast(Object entry) {
        return (T) entry;
    }
}