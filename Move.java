public class Move {
    public final int fromRow, fromCol, toRow, toCol;
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
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = promotion;
        this.isEnPassant = false;
    }

    public Move(int fromRow, int fromCol, int toRow, int toCol, boolean isEnPassant) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.promotion = null;
        this.isEnPassant = isEnPassant;
    }

    @Override
    public String toString() {
        return "Move from (" + fromRow + "," + fromCol + ") to (" + toRow + "," + toCol + ")" +
                (promotion != null ? " with promotion to " + promotion : "") +
                (isEnPassant ? " (en passant)" : "");
    }
}
