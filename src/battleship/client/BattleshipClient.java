package battleship.client;

import battleship.server.RaftServerInterface;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class BattleshipClient {

    /**
     * Connects to the Raft server and returns the leader server instance.
     *
     * @return the leader server if successfully connected, null otherwise.
     */
    private static RaftServerInterface connect() {
        RaftServerInterface server = null;
        try {
            // Get the registry from the server
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            String[] serverNames = registry.list();
            // Try to find the leader server
            for (String serverName : serverNames) {
                server = (RaftServerInterface) Naming.lookup("//localhost/" + serverName);
                if (server.isLeader()) {
                    return server; // Return the leader server
                }
            }
        } catch (ConnectException ex) {
            System.err.println("Sorry, servers are down!");
        } catch (RemoteException | MalformedURLException | NotBoundException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
        return server; // Return null if no leader server is found
    }

    /**
     * Waits for the player's turn by checking the server's current turn.
     *
     * @param server   the Raft server interface to communicate with.
     * @param playerId the ID of the current player.
     * @throws RemoteException      if a remote error occurs during communication
     *                              with the server.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    private static void waitYourTurn(RaftServerInterface server, int playerId)
            throws InterruptedException {
        while (true) {
            try {
                if (server.getCurrentTurn() != playerId) {
                    // Inform the player it's not their turn yet
                    System.out.println("Other player's turn, please wait...");

                    // Search if other player is exit
                    if (server.isMatchFinished()) {
                        handleDisconnection(server, playerId);
                        System.exit(0);
                    } else {
                        // Sleep for 5 seconds before checking again
                        Thread.sleep(5000);
                    }
                } else {
                    break;
                }
            } catch (RemoteException e) {
                server = connect();

                if (server == null) {
                    System.out.println("Servers are down...sorry!");
                    break;
                }
            }
        }
    }

    /**
     * Handles the disconnection of a player from the server.
     *
     * @param leader      the leader server interface.
     * @param playerIndex the index of the player to disconnect.
     * @throws RemoteException if a remote error occurs during communication with
     *                         the server.
     */
    private static void handleDisconnection(RaftServerInterface leader, int playerIndex) throws RemoteException {
        leader.clientDisconnection(playerIndex); // Notify server of disconnection
        System.out.println("Match terminated, goodbye!");
    }

    /**
     * Attempts to find a new leader server if the current one becomes unavailable.
     *
     * @param leaderServer the current leader server (used for finding a new
     *                     leader).
     */
    private static RaftServerInterface searchNewLeader() {
        System.out.println("**********************************************");
        System.out.println("* Repeat the instruction, previous not taken! *");
        System.out.println("**********************************************");
        return connect(); // Reconnect to the server
    }

    public static void main(String[] args) {
        RaftServerInterface leader = connect(); // Connect to the leader server
        if (leader == null) {
            System.out.println("Servers are down...sorry!");
            return;
        }

        int playerIndex = -1;
        try {
            // Notify the server that the player is ready for the match
            playerIndex = leader.initMatch();
        } catch (RemoteException re) {
            System.out.println("Impossible to start match...sorry!");
            return;
        }

        System.out.println("Waiting for another player to join...");

        // Wait for the second player to join the game
        while (true) {
            try {
                // Wait until both players are ready
                if (!leader.arePlayersReady()) {
                    Thread.sleep(3000);
                } else {
                    break;
                }
            } catch (RemoteException re) {
                // Reconnect if there's a RemoteException
                leader = connect();
            } catch (InterruptedException ie) {
                System.err.println("Match terminated!");
                return;
            }
        }

        Scanner scanner = new Scanner(System.in); // Scanner for user input
        while (true) {
            try {
                // Wait for the player's turn
                waitYourTurn(leader, playerIndex);

                if (leader.getNumShipsPlaced(playerIndex) < 5) {
                    leader.getPlayerGrid(playerIndex).displayGrid();
                    System.out.print("Enter position where want to place ship (place,x,y)or 'exit':");

                    String placeInstruction = scanner.nextLine(); // Get user input
                    String placeResponce;

                    if (placeInstruction.equalsIgnoreCase("exit")) {
                        // Handle exit condition by disconnecting the player
                        handleDisconnection(leader, playerIndex);
                        System.exit(0);
                        break;
                    } else if (!placeInstruction.matches("^(place),\\d+,\\d+$")) {
                        // Validate the format of the ship placement command
                        placeResponce = "Invalid move format! Use 'place,x,y'. Try again!";
                    } else {
                        // Process the move on the server
                        // long startTime = System.nanoTime();

                        placeResponce = leader.processMove(placeInstruction, playerIndex);

                        // long endTime = System.nanoTime();
                        // long latency = (endTime - startTime) / 1000000;
                        // System.out.println("Round-trip latency: " + latency + " ms");
                    }

                    System.out.println(placeResponce); // Display the response
                } else {
                    break; // End the ship placement phase after placing 5 ships
                }
            } catch (RemoteException e) {
                // If the server goes down, try to find a new leader
                leader = searchNewLeader();

                if (leader == null) {
                    System.out.println("Servers are down...sorry!");
                    break;
                }
            } catch (InterruptedException ie) {
                System.err.println("Match terminated!");
                break;
            }
        }

        // The phase where players attack each other's ships
        while (true) {
            try {
                waitYourTurn(leader, playerIndex);

                if (leader.isMatchFinished()) {
                    handleDisconnection(leader, playerIndex);
                    break;
                } else {
                    // Display the opponent's grid with fog
                    System.err.println("Opponent grid:");
                    leader.getFoggedOpponentGrid(playerIndex).displayGrid();
                    System.out.print("Your turn! Enter move (attack,x,y) or 'exit': ");
                }

                String move = scanner.nextLine();

                String response;

                if (move.equalsIgnoreCase("exit")) {
                    leader.clientDisconnection(playerIndex);
                    System.out.println("GoodBye!");
                    break;
                } else if (!move.matches("^(attack),\\d+,\\d+$")) {
                    response = "Invalid move format! Use 'attack,x,y'. Try again!";
                } else {
                    // long startTime = System.nanoTime();

                    response = leader.processMove(move, playerIndex);

                    /*
                     * long endTime = System.nanoTime();
                     * long latency = (endTime - startTime) / 1000000;
                     * System.out.println("Round-trip latency: " + latency + " ms");
                     */
                }

                System.out.println("Response: " + response); // Display the server's response

            } catch (RemoteException ex) {
                leader = searchNewLeader();
                if (leader == null) {
                    System.out.println("Servers are down...sorry!");
                    break;
                }
            } catch (InterruptedException ie) {
                System.err.println("Match terminated!");
                break;
            }
        }
        scanner.close();
    }
}