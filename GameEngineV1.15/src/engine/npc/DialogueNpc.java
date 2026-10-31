package engine.npc;

import java.util.List;

public class DialogueNpc extends Npc {
    private final List<String> lines;
    private final List<DialogueNode> dialogues;

    public DialogueNpc(String id, String name, String roomId, List<String> lines) {
        this(id, name, roomId, lines, null);
    }

    public DialogueNpc(String id, String name, String roomId, List<String> lines, List<DialogueNode> dialogues) {
        super(id, name, roomId, "dialogue");
        this.lines = lines;
        this.dialogues = dialogues;
    }

    public List<String> getLines() { return lines; }
    public List<DialogueNode> getDialogues() { return dialogues; }
}