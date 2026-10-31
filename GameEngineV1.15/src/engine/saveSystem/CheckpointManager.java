package engine.saveSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import engine.world.Player;
import engine.world.Room;
import engine.world.World;
import util.GameOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages checkpoint creation, loading, and metadata.
 * Evaluates triggers after each command and saves checkpoints automatically.
 */
public class CheckpointManager {
    private final Path slotFolder;
    private final Path checkpointsFolder;
    private final Path metadataFile;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<CheckpointMetadata> metadataList = new ArrayList<>();
    private int nextCheckpointId = 1;
    private final int maxCheckpoints;
    private final List<CheckpointTrigger> triggers;

    public CheckpointManager(Path slotFolder, int maxCheckpoints, List<CheckpointTrigger> triggers) throws IOException {
        this.slotFolder = slotFolder;
        this.checkpointsFolder = slotFolder.resolve("checkpoints");
        this.metadataFile = slotFolder.resolve("checkpoint_metadata.json");
        this.maxCheckpoints = maxCheckpoints;
        this.triggers = (triggers == null) ? new ArrayList<>() : new ArrayList<>(triggers);

        ensureCheckpointFolders();
        loadMetadata();
    }

    /**
     * Create checkpoint directories if they don't exist
     */
    private void ensureCheckpointFolders() throws IOException {
        Files.createDirectories(checkpointsFolder);
    }

    /**
     * Load existing checkpoint metadata from JSON
     */
    private void loadMetadata() throws IOException {
        if (!Files.exists(metadataFile)) {
            metadataList = new ArrayList<>();
            nextCheckpointId = 1;
            return;
        }

        try {
            CheckpointMetadataWrapper wrapper = objectMapper.readValue(
                    metadataFile.toFile(),
                    CheckpointMetadataWrapper.class
            );
            metadataList = wrapper.checkpoints;
            nextCheckpointId = metadataList.isEmpty() ? 1 : metadataList.get(metadataList.size() - 1).getId() + 1;
        } catch (Exception e) {
            metadataList = new ArrayList<>();
            nextCheckpointId = 1;
        }
    }

    /**
     * Evaluate if a command matches any trigger, and save checkpoint if so
     */
    public void evaluateAndSave(String command, Player player, World world, GameRecorder recorder) {
        if (command == null || command.isBlank() || triggers.isEmpty()) {
            return;
        }

        for (CheckpointTrigger trigger : triggers) {
            if (trigger.matches(command)) {
                try {
                    saveCheckpoint(trigger, player, world, recorder);
                } catch (IOException e) {
                    GameOutput.println("Warning: Failed to save checkpoint: " + e.getMessage());
                }
                return;
            }
        }
    }

    /**
     * Save a checkpoint with full game state
     */
    private void saveCheckpoint(CheckpointTrigger trigger, Player player, World world, GameRecorder recorder) throws IOException {
        GameState state = captureGameState(player, world);

        // Save checkpoint file
        int checkpointId = nextCheckpointId++;
        Path checkpointFile = checkpointsFolder.resolve("checkpoint_" + checkpointId + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(checkpointFile.toFile(), state);

        // Get current command count BEFORE we save
        int commandCount = recorder.getCurrentCommandCount();

        // Add metadata entry
        CheckpointMetadata metadata = new CheckpointMetadata(
                checkpointId,
                trigger.getName(),
                trigger.getDisplayName(),
                System.currentTimeMillis()
        );
        metadata.setCommandCount(commandCount);
        metadataList.add(metadata);

        // Enforce max checkpoints (remove oldest)
        if (metadataList.size() > maxCheckpoints) {
            CheckpointMetadata oldest = metadataList.remove(0);
            Path oldFile = checkpointsFolder.resolve("checkpoint_" + oldest.getId() + ".json");
            try {
                Files.delete(oldFile);
            } catch (IOException ignored) {
            }
        }

        // Save updated metadata
        saveMetadata();

        GameOutput.println("Checkpoint saved: " + trigger.getDisplayName());
    }

    /**
     * Capture complete game state into a serializable object
     */
    private GameState captureGameState(Player player, World world) {
        GameState state = new GameState();

        // Player state
        GameState.PlayerState ps = new GameState.PlayerState();
        ps.hp = player.getHp();
        ps.maxHp = player.getMaxHp();
        ps.attackDamage = player.getAttackDamage();
        ps.baseEscapeChance = player.getBaseEscapeChance();
        ps.carryCapacity = player.getCarryCapacity();
        ps.money = player.getMoney();
        ps.currentRoomId = player.getCurrentRoom() != null ? player.getCurrentRoom().getId() : null;
        ps.previousRoomId = player.getPreviousRoom() != null ? player.getPreviousRoom().getId() : null;
        ps.equippedWeaponId = player.getEquippedWeaponId();
        ps.equippedArmorId = player.getEquippedArmorId();
        ps.inventoryItemIds = new ArrayList<>(player.getInventoryItemIds());
        ps.statusEffects = new ArrayList<>(player.getStatusEffects());
        state.setPlayerState(ps);

        // Room states - CHANGED: Add exits
        Map<String, GameState.RoomState> roomStates = new java.util.HashMap<>();
        if (world != null && world.getRooms() != null) {
            for (Room room : world.getRooms().values()) {
                GameState.RoomState rs = new GameState.RoomState();
                rs.roomId = room.getId();
                rs.itemIds = new ArrayList<>(room.getItemIds());
                rs.monsterIds = new ArrayList<>(room.getMonsterIds());
                rs.targetIds = new ArrayList<>(room.getTargetIds());
                rs.currencyAmount = room.getCurrencyAmount();
                rs.exits = new java.util.HashMap<>(room.getExits()); // NEW: Save exits
                roomStates.put(room.getId(), rs);
            }
        }
        state.setRoomStates(roomStates);

        // World state
        if (world != null) {
            state.setWorldInteractionStage(world.getInteractionStage());
            state.setWorldInCombat(world.isInCombat());
            state.setWorldPendingTraderId(world.getPendingTraderId());
            state.setWorldPendingDialogueNpcId(world.getPendingDialogueNpcId());
        }

        return state;
    }

    /**
     * Load a checkpoint by ID and restore all state
     */
    public boolean loadCheckpoint(int checkpointId, Player player, World world) throws IOException {
        Path checkpointFile = checkpointsFolder.resolve("checkpoint_" + checkpointId + ".json");

        if (!Files.exists(checkpointFile)) {
            GameOutput.println("Checkpoint not found.");
            return false;
        }

        try {
            GameState state = objectMapper.readValue(checkpointFile.toFile(), GameState.class);
            restoreGameState(state, player, world);
            return true;
        } catch (Exception e) {
            GameOutput.println("Error loading checkpoint: " + e.getMessage());
            return false;
        }
    }

    /**
     * Restore game state from a checkpoint
     */
    private void restoreGameState(GameState state, Player player, World world) {
        if (state == null) return;

        // Restore player state
        GameState.PlayerState ps = state.getPlayerState();
        if (ps != null) {
            player.setHp(ps.hp);
            player.setMaxHp(ps.maxHp);
            player.setAttackDamage(ps.attackDamage);
            player.setBaseEscapeChance(ps.baseEscapeChance);
            player.setCarryCapacity(ps.carryCapacity);
            player.setMoney(ps.money);
            player.setEquippedWeaponId(ps.equippedWeaponId);
            player.setEquippedArmorId(ps.equippedArmorId);

            player.getInventoryItemIds().clear();
            if (ps.inventoryItemIds != null) {
                player.getInventoryItemIds().addAll(ps.inventoryItemIds);
            }

            player.getStatusEffects().clear();
            if (ps.statusEffects != null) {
                player.getStatusEffects().addAll(ps.statusEffects);
            }

            // Restore player location
            if (world != null && ps.currentRoomId != null) {
                Room currentRoom = world.getRooms().get(ps.currentRoomId);
                if (currentRoom != null) {
                    player.setCurrentRoom(currentRoom);
                }
            }

            if (world != null && ps.previousRoomId != null) {
                Room previousRoom = world.getRooms().get(ps.previousRoomId);
                if (previousRoom != null) {
                    player.setPreviousRoom(previousRoom);
                }
            }
        }

        // Restore room states
        if (state.getRoomStates() != null && world != null) {
            for (GameState.RoomState rs : state.getRoomStates().values()) {
                Room room = world.getRooms().get(rs.roomId);
                if (room != null) {
                    room.getItemIds().clear();
                    if (rs.itemIds != null) {
                        room.getItemIds().addAll(rs.itemIds);
                    }

                    room.getMonsterIds().clear();
                    if (rs.monsterIds != null) {
                        room.getMonsterIds().addAll(rs.monsterIds);
                    }

                    room.getTargetIds().clear();
                    if (rs.targetIds != null) {
                        room.getTargetIds().addAll(rs.targetIds);
                    }

                    room.setCurrencyAmount(rs.currencyAmount);

                    // NEW: Restore exits
                    if (rs.exits != null) {
                        room.getExits().clear();
                        room.getExits().putAll(rs.exits);
                    }
                }
            }
        }

        // Restore world state
        if (world != null) {
            world.setInteractionStage(state.getWorldInteractionStage());
            world.setInCombat(state.isWorldInCombat());
            world.setPendingTraderId(state.getWorldPendingTraderId());
            world.setPendingDialogueNpcId(state.getWorldPendingDialogueNpcId());
        }
    }

    /**
     * Get all available checkpoints
     */
    public List<CheckpointMetadata> getCheckpoints() {
        return new ArrayList<>(metadataList);
    }

    /**
     * Format timestamp as readable date string
     */
    public String formatTimestamp(long timestamp) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp));
    }

    /**
     * Save metadata to JSON file
     */
    private void saveMetadata() throws IOException {
        CheckpointMetadataWrapper wrapper = new CheckpointMetadataWrapper();
        wrapper.checkpoints = metadataList;
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), wrapper);
    }

    /**
     * Wrapper for JSON serialization
     */
    public static class CheckpointMetadataWrapper {
        @com.fasterxml.jackson.annotation.JsonProperty("checkpoints")
        public List<CheckpointMetadata> checkpoints = new ArrayList<>();
    }
}