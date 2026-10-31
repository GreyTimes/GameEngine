package engine.commands;

import engine.misc.Command;
import engine.world.Player;
import engine.world.World;
import util.GameOutput;

public class ExitCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world){
        world.setExitConfirmationPending(true);
        GameOutput.println("Are you sure you want to exit?");
        GameOutput.println("Make sure you saved your game first.");
        GameOutput.println("Type yes/no.");
    }
}
