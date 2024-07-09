package net.fxnt.fxntchunks.chunkloading;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fxnt.fxntchunks.FXNTChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ChunkServer {
    public static boolean INITIALIZED = false;
    public static Set<String> FORCE_LOADED_CHUNKS = new HashSet<>();
    public static boolean FORCE_LOADING_STATUS = true;
    public static ResourceLocation FORCE_LOADED_CHUNK_CHANNEL = new ResourceLocation(FXNTChunks.MOD_ID, "force_loaded_chunk_channel");

    public static void updateForceLoadedChunks(MinecraftServer server) {

        if (!INITIALIZED) return;

        // If force loading status disabled, unload all non vanilla chunks
        for (String listedChunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(listedChunkKey);
            if (!Objects.equals(chunkData.type, "VANILLA")) {
                if (!FORCE_LOADING_STATUS) {
                    // Unload all chunks if disabled
                    unloadChunk(server, chunkData);
                } else {
                    // Load unloaded chunks if enabled
                    loadChunk(server, chunkData);
                }
            }
        }

        if (!FORCE_LOADING_STATUS) return;

        // Determine if all forced keys are already listed
        // Get all force loaded chunks
        Set<String> allForcedChunkKeys = getAllLoadedChunks(server);
        for (String allChunkKey : allForcedChunkKeys) {
            ForceLoadedChunkData allChunkData = ChunkUtil.getChunkData(allChunkKey);
            boolean exists = false;
            for (String listedChunkKey : FORCE_LOADED_CHUNKS) {
                ForceLoadedChunkData listedChunkData = ChunkUtil.getChunkData(listedChunkKey);
                if (allChunkKey.equals(listedChunkKey) || (
                        Objects.equals(allChunkData.dimension, listedChunkData.dimension) && Objects.equals(allChunkData.chunkPos, listedChunkData.chunkPos))
                ) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(allChunkKey);
                FORCE_LOADED_CHUNKS.add(ChunkUtil.generateChunkKey(chunkData.dimension, chunkData.chunkPos, chunkData.type, chunkData.playerID, chunkData.playerName));
            }
        }

        // Determine if all vanilla listed keys are still loaded
        // Refresh in case new chunks loaded / unloaded
        allForcedChunkKeys = getAllLoadedChunks(server);
        Set<String> keysToRemove = new HashSet<>();

        for (String listedChunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData listedChunkData = ChunkUtil.getChunkData(listedChunkKey);
            boolean exists = false;
            for (String allChunkKey : allForcedChunkKeys) {
                ForceLoadedChunkData allChunkData = ChunkUtil.getChunkData(allChunkKey);
                if (Objects.equals(allChunkData.dimension, listedChunkData.dimension) && Objects.equals(allChunkData.chunkPos, listedChunkData.chunkPos)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                keysToRemove.add(listedChunkKey);
            }
        }
        if (!keysToRemove.isEmpty()) {
            for (String removeKey : keysToRemove) {
                ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(removeKey);
                unloadChunk(server, chunkData);
                removeChunk(chunkData.dimension, chunkData.chunkPos);
            }
        }
    }

    public static Set<String> getAllForceLoadedChunks() {
        Set <String> chunkKeys = new HashSet<>();
        for (String chunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
            if (chunkData.type.equals("FORCELOAD") || chunkData.type.equals("FORCETICK")) {
                chunkKeys.add(chunkKey);
            }
        }
        return chunkKeys;
    }

    public static Set<String> getAllForceTickingChunks() {
        Set <String> chunkKeys = new HashSet<>();
        for (String chunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
            if (chunkData.type.equals("FORCETICK")) {
                chunkKeys.add(chunkKey);
            }
        }
        return chunkKeys;
    }

    public static Set<String> getPlayerForceLoadedChunks(String PlayerID) {
        Set <String> chunkKeys = new HashSet<>();
        for (String chunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
            if (chunkData.playerID.equals(PlayerID)) {
                chunkKeys.add(chunkKey);
            }
        }
        return chunkKeys;
    }

    public static Set<String> getPlayerForceTickingChunks(String PlayerID) {
        Set <String> chunkKeys = new HashSet<>();
        for (String chunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
            if (chunkData.playerID.equals(PlayerID) && chunkData.type.equals("FORCETICK")) {
                chunkKeys.add(chunkKey);
            }
        }
        return chunkKeys;
    }

    private static Set<String> getAllLoadedChunks(MinecraftServer server) {
        Set<String> chunkKeys = new HashSet<>();
        server.getAllLevels().forEach(serverLevel -> {
            String dimension = serverLevel.dimension().location().toString();
            List<ChunkPos> chunks = ChunkUtil.getForceLoadedChunks(serverLevel);
            for (ChunkPos chunkPos : chunks) {
                chunkKeys.add(ChunkUtil.generateChunkKey(dimension, chunkPos, "VANILLA", "SERVER", "NULL"));
            }
        });
        return chunkKeys;
    }

    private static void loadChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!ChunkUtil.isForceLoaded(level, chunkPos)) {
            FXNTChunks.LOGGER.info("Force Load Chunk: {} {}", level.dimension().location().toString(), chunkPos);
            level.setChunkForced(chunkPos.x, chunkPos.z, true);
        }
    }

    private static void loadChunk(MinecraftServer server, ForceLoadedChunkData chunkData) {
        if (!FORCE_LOADING_STATUS) return;
        Set<ServerLevel> allLevels = new HashSet<>();
        server.getAllLevels().forEach(allLevels::add);
        for (ServerLevel level : allLevels) {
            if (level.dimension().location().toString().equals(chunkData.dimension)) {
                loadChunk(level, chunkData.chunkPos);
            }
        }
    }

    private static void unloadChunk(ServerLevel level, ChunkPos chunkPos) {
        if (ChunkUtil.isForceLoaded(level, chunkPos)) {
            FXNTChunks.LOGGER.info("Disable Force Loading on Chunk: {} {}", level.dimension().location().toString(), chunkPos);
            level.setChunkForced(chunkPos.x, chunkPos.z, false);
        }
    }

    private static void unloadChunk(MinecraftServer server, ForceLoadedChunkData chunkData) {
        Set<ServerLevel> allLevels = new HashSet<>();
        server.getAllLevels().forEach(allLevels::add);
        for (ServerLevel level : allLevels) {
            if (level.dimension().location().toString().equals(chunkData.dimension)) {
                unloadChunk(level, chunkData.chunkPos);
            }
        }
    }

    public static ForceLoadedChunkData getChunkData(String dimension, ChunkPos chunkPos) {
        ForceLoadedChunkData chunkData = null;
        List<String> forceLoadedChunks = FORCE_LOADED_CHUNKS.stream().toList();
        for (String chunkKey : forceLoadedChunks) {
            ForceLoadedChunkData thisChunkData = ChunkUtil.getChunkData(chunkKey);
            if (Objects.equals(thisChunkData.dimension, dimension) && Objects.equals(thisChunkData.chunkPos, chunkPos)) {
                chunkData = thisChunkData;
            }
        }
        return chunkData;
    }

    public static ForceLoadedChunkData getChunkData(ServerLevel level, ChunkPos chunkPos) {
        String dimension = level.dimension().location().toString();
        return getChunkData(dimension, chunkPos);
    }

    public static void addChunk(String dimension, ChunkPos chunkPos, String type, String playerID, String playerName) {
        FORCE_LOADED_CHUNKS.add(ChunkUtil.generateChunkKey(dimension, chunkPos, type, playerID, playerName));
    }
    public static void addChunk(ServerLevel level, ChunkPos chunkPos, String type, String playerID, String playerName) {
        String dimension = level.dimension().location().toString();
        addChunk(dimension, chunkPos, type, playerID, playerName);
        loadChunk(level, chunkPos);
        saveForceLoadedChunks(level.getServer());
        ChunkServer.sendToClient(level.getServer());
    }

    public static void removeChunk(String dimension, ChunkPos chunkPos) {
        FORCE_LOADED_CHUNKS.removeIf(chunkKey -> {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
            return Objects.equals(chunkData.dimension, dimension) && Objects.equals(chunkData.chunkPos, chunkPos);
        });
    }

    public static void removeChunk(ServerLevel level, ChunkPos chunkPos) {
        String dimension = level.dimension().location().toString();
        removeChunk(dimension, chunkPos);
        unloadChunk(level, chunkPos);
        saveForceLoadedChunks(level.getServer());
        ChunkServer.sendToClient(level.getServer());
    }

    public static void updateChunkType(String dimension, ChunkPos chunkPos, String type, String playerID, String playerName) {
        removeChunk(dimension, chunkPos);
        addChunk(dimension, chunkPos, type, playerID, playerName);
    }
    public static void updateChunkType(ServerLevel level, ChunkPos chunkPos, String type, String playerID, String playerName) {
        String dimension = level.dimension().location().toString();
        updateChunkType(dimension, chunkPos, type, playerID, playerName);
        saveForceLoadedChunks(level.getServer());
        ChunkServer.sendToClient(level.getServer());
    }

    public static void sendToClient(MinecraftServer server) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (String chunkKey : FORCE_LOADED_CHUNKS) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putString("ChunkKey", chunkKey);
            listTag.add(chunkTag);
        }
        tag.put("Chunks", listTag);
        tag.putBoolean("Status", FORCE_LOADING_STATUS);
        buf.writeNbt(tag);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, FORCE_LOADED_CHUNK_CHANNEL, buf);
        }
    }

    public static void sortChunkList() {
        List<String> sortedList = new ArrayList<>(FORCE_LOADED_CHUNKS);
        Collections.sort(sortedList);
        FORCE_LOADED_CHUNKS = new HashSet<>(sortedList);
    }

    public static void saveForceLoadedChunks(MinecraftServer server) {
        StringBuilder stringBuilder = new StringBuilder();
        if (FORCE_LOADING_STATUS) {
            stringBuilder.append("status:enabled");
        } else {
            stringBuilder.append("status:disabled");
        }
        stringBuilder.append("\n");

        sortChunkList();
        List<String> forceLoadedChunks = FORCE_LOADED_CHUNKS.stream().toList();
        for (String chunkKey : forceLoadedChunks) {
            stringBuilder.append(chunkKey).append("\n");
        }

        // Write the JSON string to a file
        try (FileWriter fileWriter = new FileWriter(FXNTChunks.CHUNK_DATA_FILE_PATH.toString())) {
            fileWriter.write(stringBuilder.toString());
        } catch (IOException e) {
            FXNTChunks.LOGGER.error("Error Saving Force Loaded Chunk Data: {}", e.toString());
        }
    }

    public static void loadForceLoadedChunks() {

        FXNTChunks.LOGGER.info("Loading Force Loaded Chunk Data");
        FORCE_LOADED_CHUNKS.clear();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FXNTChunks.CHUNK_DATA_FILE_PATH.toString()));

            String chunkKey  = reader.readLine();
            while (chunkKey != null) {
                if (chunkKey.contains("status:")) {
                    String[] parts = chunkKey.split(":");
                    FORCE_LOADING_STATUS = Objects.equals(parts[1], "enabled");
                } else {
                    if (!chunkKey.trim().isEmpty()) {
                        FORCE_LOADED_CHUNKS.add(chunkKey);
                    }
                }
                chunkKey = reader.readLine();
            }

            reader.close();
            INITIALIZED = true;
        } catch (IOException e) {
            FXNTChunks.LOGGER.error("Force Loaded Chunk Data File Not Found");
        }
    }

    public static void randomTickForceLoadedChunks(MinecraftServer server) {
        if (!INITIALIZED) return;
        if (FXNTChunks.randomTickSpeed == 0) return;

        Set<ServerLevel> allLevels = new HashSet<>();
        server.getAllLevels().forEach(allLevels::add);

        for (String chunkKey : FORCE_LOADED_CHUNKS) {
            ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
            if (!chunkData.type.equals("FORCETICK")) continue;

            ServerLevel level = null;
            for (ServerLevel serverLevel : allLevels) {
                if (serverLevel.dimension().location().toString().equals(chunkData.dimension)) {
                    level = serverLevel;
                }
            }
            if (level == null) return;

            LevelChunk chunk = level.getChunk(chunkData.chunkPos.x, chunkData.chunkPos.z);

            ProfilerFiller profiler = level.getProfiler();
            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            int yOffset = level.getMinBuildHeight();
            for(LevelChunkSection chunkSection : chunk.getSections()) {

                if (chunkSection != null && !chunkSection.hasOnlyAir() && chunkSection.isRandomlyTicking()) {

                    for (int m = 0; m < FXNTChunks.randomTickSpeed; m++) {

                        RandomSource random = level.random;
                        BlockPos randomPosInChunk = level.getBlockRandomPos(startX, yOffset, startZ, 15);
                        profiler.push("randomTick");
                        BlockState blockState = chunkSection.getBlockState(
                                randomPosInChunk.getX() - startX,
                                randomPosInChunk.getY() - yOffset,
                                randomPosInChunk.getZ() - startZ);

                        if (blockState.isRandomlyTicking()) {
                            blockState.randomTick(level, randomPosInChunk, random);
                        }
                        FluidState fluidState = blockState.getFluidState();
                        if (fluidState.isRandomlyTicking()) {
                            fluidState.randomTick(level, randomPosInChunk, random);
                        }
                        profiler.pop();
                    }
                }
                yOffset += LevelChunkSection.SECTION_HEIGHT;
            }
        }
    }

    public static boolean getForceLoadingStatus() {
        return FORCE_LOADING_STATUS;
    }
    public static void enableForceLoading() {
        FORCE_LOADING_STATUS = true;
    }

    public static void disableForceLoading() {
        FORCE_LOADING_STATUS = false;
    }
}
