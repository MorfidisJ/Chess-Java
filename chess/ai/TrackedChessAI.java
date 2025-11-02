package chess.ai;


/**
 * Interface for ChessAI implementations that track nodes evaluated during move calculation.
 */
public interface TrackedChessAI {
    /**
     * Gets the number of nodes evaluated during the last move calculation.
     * 
     * @return The number of nodes evaluated
     */
    long getLastMoveNodesEvaluated();
}
