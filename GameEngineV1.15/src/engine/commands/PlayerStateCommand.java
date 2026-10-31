package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

import java.util.Map; // CHANGED

public class PlayerStateCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        // Current Room
        String roomId = (player.getCurrentRoom() != null) ? player.getCurrentRoom().getId() : "Unknown";
        GameOutput.println("Room: " + roomId);

        // Exits (for current room) // CHANGED
        if (player.getCurrentRoom() != null) { // CHANGED
            Map<String, String> exits = player.getCurrentRoom().getExits(); // CHANGED
            if (exits != null && !exits.isEmpty()) { // CHANGED
                StringBuilder sb = new StringBuilder("Exits: "); // CHANGED
                int i = 0; // CHANGED
                for (Map.Entry<String, String> e : exits.entrySet()) { // CHANGED
                    if (i++ > 0) sb.append(", "); // CHANGED
                    sb.append(e.getKey()).append(" -> ").append(e.getValue()); // CHANGED
                }
                GameOutput.println(sb.toString()); // CHANGED
            } else { // CHANGED
                GameOutput.println("Exits: none"); // CHANGED
            }
        } // CHANGED

        // HP
        GameOutput.println("HP: " + player.getHp() + "/" + player.getMaxHp());

        // Attack + Weapon
        int baseAttack = player.getAttackDamage();
        int weaponBonus = 0;
        String weaponName = "None";

        String weaponId = player.getEquippedWeaponId();
        if (weaponId != null) {
            Item weapon = world.getItems().get(weaponId);
            if (weapon != null) {
                weaponBonus = weapon.attackDamage();
                weaponName = weapon.name();
            }
        }

        int totalAttack = baseAttack + weaponBonus;
        GameOutput.println("Attack: " + totalAttack + " (Weapon: " + weaponName + ")");

        // Armor
        int armorReduction = 0;
        String armorName = "None";

        String armorId = player.getEquippedArmorId();
        if (armorId != null) {
            Item armor = world.getItems().get(armorId);
            if (armor != null) {
                armorReduction = armor.damageReduction();
                armorName = armor.name();
            }
        }

        GameOutput.println("Armor: " + armorReduction + " (Armor: " + armorName + ")");

        // Carry Capacity
        GameOutput.println("Carry: " + player.getInventoryItemIds().size() + "/" + player.getCarryCapacity());

        // Gold
        GameOutput.println("Gold: " + player.getMoney());
    }
}