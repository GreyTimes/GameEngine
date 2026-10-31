package engine.world;

public record Item(
        String id,
        String name,
        String description,
        int attackDamage,
        int damageReduction,
        boolean consumable
) {
    public Item(String id, String name, String description) {
        this(id, name, description, 0, 0, false);
    }

    public boolean isWeapon() {
        return attackDamage > 0;
    }

    public boolean isArmor() {
        return damageReduction > 0;
    }
}