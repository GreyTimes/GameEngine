package engine.combat;

import engine.world.Player;
import engine.world.World;
import util.GameOutput;

public final class CombatEffects {
    private CombatEffects() {}

    public static void maybeApplyPoison(Player player, World world, Monster m) {
        int chance = m.getPoisonChance();
        int dmg = m.getPoisonDamagePerTurn();
        int dur = m.getPoisonDurationTurns();

        if (chance <= 0 || dmg <= 0 || dur <= 0) return;

        if (chance > 100) chance = 100;

        int roll = world.getRng().nextInt(100);
        if (roll >= chance) return;

        player.getStatusEffects().removeIf(e -> "poison".equalsIgnoreCase(e.id()));

        player.getStatusEffects().add(new StatusEffectInstance("poison", "Poisoned", dur, dmg));
        GameOutput.println("You are poisoned! (" + dmg + " dmg/turn for " + dur + " turns)");
    }
}
