package util;

import engine.world.*;
import engine.npc.*;
import engine.combat.Monster;
import engine.saveSystem.CheckpointTrigger;

import java.util.HashSet;
import java.util.Set;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonLoader {
    private static int playerHp;
    private static int playerMaxHp;
    private static int playerAttackDamage;
    private static int playerBaseEscapeChance = 30;
    private static int playerCarryCapacity = 10;
    private static int playerMoney = 0;
    private static String gameTitle = "Text Adventure";

    // NEW: Checkpoint configuration
    private static int maxCheckpoints = 5;
    private static List<CheckpointTrigger> checkpointTriggers = new ArrayList<>();

    private static List<WinRule> winRules = new ArrayList<>(); // CHANGED

    public static World loadWorld(String path) {
        try {
            Map<String, Object> json = SimpleJsonParser.parseFile(path);

            gameTitle = getString(json, "gameTitle", "Text Adventure");

            String startRoom = getString(json, "startRoom", null);
            if (startRoom == null || startRoom.isBlank()) {
                throw new IllegalArgumentException("world.json missing required field: startRoom");
            }

            Object rawRooms = json.get("rooms");
            if (!(rawRooms instanceof List<?>)) {
                throw new IllegalArgumentException("world.json missing required field: rooms (must be an array)");
            }

            loadPlayerStats(json);

            // NEW: Load checkpoint configuration
            loadCheckpointConfig(json);

            Map<String, Item> items = loadItems(json);
            Map<String, Target> targets = loadTargets(json);
            Map<String, Monster> monsters = loadMonsters(json);
            Map<String, Room> rooms = loadRooms(json);
            List<UseRule> useRules = loadUseRules(json);
            List<String> helpLines = loadHelpLines(json);
            CommandConfig commandConfig = loadCommandConfig(json);
            String currencyName = loadCurrencyName(json);
            Map<String, Npc> npcs = loadNpcs(json);

            winRules = loadWinRules(json); // CHANGED

            return new World( // CHANGED
                    rooms, startRoom, items, targets, monsters, useRules,
                    helpLines, commandConfig, npcs, currencyName, winRules
            );

        } catch (IOException e) {
            System.err.println("Error loading world (IO): " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Error loading world (JSON): " + e.getMessage());
            return null;
        }
    }

    private static String loadCurrencyName(Map<String, Object> json) {
        Map<?, ?> currency = asMap(json.get("currency"));
        return getString(currency, "name", "Gold");
    }

    private static Map<String, Npc> loadNpcs(Map<String, Object> json) {
        Map<String, Npc> npcs = new HashMap<>();

        for (Object obj : getList(json, "npcs")) {
            Map<?, ?> data = asMap(obj);
            if (data == null) continue;

            String id = getString(data, "id", "");
            if (id.isBlank()) continue;

            String name = getString(data, "name", id);
            String type = getString(data, "type", "");
            String roomId = getString(data, "room", "");

            if ("trader".equalsIgnoreCase(type)) {
                List<TradeOffer> offers = new ArrayList<>();
                Object invRaw = data.get("inventory");
                if (invRaw instanceof List<?> list) {
                    for (Object entry : list) {
                        Map<?, ?> m = asMap(entry);
                        if (m == null) continue;
                        String itemId = getString(m, "item", "");
                        int price = getInt(m, "price", 0);
                        if (!itemId.isBlank() && price > 0) {
                            offers.add(new TradeOffer(itemId, price));
                        }
                    }
                }

                List<String> greetings = toStringList(data.get("greetings"));
                npcs.put(id, new TraderNpc(id, name, roomId, offers, greetings));
            } else if ("dialogue".equalsIgnoreCase(type)) {
                List<String> lines = toStringList(data.get("lines"));
                List<DialogueNode> dialogues = loadDialogueNodes(data);
                npcs.put(id, new DialogueNpc(id, name, roomId, lines, dialogues));
            }
        }
        return npcs;
    }

    private static List<DialogueNode> loadDialogueNodes(Map<?, ?> data) {
        List<DialogueNode> nodes = new ArrayList<>();
        Object raw = (data == null) ? null : data.get("dialogues");
        if (!(raw instanceof List<?> list)) return nodes;

        for (Object entry : list) {
            Map<?, ?> m = asMap(entry);
            if (m == null) continue;

            String npcLine = getString(m, "npc", "");
            Object rawOptions = m.get("player");
            List<DialogueOption> options = new ArrayList<>();

            if (rawOptions instanceof List<?> optList) {
                for (Object optObj : optList) {
                    Map<?, ?> opt = asMap(optObj);
                    if (opt == null) continue;

                    String playerLine = getString(opt, "line", "");
                    String npcResponse = getString(opt, "response", "");

                    if (!playerLine.isBlank()) {
                        options.add(new DialogueOption(playerLine, npcResponse));
                    }
                }
            }

            if (!npcLine.isBlank()) {
                nodes.add(new DialogueNode(npcLine, options));
            }
        }
        return nodes;
    }

    private static void loadPlayerStats(Map<String, Object> json) {
        Map<?, ?> player = asMap(json.get("player"));
        if (player == null) return;

        playerHp = getInt(player, "hp", playerHp);
        playerMaxHp = getInt(player, "maxHp", playerHp);
        playerAttackDamage = getInt(player, "attackDamage", playerAttackDamage);
        playerBaseEscapeChance = getInt(player, "baseEscapeChance", playerBaseEscapeChance);
        playerCarryCapacity = getInt(player, "carryCapacity", playerCarryCapacity);
        playerMoney = getInt(player, "money", playerMoney);
    }

    /**
     * NEW: Load checkpoint configuration from world.json
     */
    private static void loadCheckpointConfig(Map<String, Object> json) {
        Map<?, ?> checkpointsObj = asMap(json.get("checkpoints"));
        if (checkpointsObj == null) {
            maxCheckpoints = 5;
            checkpointTriggers = new ArrayList<>();
            return;
        }

        maxCheckpoints = getInt(checkpointsObj, "maxCheckpoints", 5);

        checkpointTriggers = new ArrayList<>();
        List<?> triggersRaw = getList(checkpointsObj, "triggers");
        for (Object triggerObj : triggersRaw) {
            Map<?, ?> triggerData = asMap(triggerObj);
            if (triggerData == null) continue;

            String name = getString(triggerData, "name", "");
            String displayName = getString(triggerData, "displayName", "");
            String commandPattern = getString(triggerData, "commandPattern", "");
            String matchType = getString(triggerData, "matchType", "exact");

            if (!name.isBlank() && !displayName.isBlank() && !commandPattern.isBlank()) {
                CheckpointTrigger trigger = new CheckpointTrigger(name, displayName, commandPattern, matchType);
                checkpointTriggers.add(trigger);
            }
        }
    }

    private static List<String> loadHelpLines(Map<String, Object> json) {
        Object raw = json.get("helpLines");
        return toStringList(raw);
    }

    private static Map<String, Item> loadItems(Map<String, Object> json) {
        Map<String, Item> items = new HashMap<>();
        for (Object obj : getList(json, "items")) {
            Map<?, ?> data = asMap(obj);
            if (data == null) continue;

            String id = getString(data, "id", "");
            if (id.isBlank()) continue;

            String name = getString(data, "name", id);
            String description = getString(data, "description", "");

            int attackDamage = (data.get("attackDamage") instanceof Number n) ? n.intValue() : 0;
            int damageReduction = (data.get("damageReduction") instanceof Number n) ? n.intValue() : 0;
            boolean consumable = (data.get("consumable") instanceof Boolean b) ? b : false;

            items.put(id, new Item(id, name, description, attackDamage, damageReduction, consumable));
        }
        return items;
    }

    private static Map<String, Target> loadTargets(Map<String, Object> json) {
        Map<String, Target> targets = new HashMap<>();

        for (Object obj : getList(json, "targets")) {
            Map<?, ?> data = asMap(obj);
            if (data == null) continue;

            String id = getString(data, "id", "");
            if (id.isBlank()) continue;

            String name = getString(data, "name", id);
            String description = getString(data, "description", "");

            targets.put(id, new Target(id, name, description));
        }
        return targets;
    }

    private static Map<String, Monster> loadMonsters(Map<String, Object> json) {
        Map<String, Monster> monsters = new HashMap<>();

        for (Object obj : getList(json, "monsters")) {
            Map<?, ?> data = asMap(obj);
            if (data == null) continue;

            String id = getString(data, "id", "");
            if (id.isBlank()) continue;

            String name = getString(data, "name", id);
            String description = getString(data, "description", "");

            int hp = getInt(data, "hp", 0);
            int attackDamage = getInt(data, "attackDamage", 0);
            String dropItemId = getString(data, "dropItemId", null);
            int fleeSuccessChance = getInt(data, "fleeSuccessChance", 30);
            int poisonChance = getInt(data, "poisonChance", 0);
            int poisonDamagePerTurn = getInt(data, "poisonDamagePerTurn", 0);
            int poisonDurationTurns = getInt(data, "poisonDurationTurns", 0);
            int dropCurrency = getInt(data, "dropCurrency", 0);

            monsters.put(id, new Monster(
                    id, name, description, hp, attackDamage, dropItemId,
                    fleeSuccessChance, poisonChance, poisonDamagePerTurn, poisonDurationTurns, dropCurrency
            ));
        }

        return monsters;
    }

    private static Map<String, Room> loadRooms(Map<String, Object> json) {
        Map<String, Room> rooms = new HashMap<>();

        for (Object roomObj : getList(json, "rooms")) {
            Map<?, ?> roomData = asMap(roomObj);
            if (roomData == null) continue;

            String id = getString(roomData, "id", "");
            if (id.isBlank()) continue;

            String description = getString(roomData, "description", "");

            Map<String, String> exits = loadExits(roomData);

            List<String> itemIds = getStringList(roomData, "items");
            List<String> targetIds = getStringList(roomData, "targets");
            List<String> monsterIds = getStringList(roomData, "monsters");
            int currency = getInt(roomData, "currency", 0);

            rooms.put(id, new Room(id, description, exits, itemIds, targetIds, monsterIds, currency));
        }
        return rooms;
    }

    private static Map<String, String> loadExits(Map<?, ?> roomData) {
        Map<String, String> exits = new HashMap<>();
        Map<?, ?> exitsData = asMap(roomData.get("exits"));
        if (exitsData == null) return exits;

        for (Object k : exitsData.keySet()) {
            Object v = exitsData.get(k);
            if (k instanceof String ks && v instanceof String vs) {
                exits.put(ks, vs);
            }
        }
        return exits;
    }

    private static List<UseRule> loadUseRules(Map<String, Object> json) {
        List<UseRule> rules = new ArrayList<>();

        for (Object obj : getList(json, "useRules")) {
            Map<?, ?> data = asMap(obj);
            if (data == null) continue;

            String itemId = getString(data, "item", "");
            String targetId = getString(data, "target", "");
            if (itemId.isBlank() || targetId.isBlank()) continue;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions =
                    (data.get("conditions") instanceof List<?> c)
                            ? (List<Map<String, Object>>) c
                            : new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> effects =
                    (data.get("effects") instanceof List<?> e)
                            ? (List<Map<String, Object>>) e
                            : new ArrayList<>();

            rules.add(new UseRule(itemId, targetId, conditions, effects));
        }

        return rules;
    }

    private static CommandConfig loadCommandConfig(Map<String, Object> json) {
        Set<String> enabled = null;
        Map<String, List<String>> aliases = new HashMap<>();

        Map<?, ?> commandsObj = asMap(json.get("commands"));
        if (commandsObj == null) {
            return new CommandConfig(null, Map.of());
        }

        Object enabledRaw = commandsObj.get("enabled");
        List<String> enabledList = toStringList(enabledRaw);
        if (!enabledList.isEmpty()) {
            enabled = new HashSet<>(enabledList);
        }

        Map<?, ?> aliasesObj = asMap(commandsObj.get("aliases"));
        if (aliasesObj != null) {
            for (Object k : aliasesObj.keySet()) {
                if (!(k instanceof String commandId)) continue;
                List<String> names = toStringList(aliasesObj.get(k));
                if (!names.isEmpty()) aliases.put(commandId, names);
            }
        }
        return new CommandConfig(enabled, aliases);
    }

    private static List<WinRule> loadWinRules(Map<String, Object> json) { // CHANGED
        List<WinRule> rules = new ArrayList<>(); // CHANGED

        for (Object obj : getList(json, "winRules")) { // CHANGED
            Map<?, ?> data = asMap(obj); // CHANGED
            if (data == null) continue; // CHANGED

            String type = getString(data, "type", ""); // CHANGED
            String monsterId = getString(data, "monsterId", ""); // CHANGED
            String message = getString(data, "message", "You win!"); // CHANGED

            if (!type.isBlank() && !monsterId.isBlank()) { // CHANGED
                rules.add(new WinRule(type, monsterId, message)); // CHANGED
            }
        }

        return rules; // CHANGED
    }

    private static Map<?, ?> asMap(Object o) {
        return (o instanceof Map<?, ?> m) ? m : null;
    }

    private static List<?> getList(Map<?, ?> m, String key) {
        if (m == null) return List.of();
        Object v = m.get(key);
        return (v instanceof List<?> l) ? l : List.of();
    }

    private static String getString(Map<?, ?> m, String key, String def) {
        Object v = (m == null) ? null : m.get(key);
        return (v instanceof String s) ? s : def;
    }

    private static int getInt(Map<?, ?> m, String key, int def) {
        Object v = (m == null) ? null : m.get(key);
        return (v instanceof Number n) ? n.intValue() : def;
    }

    private static List<String> getStringList(Map<?, ?> m, String key) {
        Object v = (m == null) ? null : m.get(key);
        return toStringList(v);
    }

    private static List<String> toStringList(Object raw) {
        if (!(raw instanceof List<?> list)) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (Object v : list) if (v instanceof String s) out.add(s);
        return out;
    }

    // Getters for checkpoint configuration
    public static int getMaxCheckpoints() {
        return maxCheckpoints;
    }

    public static List<CheckpointTrigger> getCheckpointTriggers() {
        return new ArrayList<>(checkpointTriggers);
    }

    public static List<WinRule> getWinRules() { // CHANGED
        return new ArrayList<>(winRules); // CHANGED
    }

    public static int getPlayerHp() { return playerHp; }
    public static int getPlayerMaxHp() { return playerMaxHp; }
    public static int getPlayerAttackDamage() { return playerAttackDamage; }
    public static int getPlayerBaseEscapeChance() { return playerBaseEscapeChance; }
    public static int getPlayerCarryCapacity() { return playerCarryCapacity; }
    public static int getPlayerMoney() { return playerMoney; }
    public static String getGameTitle() { return gameTitle; }
}