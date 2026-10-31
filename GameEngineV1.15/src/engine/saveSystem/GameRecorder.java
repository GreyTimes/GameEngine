package engine.saveSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * UPDATED VERSION: Now handles folder-based slot structure
 * Slot is now a directory containing:
 * - commands.txt (command history)
 * - checkpoints/ (checkpoint JSON files)
 * - checkpoint_metadata.json (checkpoint index)
 */
public class GameRecorder {
    private final Path slotFolder;
    private final Path commandFile;
    private boolean recording = false;

    public GameRecorder(Path slotFolder) throws IOException {
        // slotFolder is now a directory (e.g., saves/slot1/)
        this.slotFolder = slotFolder;
        this.commandFile = slotFolder.resolve("commands.txt");
        ensureSlotStructure();
    }

    /**
     * Create slot folder and commands.txt if they don't exist
     */
    private void ensureSlotStructure() throws IOException {
        Files.createDirectories(slotFolder);
        if (!Files.exists(commandFile)) {
            Files.createFile(commandFile);
        }
    }

    /**
     * Start recording (append mode - for loading and continuing)
     */
    public void startRecording() throws IOException {
        recording = true;
    }

    /**
     * Start new game (clear commands file)
     */
    public void startNewGame() throws IOException {
        recording = true;
        Files.write(commandFile, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Record a single state-changing command
     */
    public void recordCommand(String command) throws IOException {
        if (!recording || command == null || command.trim().isEmpty()) {
            return;
        }
        Files.writeString(commandFile, command.trim() + "\n", StandardOpenOption.APPEND);
    }

    /**
     * Load all commands from file
     */
    public List<String> loadCommands() throws IOException {
        List<String> commands = new ArrayList<>();
        if (!Files.exists(commandFile)) {
            return commands;
        }

        List<String> lines = Files.readAllLines(commandFile);
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                commands.add(line.trim());
            }
        }
        return commands;
    }

    /**
     * Clear all commands (called when loading from a checkpoint)
     */
    public void clearCommands() throws IOException {
        Files.write(commandFile, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Truncate commands to a specific line count
     */
    public void truncateCommands(int lineCount) throws IOException {
        List<String> lines = Files.readAllLines(commandFile);
        if (lineCount >= lines.size()) {
            return; // Nothing to truncate
        }

        List<String> truncated = lines.subList(0, lineCount);

        String content = String.join("\n", truncated);
        if (!truncated.isEmpty()) {
            content += "\n"; // Add final newline
        }

        Files.write(commandFile, String.join("\n", truncated).getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Get the current number of commands in the file
     */
    public int getCurrentCommandCount() throws IOException {
        if (!Files.exists(commandFile)) {
            return 0;
        }
        List<String> lines = Files.readAllLines(commandFile);
        return lines.size();
    }

    /**
     * Get slot folder path
     */
    public Path getSlotFolder() {
        return slotFolder;
    }

    /**
     * Get command file path
     */
    public Path getCommandFile() {
        return commandFile;
    }
}