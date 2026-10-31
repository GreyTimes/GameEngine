package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

public class UnequipCommand implements Command {
    @Override
    public void execute(String[] args, Player player, World world) {
        if (args.length < 2) {
            GameOutput.println("Unequip what? (Usage: unequip <weapon|armor>)");
            return;
        }

        String type = args[1].toLowerCase();

        if (type.equals("weapon")) {
            String weaponId = player.getEquippedWeaponId();
            if (weaponId == null) {
                GameOutput.println("You don't have a weapon equipped.");
                return;
            }

            if (!player.canCarryMore()) {
                GameOutput.println("You can't carry any more.");
                return;
            }

            player.setEquippedWeaponId(null);
            player.addToInventory(weaponId);

            Item weapon = world.getItems().get(weaponId);
            String name = (weapon != null && weapon.name() != null) ? weapon.name() : weaponId;
            GameOutput.println("You unequipped your weapon: " + name + ".");

            // CHANGED: end of player's turn => monster attacks, then status ticks
            world.monsterAttackIfInCombat(player); // CHANGED
            if (!world.isReturnToMenuPending()) {  // CHANGED
                world.advanceTurn(player);         // CHANGED
            }
            return;
        }

        if (type.equals("armor")) {
            String armorId = player.getEquippedArmorId();
            if (armorId == null) {
                GameOutput.println("You don't have armor equipped.");
                return;
            }

            if (!player.canCarryMore()) {
                GameOutput.println("You can't carry any more.");
                return;
            }

            player.setEquippedArmorId(null);
            player.addToInventory(armorId);

            Item armor = world.getItems().get(armorId);
            String name = (armor != null && armor.name() != null) ? armor.name() : armorId;
            GameOutput.println("You unequipped your armor: " + name + ".");

            // CHANGED: end of player's turn => monster attacks, then status ticks
            world.monsterAttackIfInCombat(player); // CHANGED
            if (!world.isReturnToMenuPending()) {  // CHANGED
                world.advanceTurn(player);         // CHANGED
            }
            return;
        }

        GameOutput.println("You can only unequip 'weapon' or 'armor'.");
    }
}