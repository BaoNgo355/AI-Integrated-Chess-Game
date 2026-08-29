# AI-Integrated Chess Game

A desktop chess game with AI powered by Minimax, Alpha-Beta Pruning, and a Policy Neural Network. Supports LAN multiplayer.

## Features

- Play against AI with 3 difficulty levels: Easy, Medium, Hard
- LAN multiplayer (P2P via TCP/UDP)
- Drag-and-drop piece movement
- Full chess rules: castling, en passant, promotion, check/checkmate/draw
- Policy Neural Network for move evaluation

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java (JDK 8+) |
| GUI | Java Swing (JFrame, JPanel) |
| Concurrency | Thread, SwingUtilities |
| AI | Minimax + Alpha-Beta Pruning + Neural Network |
| Networking | java.net (TCP/UDP) |
| ML Training | Python, PyTorch |

## Project Structure

```
├── src/                        # Java source code
│   ├── ai/                     # AI engine (ChessAI, NeuralNet, BoardState, ...)
│   ├── common/                 # Config, PieceType, MenuLauncher
│   ├── core/                   # GamePanel, GameRenderer, MoveHistoryManager
│   ├── input/                  # Mouse handler
│   ├── main/                   # Main entry point
│   ├── network/                # LAN multiplayer (GameServer, GameClient, ...)
│   ├── piece/                  # Chess pieces (Pawn, Rook, Knight, Bishop, Queen, King)
│   ├── rendering/              # Board, PieceSprite
│   ├── state/                  # GameState
│   ├── ui/                     # MenuWindow
│   └── res/image/              # Piece sprites (PNG)
├── python/                     # ML training scripts
│   ├── model.py                # Neural network architecture
│   ├── train.py                # Training loop
│   ├── dataset.py              # Dataset handling
│   ├── config.py               # Training config
│   └── fetch_*.py              # Data fetching scripts
├── res/                        # Runtime config
│   └── ai_config.properties
└── sources.txt                 # Java source file list
```

## How to Run

### Java (game)

```bash
# Compile
javac -d bin -cp src @sources.txt

# Run
java -cp bin main.Main
```

Or open the project in Eclipse and run `Main.java`.

### Python (training the policy network)

```bash
cd python
pip install torch numpy chess
python train.py
```

## AI Algorithm

- **Search**: Minimax + Alpha-Beta Pruning with iterative deepening, null-move pruning, and LMR
- **Depth**: Easy=3, Medium=4, Hard=5 ply
- **Move Ordering**: Promotion > MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
- **Evaluation**: Material values + Piece-square tables + King safety + Check bonus
- **Neural Network**: Policy network with residual blocks for move evaluation
