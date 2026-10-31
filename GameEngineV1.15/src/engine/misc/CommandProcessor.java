package engine.misc;

import engine.commands.*;
import engine.npc.*;
import engine.world.Player;
import engine.world.World;
import engine.world.Item;
import util.GameOutput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandProcessor {
    private final Map<String, Command> commands = new HashMap<>();

    public void register(String name, Command command) {
        commands.put(name, command);
    }

    public void process(String input, Player player, World world) {
        String[] parts = input.split(" ");
        String commandName = parts[0];

        if (handleTradeInteraction(input, player, world)) {
            return;
        }

        Command command = commands.get(commandName);
        if (command == null) {
            GameOutput.println("Unknown command: '" + commandName + "'");
            return;
        }

        if (world.isInCombat() // CHANGED (block updated list below)
                && !(command instanceof HitCommand)
                && !(command instanceof ThrowCommand)
                && !(command instanceof InventoryCommand)
                && !(command instanceof PlayerStateCommand) // CHANGED
                && !(command instanceof UseCommand)         // CHANGED
                && !(command instanceof EquipCommand)       // CHANGED
                && !(command instanceof UnequipCommand)     // CHANGED
                && !(command instanceof FleeCommand)
                && !(command instanceof ExitCommand)) {
            GameOutput.println("You are in combat. Available: hit, throw, flee, inventory, playerstate, use, equip, unequip."); // CHANGED
            return;
        }

        command.execute(parts, player, world);
    }

    public boolean isCommandRegistered(String commandAlias) {
        return commands.containsKey(commandAlias);
    }

    private boolean handleTradeInteraction(String input, Player player, World world) {
        if (world == null) return false;

        int stage = world.getInteractionStage();
        if (stage == World.INTERACT_NONE) return false;

        String typed = (input == null) ? "" : input.trim();
        String normalized = typed.toLowerCase();

        // CHANGED: let "talk/interact/speak" restart dialogue instead of being swallowed
        if (normalized.equals("talk") || normalized.equals("interact") || normalized.equals("speak")) { // CHANGED
            clearInteraction(world); // CHANGED
            return false;            // CHANGED
        }

        if (stage == World.INTERACT_DIALOGUE_WAIT) {
            if (normalized.equals("back")) {
                clearInteraction(world);
                GameOutput.println("You step away.");
                return true;
            }
            if (typed.isEmpty()) {
                List<DialogueOption> options = world.getPendingDialogueOptions();
                if (options == null || options.isEmpty()) {
                    clearInteraction(world);
                    GameOutput.println("The conversation ends.");
                    return true;
                }
                printDialogueMenu(options);
                world.setInteractionStage(World.INTERACT_DIALOGUE_CHOICE);
                return true;
            }
            GameOutput.println("Press Enter to see options, or type back.");
            return true;
        }

        if (stage == World.INTERACT_DIALOGUE_CHOICE) {
            if (normalized.equals("back")) {
                clearInteraction(world);
                GameOutput.println("You step away.");
                return true;
            }

            List<DialogueOption> options = world.getPendingDialogueOptions();
            if (options == null || options.isEmpty()) {
                clearInteraction(world);
                GameOutput.println("The conversation ends.");
                return true;
            }

            int index = parseIndex(normalized);
            if (index < 1 || index > options.size()) {
                GameOutput.println("Choose a valid option number or type back.");
                return true;
            }

            DialogueOption option = options.get(index - 1);
            GameOutput.println("You: " + option.playerLine());

            String npcResponse = option.npcResponse();
            if (npcResponse != null && !npcResponse.isBlank()) {
                GameOutput.println("> " + npcResponse);
                GameOutput.println("Press Enter to continue...");
            }

            world.setInteractionStage(World.INTERACT_DIALOGUE_WAIT);
            return true;
        }

        TraderNpc trader = getPendingTrader(world, player);
        if (trader == null) {
            clearInteraction(world);
            GameOutput.println("No trader is available right now.");
            return true;
        }

        if (stage == World.INTERACT_TRADER_CONFIRM) {
            if (normalized.equals("yes") || normalized.equals("y")) {
                world.setInteractionStage(World.INTERACT_TRADER_BUY);
                printTradeMenu(trader, world, player);
                return true;
            }
            if (normalized.equals("no") || normalized.equals("n") || normalized.equals("back")) {
                clearInteraction(world);
                GameOutput.println("Maybe next time.");
                return true;
            }
            GameOutput.println("Please type yes or no.");
            return true;
        }

        if (stage == World.INTERACT_TRADER_BUY) {
            if (normalized.equals("back") || normalized.equals("no") || normalized.equals("n")) {
                clearInteraction(world);
                GameOutput.println("You step away from the trader.");
                return true;
            }

            TradeOffer offer = findOffer(trader, world, normalized);
            if (offer == null) {
                GameOutput.println("That item is not for sale. Type a number, item name, or back.");
                return true;
            }

            Item item = world.getItems().get(offer.itemId());
            if (item == null) {
                GameOutput.println("That item is unavailable.");
                return true;
            }

            if (!player.canCarryMore()) {
                GameOutput.println("You can't carry any more.");
                return true;
            }

            if (!player.spendMoney(offer.price())) {
                GameOutput.println("You don't have enough " + world.getCurrencyName() + ".");
                return true;
            }

            player.addToInventory(offer.itemId());
            GameOutput.println("You bought " + item.name() + " for " + offer.price() + " " + world.getCurrencyName() + ".");
            return true;
        }

        return false;
    }

    private int parseIndex(String normalized) {
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private TraderNpc getPendingTrader(World world, Player player) {
        String pendingId = world.getPendingTraderId();
        if (pendingId == null || pendingId.isBlank()) return null;

        Npc npc = world.getNpcs().get(pendingId);
        if (!(npc instanceof TraderNpc trader)) return null;

        String roomId = (player.getCurrentRoom() == null) ? null : player.getCurrentRoom().getId();
        if (roomId == null || !roomId.equals(trader.getRoomId())) return null;

        return trader;
    }

    private void printTradeMenu(TraderNpc trader, World world, Player player) {
        List<TradeOffer> offers = trader.getInventory();
        if (offers == null || offers.isEmpty()) {
            GameOutput.println(trader.getName() + ": Nothing for sale right now.");
            clearInteraction(world);
            return;
        }

        GameOutput.println("Items for sale (" + world.getCurrencyName() + "):" );
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer o = offers.get(i);
            Item item = world.getItems().get(o.itemId());
            String name = (item != null && item.name() != null) ? item.name() : o.itemId();
            GameOutput.println((i + 1) + ") " + name + " - " + o.price() + " " + world.getCurrencyName());
        }
        GameOutput.println("You have: " + player.getMoney() + " " + world.getCurrencyName());
        GameOutput.println("Type the number, item name, or back.");
    }

    private void printDialogueMenu(List<DialogueOption> options) {
        final int width = 64;
        GameOutput.println("┌" + "─".repeat(width) + "┐");
        GameOutput.println(paddedLine(" Dialogue Options", width));
        GameOutput.println("├" + "─".repeat(width) + "┤");

        for (int i = 0; i < options.size(); i++) {
            String line = " " + (i + 1) + ") " + options.get(i).playerLine();
            GameOutput.println(paddedLine(line, width));
        }

        GameOutput.println(paddedLine(" Type number or Back", width));
        GameOutput.println("└" + "─".repeat(width) + "┘");
    }

    private String paddedLine(String text, int width) {
        if (text == null) text = "";
        if (text.length() > width) text = text.substring(0, width);
        return "│" + text + " ".repeat(width - text.length()) + "│";
    }

    private TradeOffer findOffer(TraderNpc trader, World world, String normalized) {
        if (trader == null || normalized == null) return null;

        List<TradeOffer> offers = trader.getInventory();
        if (offers == null || offers.isEmpty()) return null;

        try {
            int index = Integer.parseInt(normalized);
            if (index >= 1 && index <= offers.size()) {
                return offers.get(index - 1);
            }
        } catch (NumberFormatException ignored) {
        }

        for (TradeOffer o : offers) {
            Item item = world.getItems().get(o.itemId());
            if (item != null && item.name() != null && item.name().equalsIgnoreCase(normalized)) {
                return o;
            }
        }

        return null;
    }

    private void clearInteraction(World world) {
        world.setInteractionStage(World.INTERACT_NONE);
        world.setPendingTraderId(null);
        world.setPendingDialogueNpcId(null);
        world.setPendingDialogueOptions(null);
    }
}