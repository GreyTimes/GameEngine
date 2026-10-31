package engine.npc;

public abstract class Npc {
    private final String id;
    private final String name;
    private final String roomId;
    private final String type;

    protected Npc(String id, String name, String roomId, String type) {
        this.id = id;
        this.name = name;
        this.roomId = roomId;
        this.type = type;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRoomId() { return roomId; }
    public String getType() { return type; }
}