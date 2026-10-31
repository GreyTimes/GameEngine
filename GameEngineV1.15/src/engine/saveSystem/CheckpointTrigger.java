package engine.saveSystem;

/**
 * Represents a checkpoint trigger configuration loaded from JSON.
 * Determines when the game should automatically create a checkpoint.
 */
public class CheckpointTrigger {
    private final String name;                  // e.g., "use_key"
    private final String displayName;           // e.g., "Used Key in Locked Room"
    private final String commandPattern;        // e.g., "use key"
    private final String matchType;             // "exact", "startsWith", or "contains"

    public CheckpointTrigger(String name, String displayName, String commandPattern, String matchType) {
        this.name = name;
        this.displayName = displayName;
        this.commandPattern = commandPattern;
        this.matchType = (matchType == null || matchType.isBlank()) ? "exact" : matchType.toLowerCase();
    }

    /**
     * Check if a command matches this trigger
     */
    public boolean matches(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }

        String cmd = command.trim().toLowerCase();
        String pattern = commandPattern.toLowerCase();

        switch (matchType) {
            case "startswith":
                return cmd.startsWith(pattern);
            case "contains":
                return cmd.contains(pattern);
            default:
                return cmd.equals(pattern);
        }
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getCommandPattern() { return commandPattern; }
    public String getMatchType() { return matchType; }
}