package net.fxnt.fxntchunks.chunkloading;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fxnt.fxntchunks.FXNTChunks;
import net.fxnt.fxntchunks.items.ChunkWand;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ChunkClient {

    public static Set<String> FORCE_LOADED_CHUNKS = new HashSet<>();
    public static boolean FORCE_LOADING_STATUS = true;
    public static ResourceLocation FORCE_LOADED_CHUNK_CHANNEL = new ResourceLocation(FXNTChunks.MOD_ID, "force_loaded_chunk_channel");

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FORCE_LOADED_CHUNK_CHANNEL, (client, handler, buf, responseSender) -> {
            CompoundTag tag = buf.readNbt();
            client.execute(() -> {
                if (tag == null) return;
                if (!tag.contains("Chunks", Tag.TAG_LIST)) return;
                FORCE_LOADED_CHUNKS.clear();
                ListTag chunks = tag.getList("Chunks", Tag.TAG_COMPOUND);
                for (int i = 0; i < chunks.size(); i++) {
                    CompoundTag chunkTag = chunks.getCompound(i);
                    String chunkKey = chunkTag.getString("ChunkKey");
                    FORCE_LOADED_CHUNKS.add(chunkKey);
                }
                FORCE_LOADING_STATUS = tag.getBoolean("Status");
            });
        });
    }
    
    public static void renderForceLoadedChunks(WorldRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;

        PoseStack poseStack = context.matrixStack();
        MultiBufferSource bufferSource = context.consumers();

        if (bufferSource == null) return;

        ClientLevel level = minecraft.level;
        if (level == null) return;
        Player player = minecraft.player;
        if (player == null) return;
        ItemStack handItem = player.getMainHandItem();
        if (!(handItem.getItem() instanceof ChunkWand)) return;

        ChunkPos chunkPos = new ChunkPos(player.chunkPosition().x, player.chunkPosition().z);

        Camera camera = minecraft.gameRenderer.getMainCamera();
        float minY = (float)((double)level.getMinBuildHeight() - camera.getPosition().y);
        float maxY = (float)((double)level.getMaxBuildHeight() - camera.getPosition().y);

        float minX = (float)((double)chunkPos.getMinBlockX() - camera.getPosition().x);
        float minZ = (float)((double)chunkPos.getMinBlockZ() - camera.getPosition().z);

        float maxX = (float)((double)chunkPos.getMaxBlockX() - camera.getPosition().x) + 1;
        float maxZ = (float)((double)chunkPos.getMaxBlockZ() - camera.getPosition().z) + 1;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugQuads());

        Matrix4f matrix4f = poseStack.last().pose();
        int chunkX;
        int chunkZ;

        poseStack.pushPose();

        for(chunkX = -5; chunkX <= 6; chunkX++) {
            for(chunkZ = -5; chunkZ <= 6; chunkZ++) {

                ChunkPos thisChunkPos = new ChunkPos(chunkPos.x + chunkX, chunkPos.z + chunkZ);

                String chunkType = "";
                for (String chunkKey : ChunkClient.FORCE_LOADED_CHUNKS) {
                    ForceLoadedChunkData chunkData = ChunkUtil.getChunkData(chunkKey);
                    if (Objects.equals(chunkData.dimension, level.dimension().location().toString()) && Objects.equals(chunkData.chunkPos, thisChunkPos)) {
                        chunkType = chunkData.type;
                    }
                }

                if (chunkType.isEmpty()) continue;

                float[] color = getColorByChunkType(chunkType);

                int blockX = chunkX * 16;
                int blockZ = chunkZ * 16;

                float chunkMinX = minX + blockX + 0.25f;
                float chunkMinZ = minZ + blockZ + 0.25f;
                float chunkMaxX = maxX + blockX - 0.25f;
                float chunkMaxZ = maxZ + blockZ - 0.25f;

                float yDistance = 3f;
                float lineThickness = 0.1f;

                float y;
                for (y = minY; y < maxY; y+= yDistance) {

                    //float partialTicks = (float) (Math.sin((double) Util.getMillis() / 1000) * 0.5 + 0.5);
                    float cycleDuration = 2.0f; // Duration for one full cycle (upwards motion and snap back)
                    float timeInCycle = (Util.getMillis() % (long)(cycleDuration * 1000)) / 1000.0f;
                    float partialTicks = timeInCycle / cycleDuration;

                    float chunkMinY = Mth.lerp(partialTicks, y, y + yDistance);
                    float chunkMaxY = Mth.lerp(partialTicks, y + lineThickness, y + yDistance + lineThickness);
                    float[] coords = new float[]{chunkMinX, chunkMinY, chunkMinZ, chunkMaxX, chunkMaxY, chunkMaxZ};
                    renderChunkLines(poseStack, vertexConsumer, matrix4f, coords, color);
                }
            }
        }
        poseStack.popPose();

    }
    
    public static void renderChunkLines(PoseStack poseStack, VertexConsumer vertexConsumer, Matrix4f matrix4f, float[] pos, float[] color) {

        float minX = pos[0];
        float minY = pos[1];
        float minZ = pos[2];
        float maxX = pos[3];
        float maxY = pos[4];
        float maxZ = pos[5];
        
        float red = color[0];
        float green = color[1];
        float blue = color[2];
        float alpha = color[3];

        poseStack.pushPose();
        // Front Plane
        vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();

        // Rear Plane
        vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();

        // Left Plane
        vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();

        // Right Plane
        vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();

        poseStack.popPose();
    }

    private static float[] getColorByChunkType(String chunkType) {
        float alpha = 0.5f;
        float[] red = new float[]{1.0f, 0.0f, 0.0f, alpha};
        return switch (chunkType) {
            case "VANILLA" -> new float[]{0.0f, 0.0f, 1.0f, alpha}; // Blue
            case "FORCELOAD" -> {
                if (FORCE_LOADING_STATUS) {
                    yield new float[]{0.0f, 1.0f, 0.0f, alpha}; // Green
                } else {
                    yield red;
                }
            }
            case "FORCETICK" -> {
                if (FORCE_LOADING_STATUS) {
                    yield new float[]{1.0f, 1.0f, 0.0f, alpha}; // Yellow
                } else {
                    yield red;
                }
            }
            default -> red;
        };
    }

}
