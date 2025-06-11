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
        
       
        statusLabel = new JLabel("White's turn (Your turn)", JLabel.CENTER);
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
                    square.setBackground(new Color(245, 222, 179)); 
                } else {
                    square.setBackground(new Color(139, 69, 19)); 
                }
                
                final int r = row, c = col;
                square.addActionListener(e -> handleSquareClick(r, c));
                
                squares[row][col] = square;
                boardPanel.add(square);
            }
        }
        
        add(boardPanel, BorderLayout.CENTER);
        
      
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton resetButton = new JButton("New Game");
        resetButton.addActionListener(e -> resetGame());
        controlPanel.add(resetButton);
        
        
        controlPanel.add(new JLabel("Difficulty:"));
        String[] difficulties = {"Easy (1)", "Medium (2)", "Hard (3)", "Expert (4)", "Master (5)"};
        difficultyCombo = new JComboBox<>(difficulties);
        difficultyCombo.setSelectedIndex(2); 
        difficultyCombo.addActionListener(e -> updateDifficulty());
        controlPanel.add(difficultyCombo);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private void updateDifficulty() {
        aiDepth = difficultyCombo.getSelectedIndex() + 1;
        statusLabel.setText("Difficulty set to: " + difficultyCombo.getSelectedItem());
    }
    
    private void handleSquareClick(int row, int col) {
        if (!playerTurn) return;
        
        if (selectedSquare == null) {
           
            ChessPiece piece = board.getPiece(row, col);
            if (piece != null && piece.color == PieceColor.WHITE) {
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
                if (board.isValidMove(move)) {
                    board.makeMove(move);
                    updateBoard();
                    clearHighlights();
                    selectedSquare = null;
                    playerTurn = false;
                    statusLabel.setText("Computer's turn...");
                    
                    if (board.isCheckmate(PieceColor.BLACK)) {
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
            }
        }
    }
    
    private void makeComputerMove() {
        Move computerMove = ai.findBestMove(board, PieceColor.BLACK, aiDepth);
        if (computerMove != null) {
            board.makeMove(computerMove);
            updateBoard();
            
            if (board.isCheckmate(PieceColor.WHITE)) {
                statusLabel.setText("Checkmate! Computer wins!");
                return;
            }
            
            if (board.isDraw()) {
                statusLabel.setText("Draw!");
                return;
            }
            
            playerTurn = true;
            statusLabel.setText("White's turn (Your turn)");
        }
    }
    
    private void showPossibleMoves(int fromRow, int fromCol) {
        for (int toRow = 0; toRow < BOARD_SIZE; toRow++) {
            for (int toCol = 0; toCol < BOARD_SIZE; toCol++) {
                Move move = new Move(fromRow, fromCol, toRow, toCol);
                if (board.isValidMove(move)) {
                    // Add a dot to indicate possible move
                    ChessPiece targetPiece = board.getPiece(toRow, toCol);
                    if (targetPiece != null) {
                        // Capture move - red dot
                        highlightSquare(toRow, toCol, new Color(255, 100, 100));
                        squares[toRow][toCol].setText("●" + targetPiece.getSymbol());
                    } else {
                        // Regular move - green dot
                        highlightSquare(toRow, toCol, new Color(144, 238, 144));
                        squares[toRow][toCol].setText("●");
                    }
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
                    squares[row][col].setBackground(new Color(245, 222, 179)); // Light beige
                } else {
                    squares[row][col].setBackground(new Color(139, 69, 19)); // Brown
                }
            }
        }
        updateBoard(); // Restore piece symbols
    }
    
    private void updateBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    squares[row][col].setText(piece.getSymbol());
                    // Set piece colors
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
        playerTurn = true;
        statusLabel.setText("White's turn (Your turn)");
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
        // Use solid piece symbols for both colors
        String symbols = "♚♛♜♝♞♟"; // Solid pieces
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
    
    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }
    
    @Override
    public String toString() {
        return String.format("(%d,%d) -> (%d,%d)", fromRow, fromCol, toRow, toCol);
    }
}