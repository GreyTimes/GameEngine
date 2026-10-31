package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.combat.Monster;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

public class HitCommand implements Command {
    @Override
    public void execute(String[] args, Player player, World world) {
        if (!world.isInCombat()) {
            GameOutput.println("You can only 'hit' during locked combat.");
            return;
        }

        Monster m = world.getMonsters().get(world.getActiveMonsterId());
        if (m == null || !m.isAlive()) {
            world.setActiveMonsterId(null);
            world.setInCombat(false);
            GameOutput.println("No valid monster to fight.");
            return;
        }

        int base = player.getAttackDamage();
        int weaponBonus = 0;

        String weaponId = player.getEquippedWeaponId();
        if (weaponId != null) {
            Item weapon = world.getItems().get(weaponId);
            if (weapon != null) weaponBonus = weapon.attackDamage();
        }

        int playerDmg = base + weaponBonus;

        m.damage(playerDmg);
        if (weaponBonus > 0) {
            GameOutput.println("You hit " + m.getName() + " for " + playerDmg + " damage. (" + base + " + " + weaponBonus + ")");
        } else {
            GameOutput.println("You hit " + m.getName() + " for " + playerDmg + " damage.");
        }
        GameOutput.println(m.getName() + " HP: " + m.getHp());

        if (!m.isAlive()) {
            GameOutput.println("You defeated " + m.getName() + "!");

            String dropItemId = m.getDropItemId();
            if (dropItemId != null && !dropItemId.isBlank()) {
                player.getCurrentRoom().addItemId(dropItemId);

                Item dropped = world.getItems().get(dropItemId);
                String droppedName = (dropped != null && dropped.name() != null) ? dropped.name() : dropItemId;

                GameOutput.println(m.getName() + " dropped " + droppedName + ".");
            }

            int drop = m.getDropCurrency();
            if (drop > 0) {
                player.getCurrentRoom().addCurrency(drop);
                GameOutput.println(m.getName() + " dropped " + drop + " " + world.getCurrencyName() + ".");
            }

            world.checkWinConditionAfterKill(m.getId());
            if (world.hasWon()) {
                GameOutput.println("");
                GameOutput.println("=== YOU WIN ===");
                GameOutput.println(world.getWinMessage());
                world.setReturnToMenuPending(true);
                world.setActiveMonsterId(null);
                world.setInCombat(false);
                return;
            }

            world.setActiveMonsterId(null);
            world.setInCombat(false);
            GameOutput.println("Combat ended.");
            world.advanceTurn(player); // status tick only
            return;
        }

        // CHANGED: end of player's turn => monster attacks, then status ticks
        world.monsterAttackIfInCombat(player); // CHANGED
        if (!world.isReturnToMenuPending()) {  // CHANGED
            world.advanceTurn(player);         // CHANGED
        }
    }
}