import engine.misc.Game;
import engine.npc.Npc;
import java.util.List;
import engine.saveSystem.CheckpointMetadata;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import engine.world.World;
import util.GameOutput;
import util.JsonLoader;

private static final String SAVES_DIR = "saves";
private static final String SLOT1 = "slot1.txt";
private static final String SLOT2 = "slot2.txt";
private static final String SLOT3 = "slot3.txt";
private static final String[] SAVE_SLOTS = {SLOT1, SLOT2, SLOT3};
private static final String WORLD_FILE = "resources/world.json";
private static final int BOX_WIDTH = 64;
private static String gameTitle = "Text Adventure";

public static void main() {
    // Load game title from JSON
    try {
        JsonLoader.loadWorld(WORLD_FILE);
        String title = JsonLoader.getGameTitle();
        if (title != null && !title.isBlank()) {
            gameTitle = title;
        }
    } catch (Exception e) {
        IO.println("Warning: Could not load game title, using default.");
    }

    // Create saves directory if it doesn't exist
    try {
        Files.createDirectories(Path.of(SAVES_DIR));
    } catch (IOException e) {
        IO.println("Error creating saves directory: " + e.getMessage());
    }

    Scanner scanner = new Scanner(System.in);
    boolean running = true;

    while (running) {
        printTitleScreen();
        String choice = readLineSafe(scanner, "> Select option: ");
        if (choice == null) {
            IO.println("\nGame Closing...");
            break;
        }

        String normalized = normalize(choice);

        if (isNewGame(normalized)) {
            try {
                startNewGame(scanner);
            } catch (IOException e) {
                IO.println("Error: " + e.getMessage());
            }
        } else if (isLoadGame(normalized)) {
            try {
                loadGame(scanner);
            } catch (IOException e) {
                IO.println("Error: " + e.getMessage());
            }
        } else if (isDeleteSave(normalized)) {
            try {
                deleteSlot(scanner);
            } catch (IOException e) {
                IO.println("Error: " + e.getMessage());
            }
        } else if (isExit(normalized)) {
            running = false;
            IO.println("Game Closing...");
        } else {
            IO.println("Invalid option. Type: New Game, Load Game, Delete Save, or Exit.");
            pause(scanner);
        }
    }

    scanner.close();
}

private static void printTitleScreen() {
    String top = "┌" + "─".repeat(BOX_WIDTH) + "┐";
    String mid = "├" + "─".repeat(BOX_WIDTH) + "┤";
    String bot = "└" + "─".repeat(BOX_WIDTH) + "┘";

    IO.println();
    IO.println(top);
    IO.println(centerLine("GAME WINDOW", BOX_WIDTH));
    IO.println(mid);
    IO.println(emptyLine(BOX_WIDTH));
    IO.println(centerLine(gameTitle, BOX_WIDTH));
    IO.println(emptyLine(BOX_WIDTH));
    IO.println(mid);
    IO.println(paddedLine("  New Game", BOX_WIDTH));
    IO.println(paddedLine("  Load Game", BOX_WIDTH));
    IO.println(paddedLine("  Delete Save", BOX_WIDTH));
    IO.println(paddedLine("  Exit", BOX_WIDTH));
    IO.println(emptyLine(BOX_WIDTH));
    IO.println(paddedLine("  In-game: type 'menu' to return here.", BOX_WIDTH));
    IO.println(bot);
    IO.println();
}

private static void startNewGame(Scanner scanner) throws IOException {
    printSlotSelectionBox("NEW GAME");

    String input = readLineSafe(scanner, "> Choose slot: ");
    if (input == null) return;

    int slot = parseSlot(input);
    if (slot < 0) {
        return;
    }

    String slotFile = SAVE_SLOTS[slot];
    Path slotPath = Path.of(SAVES_DIR, slotFile).toAbsolutePath();

    // Check if slot already has a save
    if (Files.exists(slotPath)) {
        boolean confirm = confirmYesNo(scanner, "This slot already has a save. Are you sure you want to OVERWRITE " + slotFile + "?");
        if (!confirm) {
            IO.println("Cancelled.");
            pause(scanner);
            return;
        }
        deleteDirectoryRecursively(slotPath);
    }

    try {
        Game game = new Game(WORLD_FILE, slotPath, false);
        GameOutput.setCurrentGame(game);
        game.startNewGame();

        IO.println("\n=== NEW GAME STARTED ===");
        IO.println(game.getPlayer().getCurrentRoom().getDescription());

        List<Npc> npcsHere = game.getWorld().getNpcsInRoom(game.getPlayer().getCurrentRoom().getId());
        if (npcsHere != null && !npcsHere.isEmpty()) {
            for (Npc npc : npcsHere) {
                String type = (npc.getType() == null || npc.getType().isBlank()) ? "npc" : npc.getType();
                IO.println("You see a " + type + " here: " + npc.getName() + ".");
            }
            IO.println("Type 'interact' to talk.");
        }

        gameLoop(game, scanner);

    } catch (Exception e) {
        IO.println("Error starting game: " + e.getMessage());
        pause(scanner);
    }
}

private static void loadGame(Scanner scanner) throws IOException {
    printSlotSelectionBox("LOAD GAME");

    String input = readLineSafe(scanner, "> Choose slot: ");
    if (input == null) return;

    int slot = parseSlot(input);
    if (slot < 0) {
        return;
    }

    String slotFile = SAVE_SLOTS[slot];
    Path slotPath = Path.of(SAVES_DIR, slotFile);

    if (!Files.exists(slotPath)) {
        IO.println("This save is empty.");
        pause(scanner);
        return;
    }

    try {
        Game game = new Game(WORLD_FILE, slotPath, false);
        GameOutput.setCurrentGame(game);

        // NEW: Show load method choice
        String loadChoice = showLoadMethodMenu(scanner);

        if ("checkpoint".equalsIgnoreCase(loadChoice)) {
            // Load from checkpoint
            List<CheckpointMetadata> checkpoints = game.getCheckpointManager().getCheckpoints();

            if (checkpoints.isEmpty()) {
                IO.println("No checkpoints available. Loading from last save...");
                game.loadGame();
            } else {
                int checkpointId = showCheckpointSelectionMenu(scanner, game, checkpoints);
                if (checkpointId > 0) {
                    game.loadFromCheckpoint(checkpointId);
                } else {
                    IO.println("Cancelled.");
                    pause(scanner);
                    return;
                }
            }
        } else {
            // Load from last command
            game.loadGame();
        }

        List<Npc> npcsHere = game.getWorld().getNpcsInRoom(game.getPlayer().getCurrentRoom().getId());
        if (npcsHere != null && !npcsHere.isEmpty()) {
            for (Npc npc : npcsHere) {
                String type = (npc.getType() == null || npc.getType().isBlank()) ? "npc" : npc.getType();
                IO.println("You see a " + type + " here: " + npc.getName() + ".");
            }
            IO.println("Type 'interact' to talk.");
        }

        gameLoop(game, scanner);

    } catch (Exception e) {
        IO.println("Error loading game: " + e.getMessage());
        pause(scanner);
    }
}

private static String showLoadMethodMenu(Scanner scanner) {
    IO.println();
    String top = "┌" + "─".repeat(BOX_WIDTH) + "┐";
    String mid = "├" + "─".repeat(BOX_WIDTH) + "┤";
    String bot = "└" + "─".repeat(BOX_WIDTH) + "┘";

    IO.println(top);
    IO.println(centerLine("LOAD GAME", BOX_WIDTH));
    IO.println(mid);
    IO.println(paddedLine("  1) Load from Last Command", BOX_WIDTH));
    IO.println(paddedLine("  2) Load from Checkpoint", BOX_WIDTH));
    IO.println(paddedLine("  Type 1 or 2:", BOX_WIDTH));
    IO.println(bot);
    IO.println();

    while (true) {
        String input = readLineSafe(scanner, "> ");
        if (input == null) return "last";

        String normalized = normalize(input);
        if (normalized.equals("1") || normalized.equals("last command")) {
            return "last";
        } else if (normalized.equals("2") || normalized.equals("checkpoint")) {
            return "checkpoint";
        } else {
            IO.println("Please type 1 or 2.");
        }
    }
}

// NEW: Show checkpoint selection menu
private static int showCheckpointSelectionMenu(Scanner scanner, Game game, List<CheckpointMetadata> checkpoints) {
    IO.println();
    String top = "┌" + "─".repeat(BOX_WIDTH) + "┐";
    String mid = "├" + "─".repeat(BOX_WIDTH) + "┤";
    String bot = "└" + "─".repeat(BOX_WIDTH) + "┘";

    IO.println(top);
    IO.println(centerLine("AVAILABLE CHECKPOINTS", BOX_WIDTH));
    IO.println(mid);

    for (CheckpointMetadata cp : checkpoints) {
        String timeStr = game.getCheckpointManager().formatTimestamp(cp.getTimestamp());
        String line = " " + cp.getId() + ") " + cp.getDisplayName() + " | " + timeStr;
        IO.println(paddedLine(line, BOX_WIDTH));
    }

    IO.println(paddedLine("  Type checkpoint number or Back", BOX_WIDTH));
    IO.println(bot);
    IO.println();

    while (true) {
        String input = readLineSafe(scanner, "> ");
        if (input == null) return -1;

        String normalized = normalize(input);
        if (normalized.equals("back")) {
            return -1;
        }

        try {
            int id = Integer.parseInt(normalized);
            for (CheckpointMetadata cp : checkpoints) {
                if (cp.getId() == id) {
                    return id;
                }
            }
            IO.println("Invalid checkpoint number.");
        } catch (NumberFormatException e) {
            IO.println("Please type a checkpoint number or Back.");
        }
    }
}

private static void deleteSlot(Scanner scanner) throws IOException {
    printSlotSelectionBox("DELETE SAVE");

    String input = readLineSafe(scanner, "> Choose slot to delete: ");
    if (input == null) return;

    int slot = parseSlot(input);
    if (slot < 0) {
        return;
    }

    String slotFile = SAVE_SLOTS[slot];
    Path slotPath = Path.of(SAVES_DIR, slotFile);

    if (!Files.exists(slotPath)) {
        IO.println("Slot is already empty.");
        pause(scanner);
        return;
    }

    boolean confirm = confirmYesNo(scanner, "Are you sure you want to DELETE " + slotFile + "?");
    if (confirm) {
        try {
            // FIXED: Delete directory recursively (folder + all contents)
            deleteDirectoryRecursively(slotPath);
            IO.println("Save deleted.");
        } catch (IOException e) {
            IO.println("Error deleting save: " + e.getMessage());
        }
    } else {
        IO.println("Cancelled.");
    }
    pause(scanner);
}

private static void deleteDirectoryRecursively(Path path) throws IOException {
    if (!Files.exists(path)) return;

    if (Files.isDirectory(path)) {
        // Delete all files/folders inside
        try (var stream = Files.list(path)) {
            stream.forEach(file -> {
                try {
                    deleteDirectoryRecursively(file);
                } catch (IOException e) {
                    IO.println("Warning: Could not delete " + file);
                }
            });
        }
    }

    // Delete the folder itself
    Files.delete(path);
}

private static void printSlotSelectionBox(String heading) throws IOException {
    IO.println();
    String top = "┌" + "─".repeat(BOX_WIDTH) + "┐";
    String mid = "├" + "─".repeat(BOX_WIDTH) + "┤";
    String bot = "└" + "─".repeat(BOX_WIDTH) + "┘";

    IO.println(top);
    IO.println(centerLine("SAVE SLOTS", BOX_WIDTH));
    IO.println(centerLine(heading, BOX_WIDTH));
    IO.println(mid);
    printSlotLines();
    IO.println(bot);
    IO.println();
}

private static void printSlotLines() throws IOException {
    for (int i = 0; i < SAVE_SLOTS.length; i++) {
        Path slotPath = Path.of(SAVES_DIR, SAVE_SLOTS[i]);

        String text;
        if (Files.exists(slotPath)) {
            long lastModified = Files.getLastModifiedTime(slotPath).toMillis();
            String date = formatSavedAtDisplay(lastModified);
            text = "Slot " + (i + 1) + " | " + date;
        } else {
            text = "Slot " + (i + 1) + " | (empty)";
        }

        IO.println(paddedLine("  " + text, BOX_WIDTH));
    }
    IO.println(paddedLine("  Type 1, 2, 3, or Back", BOX_WIDTH));
}

private static String formatSavedAtDisplay(long millis) {
    try {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(millis));
    } catch (Exception ignored) {
        return "(unknown)";
    }
}

private static void gameLoop(Game game, Scanner scanner) {
    while (true) {

        if (game.getWorld().isReturnToMenuPending()) { // CHANGED
            game.getWorld().setReturnToMenuPending(false); // CHANGED
            IO.println("\nReturning to main menu..."); // CHANGED
            pause(scanner); // CHANGED
            return; // CHANGED
        }

        if (game.getPlayer().isDead()) {
            IO.println("\n╔════════════════════════════════════════╗");
            IO.println("║           YOU HAVE DIED               ║");
            IO.println("╚════════════════════════════════════════╝");

            // Try to load from most recent checkpoint
            CheckpointMetadata lastCheckpoint = game.getMostRecentCheckpoint();
            if (lastCheckpoint != null) {
                IO.println("\nLoading from last checkpoint: " + lastCheckpoint.getDisplayName());
                try {
                    game.loadFromCheckpoint(lastCheckpoint.getId());
                    IO.println("\n✓ Checkpoint loaded. Continue playing...\n");
                } catch (IOException e) {
                    IO.println("Error loading checkpoint: " + e.getMessage());
                    IO.println("Returning to main menu...");
                    return;
                }
            } else {
                IO.println("\nNo checkpoints available. Game Over!");
                IO.println("Returning to main menu...");
                pause(scanner);
                return;
            }
        }

        String input = readLineSafe(scanner, "> ");
        if (input == null) {
            IO.println("\nGame Closing...");
            System.exit(0);
        }

        String trimmed = input.trim();

        // CHANGED: allow empty input if waiting to show dialogue options
        if (trimmed.isEmpty()) {
            if (game.getWorld().getInteractionStage() == World.INTERACT_DIALOGUE_WAIT) { // CHANGED
                try {
                    game.executeCommand(""); // CHANGED
                } catch (Exception ignored) {
                }
            }
            continue;
        }

        if (trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit") || trimmed.equalsIgnoreCase("q")) {
            IO.println("Thanks for playing!");
            break;
        }

        if (trimmed.equalsIgnoreCase("menu")) {
            boolean confirm = confirmYesNo(scanner, "Return to main menu?");
            if (confirm) {
                IO.println("Returning to main menu...");
                return;
            }
            continue;
        }

        try {
            game.executeCommand(trimmed);
        } catch (Exception e) {
            // Don't show error, just ignore (syntax error)
        }
    }
}

private static boolean confirmYesNo(Scanner scanner, String prompt) {
    while (true) {
        String input = readLineSafe(scanner, prompt + " (yes/no): ");
        if (input == null) return false;

        String normalized = normalize(input);
        if (normalized.equals("yes") || normalized.equals("y")) {
            return true;
        } else if (normalized.equals("no") || normalized.equals("n")) {
            return false;
        } else {
            IO.println("Please type yes or no.");
        }
    }
}

private static int parseSlot(String input) {
    String normalized = normalize(input);
    if (normalized.equals("back") || normalized.equals("0")) return -1;
    if (normalized.equals("1")) return 0;
    if (normalized.equals("2")) return 1;
    if (normalized.equals("3")) return 2;
    return -1;
}

private static String readLineSafe(Scanner scanner, String prompt) {
    try {
        IO.print(prompt);
        return scanner.nextLine();
    } catch (Exception e) {
        return null;
    }
}

private static void pause(Scanner scanner) {
    IO.print("Press ENTER to continue...");
    try {
        scanner.nextLine();
    } catch (Exception ignored) {
    }
}

private static String centerLine(String text, int width) {
    if (text == null) text = "";
    if (text.length() > width) text = text.substring(0, width);

    int totalPadding = width - text.length();
    int left = totalPadding / 2;
    int right = totalPadding - left;

    return "│" + " ".repeat(left) + text + " ".repeat(right) + "│";
}

private static String paddedLine(String text, int width) {
    if (text == null) text = "";
    if (text.length() > width) text = text.substring(0, width);

    return "│" + text + " ".repeat(width - text.length()) + "│";
}

private static String emptyLine(int width) {
    return "│" + " ".repeat(width) + "│";
}

private static String normalize(String s) {
    if (s == null) return "";
    return s.trim().toLowerCase().replaceAll("\\s+", " ");
}

private static boolean isNewGame(String s) {
    return s.equals("new game") || s.equals("new") || s.equals("start") || s.equals("1");
}

private static boolean isLoadGame(String s) {
    return s.equals("load game") || s.equals("load") || s.equals("continue") || s.equals("2");
}

private static boolean isDeleteSave(String s) {
    return s.equals("delete save") || s.equals("delete") || s.equals("3");
}

private static boolean isExit(String s) {
    return s.equals("exit") || s.equals("quit") || s.equals("4");
}