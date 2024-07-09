package net.fxnt.fxntchunks.items;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fxnt.fxntchunks.FXNTChunks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item CHUNK_WAND = registerItem("chunk_wand", new ChunkWand(new FabricItemSettings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(FXNTChunks.MOD_ID, name), item);
    }
    public static void registerModItems() {
        FXNTChunks.LOGGER.info("Registering Mod Items for " + FXNTChunks.MOD_ID);
    }
}
