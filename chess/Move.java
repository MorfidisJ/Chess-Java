package chess;

import java.util.Objects;

/**
 * Represents a move in the chess game.
 */
public class Move {
    public final int fromRow;
    public final int fromCol;
    public final int toRow;
    public final int toCol;
    public PieceType promotion;
    public boolean isEnPassant;

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = null;
        this.isEnPassant = false;
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol, PieceType promotion) {
        this(fromRow, fromCol, toRow, toCol);
        this.promotion = promotion;
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol, boolean isEnPassant) {
        this(fromRow, fromCol, toRow, toCol);
        this.isEnPassant = isEnPassant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return fromRow == move.fromRow &&
               fromCol == move.fromCol &&
               toRow == move.toRow &&
               toCol == move.toCol &&
               isEnPassant == move.isEnPassant &&
               promotion == move.promotion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromRow, fromCol, toRow, toCol, promotion, isEnPassant);
    }

    @Override
    public String toString() {
        return String.format("Move[%c%d to %c%d%s%s]",
            (char)('a' + fromCol), 8 - fromRow,
            (char)('a' + toCol), 8 - toRow,
            promotion != null ? " promote to " + promotion : "",
            isEnPassant ? " (en passant)" : "");
    }
}
