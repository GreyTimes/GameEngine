package engine.commands;

import engine.misc.Command;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

import java.util.List;

public class HelpCommand implements Command {
    @Override
    public void execute(String[] args, Player player, World world) {
        List<String> lines = world.getHelpLines();

        if (lines == null || lines.isEmpty()) {
            GameOutput.println("No help text configured.");
            return;
        }

        for (String line : lines) {
            GameOutput.println(line);
        }
    }
}