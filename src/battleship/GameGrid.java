package battleship;

import java.io.Serializable;

/**
 * Represents a game grid for battleship-style gameplay.
 * The grid has a fixed size and supports placing ships, attacking, and checking
 * game status.
 */
public class GameGrid implements Serializable {
    private final int SIZE = 6;
    private final char[][] grid;

    /**
     * Initializes a new game grid with water ('~') in all cells.
     */
    public GameGrid() {
        grid = new char[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = '~'; // Water
            }
        }
    }

    /**
     * Places a ship at the specified coordinates.
     * 
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return A message indicating whether the placement was successful.
     */
    public String placeShip(int x, int y) {
        if (grid[x][y] == '~') {
            grid[x][y] = 'S'; // Ship
            return "Placed";
        }
        return "Space already occupied!";
    }

    /**
     * Attacks a specified coordinate.
     * 
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return A message indicating whether the attack was a hit or a miss.
     */
    public String attack(int x, int y) {
        if (grid[x][y] == 'S') {
            grid[x][y] = 'X'; // Hit
            return "Hit!";
        } else if (grid[x][y] == 'X' || grid[x][y] == 'O') {
            return "Already hitted or missed!";
        } else {
            grid[x][y] = 'O'; // Miss
            return "Miss!";
        }
    }

    /**
     * Returns a fogged version of the grid, hiding ship locations.
     * 
     * @return A new GameGrid with only hits and misses visible.
     */
    public GameGrid getFoggedGrid() {
        GameGrid foggedGrid = new GameGrid();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == 'X' || grid[i][j] == 'O') {
                    foggedGrid.grid[i][j] = grid[i][j]; // Show hits/misses
                } else {
                    foggedGrid.grid[i][j] = '~'; // Hide ships
                }
            }
        }
        return foggedGrid;
    }

    /**
     * Checks if all ships on the grid have been sunk.
     * 
     * @return true if all ships are sunk, false otherwise.
     */
    public boolean isAllShipsSunk() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == 'S') {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Displays the current state of the grid in the console.
     */
    public void displayGrid() {
        System.out.println("   0 1 2 3 4 5");
        for (int i = 0; i < grid.length; i++) {
            System.out.print(i + " ");
            for (char c : grid[i]) {
                System.out.print(" " + c);
            }
            System.out.println();
        }
    }

    /**
     * Retrieves the grid as a 2D character array.
     * 
     * @return The grid.
     */
    public char[][] getGrid() {
        return grid;
    }
}