package engine.world;

import engine.combat.Monster;
import engine.combat.StatusEffectInstance;
import engine.combat.CombatEffects; // CHANGED
import engine.npc.DialogueOption;
import engine.npc.Npc;
import util.GameOutput;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class World {
    private final Map<String, Room> rooms;
    private final String startRoomId;

    private final Map<String, Item> items;
    private final Map<String, Target> targets;
    private final List<UseRule> useRules;
    private final Map<String, Monster> monsters;
    private final List<String> helpLines;
    private final CommandConfig commandConfig;

    private final Map<String, Npc> npcs;
    private final String currencyName;

    private final Random rng = new Random();

    private String activeMonsterId;

    private boolean exitConfirmationPending = false;
    private boolean returnToMenuPending = false;

    public static final int INTERACT_NONE = 0;
    public static final int INTERACT_TRADER_CONFIRM = 1;
    public static final int INTERACT_TRADER_BUY = 2;
    public static final int INTERACT_DIALOGUE_CHOICE = 3;
    public static final int INTERACT_DIALOGUE_WAIT = 4;

    private boolean inCombat = false;
    private int interactionStage = INTERACT_NONE;
    private String pendingTraderId;
    private String pendingDialogueNpcId;
    private List<DialogueOption> pendingDialogueOptions;

    private final List<WinRule> winRules; // CHANGED
    private boolean hasWon = false;       // CHANGED
    private String winMessage = "";       // CHANGED

    public World(
            Map<String, Room> rooms,
            String startRoomId,
            Map<String, Item> items,
            Map<String, Target> targets,
            Map<String, Monster> monsters,
            List<UseRule> useRules,
            List<String> helpLines,
            CommandConfig commandConfig,
            Map<String, Npc> npcs,
            String currencyName,
            List<WinRule> winRules // CHANGED
    ) {
        this.rooms = rooms;
        this.startRoomId = startRoomId;
        this.items = items;
        this.targets = targets;
        this.useRules = useRules;
        this.monsters = monsters;
        this.helpLines = helpLines;
        this.commandConfig = commandConfig;
        this.npcs = npcs;
        this.currencyName = (currencyName == null || currencyName.isBlank()) ? "Gold" : currencyName;
        this.winRules = (winRules == null) ? List.of() : new ArrayList<>(winRules); // CHANGED
    }

    public Room getStartRoom() { return rooms.get(startRoomId); }
    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Item> getItems() { return items; }
    public Map<String, Target> getTargets() { return targets; }
    public List<UseRule> getUseRules() { return useRules; }
    public Map<String, Monster> getMonsters() { return monsters; }
    public List<String> getHelpLines() { return helpLines; }
    public Random getRng() { return rng; }
    public CommandConfig getCommandConfig() { return commandConfig; }

    public Map<String, Npc> getNpcs() { return npcs; }
    public String getCurrencyName() { return currencyName; }

    public List<WinRule> getWinRules() { return winRules; } // CHANGED
    public boolean hasWon() { return hasWon; } // CHANGED
    public String getWinMessage() { return winMessage; } // CHANGED

    public void checkWinConditionAfterKill(String killedMonsterId) { // CHANGED
        if (hasWon) return;
        if (killedMonsterId == null || killedMonsterId.isBlank()) return;
        if (winRules == null || winRules.isEmpty()) return;

        for (WinRule r : winRules) {
            if (r == null) continue;

            String type = (r.type() == null) ? "" : r.type();
            if (!"killMonster".equalsIgnoreCase(type)) continue;

            if (killedMonsterId.equals(r.monsterId())) {
                hasWon = true;
                String msg = r.message();
                winMessage = (msg == null || msg.isBlank()) ? "You win!" : msg;
                return;
            }
        }
    }

    public List<Npc> getNpcsInRoom(String roomId) {
        if (npcs == null) return List.of();
        return npcs.values().stream()
                .filter(n -> n.getRoomId().equals(roomId))
                .collect(Collectors.toList());
    }

    public boolean isInCombat() { return inCombat; }
    public void setInCombat(boolean inCombat) { this.inCombat = inCombat; }

    public String getActiveMonsterId() { return activeMonsterId; }
    public void setActiveMonsterId(String activeMonsterId) { this.activeMonsterId = activeMonsterId; }

    public boolean isExitConfirmationPending() { return exitConfirmationPending; }
    public void setExitConfirmationPending(boolean pending) { this.exitConfirmationPending = pending; }

    public boolean isReturnToMenuPending() { return returnToMenuPending; }
    public void setReturnToMenuPending(boolean pending) { this.returnToMenuPending = pending; }

    public int getInteractionStage() { return interactionStage; }
    public void setInteractionStage(int stage) { this.interactionStage = stage; }

    public String getPendingTraderId() { return pendingTraderId; }
    public void setPendingTraderId(String id) { this.pendingTraderId = id; }

    public String getPendingDialogueNpcId() { return pendingDialogueNpcId; }
    public void setPendingDialogueNpcId(String id) { this.pendingDialogueNpcId = id; }

    public List<DialogueOption> getPendingDialogueOptions() { return pendingDialogueOptions; }
    public void setPendingDialogueOptions(List<DialogueOption> options) { this.pendingDialogueOptions = options; }

    // CHANGED: unified "monster's turn" logic (Pokemon-style)
    public void monsterAttackIfInCombat(Player player) { // CHANGED
        if (player == null) return; // CHANGED
        if (!isInCombat()) return;  // CHANGED

        String mid = getActiveMonsterId(); // CHANGED
        if (mid == null || mid.isBlank()) { // CHANGED
            setInCombat(false); // CHANGED
            return; // CHANGED
        }

        Monster m = monsters.get(mid); // CHANGED
        if (m == null || !m.isAlive()) { // CHANGED
            setActiveMonsterId(null); // CHANGED
            setInCombat(false); // CHANGED
            return; // CHANGED
        }

        applyMonsterAttack(player, m); // CHANGED
    }

    private void applyMonsterAttack(Player player, Monster m) { // CHANGED
        int rawMonsterDmg = m.getAttackDamage(); // CHANGED

        int reduction = 0; // CHANGED
        String armorId = player.getEquippedArmorId(); // CHANGED
        if (armorId != null) { // CHANGED
            Item armor = items.get(armorId); // CHANGED
            if (armor != null) reduction = armor.damageReduction(); // CHANGED
        }

        int finalMonsterDmg = Math.max(0, rawMonsterDmg - reduction); // CHANGED
        player.damage(finalMonsterDmg); // CHANGED

        if (reduction > 0) { // CHANGED
            GameOutput.println(m.getName() + " hits you for " + finalMonsterDmg +
                    " damage. (" + rawMonsterDmg + " - " + reduction + ")"); // CHANGED
        } else { // CHANGED
            GameOutput.println(m.getName() + " hits you for " + finalMonsterDmg + " damage."); // CHANGED
        }
        GameOutput.println("Your HP: " + player.getHp()); // CHANGED

        CombatEffects.maybeApplyPoison(player, this, m); // CHANGED

        if (player.getHp() <= 0) { // CHANGED
            GameOutput.println("You Died"); // CHANGED
            setReturnToMenuPending(true); // CHANGED (match your main-menu behavior)
        }
    }

    public void advanceTurn(Player player) {
        if (player == null) return;

        List<StatusEffectInstance> effects = player.getStatusEffects();
        if (effects == null || effects.isEmpty()) return;

        for (StatusEffectInstance e : effects) {
            if ("poison".equalsIgnoreCase(e.id()) && e.damagePerTurn() > 0) {
                player.damage(e.damagePerTurn());
                GameOutput.println("Poison deals " + e.damagePerTurn() + " damage. (HP: " +
                        player.getHp() + "/" + player.getMaxHp() + ")");
                if (player.getHp() <= 0) {
                    GameOutput.println("You Died");
                    setReturnToMenuPending(true); // CHANGED (don’t System.exit)
                    return; // CHANGED
                }
            }
        }

        List<StatusEffectInstance> next = new ArrayList<>();
        for (StatusEffectInstance e : effects) {
            StatusEffectInstance ticked = e.tickDown();
            if (!ticked.isExpired()) next.add(ticked);
        }

        effects.clear();
        effects.addAll(next);
    }
}