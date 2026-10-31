package engine.combat;

public class Monster {
    private final String id;
    private final String name;
    private final String description;
    private int hp;
    private final int attackDamage;
    private final String dropItemId;
    private final int dropCurrency;
    private boolean alive = true;
    private final int fleeSuccessChance;
    private final int poisonChance;
    private final int poisonDamagePerTurn;
    private final int poisonDurationTurns;

    public Monster(
            String id,
            String name,
            String description,
            int hp,
            int attackDamage,
            String dropItemId,
            int fleeSuccessChance,
            int poisonChance,
            int poisonDamagePerTurn,
            int poisonDurationTurns,
            int dropCurrency
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hp = hp;
        this.attackDamage = attackDamage;
        this.dropItemId = dropItemId;
        this.fleeSuccessChance = fleeSuccessChance;
        this.poisonChance = poisonChance;
        this.poisonDamagePerTurn = poisonDamagePerTurn;
        this.poisonDurationTurns = poisonDurationTurns;
        this.dropCurrency = Math.max(0, dropCurrency);
    }

    public Monster(String id, String name, String description, int hp, int attackDamage, String dropItemId) {
        this(id, name, description, hp, attackDamage, dropItemId, 30, 0, 0, 0, 0);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getHp() { return hp; }
    public int getAttackDamage() { return attackDamage; }
    public String getDropItemId() { return dropItemId; }
    public boolean isAlive() { return alive; }
    public int getFleeSuccessChance() { return fleeSuccessChance; }
    public int getPoisonChance() { return poisonChance; }
    public int getPoisonDamagePerTurn() { return poisonDamagePerTurn; }
    public int getPoisonDurationTurns() { return poisonDurationTurns; }
    public int getDropCurrency() { return dropCurrency; }

    public void damage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }

    public void setHp(int hp) {
        this.hp = hp;
        if (hp > 0) this.alive = true;
        if (hp <= 0) this.alive = false;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}