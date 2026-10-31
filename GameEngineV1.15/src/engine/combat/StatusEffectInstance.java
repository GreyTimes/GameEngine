package engine.combat;

public record StatusEffectInstance(String id, String name, int remainingTurns, int damagePerTurn) {

    public StatusEffectInstance tickDown() {
        return new StatusEffectInstance(id, name, remainingTurns - 1, damagePerTurn);
    }

    public boolean isExpired() {
        return remainingTurns <= 0;
    }
}