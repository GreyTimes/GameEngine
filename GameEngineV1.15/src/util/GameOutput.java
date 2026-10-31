package util;

import engine.misc.Game;

public class GameOutput {
    private static Game currentGame;

    public static void setCurrentGame(Game game) {
        currentGame = game;
    }

    public static void println(String msg) {
        if (shouldPrint()) System.out.println(msg);
    }

    public static void print(String msg) {
        if (shouldPrint()) System.out.print(msg);
    }

    private static boolean shouldPrint() {
        return currentGame == null || !currentGame.isReplaying();
    }
}