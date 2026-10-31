package engine.world;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandConfig {
    private final Set<String> enabledCommandIds;
    private final Map<String, List<String>> aliasesByCommandId;

    public CommandConfig(Set<String> enabledCommandIds, Map<String, List<String>> aliasesByCommandId) {
        this.enabledCommandIds = enabledCommandIds;
        this.aliasesByCommandId = aliasesByCommandId;
    }

    public Set<String> enabledCommandIds() {
        return enabledCommandIds;
    }

    public Map<String, List<String>> aliasesByCommandId() {
        return aliasesByCommandId;
    }
}