public class ChessPiece {
    private final PieceType type;
    private final PieceColor color;

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

    public String getSymbol() {
        String symbols = "\u265A\u265B\u265C\u265D\u265E\u265F"; // ♚♛♜♝♞♟
        switch (type) {
            case KING:
                return symbols.substring(0, 1);
            case QUEEN:
                return symbols.substring(1, 2);
            case ROOK:
                return symbols.substring(2, 3);
            case BISHOP:
                return symbols.substring(3, 4);
            case KNIGHT:
                return symbols.substring(4, 5);
            case PAWN:
                return symbols.substring(5, 6);
            default:
                return "";
        }
    }
}
