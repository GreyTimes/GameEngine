package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.world.Player;
import engine.world.Room;
import engine.world.World;
import util.GameOutput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DropCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        if (args.length < 2) {
            GameOutput.println("Drop what? (Usage: drop <item> | drop all)");
            return;
        }

        String typed = joinFrom(args, 1).trim();

        Room room = player.getCurrentRoom();

        if (typed.equalsIgnoreCase("all") || typed.equalsIgnoreCase("everything")) {
            dropAll(room, player, world);
            return;
        }

        String typedName = typed.toLowerCase();
        String foundItemId = findInventoryItemIdByName(player.getInventoryItemIds(), world.getItems(), typedName);

        if (foundItemId == null) {
            GameOutput.println("You don't have \"" + typedName + "\".");
            return;
        }

        if (foundItemId.equals(player.getEquippedWeaponId())) {
            player.setEquippedWeaponId(null);
        }
        player.getInventoryItemIds().remove(foundItemId);
        room.addItemId(foundItemId);

        Item item = world.getItems().get(foundItemId);
        String name = (item != null && item.name() != null) ? item.name() : foundItemId;
        GameOutput.println("You drop the " + name + ".");
    }

    private static void dropAll(Room room, Player player, World world) {
        List<String> inv = player.getInventoryItemIds();
        if (inv == null || inv.isEmpty()) {
            GameOutput.println("You have nothing to drop.");
            return;
        }

        List<String> toDrop = new ArrayList<>(inv);
        inv.clear();

        for (String id : toDrop) {
            room.addItemId(id);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : toDrop) counts.put(id, counts.getOrDefault(id, 0) + 1);

        GameOutput.print("You drop: ");
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Item item = world.getItems().get(e.getKey());
            String name = (item != null && item.name() != null) ? item.name() : e.getKey();

            if (i++ > 0) GameOutput.print(", ");
            GameOutput.print(name);
            if (e.getValue() > 1) GameOutput.print(" x" + e.getValue());
        }
        GameOutput.println(".");
    }

    private static String findInventoryItemIdByName(List<String> invIds, Map<String, Item> itemsById, String typedName) {
        for (String itemId : invIds) {
            Item item = itemsById.get(itemId);
            if (item != null && item.name() != null && item.name().equalsIgnoreCase(typedName)) {
                return itemId;
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