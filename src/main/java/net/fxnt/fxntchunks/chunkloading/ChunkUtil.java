package net.fxnt.fxntchunks.chunkloading;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class ChunkUtil {

    public static ForceLoadedChunkData getChunkData(String chunkKey) {
        String[] chunkData = readChunkKey(chunkKey);
        String dimension = chunkData[0];
        int chunkPosX = Integer.parseInt(chunkData[1]);
        int chunkPosZ = Integer.parseInt(chunkData[2]);
        ChunkPos chunkPos = new ChunkPos(chunkPosX, chunkPosZ);
        String type = chunkData[3];
        String playerID = chunkData[4];
        String playerName = chunkData[5];
        return new ForceLoadedChunkData(dimension, chunkPos, type, playerID, playerName);
    }


    public static String[] readChunkKey(String chunkKey) {
        return chunkKey.split(",");
    }

    public static String generateChunkKey(String dimension, ChunkPos chunkPos, String type, String playerID, String playerName) {
        return dimension + "," + chunkPos.x + "," + chunkPos.z + "," + type + "," + playerID + "," + playerName;
    }

    public static String generateChunkKey(ForceLoadedChunkData chunkData) {
        return generateChunkKey(chunkData.dimension, chunkData.chunkPos, chunkData.type, chunkData.playerID, chunkData.playerName);
    }

    public static boolean isForceLoaded(ServerLevel level, ChunkPos chunkPos) {

        LongSet forceLoadedChunksLongSet = level.getForcedChunks();
        List<ChunkPos> forceLoadedChunks = convertLongSetToChunkPosSet(forceLoadedChunksLongSet);
        for(ChunkPos forceLoadedChunkPos : forceLoadedChunks) {
            if (forceLoadedChunkPos.equals(chunkPos)) {
                return true;
            }
        }
        return false;

    }

    public static List<ChunkPos> getForceLoadedChunks(ServerLevel level) {
        LongSet forceLoadedChunksLongSet = level.getForcedChunks();
        return convertLongSetToChunkPosSet(forceLoadedChunksLongSet);
    }

    public static List<ChunkPos> convertLongSetToChunkPosSet(LongSet longSet) {
        List<ChunkPos> chunkPosSet = new ArrayList<>();
        for (long chunkLong : longSet) {
            int x = (int) chunkLong;
            int z = (int) (chunkLong >> 32);
            chunkPosSet.add(new ChunkPos(x, z));
        }
        return chunkPosSet;
    }
}
