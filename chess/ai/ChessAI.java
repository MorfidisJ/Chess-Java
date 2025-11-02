package chess.ai;

import chess.ChessBoard;
import chess.Move;
import chess.PieceColor;

/**
 * Interface for different chess AI implementations.
 */
public interface ChessAI {
    /**
     * Finds the best move for the given board state and color.
     * @param board The current board state
     * @param color The color to move
     * @param timeLimitMs Maximum time to spend on the move in milliseconds
     * @return The best move found, or null if no move is possible
     */
    Move findBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs);
    
    /**
     * Gets the name of the AI implementation.
     * @return The name of the AI
     */
    String getName();
    
    /**
     * Gets performance metrics from the last move search.
     * @return A string containing performance metrics
     */
    String getMetrics();
    
    /**
     * Resets any internal state or metrics.
     */
    void reset();
}
