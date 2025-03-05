package battleship.server;

import battleship.GameGrid;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftServer extends UnicastRemoteObject implements RaftServerInterface {
    private final Registry reg; // RMI register
    private final Log log; // Log
    private final int index; // Server ID
    private int currentTerm; // Term number
    private int votedFor;
    private boolean isLeader;
    private boolean isFollower;
    private String[] boundNames; // Registered servers RMI
    private Timer electionTimer;
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private final int[] playersId = { -1, -1 };
    private final int[] shipsPlaced = { 0, 0 };
    private GameGrid player1Grid;
    private GameGrid player2Grid;
    private int currentTurn;
    private boolean isMatchFinished;

    // Constructor
    public RaftServer(int index) throws RemoteException {
        super();
        this.reg = LocateRegistry.getRegistry("localhost", 1099);
        this.log = new Log();
        this.index = index;
        this.currentTerm = 0;
        this.votedFor = -1;
        this.isLeader = false;
        this.isFollower = true;
        resetElectionTimer();
    }

    /*
     * Leader election methods
     */

    /**
     * Follower times out and starts an election.
     * Increments the term, votes for itself, and requests votes from other servers.
     */
    public void becomeCandidate() {
        if (!electionInProgress.getAndSet(true)) {
            currentTerm++; // Increase term
            votedFor = this.hashCode(); // Vote for self
            isFollower = false;

            int votes = 1; // Candidate votes for itself

            try {
                boundNames = reg.list(); // Get all registered servers
                String thisServerName = "RaftServer" + index; // Current server name

                for (String boundName : boundNames) {
                    if (!boundName.equals(thisServerName)) { // Avoid voting for itself
                        try {
                            RaftServerInterface follower = (RaftServerInterface) Naming
                                    .lookup("//localhost/" + boundName);
                            boolean voteGranted = follower
                                    .requestVote(new RequestVoteRequest(currentTerm, this.hashCode()));
                            if (voteGranted) {
                                votes++;
                            }
                        } catch (NotBoundException | MalformedURLException e) {
                            System.err.println("Failed to connect to " + boundName);
                        }
                    }
                }

                // Check if received majority votes
                if (votes > (boundNames.length / 2)) {
                    becomeLeader();
                } else {
                    becomeFollower(); // If election fails, return to follower
                }
            } catch (ConnectException ex) {
                System.out.println("Need to reconnect...");
                becomeFollower(); // Become a follower after reconnecting
            } catch (RemoteException e) {
                System.err.println("Error in candidate procedure");
                e.printStackTrace();
            }
        }
    }

    /**
     * Transitions the node to leader state and starts sending heartbeats.
     */
    @Override
    public void becomeLeader() {
        isFollower = false;
        isLeader = true;

        // Create new GameGrid (follower servers do not store grids)
        player1Grid = new GameGrid();
        player2Grid = new GameGrid();

        if (log.get(0) != null) { // If match is already started, rebuild grids based on logs
            for (int i = 0; i < log.size(); i++) {
                processLogEntry(log.get(i));
            }

            /*
             * If the last log entry corresponds to player 0 and not still placing ships,
             * set the current turn to player 1 viceversa not needed because currentTurn is
             * initialized to 0 in constructor
             */
            if ((log.getLastElem().getPlayerIndex()) == 0 && !log.getLastElem().getCommand().equals("place")) {
                currentTurn = 1;
            }
        }

        System.out.println("Node became leader for term " + currentTerm);
        startHeartbeat();
    }

    /**
     * Processes a single log entry and updates the corresponding game grid.
     * 
     * @param entry The log entry to process.
     */
    private void processLogEntry(LogEntry entry) {
        GameGrid targetGrid;

        if ("attack".equals(entry.getCommand())) {
            targetGrid = (entry.getPlayerIndex() == 0) ? player2Grid : player1Grid;
            targetGrid.attack(entry.getPosX(), entry.getPosY());
        } else {
            targetGrid = (entry.getPlayerIndex() == 0) ? player1Grid : player2Grid;
            String result = targetGrid.placeShip(entry.getPosX(), entry.getPosY());

            if (result.equals("Placed")) {
                shipsPlaced[entry.getPlayerIndex()]++;
            }
        }
    }

    /**
     * Starts the heartbeat mechanism where the leader sends periodic heartbeats to
     * followers.
     */
    private void startHeartbeat() {
        Timer heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isLeader) {
                    try {
                        boundNames = reg.list(); // Get all registered servers
                        for (String name : boundNames) {
                            if (!name.equals("RaftServer" + index)) { // Avoid sending to self
                                try {
                                    RaftServerInterface follower = (RaftServerInterface) Naming
                                            .lookup("//localhost/" + name);
                                    follower.receiveHeartbeat(currentTerm);
                                } catch (NotBoundException | RemoteException | MalformedURLException e) {
                                    System.err.println("Failed to send heartbeat to: " + name);
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        System.err.println("Error accessing RMI registry.");
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 150); // Heartbeat interval: 150ms
    }

    /**
     * Checks if this node is the leader.
     * 
     * @return true if the node is the leader, false otherwise.
     */
    @Override
    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Transitions the node to a follower state.
     */
    @Override
    public void becomeFollower() {
        System.out.println("Node became follower for term " + currentTerm);
        isLeader = false;
        isFollower = true;
        votedFor = -1; // Reset vote
        electionInProgress.set(false);
        resetElectionTimer();
    }

    /**
     * Resets the election timer, ensuring the follower waits for the leader's
     * heartbeat.
     */
    private void resetElectionTimer() {
        if (electionTimer != null) {
            electionTimer.cancel();
        }
        electionTimer = new Timer();
        electionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isFollower) {
                    becomeCandidate(); // Start election if heartbeat is missing
                }
            }
        }, 300 + new Random().nextInt(200)); // Random timeout: 300-500ms
    }

    /**
     * Handles receiving a heartbeat and resets the election timer.
     * 
     * @param currentTerm The term of the leader sending the heartbeat.
     */
    @Override
    public void receiveHeartbeat(int currentTerm) throws RemoteException {
        if (this.currentTerm < currentTerm) {
            this.currentTerm = currentTerm;
        }
        resetElectionTimer();
    }

    /**
     * Handles a vote request from another node.
     * 
     * @param request The vote request containing the candidate's information.
     * @return true if the vote is granted, false otherwise.
     */
    @Override
    public boolean requestVote(RequestVoteRequest request) throws RemoteException {
        if (request.getTerm() < currentTerm) {
            return false; // Reject outdated candidates
        }

        if (request.getTerm() > currentTerm) {
            currentTerm = request.getTerm();
            votedFor = -1; // Reset votes
            becomeFollower();
        }

        if (votedFor == -1) {
            votedFor = request.getCandidateId(); // Vote for the candidate
            return true;
        }

        return false;
    }

    /**
     * Leader receives a log entry, stores it, and propagates it to followers.
     * 
     * @param entry The log entry to be appended.
     * @return true if the entry is successfully replicated to the majority, false
     *         otherwise.
     * @throws RemoteException If there is an issue with remote communication.
     */
    @Override
    public boolean appendLogEntry(LogEntry entry) throws RemoteException {
        if (isLeader) {
            log.append(entry);
            System.out.println("Appended log!");

            int successCount = 1; // Leader itself counts as 1

            try {
                boundNames = reg.list(); // Get all registered servers
                for (String name : boundNames) {
                    if (!name.equals("RaftServer" + index)) { // Avoid sending to self
                        try {
                            RaftServerInterface follower = (RaftServerInterface) Naming
                                    .lookup("//localhost/" + name);
                            if (follower.receiveLogEntry(entry)) {
                                successCount++;
                            }
                        } catch (NotBoundException | RemoteException | MalformedURLException e) {
                            System.err.println("Failed to send log entry to: " + name);
                        }
                    }
                }
            } catch (RemoteException e) {
                System.err.println("Error accessing RMI registry.");
                e.printStackTrace();
            }

            if (successCount >= (boundNames.length / 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Follower stores a log entry received from the leader.
     * 
     * @param entry The log entry to be stored.
     * @return true if successfully stored, false otherwise.
     * @throws RemoteException If there is an issue with remote communication.
     */
    @Override
    public boolean receiveLogEntry(LogEntry entry) throws RemoteException {
        if (isFollower) {
            log.append(entry);
            System.out.println("Follower " + index + " received log: " + entry.getCommand());
            return true;
        }
        return false;
    }

    /**
     * Game Logic Methods
     */

    /**
     * Initializes a match by assigning a player ID and setting up game grids.
     * 
     * @return The assigned player ID (0 or 1), or -1 if the match is full.
     */
    @Override
    public synchronized int initMatch() {
        int playerId = -1;
        if (playersId[0] == -1) {
            playersId[0] = playerId = 0;
            player1Grid = new GameGrid();
        } else if (playersId[1] == -1) {
            playersId[1] = playerId = 1;
            player2Grid = new GameGrid();
        }

        isMatchFinished = false;
        currentTurn = 0;

        return playerId;
    }

    /**
     * Checks if both players have joined the match.
     * 
     * @return true if both player slots are filled, false otherwise.
     */
    @Override
    public synchronized boolean arePlayersReady() {
        return playersId[0] != -1 && playersId[1] != -1;
    }

    /**
     * Processes a move from a player.
     * First check if command in not out of bound and if not
     * 
     * @param move The move command in format "command,x,y".
     * @param id   The ID of the player making the move.
     * @return A response message indicating the result of the move.
     */
    @Override
    public String processMove(String move, int id) {
        String response = "";
        String[] splitMove = move.split(",");
        String command = splitMove[0];
        int posX = Integer.parseInt(splitMove[1]);
        int posY = Integer.parseInt(splitMove[2]);

        if (posX >= 6 || posY >= 6) {
            response = "!! Out of bound, grid have max 5 row/cols !!";
            return response;
        }

        LogEntry logEntry = new LogEntry(currentTerm, id, command, posX, posY);
        try {
            boolean appendLog = appendLogEntry(logEntry);

            if (appendLog) {
                if (splitMove[0].equals("attack")) {
                    GameGrid grid = getOpponentGrid(id);
                    response = processAttack(grid, posX, posY);
                } else {
                    GameGrid grid = getPlayerGrid(id);
                    response = processPlace(grid, id, posX, posY);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Processes an attack move on the target grid.
     * 
     * @param targetGrid The grid where the ship to attack are placed.
     * @param posX       The X-coordinate of the attack.
     * @param posY       The Y-coordinate of the attack.
     * @return A response message indicating the attack result.
     */
    private String processAttack(GameGrid targetGrid, int posX, int posY) {
        String response = targetGrid.attack(posX, posY);

        if (targetGrid.isAllShipsSunk()) {
            response += " | All ships sunk! You win!";
            isMatchFinished = true;
            return response;
        }

        System.out.println(log.getLastElem().toString());
        nextTurn();
        return response;
    }

    /**
     * Processes a ship placement move.
     * 
     * @param targetGrid The grid where the ship is placed.
     * @param playerId   The ID of the player placing the ship.
     * @param posX       The X-coordinate for placement.
     * @param posY       The Y-coordinate for placement.
     * @return A response message indicating the placement result.
     */
    private String processPlace(GameGrid targetGrid, int playerId, int posX, int posY) {
        String response = targetGrid.placeShip(posX, posY);

        if (response.equals("Placed")) {
            shipsPlaced[playerId]++;
            if (shipsPlaced[playerId] == 5) {
                response += "|All ships placed!";
                nextTurn();
            }
        }
        System.out.println(log.getLastElem().toString());

        return response;
    }

    /**
     * Advances the turn to the next player.
     */
    private void nextTurn() {
        currentTurn = (currentTurn == 0) ? 1 : 0;
        System.out.println("Now it's " + currentTurn + "'s turn");
    }

    /**
     * Retrieves the game grid for a given player.
     * 
     * @param id The player's ID (0 or 1).
     * @return The player's game grid.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public GameGrid getPlayerGrid(int id) throws RemoteException {
        GameGrid targetGrid = (id == 0) ? player1Grid : player2Grid;
        return targetGrid;
    }

    /**
     * Retrieves the opponent's game grid.
     * 
     * @param id The player's ID (0 or 1).
     * @return The opponent's game grid.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public GameGrid getOpponentGrid(int id) throws RemoteException {
        GameGrid targetGrid = (id == 0) ? player2Grid : player1Grid;
        return targetGrid;
    }

    /**
     * Retrieves a fogged version of the opponent's game grid (hiding ship
     * locations).
     * 
     * @param id The player's ID (0 or 1).
     * @return The fogged opponent's game grid.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public GameGrid getFoggedOpponentGrid(int id) throws RemoteException {
        GameGrid targetGrid = (id == 0) ? player2Grid : player1Grid;
        return targetGrid.getFoggedGrid();
    }

    /**
     * Gets the current player's turn.
     * 
     * @return The ID of the player whose turn it is.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public synchronized int getCurrentTurn() throws RemoteException {
        return currentTurn;
    }

    /**
     * Retrieves the number of ships placed by a player.
     * 
     * @param id The player's ID (0 or 1).
     * @return The number of ships placed by the player.
     */
    @Override
    public synchronized int getNumShipsPlaced(int id) {
        return shipsPlaced[id];
    }

    /**
     * Checks if the match has finished.
     * 
     * @return true if the match is finished, false otherwise.
     */
    @Override
    public synchronized boolean isMatchFinished() {
        return isMatchFinished;
    }

    /**
     * Clean server log.
     */

    @Override
    public void cleanLog() {
        log.cleanup();
    }

    /**
     * Handles client disconnection by resetting player data, cleaning logs and
     * ending the match.
     * 
     * @param playerId The ID of the player who disconnected.
     */
    @Override
    public void clientDisconnection(int playerId) {
        shipsPlaced[playerId] = 0;

        if (playerId == 0) {
            playersId[0] = -1;
        } else {
            playersId[1] = -1;
        }

        try {
            boundNames = reg.list(); // Get all registered servers
            for (String name : boundNames) {
                RaftServerInterface server = (RaftServerInterface) Naming
                        .lookup("//localhost/" + name);
                server.cleanLog();
            }
        } catch (RemoteException | MalformedURLException | NotBoundException re) {
            System.err.println("Error accessing RMI registry.");
            re.printStackTrace();
        }

        isMatchFinished = true;
    }

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java RaftServerMain <serverIndex>");
                return;
            }

            int index = Integer.parseInt(args[0]);
            String serverName = "RaftServer" + index;

            // Create and register the server
            RaftServer server = new RaftServer(index);
            Naming.rebind("//localhost:1099/" + serverName, server);

            System.out.println(serverName + " is running...");

            /// Add shutdown hook for graceful exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down " + serverName + "...");
                try {
                    // Unbind the server from the registry
                    if (serverName != null) {
                        try {
                            Naming.unbind("//localhost/" + serverName);
                            System.out.println(serverName + " unbound from registry.");
                        } catch (Exception e) {
                            System.err.println("Could not unbind " + serverName + " (possibly already removed).");
                        }
                    }

                    // Unexport the remote object
                    if (server != null) {
                        UnicastRemoteObject.unexportObject(server, true);
                        System.out.println(serverName + " unexported.");
                    }

                } catch (Exception e) {
                    System.err.println("Error during cleanup: " + e.getMessage());
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}