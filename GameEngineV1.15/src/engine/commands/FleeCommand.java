package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.combat.Monster;
import engine.world.Player;
import engine.world.Room;
import engine.world.World;
import engine.combat.CombatEffects;
import util.GameOutput;

public class FleeCommand implements Command {
    @Override
    public void execute(String[] args, Player player, World world) {
        if (!world.isInCombat()) {
            GameOutput.println("You can't flee right now.");
            return;
        }

        Room previous = player.getPreviousRoom();
        if (previous == null) {
            GameOutput.println("You cannot flee from here.");
            return;
        }

        Monster m = world.getMonsters().get(world.getActiveMonsterId());
        if (m == null || !m.isAlive()) {
            world.setActiveMonsterId(null);
            world.setInCombat(false);
            GameOutput.println("There is nothing to flee from.");
            return;
        }

        int chance = m.getFleeSuccessChance();
        chance = Math.max(0, Math.min(100, chance));

        int roll = world.getRng().nextInt(100);
        if (roll >= chance) {
            int rawMonsterDmg = m.getAttackDamage();

            int reduction = 0;
            String armorId = player.getEquippedArmorId();
            if (armorId != null) {
                Item armor = world.getItems().get(armorId);
                if (armor != null) reduction = armor.damageReduction();
            }

            int finalMonsterDmg = Math.max(0, rawMonsterDmg - reduction);
            player.damage(finalMonsterDmg);

            GameOutput.println("You failed to flee! (" + chance + "% success chance)");
            if (reduction > 0) {
                GameOutput.println(m.getName() + " hits you for " + finalMonsterDmg +
                        " damage. (" + rawMonsterDmg + " - " + reduction + ")");
            } else {
                GameOutput.println(m.getName() + " hits you for " + finalMonsterDmg + " damage.");
            }
            GameOutput.println("Your HP: " + player.getHp());

            CombatEffects.maybeApplyPoison(player, world, m);

            if (player.getHp() <= 0) {
                GameOutput.println("You Died");
                System.exit(0);
            }

            world.advanceTurn(player);
            return;
        }

        player.setCurrentRoom(previous);

        world.setActiveMonsterId(null);
        world.setInCombat(false);

        GameOutput.println("You fled back to the previous room! (" + chance + "% success chance)");
        GameOutput.println(previous.getDescription());

        world.advanceTurn(player);
    }
}