package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

public class EquipCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        if (args.length < 2) {
            GameOutput.println("Equip what? (Usage: equip <item name>)");
            return;
        }

        String typedName = joinFrom(args, 1).toLowerCase();

        String foundItemId = findInventoryItemIdByName(player, world, typedName);
        if (foundItemId == null) {
            GameOutput.println("You don't have \"" + typedName + "\".");
            return;
        }

        Item item = world.getItems().get(foundItemId);
        if (item == null) {
            GameOutput.println("Unknown item.");
            return;
        }

        if (item.isWeapon()) {
            player.getInventoryItemIds().remove(foundItemId);

            String oldWeapon = player.getEquippedWeaponId();
            if (oldWeapon != null) {
                player.addToInventory(oldWeapon);
            }

            player.setEquippedWeaponId(foundItemId);

            GameOutput.println("You equipped weapon: " + item.name() + ".");

            // CHANGED: end of player's turn => monster attacks, then status ticks
            world.monsterAttackIfInCombat(player); // CHANGED
            if (!world.isReturnToMenuPending()) {  // CHANGED
                world.advanceTurn(player);         // CHANGED
            }
            return;
        }

        if (item.isArmor()) {
            player.getInventoryItemIds().remove(foundItemId);

            String oldArmor = player.getEquippedArmorId();
            if (oldArmor != null) {
                player.addToInventory(oldArmor);
            }

            player.setEquippedArmorId(foundItemId);

            GameOutput.println("You equipped armor: " + item.name() + ".");

            // CHANGED: end of player's turn => monster attacks, then status ticks
            world.monsterAttackIfInCombat(player); // CHANGED
            if (!world.isReturnToMenuPending()) {  // CHANGED
                world.advanceTurn(player);         // CHANGED
            }
            return;
        }

        GameOutput.println("You can't equip that.");
    }

    private static String findInventoryItemIdByName(Player player, World world, String typedName) {
        for (String itemId : player.getInventoryItemIds()) {
            Item item = world.getItems().get(itemId);
            if (item != null && item.name() != null && item.name().equalsIgnoreCase(typedName)) {
                return itemId;
            }
        }
        return null;
    }

    private static String joinFrom(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}