package engine.saveSystem;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight metadata for a single checkpoint.
 * Used in checkpoint_metadata.json to track all checkpoints in a slot.
 */
public class CheckpointMetadata {
    @JsonProperty("id")
    private int id;

    @JsonProperty("triggerName")
    private String triggerName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("commandCount")
    private int commandCount;

    // Default constructor for Jackson
    public CheckpointMetadata() {}

    public CheckpointMetadata(int id, String triggerName, String displayName, long timestamp) {
        this.id = id;
        this.triggerName = triggerName;
        this.displayName = displayName;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTriggerName() { return triggerName; }
    public void setTriggerName(String triggerName) { this.triggerName = triggerName; }

    public int getCommandCount() { return commandCount; }
    public void setCommandCount(int count) { this.commandCount = count; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}