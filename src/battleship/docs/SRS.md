# Software Requirements Specification (SRS)  
## Distributed Multiplayer Battleship Game  

## 1. Introduction  
| Section | Description |
|---------|------------|
| **Project Name** | Distributed Battleship Game |
| **Purpose** | Develop a multiplayer Battleship game based on a distributed multi-server architecture using Raft algorithm, ensuring synchronization and fault tolerance. |
| **Objectives** | Enable two players to engage in an online Battleship match with distributed coordination, automatic leader election, and fault recovery. |

---

## 2. Actors and Roles  
| Role | Description |
|------|------------|
| **Player** | A user who participates in a Battleship match. |
| **Game Server** | A server managing game state and synchronizing player turns. Multiple game servers exist for fault tolerance. |
| **Leader Server** | The elected main server responsible for coordinating turns and maintaining consistency. |
| **Backup Server** | A secondary server ready to take over if the leader server fails. |

---

## **3. Functional Requirements**  

| ID   | Requirement        | Description |
|------|--------------------|-------------|
| **FR-1** | **Start Game** | As a player, I want to connect to a server and start a game with a remote opponent. |
| **FR-2** | **Ship Placement** | As a player, I want to place my 5 ships on a 6x6 grid. |
| **FR-3** | **Turn-Based System** | As a player, I want turns to alternate and be notified when it's my turn. |
| **FR-4** | **Attack Mechanism** | As a player, I want to input attack coordinates and receive feedback (hit/miss). |
| **FR-5** | **Win/Loss Condition** | As a player, I want the system to declare a winner when all of one player's ships are sunk. |
| **FR-6** | **Grid Visualization** | As a player, I want to see my grid when I need to place my ships and the opponent’s grid when I need to attack with recorded placement/attacks. |
| **FR-7** | **Leader Election** | As a server, I want a leader election to determine which server will manage the match. |
| **FR-8** | **Process Attacks** | As the leader server, I want to process player attack requests and validate their execution. |
| **FR-9** | **Determine Hit or Miss** | As the leader server, I want to determine if an attack results in a hit or a miss based on the opponent's grid. |
| **FR-10** | **Inform Players of Attack Results** | As the leader server, I want to notify players of the outcome of an attack (hit or miss). |
| **FR-11** | **Manage Player Turns** | As the leader server, I want to enforce turn-taking and notify the current player when it's their turn. |
| **FR-12** | **Declare a Winner** | As the leader server, I want to declare a winner when all ships of a player are sunk. |
| **FR-13** | **Manage Game State** | As the leader server, I want to manage the game state to ensure consistency and synchronization. |
| **FR-14** | **Follower Server Synchronization** | As a follower server, I want to receive game data updates from the leader server and maintain synchronization to ensure fault tolerance. |
| **FR-15** | **Detect Leader Server Failure** | As a follower server, I want to detect when the leader server crashes. |
| **FR-16** | **Participate in Leader Election** | As a follower server, I want to participate in the leader election when the current leader fails. |
| **FR-17** | **Resume Game as New Leader** | As a follower server, I want to take over as the new leader server and continue the game seamlessly after a leader failure. |

---

## **4. Non-Functional Requirements**  

| ID    | Requirement        | Description |
|-------|--------------------|-------------|
| **NFR-1** | **Minimal Interface** | The game must be playable via a command-line interface (CLI)|
| **NFR-2** | **Performance** | The system must maintain low latency (maximum 10 ms) for communication between clients and servers to ensure smooth and responsive gameplay. |

---

## **5. Constraints**  

| ID    | Constraint         | Description |
|-------|--------------------|-------------|
| **C-1** | **Players** | The system must ensure two player to play. |
| **C-2** | **Technology** | The system must be developed in Java using RMI for distributed communication between servers and clients. |
| **C-3** | **Leader Election Algorithm** | The servers must implement the Raft Algorithm to determine which server will act as the leader and coordinate the game. |
| **C-4** | **Data Replication** | Follower servers must maintain up-to-date copies of the game state to ensure no data is lost if a leader failure occurs. |
