package chess;

/**
 * Represents the color of a chess piece.
 */
public enum PieceColor {
    WHITE,
    BLACK;
    
    /**
     * Returns the opposite color.
     * @return The opposite color (WHITE for BLACK and vice versa)
     */
    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
