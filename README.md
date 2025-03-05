# Battleship with Distributed Server

## 📌 Project Overview

This project implements a **distributed multiplayer Battleship game** using the **Raft consensus algorithm** for fault tolerance and synchronization. The system consists of a distributed server architecture where multiple game servers coordinate to maintain consistency and handle player interactions.

## 🏗️ System Architecture

- **Client-Server Model**: Players interact with the game through a client, which communicates with a distributed network of servers.
- **Raft Consensus Algorithm**: Implemented from scratch to ensure leader election, fault tolerance, and data consistency across multiple servers.
- **Java RMI Communication**: Used for distributed interactions between the client and game servers.

## 🎮 Features

- 🛳️ **Multiplayer Game**: Two players can connect and play Battleship online.
- 🔄 **Turn-Based System**: Players take turns attacking each other's grids.
- ⚡ **Leader Election**: Servers elect a leader to coordinate the game.
- 🎯 **Attack Mechanism**: Players input coordinates and receive hit/miss feedback.
- 🏆 **Win Condition**: A player wins when all enemy ships are destroyed.

## 🚀 How to Run

1. **Start the RMI Registry**:
   ```sh
   java RMIRegistryLauncher
   ```
2. **Launch the Game Servers**:
   ```sh
   java RaftServerMain <serverIndex>
   ```
3. **Run the Client**:
   ```sh
   java BattleshipClient
   ```

## 🔧 Future Improvements

- Optimize thread management to improve efficiency.
- Expand game support to more than two players.
- Improve user interface beyond CLI.
