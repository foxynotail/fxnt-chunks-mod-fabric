package net.fxnt.fxntchunks.items;

import net.fxnt.fxntchunks.chunkloading.ChunkServer;
import net.fxnt.fxntchunks.chunkloading.ForceLoadedChunkData;
import net.fxnt.fxntchunks.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ChunkWand extends Item {
    private static int maxForceLoadedChunks = 0;
    private static int maxForceTickingChunks = 0;
    private static int maxPlayerForceLoadedChunks = 0;
    private static int maxPlayerForceTickingChunks = 0;
    private static int totalForceLoadedChunks = 0;
    private static int totalForceTickingChunks = 0;
    private static int playerForceLoadedChunks = 0;
    private static int playerForceTickingChunks = 0;
    public ChunkWand(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        Player player = context.getPlayer();
        InteractionHand usedHand = context.getHand();

        if (player == null) {
            return InteractionResult.FAIL;
        } else if (player.isSpectator() || level.isClientSide || !usedHand.equals(InteractionHand.MAIN_HAND)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {

            ChunkPos chunkPos = new ChunkPos(context.getClickedPos());
            String playerID = player.getStringUUID();
            String playerName = player.getName().getString();

            maxForceLoadedChunks = Config.MAX_FORCE_LOADED_CHUNKS.get();
            maxForceTickingChunks = Config.MAX_FORCE_TICKING_CHUNKS.get();
            maxPlayerForceLoadedChunks = Config.MAX_PLAYER_FORCE_LOADED_CHUNKS.get();
            maxPlayerForceTickingChunks = Config.MAX_PLAYER_FORCE_TICKING_CHUNKS.get();

            totalForceLoadedChunks = ChunkServer.getAllForceLoadedChunks().size();
            totalForceTickingChunks = ChunkServer.getAllForceTickingChunks().size();
            playerForceLoadedChunks = ChunkServer.getPlayerForceLoadedChunks(player.getStringUUID()).size();
            playerForceTickingChunks = ChunkServer.getPlayerForceTickingChunks(player.getStringUUID()).size();

            // Prevent loading chunks when globally disabled
            if (!ChunkServer.getForceLoadingStatus()) {
                actionMsg(player, "Force loading is currently disabled", "red");
                return InteractionResult.FAIL;
            }

            ForceLoadedChunkData chunkData = ChunkServer.getChunkData(serverLevel, chunkPos);

            // If sneaking disable
            if (player.isShiftKeyDown()) {

                if (chunkData == null) {
                    chatMsg(player, "Cannot disable Force Loading\nChunk is not force loaded", "red");
                    return InteractionResult.FAIL;

                } else if (chunkData.type.equals("VANILLA")) {
                    chatMsg(player, "Cannot disable Force Loading\nChunk is not controlled by this mod", "blue");
                    return InteractionResult.FAIL;

                } else if (!player.canUseGameMasterBlocks() && !chunkData.playerID.equals(playerID)) {
                    chatMsg(player, "Cannot Disable Chunk Loading\nChunk was loaded by " + chunkData.playerName, "purple");
                    return InteractionResult.FAIL;

                } else if (player.canUseGameMasterBlocks() && !chunkData.playerID.equals(playerID)) {
                    chatMsg(player, "Operator: Disabling Force Loading\nChunk was loaded by " + chunkData.playerName, "red");
                    ChunkServer.removeChunk(serverLevel, chunkPos);
                    return InteractionResult.SUCCESS;

                } else {
                    if (chunkData.type.equals("FORCETICK")) {
                        totalForceTickingChunks--;
                        playerForceTickingChunks--;
                    }
                    totalForceLoadedChunks--;
                    playerForceLoadedChunks--;
                    actionMsg(player, "Disabling Force Loading", "red");
                    ChunkServer.removeChunk(serverLevel,chunkPos);
                    return InteractionResult.SUCCESS;
                }
            } else {

                // Player Not Sneaking (Change Loading Mode)
                if (chunkData == null) {

                    if (totalForceLoadedChunks >= maxForceLoadedChunks) {
                        chatMsg(player, "Cannot Force Load\nGlobal Force Loaded Chunks Limit Reached", "purple");
                        return InteractionResult.FAIL;

                    } else if (playerForceLoadedChunks >= maxPlayerForceLoadedChunks) {
                        chatMsg(player, "Cannot Force Load\nPlayer Force Loaded Chunks Limit Reached", "purple");
                        return InteractionResult.FAIL;

                    } else {

                        // ENABLE FORCE LOADING
                        totalForceLoadedChunks++;
                        playerForceLoadedChunks++;
                        actionMsg(player, "Force Load [" + playerForceLoadedChunks + "/" + maxPlayerForceLoadedChunks + "]", "green");
                        ChunkServer.addChunk(serverLevel, chunkPos, "FORCELOAD", playerID, playerName);
                        return InteractionResult.SUCCESS;
                    }

                } else if (chunkData.type.equals("VANILLA")) {
                    actionMsg(player, "Chunk is not controlled by this mod", "blue");
                    return InteractionResult.FAIL;

                } else if (chunkData.type.equals("FORCELOAD")) {

                    if (!player.canUseGameMasterBlocks() && !chunkData.playerID.equals(playerID)) {
                        chatMsg(player, "Cannot Force Tick\nChunk loaded by another player", "purple");
                        return InteractionResult.FAIL;

                    } else if (totalForceTickingChunks >= maxForceTickingChunks) {
                        chatMsg(player, "Cannot Force Tick\nGlobal Force Ticking Chunks Limit Reached", "purple");
                        return InteractionResult.FAIL;

                    } else if (playerForceTickingChunks >= maxPlayerForceTickingChunks) {
                        chatMsg(player, "Cannot Force Tick\nPlayer Force Ticking Chunks Limit Reached", "purple");
                        return InteractionResult.FAIL;

                    } else {
                        // ENABLE FORCE TICKING
                        totalForceTickingChunks++;
                        playerForceTickingChunks++;
                        actionMsg(player, "Force Tick [" + playerForceTickingChunks + "/" + maxPlayerForceTickingChunks + "]", "yellow");
                        ChunkServer.updateChunkType(serverLevel, chunkPos, "FORCETICK", playerID, playerName);
                        return InteractionResult.SUCCESS;

                    }

                } else if (chunkData.type.equals("FORCETICK")) {
                    if (!player.canUseGameMasterBlocks() && !chunkData.playerID.equals(playerID)) {
                        chatMsg(player, "Cannot Force Load\nChunk loaded by another player", "purple");
                        return InteractionResult.FAIL;

                    } else if (totalForceLoadedChunks >= maxForceLoadedChunks) {
                        chatMsg(player, "Cannot Force Load\nGlobal Force Loading Chunks Limit Reached", "purple");
                        return InteractionResult.FAIL;

                    } else if (playerForceLoadedChunks >= maxPlayerForceLoadedChunks) {
                        chatMsg(player, "Cannot Force Load\nPlayer Force Loading Chunks Limit Reached", "purple");
                        return InteractionResult.FAIL;

                    } else {
                        // ENABLE FORCE LOADING
                        totalForceTickingChunks--;
                        playerForceTickingChunks--;
                        actionMsg(player, "Force Load [" + playerForceLoadedChunks + "/" + maxPlayerForceLoadedChunks + "]", "green");
                        ChunkServer.updateChunkType(serverLevel, chunkPos, "FORCELOAD", playerID, playerName);
                        return InteractionResult.SUCCESS;

                    }
                }
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {

        if (player.isSpectator() || level.isClientSide || !usedHand.equals(InteractionHand.MAIN_HAND)) {
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }

        if (level instanceof ServerLevel serverLevel) {

            // Enable / Disable global chunks (operator only)
            if (player.isShiftKeyDown()) {

                if (player.canUseGameMasterBlocks()) {

                    if (ChunkServer.getForceLoadingStatus()) {
                        actionMsg(player, "Disabling Global Force Chunk Loading", "red");
                        ChunkServer.disableForceLoading();
                    } else {
                        actionMsg(player, "Enabling Global Force Loading", "green");
                        ChunkServer.enableForceLoading();
                    }
                    ChunkServer.sendToClient(serverLevel.getServer());
                    return InteractionResultHolder.success(player.getItemInHand(usedHand));

                } else {
                    chatMsg(player, "Global Chunk Loading Action Failed\nOnly Operators can enable or disable global chunk loading", "red");
                    return InteractionResultHolder.fail(player.getItemInHand(usedHand));

                }
            }

        }

        return super.use(level, player, usedHand);
    }

    private void actionMsg(Player player, String message, String type) {
        Component msg = switch (type) {
            default -> Component.literal(message);
            case "green" -> Component.literal(message).withStyle(ChatFormatting.GREEN);
            case "yellow" -> Component.literal(message).withStyle(ChatFormatting.YELLOW);
            case "red" -> Component.literal(message).withStyle(ChatFormatting.RED);
            case "blue" -> Component.literal(message).withStyle(ChatFormatting.BLUE);
            case "purple" -> Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE);
        };
        player.displayClientMessage(msg, true);
    }

    private void actionMsg(Player player, String message) {
        actionMsg(player, message, "");
    }

    private void chatMsg(Player player, String message, String type) {

        String[] lines = message.split("\n");

        player.displayClientMessage(Component.literal("Chunk Wand"), false);
        player.displayClientMessage(Component.literal("=========="), false);

        for (String line : lines) {
            Component msg = switch (type) {
                default -> Component.literal(line);
                case "green" -> Component.literal(line).withStyle(ChatFormatting.GREEN);
                case "yellow" -> Component.literal(line).withStyle(ChatFormatting.YELLOW);
                case "red" -> Component.literal(line).withStyle(ChatFormatting.RED);
                case "blue" -> Component.literal(line).withStyle(ChatFormatting.BLUE);
                case "purple" -> Component.literal(line).withStyle(ChatFormatting.LIGHT_PURPLE);
            };
            player.displayClientMessage(msg, false);
        }

        player.displayClientMessage(Component.literal("Player Force Loaded Chunks: " + playerForceLoadedChunks + "/" + maxPlayerForceLoadedChunks), false);
        player.displayClientMessage(Component.literal("Player Force Ticking Chunks: " + playerForceTickingChunks + "/" + maxPlayerForceTickingChunks), false);
        player.displayClientMessage(Component.literal("Global Force Loaded Chunks: " + totalForceLoadedChunks + "/" + maxForceLoadedChunks), false);
        player.displayClientMessage(Component.literal("Global Force Ticking Chunks: " + totalForceTickingChunks + "/" + maxForceTickingChunks), false);
    }

    private void chatMsg(Player player, String message) {
        chatMsg(player, message, "");
    }
}
