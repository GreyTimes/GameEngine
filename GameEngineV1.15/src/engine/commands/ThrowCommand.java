package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.combat.Monster;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

public class ThrowCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        if (!world.isInCombat()) {
            GameOutput.println("You can only use combat items during locked combat.");
            return;
        }

        if (args.length < 2) {
            GameOutput.println("Usage: throw <itemId>");
            return;
        }

        String typed = joinFrom(args, 1).trim();
        String itemId = resolveInventoryItemId(player, world, typed);

        if (itemId == null || !player.hasItem(itemId)) { // CHANGED: null safe
            GameOutput.println("You don't have that item.");
            return;
        }

        Monster m = world.getMonsters().get(world.getActiveMonsterId());
        if (m == null || !m.isAlive()) {
            GameOutput.println("No valid monster to use that on.");
            world.setActiveMonsterId(null);
            world.setInCombat(false);
            return;
        }

        Item item = world.getItems().get(itemId);
        if (item == null) {
            GameOutput.println("Unknown item.");
            return;
        }

        int dmg = item.attackDamage();
        if (dmg <= 0) {
            GameOutput.println("That item can't be used to deal damage.");
            return;
        }

        m.damage(dmg);
        GameOutput.println("You used " + item.name() + " and dealt " + dmg + " damage.");
        GameOutput.println(m.getName() + " HP: " + m.getHp());

        if (item.consumable()) {
            player.getInventoryItemIds().remove(itemId);
        }

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
            world.advanceTurn(player); // status tick only
            return;
        }

        // CHANGED: end of player's turn => monster attacks, then status ticks
        world.monsterAttackIfInCombat(player); // CHANGED
        if (!world.isReturnToMenuPending()) {  // CHANGED
            world.advanceTurn(player);         // CHANGED
        }
    }

    private static String resolveInventoryItemId(Player player, World world, String typed) {
        if (typed == null || typed.isBlank()) return null;

        if (player.hasItem(typed)) return typed;

        String typedName = typed.toLowerCase();
        for (String invId : player.getInventoryItemIds()) {
            Item invItem = world.getItems().get(invId);
            if (invItem != null && invItem.name() != null && invItem.name().equalsIgnoreCase(typedName)) {
                return invId;
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