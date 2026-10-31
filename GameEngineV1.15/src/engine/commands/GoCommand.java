package engine.commands;

import engine.misc.Command;
import engine.combat.Monster;
import engine.npc.Npc;
import engine.world.Player;
import engine.world.Room;
import engine.world.World;
import util.GameOutput;

import java.util.List;

public class GoCommand implements Command {

    @Override
    public void execute(String[] args, Player player, World world) {
        if (args.length < 2) {
            GameOutput.println("Go where? (Usage: go <direction>)");
            return;
        }

        if (world.isInCombat()){
            GameOutput.println("You cannot move while locked in combat! Use 'hit'.");
            return;
        }

        String direction = args[1].toLowerCase();
        Room currentRoom = player.getCurrentRoom();
        String nextRoomId = currentRoom.getExit(direction);

        if (nextRoomId == null) {
            GameOutput.println("You can't go " + direction + ".");
            return;
        }

        Room nextRoom = world.getRooms().get(nextRoomId);

        if (nextRoom == null) {
            GameOutput.println("That exit leads nowhere (missing room: " + nextRoomId + ").");
            return;
        }

        player.setPreviousRoom(currentRoom);
        player.setCurrentRoom(nextRoom);

        GameOutput.println(nextRoom.getDescription());

        List<Npc> npcsHere = world.getNpcsInRoom(nextRoom.getId());
        if (npcsHere != null && !npcsHere.isEmpty()) {
            for (Npc npc : npcsHere) {
                String type = (npc.getType() == null || npc.getType().isBlank()) ? "npc" : npc.getType();
                GameOutput.println("You see a " + type + " here: " + npc.getName() + ".");
            }
            GameOutput.println("Type 'interact' to talk.");
        }

        world.setActiveMonsterId(null);
        world.setInCombat(false);

        for (String monsterId : nextRoom.getMonsterIds()) {
            Monster m = world.getMonsters().get(monsterId);
            if (m != null && m.isAlive()) {
                world.setActiveMonsterId(monsterId);
                world.setInCombat(true);

                GameOutput.println("A " + m.getName() + " appears!");
                GameOutput.println("Combat starts now. Use 'hit' or 'flee'.");
                break;
            }
        }

        world.advanceTurn(player);
    }
}