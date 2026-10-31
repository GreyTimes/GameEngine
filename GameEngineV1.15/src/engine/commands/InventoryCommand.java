package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        List<String> inv = player.getInventoryItemIds();

        if (inv == null || inv.isEmpty()) {
            GameOutput.println("Your inventory is empty.");
            return;
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String id : inv) {
            counts.put(id, counts.getOrDefault(id, 0) + 1);
        }

        GameOutput.println("You are carrying:");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String itemId = e.getKey();
            int count = e.getValue();

            Item item = world.getItems().get(itemId);
            String name = (item != null && item.name() != null) ? item.name() : itemId;

            GameOutput.print("- " + name);
            if (count > 1) GameOutput.print(" x" + count);
            GameOutput.println("");
        }
    }
}