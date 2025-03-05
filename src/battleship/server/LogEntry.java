package battleship.server;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private int term; // The term in which the log was created
    private int playerIndex;
    private String command; // The actual command (e.g., "Attack")
    private int posX; // X term of command
    private int posY; // Y term of command

    public LogEntry(int term, int playerIndex, String command, int posX, int posY) {
        this.term = term;
        this.playerIndex = playerIndex;
        this.command = command;
        this.posX = posX;
        this.posY = posY;
    }

    public int getTerm() {
        return term;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public String getCommand() {
        return command;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "term=" + term +
                "index="+ playerIndex +
                ", command='" + command + '\'' +
                ", position='"+ posX+","+posY + '\'' +
                '}';
    }
}
