package chess;

/**
 * Represents a chess piece with a type and color.
 */
public class ChessPiece {
    private final PieceType type;
    private final PieceColor color;
    private boolean hasMoved = false;

    public ChessPiece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor getColor() {
        return color;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    @Override
    public ChessPiece clone() {
        ChessPiece clone = new ChessPiece(type, color);
        clone.hasMoved = this.hasMoved;
        return clone;
    }

    @Override
    public String toString() {
        return color + " " + type;
    }

    /**
     * Gets the symbol representation of the piece.
     * Uppercase for white, lowercase for black.
     */
    /**
     * Gets the Unicode symbol representation of the piece.
     * Uses proper chess piece characters for better visual representation.
     */
    public char getSymbol() {
        return switch (type) {
            case KING -> color == PieceColor.WHITE ? '♔' : '♚';
            case QUEEN -> color == PieceColor.WHITE ? '♕' : '♛';
            case ROOK -> color == PieceColor.WHITE ? '♖' : '♜';
            case BISHOP -> color == PieceColor.WHITE ? '♗' : '♝';
            case KNIGHT -> color == PieceColor.WHITE ? '♘' : '♞';
            case PAWN -> color == PieceColor.WHITE ? '♙' : '♟';
        };
    }
}
