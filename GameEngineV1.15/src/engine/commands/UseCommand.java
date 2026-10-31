package engine.commands;

import engine.misc.Command;
import engine.world.Player;
import engine.world.Room;
import engine.world.UseRule;
import engine.world.World;
import util.GameOutput;

import java.util.List;
import java.util.Map;

public class UseCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        String itemId;
        String targetId;

        int onIndex = indexOfIgnoreCase(args, "on");

        if (onIndex != -1) {
            String itemName = join(args, 1, onIndex).toLowerCase();
            String targetName = join(args, onIndex + 1, args.length).toLowerCase();

            itemId = findInventoryItemIdByName(itemName, player, world);
            if (itemId == null) {
                GameOutput.println("You don't have \"" + itemName + "\".");
                return;
            }

            targetId = findRoomTargetIdByName(targetName, player.getCurrentRoom(), world);
            if (targetId == null) {
                GameOutput.println("There is no \"" + targetName + "\" here.");
                return;
            }
        } else {
            if (args.length < 3) {
                GameOutput.println("Usage: use <itemId> <targetId>  OR  use <item name> on <target name>");
                return;
            }
            itemId = args[1];
            targetId = args[2];
        }

        if (!player.hasItem(itemId)) {
            GameOutput.println("You don't have that item.");
            return;
        }

        Room currentRoom = player.getCurrentRoom();

        if (!currentRoom.getTargetIds().contains(targetId)) {
            GameOutput.println("You can't use that here.");
            return;
        }

        for (UseRule rule : world.getUseRules()) {
            if (!rule.itemId().equals(itemId)) continue;
            if (!rule.targetId().equals(targetId)) continue;

            if (!conditionsPass(rule.conditions(), player, currentRoom)) {
                continue;
            }

            applyEffects(rule.effects(), player, world, itemId);

            // CHANGED: end of player's turn => monster attacks (if in combat), then status ticks
            world.monsterAttackIfInCombat(player); // CHANGED
            if (!world.isReturnToMenuPending()) {  // CHANGED
                world.advanceTurn(player);         // CHANGED
            }
            return;
        }

        GameOutput.println("You can't use that.");
    }

    private boolean conditionsPass(List<Map<String, Object>> conditions, Player player, Room currentRoom) {
        if (conditions == null) return true;

        for (Map<String, Object> c : conditions) {
            String type = str(c.get("type"));

            if ("inRoom".equals(type)) {
                String roomId = str(c.get("room"));
                if (!currentRoom.getId().equals(roomId)) return false;
            } else if ("hasItem".equals(type)) {
                String itemId = str(c.get("item"));
                if (!player.hasItem(itemId)) return false;
            }
        }

        return true;
    }

    private void applyEffects(List<Map<String, Object>> effects,
                              Player player,
                              World world,
                              String usedItemId) {
        if (effects == null) return;

        int lastHealedAmount = 0;

        for (Map<String, Object> e : effects) {
            switch (str(e.get("type"))) {

                case "message" -> GameOutput.println(str(e.get("text")));

                case "addExit" -> {
                    Room r = world.getRooms().get(str(e.get("room")));
                    if (r != null) {
                        r.setExit(str(e.get("direction")), str(e.get("to")));
                    }
                }

                case "heal" -> lastHealedAmount = applyHeal(e, player);

                case "consumeItem" -> {
                    String itemToConsume = str(e.get("item"));
                    if (itemToConsume.isEmpty()) itemToConsume = usedItemId;

                    boolean skipIfNoEffect = boolVal(e.get("skipIfNoEffect"), false);
                    if (skipIfNoEffect && lastHealedAmount == 0) {
                        break;
                    }

                    player.getInventoryItemIds().remove(itemToConsume);
                }
            }
        }
    }

    private int applyHeal(Map<String, Object> e, Player player) {
        int amount = intVal(e.get("amount"), 0);
        int cap = intVal(e.get("cap"), 100);

        int oldHp = player.getHp();
        int newHp = Math.min(cap, oldHp + amount);
        int healed = newHp - oldHp;

        player.setHp(newHp);

        if (healed > 0) {
            GameOutput.println("You recover " + healed + " HP. (HP: " + player.getHp() + ")");
        } else {
            GameOutput.println("You're already at full health.");
        }

        return healed;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private int intVal(Object o, int defaultValue) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(str(o)); } catch (Exception ignored) { return defaultValue; }
    }

    private boolean boolVal(Object o, boolean defaultValue) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private static int indexOfIgnoreCase(String[] args, String needle) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(needle)) return i;
        }
        return -1;
    }

    private static String join(String[] args, int startInclusive, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (i > startInclusive) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private String findInventoryItemIdByName(String name, Player player, World world) {
        for (String id : player.getInventoryItemIds()) {
            var item = world.getItems().get(id);
            if (item != null && item.name() != null && item.name().equalsIgnoreCase(name)) return id;
        }
        return null;
    }

    private String findRoomTargetIdByName(String name, Room room, World world) {
        for (String id : room.getTargetIds()) {
            var t = world.getTargets().get(id);
            if (t != null && t.name() != null && t.name().equalsIgnoreCase(name)) return id;
        }
        return null;
    }
}