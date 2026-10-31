package engine.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {
    private final String id;
    private final String description;

    private final Map<String, String> exits;

    private final List<String> itemIds;
    private final List<String> targetIds;
    private final List<String> monsterIds;

    private int currencyAmount;

    public Room(String id, String description, Map<String, String> exits,
                List<String> itemIds, List<String> targetIds, List<String> monsterIds, int currencyAmount) {
        this.id = id;
        this.description = description;

        this.exits = (exits == null) ? new HashMap<>() : new HashMap<>(exits);
        this.itemIds = (itemIds == null) ? new ArrayList<>() : new ArrayList<>(itemIds);
        this.targetIds = (targetIds == null) ? new ArrayList<>() : new ArrayList<>(targetIds);
        this.monsterIds = (monsterIds == null) ? new ArrayList<>() : new ArrayList<>(monsterIds);

        this.currencyAmount = Math.max(0, currencyAmount);
    }

    public String getId() { return id; }
    public String getDescription() { return description; }

    public String getExit(String direction) { return exits.get(direction); }
    public Map<String, String> getExits() { return exits; }

    public void setExit(String direction, String toRoomId) { exits.put(direction, toRoomId); }

    public List<String> getItemIds() { return itemIds; }
    public List<String> getTargetIds() { return targetIds; }
    public List<String> getMonsterIds() { return monsterIds; }

    public boolean removeItemId(String itemId) { return itemIds.remove(itemId); } // removes one copy
    public void addItemId(String itemId) { itemIds.add(itemId); }

    public int getCurrencyAmount() { return currencyAmount; }
    public void setCurrencyAmount(int amount) { currencyAmount = Math.max(0, amount); }

    public void addCurrency(int amount) {
        if (amount > 0) currencyAmount += amount;
    }

    public int takeAllCurrency() {
        int amt = currencyAmount;
        currencyAmount = 0;
        return amt;
    }
}