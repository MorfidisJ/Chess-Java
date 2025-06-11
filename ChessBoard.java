import java.util.*;
import java.awt.Point;

public class ChessBoard implements Cloneable {
    private ChessPiece[][] board = new ChessPiece[8][8];
    
    public ChessBoard() {
        setupBoard();
    }
    
    public ChessBoard(ChessBoard other) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = other.board[row][col];
                if (piece != null) {
                    this.board[row][col] = new ChessPiece(piece.type, piece.color);
                }
            }
        }
    }
    
    private void setupBoard() {
        
        for (int col = 0; col < 8; col++) {
            board[1][col] = new ChessPiece(PieceType.PAWN, PieceColor.BLACK);
            board[6][col] = new ChessPiece(PieceType.PAWN, PieceColor.WHITE);
        }
        
        
        PieceType[] backRow = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                              PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        
        for (int col = 0; col < 8; col++) {
            board[0][col] = new ChessPiece(backRow[col], PieceColor.BLACK);
            board[7][col] = new ChessPiece(backRow[col], PieceColor.WHITE);
        }
    }
    
    @Override
    public ChessBoard clone() {
        return new ChessBoard(this);
    }
    
    public ChessPiece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) return null;
        return board[row][col];
    }
    
    public boolean isValidMove(Move move) {
        ChessPiece piece = getPiece(move.fromRow, move.fromCol);
        if (piece == null) return false;
        
        ChessPiece target = getPiece(move.toRow, move.toCol);
        if (target != null && target.color == piece.color) return false;
        
       
        if (wouldMoveExposeKing(piece.color, move)) return false;
        
        return isValidMoveForPiece(piece, move);
    }
    
    private boolean wouldMoveExposeKing(PieceColor color, Move move) {
        
        ChessPiece captured = board[move.toRow][move.toCol];
        board[move.toRow][move.toCol] = board[move.fromRow][move.fromCol];
        board[move.fromRow][move.fromCol] = null;
        
        
        boolean inCheck = isKingInCheck(color);
        
        
        board[move.fromRow][move.fromCol] = board[move.toRow][move.toCol];
        board[move.toRow][move.toCol] = captured;
        
        return inCheck;
    }
    
    public boolean isKingInCheck(PieceColor color) {
        Point kingPos = findKing(color);
        if (kingPos == null) return false;
        
        PieceColor opponentColor = (color == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getPiece(row, col);
                if (piece != null && piece.color == opponentColor) {
                    Move attackMove = new Move(row, col, kingPos.x, kingPos.y);
                    if (isValidMoveForPiece(piece, attackMove)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private Point findKing(PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getPiece(row, col);
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return new Point(row, col);
                }
            }
        }
        return null;
    }
    
    private boolean isValidMoveForPiece(ChessPiece piece, Move move) {
        int rowDiff = move.toRow - move.fromRow;
        int colDiff = move.toCol - move.fromCol;
        
        
        if (rowDiff == 0 && colDiff == 0) return false;
        
        switch (piece.type) {
            case PAWN:
                return isValidPawnMove(piece, move, rowDiff, colDiff);
            case ROOK:
                return (rowDiff == 0 || colDiff == 0) && isPathClear(move);
            case BISHOP:
                return Math.abs(rowDiff) == Math.abs(colDiff) && rowDiff != 0 && isPathClear(move);
            case QUEEN:
                return (rowDiff == 0 || colDiff == 0 || Math.abs(rowDiff) == Math.abs(colDiff)) && isPathClear(move);
            case KING:
                return Math.abs(rowDiff) <= 1 && Math.abs(colDiff) <= 1;
            case KNIGHT:
                return (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 1) || 
                       (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 2);
            default:
                return false;
        }
    }
    
    private boolean isValidPawnMove(ChessPiece piece, Move move, int rowDiff, int colDiff) {
        int direction = piece.color == PieceColor.WHITE ? -1 : 1;
        
        
        if (colDiff == 0 && getPiece(move.toRow, move.toCol) == null) {
            
            if (rowDiff == direction) {
                return true;
            }
            
            if (rowDiff == 2 * direction) {
                if ((piece.color == PieceColor.WHITE && move.fromRow == 6) || 
                    (piece.color == PieceColor.BLACK && move.fromRow == 1)) {
                    
                    int middleRow = move.fromRow + direction;
                    return getPiece(middleRow, move.fromCol) == null;
                }
            }
        }
        
        else if (Math.abs(colDiff) == 1 && rowDiff == direction) {
            ChessPiece target = getPiece(move.toRow, move.toCol);
            return target != null && target.color != piece.color;
        }
        
        return false;
    }
    
    private boolean isPathClear(Move move) {
        int rowStep = Integer.compare(move.toRow, move.fromRow);
        int colStep = Integer.compare(move.toCol, move.fromCol);
        
        int row = move.fromRow + rowStep;
        int col = move.fromCol + colStep;
        
        while (row != move.toRow || col != move.toCol) {
            if (getPiece(row, col) != null) return false;
            row += rowStep;
            col += colStep;
        }
        
        return true;
    }
    
    public void makeMove(Move move) {
        board[move.toRow][move.toCol] = board[move.fromRow][move.fromCol];
        board[move.fromRow][move.fromCol] = null;
    }
    
    public List<Move> getAllValidMoves(PieceColor color) {
        List<Move> moves = new ArrayList<>();
        
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = getPiece(fromRow, fromCol);
                if (piece != null && piece.color == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            Move move = new Move(fromRow, fromCol, toRow, toCol);
                            if (isValidMove(move)) {
                                moves.add(move);
                            }
                        }
                    }
                }
            }
        }
        
        return moves;
    }
    
    public boolean isCheckmate(PieceColor color) {
        if (!isKingInCheck(color)) return false;
        return getAllValidMoves(color).isEmpty();
    }
    
    public boolean isStalemate(PieceColor color) {
        if (isKingInCheck(color)) return false;
        return getAllValidMoves(color).isEmpty();
    }
    
    public boolean isDraw() {
        
        if (hasInsufficientMaterial()) return true;
        
        
        if (isStalemate(PieceColor.WHITE) || isStalemate(PieceColor.BLACK)) return true;
        
        return false;
    }
    
    private boolean hasInsufficientMaterial() {
        int whitePieces = 0, blackPieces = 0;
        boolean whiteHasPawn = false, blackHasPawn = false;
        boolean whiteHasRook = false, blackHasRook = false;
        boolean whiteHasQueen = false, blackHasQueen = false;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getPiece(row, col);
                if (piece != null) {
                    if (piece.color == PieceColor.WHITE) {
                        whitePieces++;
                        if (piece.type == PieceType.PAWN) whiteHasPawn = true;
                        if (piece.type == PieceType.ROOK) whiteHasRook = true;
                        if (piece.type == PieceType.QUEEN) whiteHasQueen = true;
                    } else {
                        blackPieces++;
                        if (piece.type == PieceType.PAWN) blackHasPawn = true;
                        if (piece.type == PieceType.ROOK) blackHasRook = true;
                        if (piece.type == PieceType.QUEEN) blackHasQueen = true;
                    }
                }
            }
        }
        
        
        if (whitePieces == 1 && blackPieces == 1) return true;
        
        
        if ((whitePieces == 2 && blackPieces == 1) || (whitePieces == 1 && blackPieces == 2)) {
            if (!whiteHasPawn && !blackHasPawn && !whiteHasRook && !blackHasRook && 
                !whiteHasQueen && !blackHasQueen) {
                return true;
            }
        }
        
        return false;
    }
    
    public String getFEN() {
        StringBuilder fen = new StringBuilder();
        
        
        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getPiece(row, col);
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    char pieceChar = getPieceChar(piece);
                    fen.append(pieceChar);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 7) fen.append("/");
        }
        
        return fen.toString();
    }
    
    private char getPieceChar(ChessPiece piece) {
        char base = piece.type.toString().charAt(0);
        return piece.color == PieceColor.WHITE ? Character.toUpperCase(base) : Character.toLowerCase(base);
    }
} 