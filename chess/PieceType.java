package chess;

/**
 * Represents the type of a chess piece.
 */
public enum PieceType {
    KING,
    QUEEN,
    ROOK,
    BISHOP,
    KNIGHT,
    PAWN;
    
    /**
     * Gets the standard value of the piece for evaluation.
     * @return The piece value in centipawns
     */
    public int getValue() {
        return switch (this) {
            case PAWN -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK -> 500;
            case QUEEN -> 900;
            case KING -> 20000;
        };
    }
}
