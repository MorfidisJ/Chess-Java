package chess.ai;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.Move;
import chess.PieceColor;
import chess.PieceType;

import java.util.*;
import java.util.concurrent.*;

/**
 * Runs experiments comparing different AI implementations.
 */
public class AIExperiment {
    // Helper method to clear the console
    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing screen fails, just print some newlines
            System.out.println("\n".repeat(10));
        }
    }
    
    // Helper method for consistent delays
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    
    // Nested class for board printing
    private static class BoardPrinter {
        public static void printBoard(ChessBoard board) {
            // Print top border
            System.out.println("  +-----------------+");
            
            // Print each row with pieces
            for (int row = 0; row < 8; row++) {
                // Print row number and left border
                System.out.print((8 - row) + " | ");
                
                // Print each square in the row
                for (int col = 0; col < 8; col++) {
                    ChessPiece piece = board.getPiece(row, col);
                    if (piece == null) {
                        // Print empty square
                        System.out.print(". ");
                    } else {
                        // Print piece with appropriate color
                        if (piece.getColor() == PieceColor.WHITE) {
                            System.out.print("\u001B[97m"); // White text
                        } else {
                            System.out.print("\u001B[30m"); // Black text
                        }
                        System.out.print(getPieceChar(piece) + " ");
                        System.out.print("\u001B[0m"); // Reset color
                    }
                }
                
                // Print right border and row number
                System.out.println("| " + (8 - row));
            }
            
            // Print bottom border and column letters
            System.out.println("  +-----------------+");
            System.out.println("    a b c d e f g h  ");
        }
        
        private static char getPieceChar(ChessPiece piece) {
            char c;
            switch (piece.getType()) {
                case PAWN: c = 'p'; break;
                case KNIGHT: c = 'n'; break;
                case BISHOP: c = 'b'; break;
                case ROOK: c = 'r'; break;
                case QUEEN: c = 'q'; break;
                case KING: c = 'k'; break;
                default: c = '?';
            }
            return piece.getColor() == PieceColor.WHITE ? Character.toUpperCase(c) : c;
        }
    }
    private final List<ChessAI> ais = new ArrayList<>();
    private final AIPerformanceTracker tracker = new AIPerformanceTracker();
    private final int gamesPerMatch;
    private final int maxMovesPerGame;
    private final int timePerMoveMs;
    private final int searchDepth;
    
    public AIExperiment(int gamesPerMatch, int maxMovesPerGame, int timePerMoveMs, int searchDepth) {
        this.gamesPerMatch = gamesPerMatch;
        this.maxMovesPerGame = maxMovesPerGame;
        this.timePerMoveMs = timePerMoveMs;
        this.searchDepth = searchDepth;
    }
    
    public void addAI(ChessAI ai) {
        ais.add(ai);
    }
    
    public void runTournament() {
        System.out.println("Starting AI Tournament");
        System.out.println("-".repeat(80));
        
        // Run matches between all pairs of AIs
        for (int i = 0; i < ais.size(); i++) {
            for (int j = i + 1; j < ais.size(); j++) {
                runMatch(ais.get(i), ais.get(j));
            }
        }
        
        // Print and save the summary
        String summary = tracker.getSummary();
        System.out.println("\n" + summary);
        
        // Save to file
        String filePath = tracker.saveSummaryToFile();
        if (filePath != null) {
            System.out.println("\nPerformance data saved to: " + filePath);
        } else {
            System.out.println("\nFailed to save performance data to file");
        }
    }
    
    private void runMatch(ChessAI ai1, ChessAI ai2) {
        System.out.printf("\n=== %s vs %s ===\n", ai1.getName(), ai2.getName());
        
        // Play games with each AI as white and black
        for (int game = 0; game < gamesPerMatch; game++) {
            // AI1 as white, AI2 as black
            playGame(ai1, ai2, game);
            
            // AI2 as white, AI1 as black
            playGame(ai2, ai1, game + gamesPerMatch);
        }
    }
    
    private void playGame(ChessAI whiteAI, ChessAI blackAI, int gameNumber) {
        ChessBoard board = new ChessBoard();
        int moveCount = 0;
        PieceColor currentPlayer = PieceColor.WHITE; // Game starts with white
        
        // Clear screen and print game header
        clearScreen();
        System.out.println("=".repeat(60));
        System.out.printf("GAME %d: %s (White) vs %s (Black)\n", 
                gameNumber + 1, whiteAI.getName(), blackAI.getName());
        System.out.println("=".repeat(60));
        
        // Print initial board
        System.out.println("\nStarting position:");
        BoardPrinter.printBoard(board);
        
        // Small delay to let the user see the initial position
        sleep(1000);
        
        while (moveCount < maxMovesPerGame) {
            // Check for game end conditions
            if (board.isCheckmate(PieceColor.WHITE) || board.isCheckmate(PieceColor.BLACK) || 
                board.isStalemate(PieceColor.WHITE) || board.isStalemate(PieceColor.BLACK) ||
                board.isDraw()) {
                break;
            }
            
            // Get current player's AI
            ChessAI currentAI = currentPlayer == PieceColor.WHITE ? whiteAI : blackAI;
            String aiName = currentAI.getName();
            
            // Track move start time
            tracker.startMove(aiName);
            
            // Make move
            try {
                Move move = currentAI.findBestMove(board, currentPlayer, searchDepth, timePerMoveMs);
                
                if (move == null) {
                    System.out.println("No valid move found! Game over.");
                    break;
                }
                
                // Record the move in the tracker
                // Get nodes evaluated if the AI implements TrackedChessAI
                long nodesEvaluated = 0;
                if (currentAI instanceof TrackedChessAI) {
                    nodesEvaluated = ((TrackedChessAI) currentAI).getLastMoveNodesEvaluated();
                }
                tracker.recordMove(aiName, move, searchDepth, nodesEvaluated);
                
                // Record move and update board
                board.makeMove(move);
                
                // Update move count and switch player after successful move
                moveCount++;
                currentPlayer = (currentPlayer == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
                
                // Clear screen and print move info
                clearScreen();
                System.out.println("=".repeat(60));
                System.out.printf("GAME %d: %s (White) vs %s (Black)\n", 
                        gameNumber + 1, whiteAI.getName(), blackAI.getName());
                System.out.println("-".repeat(60));
                
                // Print move information
                System.out.printf("Move %d: %s (%s) plays %s\n\n", 
                        moveCount, 
                        currentAI.getName(),
                        currentPlayer == PieceColor.WHITE ? "White" : "Black",
                        move.toString());
                
                // Print the board after the move
                BoardPrinter.printBoard(board);
                
                // Print game status (check, checkmate, etc.)
                if (board.isKingInCheck(currentPlayer.opposite())) {
                    System.out.println("\nCHECK!");
                }
                if (board.isCheckmate(currentPlayer.opposite())) {
                    System.out.println("\nCHECKMATE! " + currentAI.getName() + " wins!");
                } else if (board.isStalemate(currentPlayer.opposite())) {
                    System.out.println("\nSTALEMATE! Game is a draw.");
                } else if (board.isDraw()) {
                    System.out.println("\nDRAW! Game ended in a draw.");
                }
                
                // Small delay to make it easier to follow
                sleep(1000);
                
                // Print move info
                System.out.printf("Depth: %d, Nodes: %d\n", 
                    searchDepth, 
                    currentAI instanceof TrackedChessAI ? 
                        ((TrackedChessAI) currentAI).getLastMoveNodesEvaluated() : 0);
                
            } catch (Exception e) {
                System.err.println("Error in AI " + aiName + ": " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
        
        // Determine winner and update stats
        String winner = null;
        String loser = null;
        
        if (board.isCheckmate(PieceColor.WHITE)) {
            winner = blackAI.getName();
            loser = whiteAI.getName();
            System.out.println("Checkmate! " + winner + " wins!");
        } else if (board.isCheckmate(PieceColor.BLACK)) {
            winner = whiteAI.getName();
            loser = blackAI.getName();
            System.out.println("Checkmate! " + winner + " wins!");
        } else if (board.isDraw()) {
            System.out.println("Game drawn!");
        } else {
            System.out.println("Game ended after " + moveCount + " moves.");
        }
        
        tracker.recordGameResult(winner, loser, moveCount);
    }
    
    public static void main(String[] args) {
        // Create experiment with 10 games per match, max 100 moves per game,
        // 5 seconds per move, and search depth of 3
        AIExperiment experiment = new AIExperiment(5, 100, 5000, 3);
        
        // Add different AI implementations
        experiment.addAI(new MinimaxAI("Minimax (Basic)"));
        experiment.addAI(new AlphaBetaAI("Alpha-Beta"));
        // experiment.addAI(new MCTSAI("MCTS"));  // Uncomment when implemented
        
        // Run the tournament
        experiment.runTournament();
    }
}
