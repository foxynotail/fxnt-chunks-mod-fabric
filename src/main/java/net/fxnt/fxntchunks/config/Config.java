package net.fxnt.fxntchunks.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;

public class Config {

    private Config() {}
    public static final String GENERAL_CATEGORY = "general_settings";
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.ConfigValue<Integer> MAX_FORCE_LOADED_CHUNKS;
    public static ForgeConfigSpec.ConfigValue<Integer> MAX_FORCE_TICKING_CHUNKS;
    public static ForgeConfigSpec.ConfigValue<Integer> MAX_PLAYER_FORCE_LOADED_CHUNKS;
    public static ForgeConfigSpec.ConfigValue<Integer> MAX_PLAYER_FORCE_TICKING_CHUNKS;

    static {

        COMMON_BUILDER.comment("General Settings").push(GENERAL_CATEGORY);

        MAX_FORCE_LOADED_CHUNKS = COMMON_BUILDER
                .comment("How many chunks can be force loaded globally")
                .define("max_global_force_loaded_chunks", 50);

        MAX_FORCE_TICKING_CHUNKS = COMMON_BUILDER
                .comment("How many chunks can be force ticked globally")
                .define("max_global_force_ticking_chunks", 25);

        MAX_PLAYER_FORCE_LOADED_CHUNKS = COMMON_BUILDER
                .comment("How many chunks can be force loaded per player")
                .define("max_player_force_loaded_chunks", 10);

        MAX_PLAYER_FORCE_TICKING_CHUNKS = COMMON_BUILDER
                .comment("How many chunks can be force ticked per player")
                .define("max_player_force_ticking_chunks", 5);


        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .preserveInsertionOrder()
                .build();
        configData.load();
        spec.setConfig(configData);
    }
}
