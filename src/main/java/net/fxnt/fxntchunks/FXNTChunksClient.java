package net.fxnt.fxntchunks;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fxnt.fxntchunks.chunkloading.ChunkClient;

@Environment(EnvType.CLIENT)
public class FXNTChunksClient implements ClientModInitializer {
    public void onInitializeClient() {
        ChunkClient.register();
        WorldRenderEvents.AFTER_SETUP.register(ChunkClient::renderForceLoadedChunks);
    }
}
