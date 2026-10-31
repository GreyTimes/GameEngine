package engine.world;

import engine.combat.StatusEffectInstance;
import engine.world.Room;

import java.util.ArrayList;
import java.util.List;

public class Player {

    private Room currentRoom;
    private Room previousRoom;

    private int hp;
    private int maxHp;
    private int attackDamage;

    private int baseEscapeChance;
    private int carryCapacity;

    private String equippedWeaponId;
    private String equippedArmorId;

    private int money;

    private final List<String> inventoryItemIds = new ArrayList<>();
    private final List<StatusEffectInstance> statusEffects = new ArrayList<>();

    public Player(Room startRoom) {
        this.currentRoom = startRoom;
        this.previousRoom = null;
    }

    public int getHp() {
        return hp;
    }
    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getAttackDamage() {
        return attackDamage;
    }
    public void setAttackDamage(int attackDamage) {
        this.attackDamage = attackDamage;
    }

    public int getBaseEscapeChance() { return baseEscapeChance; }
    public void setBaseEscapeChance(int baseEscapeChance) { this.baseEscapeChance = baseEscapeChance; }

    public int getCarryCapacity() { return carryCapacity; }
    public void setCarryCapacity(int carryCapacity) { this.carryCapacity = carryCapacity; }

    public void damage(int amount) {
        this.hp -= amount;
        if (this.hp < 0) this.hp = 0;
    }

    public void heal(int amount) {
        this.hp += amount;
        if (this.hp > this.maxHp) this.hp = this.maxHp;
    }

    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { this.currentRoom = room; }

    public Room getPreviousRoom() { return previousRoom; }
    public void setPreviousRoom(Room previousRoom) { this.previousRoom = previousRoom; }

    public List<String> getInventoryItemIds() { return inventoryItemIds; }

    public boolean canCarryMore() {
        return inventoryItemIds.size() < carryCapacity;
    }

    public void addToInventory(String itemId) { inventoryItemIds.add(itemId); }
    public boolean hasItem(String itemId) { return inventoryItemIds.contains(itemId); }

    public String getEquippedWeaponId() { return equippedWeaponId; }
    public void setEquippedWeaponId(String id) { this.equippedWeaponId = id; }

    public String getEquippedArmorId() { return equippedArmorId; }
    public void setEquippedArmorId(String id) { this.equippedArmorId = id; }

    public List<StatusEffectInstance> getStatusEffects() { return statusEffects; }

    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = Math.max(0, money); }
    public void addMoney(int amount) { if (amount > 0) money += amount; }

    public boolean spendMoney(int amount) {
        if (amount <= 0) return true;
        if (money < amount) return false;
        money -= amount;
        return true;
    }

    // Add this method to Player class
    public boolean isDead() {
        return hp <= 0;
    }
}