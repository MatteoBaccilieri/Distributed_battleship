package battleship.server;

import battleship.GameGrid;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RaftServerInterface extends Remote {
    boolean appendLogEntry(LogEntry entry) throws RemoteException;

    boolean receiveLogEntry(LogEntry entry) throws RemoteException;

    void receiveHeartbeat(int currentTerm) throws RemoteException;

    boolean requestVote(RequestVoteRequest request) throws RemoteException;

    void becomeLeader() throws RemoteException;

    boolean isLeader() throws RemoteException;

    void becomeFollower() throws RemoteException;

    int initMatch() throws RemoteException;

    boolean arePlayersReady() throws RemoteException;

    GameGrid getPlayerGrid(int id) throws RemoteException;

    GameGrid getOpponentGrid(int id) throws RemoteException;

    GameGrid getFoggedOpponentGrid(int id) throws RemoteException;

    String processMove(String move, int id) throws RemoteException;

    int getCurrentTurn() throws RemoteException;

    int getNumShipsPlaced(int id) throws RemoteException;

    boolean isMatchFinished()throws RemoteException;

    void cleanLog() throws RemoteException;

    void clientDisconnection(int id) throws RemoteException;
}
