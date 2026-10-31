package engine.world;

import java.util.List;
import java.util.Map;

public record UseRule(
        String itemId,
        String targetId,
        List<Map<String, Object>> conditions,
        List<Map<String, Object>> effects
) {}
