package chess;

/**
 * Represents an entry in the transposition table for the chess AI.
 */
public class TranspositionEntry {
    public final int score;
    public final int depth;
    public final TranspositionFlag flag;
    
    public TranspositionEntry(int score, int depth, TranspositionFlag flag) {
        this.score = score;
        this.depth = depth;
        this.flag = flag;
    }
}
