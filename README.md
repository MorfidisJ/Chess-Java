# Chess Game - Player vs Computer

A Java-based chess game featuring a sophisticated AI opponent with advanced search algorithms and optimization techniques.

## 🎮 Features

### Game Features
- **Complete Chess Rules**: All standard chess rules implemented
- **Visual Interface**: Clean, intuitive GUI with piece highlighting
- **Move Validation**: Real-time move validation and legal move highlighting
- **Game State Detection**: Checkmate, stalemate, and draw detection
- **Difficulty Levels**: 5 difficulty levels (Easy to Master)
- **Color Selection**: Choose to play as White or Black
- **One-Time Setup**: Color and difficulty locked once game starts

### AI Features
- **Alpha-Beta Pruning**: Efficient search tree exploration
- **Transposition Table**: Caches evaluated positions for performance
- **Iterative Deepening**: Progressive search with time management
- **Move Ordering**: Intelligent move prioritization for better pruning
- **Comprehensive Evaluation**: Material, positional, king safety, and mobility assessment

## 🏗️ Architecture

### Core Classes

```
ChessGame.java          - Main GUI and game controller
├── ChessBoard.java     - Board representation and game logic
├── ChessAI.java        - AI engine with advanced algorithms
└── ChessPiece.java     - Piece representation and behavior
```

### Key Components

#### 1. ChessGame (GUI Controller)
- Manages the graphical interface
- Handles user interactions
- Coordinates game flow
- Displays game status and controls
- Manages color selection and game setup

#### 2. ChessBoard (Game Logic)
- Board state representation
- Move validation and execution
- Game state detection (checkmate, draw, etc.)
- Legal move generation

#### 3. ChessAI (AI Engine)
- Advanced search algorithms
- Position evaluation
- Move selection and optimization
- Performance monitoring

## 🔄 Game Flow

```mermaid
flowchart TD
    A[Launch Game] --> B[Setup Phase]
    B --> C[Choose Color: White/Black]
    B --> D[Choose Difficulty: Easy to Master]
    C --> E[Click Start Game]
    D --> E
    E --> F[Game Begins]
    F --> G{Player's Turn?}
    G -->|Yes| H[Wait for Player Move]
    G -->|No| I[AI Calculate Move]
    H --> J[Validate Move]
    J -->|Invalid| H
    J -->|Valid| K[Execute Move]
    I --> L[Alpha-Beta Search]
    L --> M[Select Best Move]
    M --> K
    K --> N[Update Board]
    N --> O{Game Over?}
    O -->|No| G
    O -->|Yes| P[Display Result]
    P --> Q[Click New Game to Reset]
    Q --> B
```

## 🧠 AI Algorithm Flow

```mermaid
flowchart TD
    A[Find Best Move] --> B[Clear Large Transposition Table]
    B --> C[Get Valid Moves]
    C --> D[Start Iterative Deepening]
    D --> E[Set Current Depth = 1]
    E --> F[Sort Moves by Priority]
    F --> G[Check Time Limit]
    G -->|Time Up| H[Return Best Move]
    G -->|Time OK| I[Make Temporary Move]
    I --> J[Alpha-Beta Search]
    J --> K[Check Transposition Table]
    K -->|Hit| L[Return Cached Score]
    K -->|Miss| M{Terminal Node?}
    M -->|Yes| N[Evaluate Position]
    M -->|No| O[Generate Moves]
    N --> P[Store in Transposition Table]
    P --> Q[Return Score]
    O --> R[Recursive Alpha-Beta]
    R --> S[Alpha-Beta Pruning]
    S -->|Prune| T[Skip Remaining Moves]
    S -->|Continue| U[Update Best Score]
    T --> V[Store Result]
    U --> V
    V --> W[Return Score]
    L --> X[Update Best Move]
    W --> X
    X --> Y{More Moves?}
    Y -->|Yes| I
    Y -->|No| Z{More Depth?}
    Z -->|Yes| AA[Increment Depth]
    AA --> F
    Z -->|No| H
```

## 🔍 Alpha-Beta Pruning

```mermaid
flowchart TD
    A[Start Alpha-Beta] --> B[Initialize Alpha = -inf, Beta = +inf]
    B --> C[Generate Moves]
    C --> D[Sort Moves by Priority]
    D --> E[Make Move]
    E --> F[Recursive Call]
    F --> G{Maximizing Player?}
    G -->|Yes| H[Update Alpha]
    G -->|No| I[Update Beta]
    H --> J[Alpha = max Alpha Score]
    I --> K[Beta = min Beta Score]
    J --> L{Beta <= Alpha?}
    K --> L
    L -->|Yes| M[Prune Branch]
    L -->|No| N{More Moves?}
    M --> O[Return Current Best]
    N -->|Yes| E
    N -->|No| P[Return Best Score]
    O --> Q[End Search]
    P --> Q
```

## 📊 Transposition Table

```mermaid
flowchart TD
    A[Generate Hash Key] --> B[Check Table]
    B --> C{Entry Found?}
    C -->|Yes| D{Depth Sufficient?}
    C -->|No| E[Evaluate Position]
    D -->|Yes| F{Flag Type?}
    D -->|No| E
    F -->|EXACT| G[Return Cached Score]
    F -->|ALPHA| H{Score <= Alpha?}
    F -->|BETA| I{Score >= Beta?}
    H -->|Yes| G
    H -->|No| E
    I -->|Yes| G
    I -->|No| E
    E --> J[Store New Entry]
    J --> K[Return Score]
    G --> L[Use Cached Result]
```

## 🎯 Position Evaluation

The AI evaluates positions using multiple factors:

### Material Evaluation
- **Pawn**: 100 points
- **Knight**: 320 points
- **Bishop**: 330 points
- **Rook**: 500 points
- **Queen**: 900 points
- **King**: 20,000 points

### Positional Bonuses
- **Pawns**: Center control (+10), advancement (+5 per rank)
- **Knights**: Center control (+20), edge penalty (-10)
- **Bishops**: Long diagonals (+15), center control (+10)
- **Rooks**: Seventh rank attack (+15)
- **Queens**: Center control (+10), early development (+5)
- **Kings**: Safety (+20), center files (+10)

### Additional Factors
- **King Safety**: Attack detection (-50), pawn shield (+10), mobility (+5 per move)
- **Mobility**: Legal moves bonus (+2 per move)

## 🚀 Performance Optimizations

### 1. Alpha-Beta Pruning
- Reduces search space by eliminating irrelevant branches
- Typical reduction: 50-90% fewer nodes evaluated

### 2. Transposition Table
- Caches evaluated positions to avoid re-computation
- Memory-efficient with size limits and cleanup

### 3. Iterative Deepening
- Starts with shallow searches and progressively deepens
- Provides good moves quickly while allowing time for deeper analysis
- Respects time limits for responsive gameplay

### 4. Move Ordering
- Prioritizes promising moves (captures, center control)
- Improves pruning efficiency by exploring good moves first

### 5. Time Management
- 5-second time limit per move
- Graceful degradation when time runs out

## 🎮 How to Play

### Starting the Game
1. **Run** `ChessGame.java`
2. **Choose Color**: Select "White" or "Black" from the dropdown
3. **Choose Difficulty**: Select from Easy (1) to Master (5)
4. **Click "Start Game"**: Begin the chess match

### Color Selection
- **White**: Play as White pieces (bottom), move first (traditional)
- **Black**: Play as Black pieces (top), AI moves first
- **Locked Choice**: Color cannot be changed once game starts

### Making Moves
1. **Click** on a piece to select it
2. **Valid moves** will be highlighted:
   - 🟢 Green: Regular moves
   - 🔴 Red: Capture moves
3. **Click** on a highlighted square to make the move

### Difficulty Levels
- **Easy (1)**: Depth 1 - Good for beginners
- **Medium (2)**: Depth 2 - Balanced challenge
- **Hard (3)**: Depth 3 - Default setting
- **Expert (4)**: Depth 4 - Advanced play
- **Master (5)**: Depth 5 - Maximum challenge

### Game Controls
- **Start Game**: Begin the match with current settings
- **New Game**: Reset to setup phase for new choices
- **Settings Locked**: Color and difficulty cannot be changed during gameplay

## 📁 File Structure

```
chess/
├── ChessGame.java      # Main game interface
├── ChessBoard.java     # Board logic and rules
├── ChessAI.java        # AI engine
└── README.md           # This file
```

## 🔧 Technical Details

### Search Algorithm
- **Algorithm**: Minimax with Alpha-Beta pruning
- **Search Depth**: 1-5 plies (configurable)
- **Time Limit**: 5 seconds per move
- **Memory Management**: 1M entry transposition table limit

### Performance Characteristics
- **Typical Move Time**: 1-3 seconds
- **Memory Usage**: ~50-100MB
- **Search Efficiency**: 10,000-100,000 nodes/second

### Compatibility
- **Java Version**: 8 or higher
- **Dependencies**: Standard Java Swing (no external libraries)
- **Platform**: Cross-platform (Windows, macOS, Linux)

## 🎯 Future Enhancements

### Potential Improvements
1. **Opening Book**: Pre-computed opening moves
2. **Endgame Database**: Specialized endgame evaluation
3. **Multi-threading**: Parallel search for better performance
4. **Network Play**: Player vs Player over network
5. **Move History**: Game replay and analysis features
6. **Custom Themes**: Different board and piece styles

### AI Enhancements
1. **Neural Network**: Machine learning-based evaluation
2. **Monte Carlo Tree Search**: Alternative search algorithm
3. **Quiescence Search**: Tactical position analysis
4. **Null Move Pruning**: Additional pruning technique

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

---

## Author
John Morfidis 