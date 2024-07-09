package net.fxnt.fxntchunks;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fxnt.fxntchunks.chunkloading.ChunkServer;
import net.fxnt.fxntchunks.config.Config;
import net.fxnt.fxntchunks.items.ModItems;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FXNTChunks implements ModInitializer {
    public static final String MOD_ID = "fxntchunks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static int serverTick = 0;
    public static int randomTickSpeed = 3;
    public static String CONFIG_FILE = MOD_ID + "-config.toml";
    public static Path CONFIG_FILE_PATH = Path.of(CONFIG_FILE);
    public static String CHUNK_DATA_FILE = MOD_ID + "-chunks.txt";
    public static Path CHUNK_DATA_FILE_PATH = Path.of(CHUNK_DATA_FILE);

    @Override
    public void onInitialize() {

        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.SERVER, Config.COMMON_CONFIG);
        ModItems.registerModItems();

        ServerWorldEvents.LOAD.register((server, level) -> {
            // Set file paths to world folders
            CHUNK_DATA_FILE_PATH = server.getWorldPath(LevelResource.ROOT).resolve( CHUNK_DATA_FILE);
            CONFIG_FILE_PATH = server.getWorldPath(LevelResource.ROOT).resolve(CONFIG_FILE);

            // Load Forge Config from file
            Config.loadConfig(Config.COMMON_CONFIG, CONFIG_FILE_PATH);

            // Reload Config on /reload command
            ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((s, m) -> Config.loadConfig(Config.COMMON_CONFIG, CONFIG_FILE_PATH));

            // Load Data from Chunk Data File
            ChunkServer.loadForceLoadedChunks();
        });


        ServerWorldEvents.UNLOAD.register((server, level) -> {
            ChunkServer.saveForceLoadedChunks(server);
        });

        ServerTickEvents.START_SERVER_TICK.register((server) -> {

            // Run Every Tick
            randomTickSpeed = server.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            ChunkServer.randomTickForceLoadedChunks(server);

            // Run Every Second
            if (serverTick % 20 == 0) {
                ChunkServer.updateForceLoadedChunks(server);
                ChunkServer.sendToClient(server);
            }
            if (serverTick >= 20) {
                serverTick = 0;
            } else {
                serverTick++;
            }
        });
    }

}
