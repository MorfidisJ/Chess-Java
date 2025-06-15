import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ChessGame extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int SQUARE_SIZE = 80;
    
    private ChessBoard board;
    private JButton[][] squares;
    private Point selectedSquare;
    private boolean playerTurn = true;
    private JLabel statusLabel;
    private ChessAI ai;
    private int aiDepth = 3; 
    private JComboBox<String> difficultyCombo;
    private PieceColor playerColor = PieceColor.WHITE; 
    private JComboBox<String> colorCombo;
    private boolean gameStarted = false; 
    
    public ChessGame() {
        setTitle("Chess Game - Player vs Computer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        board = new ChessBoard();
        squares = new JButton[BOARD_SIZE][BOARD_SIZE];
        ai = new ChessAI();
        
        initializeGUI();
        updateBoard();
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initializeGUI() {
        setLayout(new BorderLayout());
        
        
        statusLabel = new JLabel("Choose your color and click 'Start Game'", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);
        
        
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE * SQUARE_SIZE, BOARD_SIZE * SQUARE_SIZE));
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                JButton square = new JButton();
                square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
                square.setFont(new Font("Arial Unicode MS", Font.PLAIN, 40));
                square.setFocusPainted(false);
                
                
                if ((row + col) % 2 == 0) {
                    square.setBackground(new Color(245, 222, 179)); // Light beige for white squares
                } else {
                    square.setBackground(new Color(139, 69, 19)); // Brown for black squares
                }
                
                final int r = row, c = col;
                square.addActionListener(e -> handleSquareClick(r, c));
                
                squares[row][col] = square;
                boardPanel.add(square);
            }
        }
        
        add(boardPanel, BorderLayout.CENTER);
        
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> startGame());
        controlPanel.add(startButton);
        
        JButton resetButton = new JButton("New Game");
        resetButton.addActionListener(e -> resetGame());
        controlPanel.add(resetButton);
        
        
        controlPanel.add(new JLabel("Play as:"));
        String[] colors = {"White", "Black"};
        colorCombo = new JComboBox<>(colors);
        colorCombo.setSelectedIndex(0); // Default to White
        colorCombo.addActionListener(e -> updateColor());
        controlPanel.add(colorCombo);
        
        
        controlPanel.add(new JLabel("Difficulty:"));
        String[] difficulties = {"Easy (1)", "Medium (2)", "Hard (3)", "Expert (4)", "Master (5)"};
        difficultyCombo = new JComboBox<>(difficulties);
        difficultyCombo.setSelectedIndex(2); // Default to Hard
        difficultyCombo.addActionListener(e -> updateDifficulty());
        controlPanel.add(difficultyCombo);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private void startGame() {
        gameStarted = true;
        colorCombo.setEnabled(false); 
        difficultyCombo.setEnabled(false); 
        
        playerColor = colorCombo.getSelectedIndex() == 0 ? PieceColor.WHITE : PieceColor.BLACK;
        playerTurn = (playerColor == PieceColor.WHITE); 
        
        if (playerColor == PieceColor.WHITE) {
            statusLabel.setText("White's turn (Your turn)");
        } else {
            statusLabel.setText("Black's turn (Your turn)");
        }
        
        
        if (playerColor == PieceColor.BLACK) {
            playerTurn = false;
            statusLabel.setText("Computer's turn...");
            
            javax.swing.Timer timer = new javax.swing.Timer(1000, e -> makeComputerMove());
            timer.setRepeats(false);
            timer.start();
        }
    }
    
    private void updateColor() {
        if (!gameStarted) {

            playerColor = colorCombo.getSelectedIndex() == 0 ? PieceColor.WHITE : PieceColor.BLACK;
            if (playerColor == PieceColor.WHITE) {
                statusLabel.setText("Choose your color and click 'Start Game' (You'll play as White)");
            } else {
                statusLabel.setText("Choose your color and click 'Start Game' (You'll play as Black)");
            }
        }
    }
    
    private void updateDifficulty() {
        if (!gameStarted) {
            aiDepth = difficultyCombo.getSelectedIndex() + 1;
            statusLabel.setText("Difficulty set to: " + difficultyCombo.getSelectedItem() + " - Click 'Start Game' to begin");
        }
    }
    
    private void handleSquareClick(int row, int col) {
        if (!gameStarted || !playerTurn) return;
        
        if (selectedSquare == null) {
            
            ChessPiece piece = board.getPiece(row, col);
            if (piece != null && piece.color == playerColor) {
                selectedSquare = new Point(row, col);
                highlightSquare(row, col, Color.YELLOW);
                showPossibleMoves(row, col);
            }
        } else {
            
            if (selectedSquare.x == row && selectedSquare.y == col) {
                
                clearHighlights();
                selectedSquare = null;
            } else {
                
                Move move = new Move(selectedSquare.x, selectedSquare.y, row, col);
                
                
                ChessPiece selectedPiece = board.getPiece(selectedSquare.x, selectedSquare.y);
                boolean isPawnPromotion = selectedPiece != null && 
                                        selectedPiece.type == PieceType.PAWN && 
                                        ((selectedPiece.color == PieceColor.WHITE && row == 0) ||
                                         (selectedPiece.color == PieceColor.BLACK && row == 7));
                
                if (board.isValidMove(move)) {
                    if (isPawnPromotion) {
                        
                        PieceType promotionPiece = showPromotionDialog();
                        if (promotionPiece != null) {
                            move = new Move(selectedSquare.x, selectedSquare.y, row, col, promotionPiece);
                            board.makeMove(move);
                            updateBoard();
                            clearHighlights();
                            selectedSquare = null;
                            playerTurn = false;
                            
                            PieceColor aiColor = (playerColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
                            statusLabel.setText("Computer's turn...");
                            
                            if (board.isCheckmate(aiColor)) {
                                statusLabel.setText("Checkmate! You win!");
                                return;
                            }
                            
                            if (board.isDraw()) {
                                statusLabel.setText("Draw!");
                                return;
                            }
                            
                            
                            javax.swing.Timer timer = new javax.swing.Timer(1000, e -> makeComputerMove());
                            timer.setRepeats(false);
                            timer.start();
                        } else {
                            
                            clearHighlights();
                            selectedSquare = null;
                        }
                    } else {
                        
                        board.makeMove(move);
                        updateBoard();
                        clearHighlights();
                        selectedSquare = null;
                        playerTurn = false;
                        
                        PieceColor aiColor = (playerColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
                        statusLabel.setText("Computer's turn...");
                        
                        if (board.isCheckmate(aiColor)) {
                            statusLabel.setText("Checkmate! You win!");
                            return;
                        }
                        
                        if (board.isDraw()) {
                            statusLabel.setText("Draw!");
                            return;
                        }
                        
                        
                        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> makeComputerMove());
                        timer.setRepeats(false);
                        timer.start();
                    }
                } else {
                    clearHighlights();
                    selectedSquare = null;
                }
            }
        }
    }
    
    private PieceType showPromotionDialog() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Choose piece for pawn promotion:",
            "Pawn Promotion",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0] 
        );
        
        switch (choice) {
            case 0: return PieceType.QUEEN;
            case 1: return PieceType.ROOK;
            case 2: return PieceType.BISHOP;
            case 3: return PieceType.KNIGHT;
            default: return null; 
        }
    }
    
    private void makeComputerMove() {
        PieceColor aiColor = (playerColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        Move computerMove = ai.findBestMove(board, aiColor, aiDepth);
        if (computerMove != null) {
            board.makeMove(computerMove);
            updateBoard();
            
            if (board.isCheckmate(playerColor)) {
                statusLabel.setText("Checkmate! Computer wins!");
                return;
            }
            
            if (board.isDraw()) {
                statusLabel.setText("Draw!");
                return;
            }
            
            playerTurn = true;
            if (playerColor == PieceColor.WHITE) {
                statusLabel.setText("White's turn (Your turn)");
            } else {
                statusLabel.setText("Black's turn (Your turn)");
            }
        }
    }
    
    private void showPossibleMoves(int fromRow, int fromCol) {
        ChessPiece selectedPiece = board.getPiece(fromRow, fromCol);
        if (selectedPiece == null) return;
        boolean isKing = selectedPiece.type == PieceType.KING;
        java.util.HashSet<java.awt.Point> castlingSquares = new java.util.HashSet<>();


        if (isKing) {
            if (selectedPiece.color == PieceColor.WHITE && fromRow == 7 && fromCol == 4) {
                
                if (!board.hasWhiteKingMoved() && !board.hasWhiteKingsideRookMoved()) {
                    if (board.getPiece(7, 5) == null && board.getPiece(7, 6) == null) {
                        if (!board.isSquareUnderAttack(7, 4, PieceColor.BLACK) &&
                            !board.isSquareUnderAttack(7, 5, PieceColor.BLACK) &&
                            !board.isSquareUnderAttack(7, 6, PieceColor.BLACK)) {
                            highlightSquare(7, 6, new Color(100, 150, 255));
                            squares[7][6].setText("●");
                            castlingSquares.add(new java.awt.Point(7, 6));
                        }
                    }
                }
                
                if (!board.hasWhiteKingMoved() && !board.hasWhiteQueensideRookMoved()) {
                    if (board.getPiece(7, 1) == null && board.getPiece(7, 2) == null && board.getPiece(7, 3) == null) {
                        if (!board.isSquareUnderAttack(7, 4, PieceColor.BLACK) &&
                            !board.isSquareUnderAttack(7, 3, PieceColor.BLACK) &&
                            !board.isSquareUnderAttack(7, 2, PieceColor.BLACK)) {
                            highlightSquare(7, 2, new Color(100, 150, 255));
                            squares[7][2].setText("●");
                            castlingSquares.add(new java.awt.Point(7, 2));
                        }
                    }
                }
            } else if (selectedPiece.color == PieceColor.BLACK && fromRow == 0 && fromCol == 4) {
                
                if (!board.hasBlackKingMoved() && !board.hasBlackKingsideRookMoved()) {
                    if (board.getPiece(0, 5) == null && board.getPiece(0, 6) == null) {
                        if (!board.isSquareUnderAttack(0, 4, PieceColor.WHITE) &&
                            !board.isSquareUnderAttack(0, 5, PieceColor.WHITE) &&
                            !board.isSquareUnderAttack(0, 6, PieceColor.WHITE)) {
                            highlightSquare(0, 6, new Color(100, 150, 255));
                            squares[0][6].setText("●");
                            castlingSquares.add(new java.awt.Point(0, 6));
                        }
                    }
                }
                
                if (!board.hasBlackKingMoved() && !board.hasBlackQueensideRookMoved()) {
                    if (board.getPiece(0, 1) == null && board.getPiece(0, 2) == null && board.getPiece(0, 3) == null) {
                        if (!board.isSquareUnderAttack(0, 4, PieceColor.WHITE) &&
                            !board.isSquareUnderAttack(0, 3, PieceColor.WHITE) &&
                            !board.isSquareUnderAttack(0, 2, PieceColor.WHITE)) {
                            highlightSquare(0, 2, new Color(100, 150, 255));
                            squares[0][2].setText("●");
                            castlingSquares.add(new java.awt.Point(0, 2));
                        }
                    }
                }
            }
        }

        
        for (int toRow = 0; toRow < BOARD_SIZE; toRow++) {
            for (int toCol = 0; toCol < BOARD_SIZE; toCol++) {
                
                if (castlingSquares.contains(new java.awt.Point(toRow, toCol))) {
                    continue;
                }
                Move move = new Move(fromRow, fromCol, toRow, toCol);
                if (!board.isValidMove(move)) continue;
                ChessPiece targetPiece = board.getPiece(toRow, toCol);
                
                boolean isPawnPromotion = selectedPiece.type == PieceType.PAWN &&
                    ((selectedPiece.color == PieceColor.WHITE && toRow == 0) ||
                     (selectedPiece.color == PieceColor.BLACK && toRow == 7));
                
                boolean isEnPassant = selectedPiece.type == PieceType.PAWN &&
                    Math.abs(toCol - fromCol) == 1 &&
                    targetPiece == null &&
                    board.getEnPassantRow() == toRow &&
                    board.getEnPassantCol() == toCol;
                if (isPawnPromotion) {
                    highlightSquare(toRow, toCol, new Color(255, 215, 0));
                    squares[toRow][toCol].setText("?");
                } else if (isEnPassant) {
                    highlightSquare(toRow, toCol, new Color(255, 165, 0));
                    squares[toRow][toCol].setText("⚡");
                } else if (targetPiece != null) {
                    highlightSquare(toRow, toCol, new Color(255, 100, 100));
                    squares[toRow][toCol].setText("●" + targetPiece.getSymbol());
                } else {
                    highlightSquare(toRow, toCol, new Color(144, 238, 144));
                    squares[toRow][toCol].setText("●");
                }
            }
        }
    }
    
    private void highlightSquare(int row, int col, Color color) {
        squares[row][col].setBackground(color);
    }
    
    private void clearHighlights() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if ((row + col) % 2 == 0) {
                    squares[row][col].setBackground(new Color(245, 222, 179)); 
                } else {
                    squares[row][col].setBackground(new Color(139, 69, 19)); 
                }
            }
        }
        updateBoard(); 
    }
    
    private void updateBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    squares[row][col].setText(piece.getSymbol());
                    
                    if (piece.color == PieceColor.WHITE) {
                        squares[row][col].setForeground(Color.WHITE);
                    } else {
                        squares[row][col].setForeground(Color.BLACK);
                    }
                } else {
                    squares[row][col].setText("");
                }
            }
        }
    }
    
    private void resetGame() {
        board = new ChessBoard();
        selectedSquare = null;
        gameStarted = false; 
        
        
        colorCombo.setEnabled(true);
        difficultyCombo.setEnabled(true);
        
        statusLabel.setText("Choose your color and click 'Start Game'");
        clearHighlights();
        updateBoard();
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGame());
    }
}

enum PieceColor {
    WHITE, BLACK
}

enum PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

class ChessPiece {
    PieceType type;
    PieceColor color;
    
    public ChessPiece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
    }
    
    public String getSymbol() {
        
        String symbols = "♚♛♜♝♞♟"; 
        switch (type) {
            case KING: return symbols.substring(0, 1);
            case QUEEN: return symbols.substring(1, 2);
            case ROOK: return symbols.substring(2, 3);
            case BISHOP: return symbols.substring(3, 4);
            case KNIGHT: return symbols.substring(4, 5);
            case PAWN: return symbols.substring(5, 6);
            default: return "";
        }
    }
}

class Move {
    int fromRow, fromCol, toRow, toCol;
    PieceType promotion; 
    boolean isEnPassant; 

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = null; 
        this.isEnPassant = false; 
    }
    
    public Move(int fromRow, int fromCol, int toRow, int toCol, PieceType promotion) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = promotion;
        this.isEnPassant = false; 
    }
    
    public Move(int fromRow, int fromCol, int toRow, int toCol, boolean isEnPassant) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = null;
        this.isEnPassant = isEnPassant;
    }

    @Override
    public String toString() {
        return "Move from (" + fromRow + "," + fromCol + ") to (" + toRow + "," + toCol + ")" + 
               (promotion != null ? " with promotion to " + promotion : "") +
               (isEnPassant ? " (en passant)" : "");
    }
}