package engine.misc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import engine.commands.*;
import engine.npc.DialogueOption;
import engine.npc.Npc;
import engine.saveSystem.CheckpointManager;
import engine.saveSystem.CheckpointMetadata;
import engine.saveSystem.CheckpointTrigger;
import engine.saveSystem.GameRecorder;
import engine.world.CommandConfig;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;
import util.JsonLoader;

public class Game {
    private final Player player;
    private final World world;
    private final CommandProcessor processor;
    private final GameRecorder recorder;
    private boolean isReplaying = false;
    private final CheckpointManager checkpointManager;

    public Game(String jsonPath, Path slotPath) throws IOException {
        this(jsonPath, slotPath, true);
    }

    public Game(String jsonPath, Path slotPath, boolean printInitialRoomDescription) throws IOException {
        this.world = JsonLoader.loadWorld(jsonPath);

        if (this.world == null) {
            throw new RuntimeException("Failed to load world from: " + jsonPath);
        }

        this.player = new Player(this.world.getStartRoom());
        this.player.setHp(JsonLoader.getPlayerHp());
        this.player.setMaxHp(JsonLoader.getPlayerMaxHp());
        this.player.setAttackDamage(JsonLoader.getPlayerAttackDamage());
        this.player.setBaseEscapeChance(JsonLoader.getPlayerBaseEscapeChance());
        this.player.setCarryCapacity(JsonLoader.getPlayerCarryCapacity());
        this.player.setMoney(JsonLoader.getPlayerMoney());

        this.processor = new CommandProcessor();
        this.recorder = new GameRecorder(slotPath);
        registerCommands();

        // FIXED: Initialize CheckpointManager here (after recorder and world are ready)
        try {
            List<CheckpointTrigger> triggers = JsonLoader.getCheckpointTriggers();
            int maxCheckpoints = JsonLoader.getMaxCheckpoints();
            this.checkpointManager = new CheckpointManager(
                    slotPath,
                    maxCheckpoints,
                    triggers
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize checkpoint manager: " + e.getMessage());
        }

        if (printInitialRoomDescription) {
            GameOutput.println(this.player.getCurrentRoom().getDescription());

            List<Npc> npcsHere = this.world.getNpcsInRoom(this.player.getCurrentRoom().getId());
            if (npcsHere != null && !npcsHere.isEmpty()) {
                for (Npc npc : npcsHere) {
                    String type = (npc.getType() == null || npc.getType().isBlank()) ? "npc" : npc.getType();
                    GameOutput.println("You see a " + type + " here: " + npc.getName() + ".");
                }
                GameOutput.println("Type 'interact' to talk.");
            }
        }
    }

    private void registerCommands() {
        registerCommand("go", new GoCommand());
        registerCommand("exit", new ExitCommand());
        registerCommand("help", new HelpCommand());
        registerCommand("look", new LookCommand());
        registerCommand("take", new TakeCommand());
        registerCommand("drop", new DropCommand());
        registerCommand("inventory", new InventoryCommand());
        registerCommand("playerstate", new PlayerStateCommand());
        registerCommand("use", new UseCommand());
        registerCommand("equip", new EquipCommand());
        registerCommand("unequip", new UnequipCommand());
        registerCommand("hit", new HitCommand());
        registerCommand("throw", new ThrowCommand());
        registerCommand("flee", new FleeCommand());
        registerCommand("interact", new InteractCommand());
    }

    private boolean isEnabled(String commandId) {
        if (this.world == null) return false;

        CommandConfig cfg = this.world.getCommandConfig();
        if (cfg == null) return false;

        Set<String> enabled = cfg.enabledCommandIds();
        if (enabled == null) return false;
        return enabled.contains(commandId);
    }

    private void registerCommand(String commandId, Command command) {
        if (!isEnabled(commandId)) return;

        CommandConfig cfg = (this.world == null) ? null : this.world.getCommandConfig();
        if (cfg == null) {
            GameOutput.println("No commands config found; '" + commandId + "' not registered.");
            return;
        }

        Map<String, List<String>> aliasesMap = cfg.aliasesByCommandId();
        List<String> jsonAliases = (aliasesMap == null) ? null : aliasesMap.get(commandId);

        if (jsonAliases == null || jsonAliases.isEmpty()) {
            GameOutput.println("Command '" + commandId + "' enabled but has no aliases; not registered.");
            return;
        }

        boolean hasAtLeastOne = false;
        for (String n : jsonAliases) {
            if (n == null || n.trim().isEmpty()) continue;
            hasAtLeastOne = true;
            this.processor.register(n, command);
        }

        if (!hasAtLeastOne) {
            GameOutput.println("Command '" + commandId + "' enabled but aliases were blank; not registered.");
        }
    }

    public void startNewGame() throws IOException {
        isReplaying = false;
        recorder.startNewGame();
    }

    public void loadGame() throws IOException {
        isReplaying = true;

        // CHANGED: replay EVERYTHING through processor (no special skips)
        List<String> commands = recorder.loadCommands();
        for (String cmd : commands) {
            if (!cmd.trim().isEmpty()) {
                this.processor.process(cmd, this.player, this.world); // CHANGED
            }
        }

        isReplaying = false;
        recorder.startRecording();

        GameOutput.println("\n=== GAME LOADED ===");
        GameOutput.println(this.player.getCurrentRoom().getDescription());
    }

    private boolean isStateChangingCommand(String input) {
        if (input == null || input.trim().isEmpty()) return false;

        // CHANGED: any input during interaction is state-changing (trader buy, dialogue)
        if (world != null && world.getInteractionStage() != World.INTERACT_NONE) return true; // CHANGED

        String trimmed = input.trim();

        // Special dialogue/trader responses (save these)
        if (trimmed.equalsIgnoreCase("yes") || trimmed.equalsIgnoreCase("no") ||
                trimmed.equalsIgnoreCase("back")) {
            return true;
        }

        try {
            Integer.parseInt(trimmed);
            return true;  // Dialogue choice numbers
        } catch (NumberFormatException e) {
            // Not a number
        }

        String command = trimmed.split("\\s+")[0].toLowerCase();

        // Commands that DON'T change state
        String[] nonStateCommands = {"look", "inventory", "help", "playerstate"};
        for (String nonState : nonStateCommands) {
            if (command.equals(nonState)) {
                return false;
            }
        }

        // Check if command is registered
        if (!processor.isCommandRegistered(command)) {
            return false;
        }

        return true;
    }

    public void processCommand(String input) throws IOException {
        try {
            this.processor.process(input, this.player, this.world);

            // Only record commands that changed state
            if (isStateChangingCommand(input)) {
                recorder.recordCommand(input);

                // ADDED: Evaluate checkpoint triggers
                checkpointManager.evaluateAndSave(input, this.player, this.world, this.recorder);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void executeCommand(String input) throws IOException {
        processCommand(input);
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public GameRecorder getGameRecorder() {
        return recorder;
    }

    public void loadFromCheckpoint(int checkpointId) throws IOException {
        // Load checkpoint state
        if (!checkpointManager.loadCheckpoint(checkpointId, player, world)) {
            throw new RuntimeException("Failed to load checkpoint " + checkpointId);
        }

        // FIXED: Only truncate commands AFTER the checkpoint
        CheckpointMetadata metadata = findCheckpointMetadata(checkpointId);
        if (metadata != null && metadata.getCommandCount() > 0) {
            recorder.truncateCommands(metadata.getCommandCount());
        }

        // Start fresh recording from this checkpoint
        isReplaying = false;
        recorder.startRecording();

        GameOutput.println("\n=== CHECKPOINT LOADED ===");
        GameOutput.println(this.player.getCurrentRoom().getDescription());
    }

    // Add this method to Game class
    public CheckpointMetadata getMostRecentCheckpoint() {
        List<CheckpointMetadata> checkpoints = checkpointManager.getCheckpoints();
        if (checkpoints.isEmpty()) {
            return null;
        }
        return checkpoints.get(checkpoints.size() - 1); // Last one is most recent
    }

    private CheckpointMetadata findCheckpointMetadata(int checkpointId) {
        for (CheckpointMetadata cp : checkpointManager.getCheckpoints()) {
            if (cp.getId() == checkpointId) {
                return cp;
            }
        }
        return null;
    }

    public boolean isReplaying() {
        return isReplaying;
    }

    public CheckpointManager getCheckpointManager() {
        return checkpointManager;
    }
}