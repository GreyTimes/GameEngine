package engine.npc;

import java.util.List;

public record DialogueNode(String npcLine, List<DialogueOption> options) {}
