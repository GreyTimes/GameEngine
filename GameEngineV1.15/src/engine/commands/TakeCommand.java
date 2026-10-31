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

public class TakeCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        if (args.length < 2) {
            GameOutput.println("Take what? (Usage: take <item> | take all)");
            return;
        }

        String typed = joinFrom(args, 1).trim();

        Room room = player.getCurrentRoom();

        if (typed.equalsIgnoreCase("all") || typed.equalsIgnoreCase("everything")) {
            takeAll(room, player, world);
            return;
        }

        if (isCurrencyName(typed, world)) {
            takeCurrency(room, player, world);
            return;
        }

        String typedName = typed.toLowerCase();
        String foundItemId = findRoomItemIdByName(room, world.getItems(), typedName);

        if (foundItemId == null) {
            GameOutput.println("There is no \"" + typedName + "\" here.");
            return;
        }

        if (!player.canCarryMore()) {
            GameOutput.println("You can't carry any more.");
            return;
        }

        room.removeItemId(foundItemId);
        player.addToInventory(foundItemId);

        Item item = world.getItems().get(foundItemId);
        String takenName = (item != null && item.name() != null) ? item.name() : foundItemId;
        GameOutput.println("You take the " + takenName + ".");
    }

    private static void takeCurrency(Room room, Player player, World world) {
        int amt = room.takeAllCurrency();
        if (amt <= 0) {
            GameOutput.println("There is no " + world.getCurrencyName() + " here.");
            return;
        }
        player.addMoney(amt);
        GameOutput.println("You pick up " + amt + " " + world.getCurrencyName() + ".");
    }

    private static void takeAll(Room room, Player player, World world) {
        // CHANGED: take gold too
        int goldTaken = room.takeAllCurrency(); // CHANGED
        if (goldTaken > 0) {                    // CHANGED
            player.addMoney(goldTaken);         // CHANGED
            GameOutput.println("You pick up " + goldTaken + " " + world.getCurrencyName() + "."); // CHANGED
        }

        List<String> ids = room.getItemIds();
        if (ids == null || ids.isEmpty()) {
            if (goldTaken == 0) { // CHANGED
                GameOutput.println("There is nothing to take.");
            }
            return;
        }

        List<String> toTake = new ArrayList<>(ids);
        List<String> taken = new ArrayList<>();
        List<String> leftInRoom = new ArrayList<>();

        for (String id : toTake) {
            if (player.canCarryMore()) {
                player.addToInventory(id);
                taken.add(id);
            } else {
                leftInRoom.add(id);
            }
        }

        // Set room contents to the leftovers (don’t clear unconditionally anymore)
        ids.clear();
        ids.addAll(leftInRoom);

        if (taken.isEmpty()) {
            if (goldTaken == 0) { // CHANGED
                GameOutput.println("You can't carry any more.");
            }
            return;
        }

        // Print summary with counts (for what was actually taken)
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : taken) counts.put(id, counts.getOrDefault(id, 0) + 1);

        GameOutput.print("You take: ");
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Item item = world.getItems().get(e.getKey());
            String name = (item != null && item.name() != null) ? item.name() : e.getKey();

            if (i++ > 0) GameOutput.print(", ");
            GameOutput.print(name);
            if (e.getValue() > 1) GameOutput.print(" x" + e.getValue());
        }
        GameOutput.println(".");

        if (!leftInRoom.isEmpty()) {
            GameOutput.println("Your inventory is full. Some items remain in the room.");
        }
    }

    private static String findRoomItemIdByName(Room room, Map<String, Item> itemsById, String typedName) {
        for (String itemId : room.getItemIds()) {
            Item item = itemsById.get(itemId);
            if (item != null && item.name() != null && item.name().equalsIgnoreCase(typedName)) {
                return itemId;
            }
        }
        return null;
    }

    private static boolean isCurrencyName(String typed, World world) {
        if (typed == null) return false;
        return typed.equalsIgnoreCase(world.getCurrencyName());
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