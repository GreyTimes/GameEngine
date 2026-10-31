package engine.commands;

import engine.misc.Command;
import engine.world.Item;
import engine.world.Player;
import engine.world.Room;
import engine.world.World;
import util.GameOutput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LookCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        Room room = player.getCurrentRoom();
        List<String> itemIds = room.getItemIds();

        boolean hadItem = false;

        if (itemIds != null && !itemIds.isEmpty()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String id : itemIds) {
                counts.put(id, counts.getOrDefault(id, 0) + 1);
            }

            GameOutput.print("You see: ");
            int i = 0;
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                String itemId = e.getKey();
                int count = e.getValue();

                Item item = world.getItems().get(itemId);
                String name = (item != null && item.name() != null) ? item.name() : itemId;

                if (i++ > 0) GameOutput.print(", ");
                GameOutput.print(name);
                if (count > 1) GameOutput.print(" x" + count);
            }
            GameOutput.println("");
            hadItem = true;
        }

        int money = room.getCurrencyAmount();
        if (money > 0) {
            GameOutput.println("You see " + money + " " + world.getCurrencyName() + " on the floor.");
            return;
        }

        if (!hadItem) {
            GameOutput.println("You see nothing useful here.");
        }
    }
}