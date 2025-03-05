# Battleship with Distributed Server

## 📌 Project Overview

This project implements a **distributed multiplayer Battleship game** using the **Raft consensus algorithm** for fault tolerance and synchronization. The system consists of a distributed server architecture where multiple game servers coordinate to maintain consistency and handle player interactions.

## 🏗️ System Architecture

- **Client-Server Model**: Players interact with the game through a client, which communicates with a distributed network of servers.
- **Raft Consensus Algorithm**: Ensures leader election, fault tolerance, and data consistency across multiple servers.
- **Java RMI Communication**: Used for distributed interactions between the client and game servers.

## 🎮 Features

### Functional Requirements

- 🛳️ **Multiplayer Game**: Two players can connect and play Battleship online.
- 🔄 **Turn-Based System**: Players take turns attacking each other's grids.
- ⚡ **Leader Election**: Servers elect a leader to coordinate the game.
- 🎯 **Attack Mechanism**: Players input coordinates and receive hit/miss feedback.
- 🏆 **Win Condition**: A player wins when all enemy ships are destroyed.

### Non-Functional Requirements

- **Command Line Interface (CLI)** for interaction.
- **Low latency (\~10ms)** for smooth gameplay.
- **Fault Tolerance**: If the leader fails, a new one is elected automatically.

## 🏛️ Technologies Used

- **Programming Language**: Java
- **Distributed Communication**: Java RMI
- **Consensus Algorithm**: Raft
- **Multithreading**: For server operations

## 🚀 How to Run

1. **Start the RMI Registry**:
   ```sh
   rmiregistry &
   ```
2. **Launch the Game Servers**:
   ```sh
   java RaftServer
   ```
3. **Run the Client**:
   ```sh
   java BattleshipClient
   ```

## 🔧 Future Improvements

- Optimize thread management to improve efficiency.
- Expand game support to more than two players.
- Improve user interface beyond CLI.
