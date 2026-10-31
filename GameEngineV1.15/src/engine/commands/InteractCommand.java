package engine.commands;

import engine.misc.Command;
import engine.npc.*;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

import java.util.List;

public class InteractCommand implements Command {
    @Override
    public void execute(String[] args, Player player, World world) {
        List<Npc> npcs = world.getNpcsInRoom(player.getCurrentRoom().getId());
        if (npcs.isEmpty()) {
            GameOutput.println("No one to interact with here.");
            return;
        }

        Npc target = null;

        if (args.length > 1) {
            String typed = joinFrom(args, 1).toLowerCase();
            for (Npc n : npcs) {
                if (n.getName().equalsIgnoreCase(typed)) {
                    target = n;
                    break;
                }
            }
            if (target == null) {
                GameOutput.println("No NPC named \"" + typed + "\" here.");
                return;
            }
        } else {
            if (npcs.size() == 1) {
                target = npcs.get(0);
            } else {
                GameOutput.print("You see: ");
                for (int i = 0; i < npcs.size(); i++) {
                    if (i > 0) GameOutput.print(", ");
                    GameOutput.print(npcs.get(i).getName());
                }
                GameOutput.println(".");
                GameOutput.println("Type: interact <name>");
                return;
            }
        }

        if (target instanceof DialogueNpc d) {
            List<String> lines = d.getLines();
            List<DialogueNode> dialogues = d.getDialogues();

            // CHANGED: always show NPC lines first (all of them)
            if (lines != null && !lines.isEmpty()) {
                GameOutput.println("[" + target.getName() + "]"); // CHANGED
                for (String line : lines) {                       // CHANGED
                    GameOutput.println("> " + line);              // CHANGED
                }
                GameOutput.println("Press Enter to continue..."); // CHANGED
            } else if (dialogues != null && !dialogues.isEmpty()) {
                DialogueNode node = dialogues.get(world.getRng().nextInt(dialogues.size()));
                GameOutput.println("[" + target.getName() + "]"); // CHANGED
                GameOutput.println("> " + node.npcLine());        // CHANGED
                GameOutput.println("Press Enter to continue..."); // CHANGED
            } else {
                GameOutput.println(target.getName() + " has nothing to say.");
                return;
            }

            // CHANGED: keep dialogue options ready (if any)
            if (dialogues != null && !dialogues.isEmpty()) {       // CHANGED
                DialogueNode node = dialogues.get(0);              // CHANGED (simple + stable)
                List<DialogueOption> options = node.options();     // CHANGED
                if (options != null && !options.isEmpty()) {
                    world.setPendingDialogueNpcId(target.getId());
                    world.setPendingDialogueOptions(options);
                    world.setInteractionStage(World.INTERACT_DIALOGUE_WAIT);
                }
            }
            return;
        }

        if (target instanceof TraderNpc t) {
            String greet = "Welcome, wanna buy?";
            List<String> greetings = t.getGreetings();
            if (greetings != null && !greetings.isEmpty()) {
                String extra = greetings.get(world.getRng().nextInt(greetings.size()));
                GameOutput.println(target.getName() + ": " + extra);
            }
            GameOutput.println(target.getName() + ": " + greet);
            GameOutput.println("Type yes/no.");

            world.setPendingTraderId(t.getId());
            world.setInteractionStage(World.INTERACT_TRADER_CONFIRM);
        }
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