package engine.npc;

import java.util.List;

public class TraderNpc extends Npc {
    private final List<TradeOffer> inventory;
    private final List<String> greetings;

    public TraderNpc(String id, String name, String roomId, List<TradeOffer> inventory, List<String> greetings) {
        super(id, name, roomId, "trader");
        this.inventory = inventory;
        this.greetings = greetings;
    }

    public List<TradeOffer> getInventory() { return inventory; }
    public List<String> getGreetings() { return greetings; }
}