package engine.misc;

import engine.world.Player;
import engine.world.World;

public interface Command {
    void execute(String[] args, Player player, World world);
}