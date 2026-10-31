# Text Adventure Game Engine

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Language-Java-orange.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Cross--Platform-lightgrey)]()

A lightweight, feature-rich Java-based game engine custom-built for text adventure and dungeon crawler RPGs, featuring automated checkpointing, JSON-driven world building, NPC interactions, and command history recording.

</div>

---

## Core Features

* **JSON-Driven Architecture:** Dynamically load worlds, rooms, items, monsters, win conditions, and commands entirely from JSON configuration files (`world.json`).
* **Advanced Save & Checkpoint System:**
    * Folder-based slot structure supporting save states and full command history tracking.
    * Automated checkpointing triggered by specific in-game commands or events with a configurable maximum retention limit.
* **Combat & Status Effects:** Turn-based combat system supporting player attacks, items, fleeing, status effects (like poison damage over time), and customizable monster drop tables.
* **Interactive NPCs & Economy:**
    * **Traders:** Buy and sell items using customizable currency names.
    * **Dialogue System:** Multi-layered branching dialogue trees and interactive text options.
* **Command Processing & Replay:** Robust command parsing with customizable aliases, action restriction handling during combat, and command replay features for loading saved sessions.

---

## Project Structure

````
GameEngine/
├── src/
│   ├── engine/
│   │   ├── combat/      # Monster stats, status effects, and combat calculations
│   │   ├── commands/
│   │   ├── misc/        # Core Game session, Command Processor, and interfaces
│   │   ├── npc/         # NPC behaviors, dialogue nodes, and trade offers
│   │   ├── saveSystem/  # Checkpoint managers, recorders, and game state snapshots
│   │   └── world/
│   └── util/            # Custom JSON parser, output handlers, and world loaders
├── resources/           # Game data files (world.json)
└── saves/               # Persistent save slots and checkpoint histories
````
Getting Started
Prerequisites

Make sure you have the following installed on your system:

    Java Development Kit (JDK) (v17 or higher recommended)

    A build tool or IDE supporting Java (IntelliJ IDEA, Eclipse, or Maven/Gradle if configured)

    Jackson Databind library (for JSON serialization/deserialization)

Running the Game

    Compile all Java source files under the engine and util packages.

    Ensure your resources/world.json file is correctly structured with your rooms, items, and start room configurations.

    Initialize the Game class by pointing to your world configuration and designated save slot path:

### Example World Configuration (world.json)

Your engine parses configuration elements such as player stats, items, monsters, and win rules directly from JSON:
JSON
````
{
  "gameTitle": "The Dungeon Awakening",
  "startRoom": "room_entrance",
  "player": {
    "hp": 100,
    "maxHp": 100,
    "attackDamage": 15,
    "carryCapacity": 10,
    "money": 50
  },
  "rooms": [
    {
      "id": "room_entrance",
      "description": "You stand at the stone-carved entrance of a dark cavern.",
      "exits": {
        "north": "room_hallway"
      }
    }
  ]
}
````
### Contributing

Contributions, feature requests, and bug reports are welcome!

    Fork the Project

    Create your Feature Branch (git checkout -b feature/AmazingFeature)

    Commit your Changes (git commit -m 'Add some AmazingFeature')

    Push to the Branch (git push origin feature/AmazingFeature)

    Open a Pull Request

### License

Distributed under the MIT License. See the LICENSE file for more information.