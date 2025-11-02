package chess;

import java.util.List;

/**
 * Interface for different chess AI strategies.
 * All AI implementations should implement this interface.
 */
public interface ChessStrategy {
    /**
     * Finds the best move for the current player
     * @param board Current game state
     * @param color Color of the player to move
     * @param depth Maximum search depth (if applicable)
     * @param timeLimitMs Maximum time to spend on move (in milliseconds)
     * @return Best move found, or null if no valid moves
     */
    Move findBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs);
    
    /**
     * Gets the name of this strategy
     */
    String getName();
    
    /**
     * Gets performance metrics from the last move
     */
    SearchMetrics getMetrics();
    
    /**
     * Resets any internal state (e.g., for a new game)
     */
    void reset();
}

/**
 * Container for search performance metrics
 */
class SearchMetrics {
    public final long nodesEvaluated;
    public final long timeTakenMs;
    public final int depthReached;
    public final double score;
    
    public SearchMetrics(long nodesEvaluated, long timeTakenMs, int depthReached, double score) {
        this.nodesEvaluated = nodesEvaluated;
        this.timeTakenMs = timeTakenMs;
        this.depthReached = depthReached;
        this.score = score;
    }
}
