package chess;

import java.util.*;
import java.awt.Point;

public class ChessBoard implements Cloneable {
    private ChessPiece[][] board = new ChessPiece[8][8];
    
    
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteKingsideRookMoved = false;
    private boolean whiteQueensideRookMoved = false;
    private boolean blackKingsideRookMoved = false;
    private boolean blackQueensideRookMoved = false;
    
   
    private int enPassantRow = -1;  
    private int enPassantCol = -1;  
    
    public ChessBoard() {
        setupBoard();
    }
    
    // Track move history for threefold repetition
    private List<String> moveHistory = new ArrayList<>();
    
    // Track half-move count for fifty-move rule
    private int halfMoveClock = 0;
    
    public boolean isInsufficientMaterial() {
        // Count pieces for each side
        Map<PieceColor, Set<PieceType>> pieces = new HashMap<>();
        pieces.put(PieceColor.WHITE, new HashSet<>());
        pieces.put(PieceColor.BLACK, new HashSet<>());
        int whitePieces = 0;
        int blackPieces = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getPiece(row, col);
                if (piece != null) {
                    pieces.get(piece.getColor()).add(piece.getType());
                    if (piece.getColor() == PieceColor.WHITE) {
                        whitePieces++;
                    } else {
                        blackPieces++;
                    }
                }
            }
        }
        
        // King vs King
        if (whitePieces == 1 && blackPieces == 1) {
            return true;
        }
        
        // King and bishop vs king
        if (whitePieces == 2 && blackPieces == 1) {
            if (pieces.get(PieceColor.WHITE).contains(PieceType.BISHOP)) {
                return true;
            }
        }
        if (blackPieces == 2 && whitePieces == 1) {
            if (pieces.get(PieceColor.BLACK).contains(PieceType.BISHOP)) {
                return true;
            }
        }
        
        // King and knight vs king
        if (whitePieces == 2 && blackPieces == 1) {
            if (pieces.get(PieceColor.WHITE).contains(PieceType.KNIGHT)) {
                return true;
            }
        }
        if (blackPieces == 2 && whitePieces == 1) {
            if (pieces.get(PieceColor.BLACK).contains(PieceType.KNIGHT)) {
                return true;
            }
        }
        
        // King and bishop vs king and bishop with bishops on same color
        if (whitePieces == 2 && blackPieces == 2) {
            if (pieces.get(PieceColor.WHITE).contains(PieceType.BISHOP) && 
                pieces.get(PieceColor.BLACK).contains(PieceType.BISHOP)) {
                // Check if both bishops are on the same color
                boolean whiteBishopOnWhite = false;
                boolean blackBishopOnWhite = false;
                
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        ChessPiece piece = getPiece(row, col);
                        if (piece != null && piece.getType() == PieceType.BISHOP) {
                            boolean isWhiteSquare = (row + col) % 2 == 0;
                            if (piece.getColor() == PieceColor.WHITE) {
                                whiteBishopOnWhite = isWhiteSquare;
                            } else {
                                blackBishopOnWhite = isWhiteSquare;
                            }
                        }
                    }
                }
                
                return whiteBishopOnWhite == blackBishopOnWhite;
            }
        }
        
        return false;
    }
    
    public boolean isThreefoldRepetition() {
        if (moveHistory.size() < 6) {
            return false; // Need at least 3 moves per player for repetition
        }
        
        String currentPosition = getPositionFEN();
        int repetitions = 0;
        
        for (String position : moveHistory) {
            if (position.equals(currentPosition)) {
                repetitions++;
                if (repetitions >= 3) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Helper method to get FEN-like position string (simplified)
    private String getPositionFEN() {
        StringBuilder fen = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            int emptySquares = 0;
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getPiece(row, col);
                if (piece == null) {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    fen.append(getPieceChar(piece));
                }
            }
            if (emptySquares > 0) {
                fen.append(emptySquares);
            }
            if (row < 7) {
                fen.append('/');
            }
        }
        return fen.toString();
    }
    
    public ChessBoard(ChessBoard other) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = other.board[row][col];
                if (piece != null) {
                    this.board[row][col] = new ChessPiece(piece.getType(), piece.getColor());
                }
            }
        }
       
        this.whiteKingMoved = other.whiteKingMoved;
        this.blackKingMoved = other.blackKingMoved;
        this.whiteKingsideRookMoved = other.whiteKingsideRookMoved;
        this.whiteQueensideRookMoved = other.whiteQueensideRookMoved;
        this.blackKingsideRookMoved = other.blackKingsideRookMoved;
        this.blackQueensideRookMoved = other.blackQueensideRookMoved;
        
        
        this.enPassantRow = other.enPassantRow;
        this.enPassantCol = other.enPassantCol;
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
        if (target != null && target.getColor() == piece.getColor()) return false;
        
        // Check for castling BEFORE checking if move would expose king
        if (piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) == 2) {
            return isValidCastlingMove(piece.getColor(), move);
        }
        
        
        if (piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) <= 1 && Math.abs(move.toRow - move.fromRow) <= 1) {
            if (wouldSquareBeUnderAttack(piece.getColor(), move.toRow, move.toCol)) {
                return false;
            }
        }
        
        
        if (wouldMoveExposeKing(piece.getColor(), move)) return false;
        
        return isValidMoveForPiece(piece, move);
    }
    
    private boolean isValidCastlingMove(PieceColor color, Move move) {
        int kingRow = (color == PieceColor.WHITE) ? 7 : 0;
        int kingCol = 4;
        
        
        if (move.fromRow != kingRow || move.fromCol != kingCol) return false;
        
        
        if ((color == PieceColor.WHITE && whiteKingMoved) || 
            (color == PieceColor.BLACK && blackKingMoved)) return false;
        
       
        if (isKingInCheck(color)) return false;
        
       
        boolean kingside = move.toCol == 6;
        boolean queenside = move.toCol == 2;
        
        if (!kingside && !queenside) return false;
        
       
        int rookCol = kingside ? 7 : 0;
        ChessPiece rook = getPiece(kingRow, rookCol);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color) return false;
        
        if (kingside) {
            if ((color == PieceColor.WHITE && whiteKingsideRookMoved) || 
                (color == PieceColor.BLACK && blackKingsideRookMoved)) return false;
        } else {
            if ((color == PieceColor.WHITE && whiteQueensideRookMoved) || 
                (color == PieceColor.BLACK && blackQueensideRookMoved)) return false;
        }
        
       
        int startCol = Math.min(kingCol, rookCol) + 1;
        int endCol = Math.max(kingCol, rookCol);
        for (int col = startCol; col < endCol; col++) {
            if (getPiece(kingRow, col) != null) return false;
        }
        
        int[] checkSquares = kingside ? new int[]{5, 6} : new int[]{3, 2};
        
        for (int col : checkSquares) {
            if (wouldSquareBeUnderAttack(color, kingRow, col)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean wouldSquareBeUnderAttack(PieceColor kingColor, int row, int col) {
        
        ChessPiece originalPiece = board[row][col];
        board[row][col] = new ChessPiece(PieceType.KING, kingColor);
        
        boolean underAttack = false;
        PieceColor opponentColor = (kingColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece piece = getPiece(r, c);
                if (piece != null && piece.getColor() == opponentColor) {
                    Move attackMove = new Move(r, c, row, col);
                    if (isValidMoveForPiece(piece, attackMove)) {
                        underAttack = true;
                        break;
                    }
                }
            }
            if (underAttack) break;
        }
        
        
        board[row][col] = originalPiece;
        
        return underAttack;
    }
    
    private boolean wouldMoveExposeKing(PieceColor color, Move move) {
        
        ChessPiece captured = board[move.toRow][move.toCol];
        ChessPiece moving = board[move.fromRow][move.fromCol];
        board[move.toRow][move.toCol] = moving;
        board[move.fromRow][move.fromCol] = null;
        
        
        boolean inCheck = isKingInCheck(color);
        
        
        board[move.fromRow][move.fromCol] = moving;
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
                if (piece != null && piece.getColor() == opponentColor) {
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
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
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
        
        switch (piece.getType()) {
            case PAWN:
                return isValidPawnMove(piece, move, rowDiff, colDiff);
            case ROOK:
                return (rowDiff == 0 || colDiff == 0) && isPathClear(move);
            case BISHOP:
                return Math.abs(rowDiff) == Math.abs(colDiff) && rowDiff != 0 && isPathClear(move);
            case QUEEN:
                return (rowDiff == 0 || colDiff == 0 || Math.abs(rowDiff) == Math.abs(colDiff)) && isPathClear(move);
            case KING:
                
                
                if (Math.abs(colDiff) == 2 && rowDiff == 0) return false;
                return Math.abs(rowDiff) <= 1 && Math.abs(colDiff) <= 1;
            case KNIGHT:
                return (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 1) || 
                       (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 2);
            default:
                return false;
        }
    }
    
    private boolean isValidPawnMove(ChessPiece piece, Move move, int rowDiff, int colDiff) {
        int direction = piece.getColor() == PieceColor.WHITE ? -1 : 1;
        
        
        if (colDiff == 0 && getPiece(move.toRow, move.toCol) == null) {
            
            if (rowDiff == direction) {
                return true;
            }
            
            if (rowDiff == 2 * direction) {
                if ((piece.getColor() == PieceColor.WHITE && move.fromRow == 6) || 
                    (piece.getColor() == PieceColor.BLACK && move.fromRow == 1)) {
                    
                    int middleRow = move.fromRow + direction;
                    return getPiece(middleRow, move.fromCol) == null;
                }
            }
        }
        
        else if (Math.abs(colDiff) == 1 && rowDiff == direction) {
            ChessPiece target = getPiece(move.toRow, move.toCol);
            
            
            if (target != null && target.getColor() != piece.getColor()) {
                return true;
            }
            
            
            if (target == null && move.toRow == enPassantRow && move.toCol == enPassantCol && 
                enPassantRow != -1 && enPassantCol != -1) {
                
                int capturedPawnRow = move.fromRow; 
                int capturedPawnCol = move.toCol;   
                ChessPiece capturedPawn = getPiece(capturedPawnRow, capturedPawnCol);
                
                return capturedPawn != null && 
                       capturedPawn.getType() == PieceType.PAWN && 
                       capturedPawn.getColor() != piece.getColor();
            }
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
        
        enPassantRow = -1;
        enPassantCol = -1;

        ChessPiece piece = getPiece(move.fromRow, move.fromCol);
        
        
        updateCastlingState(piece, move);
        
        
        if (piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) == 2) {
            executeCastling(move);
        } else {
            
            board[move.toRow][move.toCol] = board[move.fromRow][move.fromCol];
            board[move.fromRow][move.fromCol] = null;
            
            
            if (move.isEnPassant) {
                int capturedPawnRow = move.fromRow;
                int capturedPawnCol = move.toCol;
                board[capturedPawnRow][capturedPawnCol] = null;
                System.out.println("En passant capture executed by pawn at (" + move.fromRow + "," + move.fromCol + ") to (" + move.toRow + "," + move.toCol + ")");
            }
            
            
            if (piece.getType() == PieceType.PAWN && 
                ((piece.getColor() == PieceColor.WHITE && move.toRow == 0) ||
                 (piece.getColor() == PieceColor.BLACK && move.toRow == 7))) {
                
                
                PieceType promotionType = (move.promotion != null) ? move.promotion : PieceType.QUEEN;
                board[move.toRow][move.toCol] = new ChessPiece(promotionType, piece.getColor());
            }
            
            if (piece.getType() == PieceType.PAWN && Math.abs(move.toRow - move.fromRow) == 2) {
                enPassantRow = (move.fromRow + move.toRow) / 2; 
                enPassantCol = move.fromCol;
                System.out.println("En passant target set at: (" + enPassantRow + "," + enPassantCol + ")");
            }
        }
        
    
        ChessPiece capturedPiece = getPiece(move.toRow, move.toCol);
        if (capturedPiece != null && capturedPiece.getType() == PieceType.ROOK) {
            updateCastlingStateForCapturedRook(capturedPiece.getColor(), move.toRow, move.toCol);
        }
    }
    
    private void updateCastlingState(ChessPiece piece, Move move) {
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == PieceColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        } else if (piece.getType() == PieceType.ROOK) {
            if (piece.getColor() == PieceColor.WHITE) {
                if (move.fromRow == 7 && move.fromCol == 7) whiteKingsideRookMoved = true;
                if (move.fromRow == 7 && move.fromCol == 0) whiteQueensideRookMoved = true;
            } else {
                if (move.fromRow == 0 && move.fromCol == 7) blackKingsideRookMoved = true;
                if (move.fromRow == 0 && move.fromCol == 0) blackQueensideRookMoved = true;
            }
        }
    }
    
    private void updateCastlingStateForCapturedRook(PieceColor rookColor, int row, int col) {
        if (rookColor == PieceColor.WHITE) {
            if (row == 7 && col == 7) whiteKingsideRookMoved = true;
            if (row == 7 && col == 0) whiteQueensideRookMoved = true;
        } else {
            if (row == 0 && col == 7) blackKingsideRookMoved = true;
            if (row == 0 && col == 0) blackQueensideRookMoved = true;
        }
    }
    
    private void executeCastling(Move move) {
        int kingRow = move.fromRow;
        int kingCol = move.fromCol;
        int newKingCol = move.toCol;
        
        boolean kingside = newKingCol > kingCol;
        
        
        board[kingRow][newKingCol] = board[kingRow][kingCol];
        board[kingRow][kingCol] = null;
        
        
        int rookCol = kingside ? 7 : 0;
        int newRookCol = kingside ? newKingCol - 1 : newKingCol + 1;
        
        board[kingRow][newRookCol] = board[kingRow][rookCol];
        board[kingRow][rookCol] = null;
    }
    
    public List<Move> getAllValidMoves(PieceColor color) {
        List<Move> moves = new ArrayList<>();
        
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = getPiece(fromRow, fromCol);
                if (piece != null && piece.getColor() == color) {
                    
                    if (piece.getType() == PieceType.PAWN && enPassantRow != -1 && enPassantCol != -1) {
                        int direction = (piece.getColor() == PieceColor.WHITE) ? -1 : 1;
                        if (fromRow + direction == enPassantRow && Math.abs(fromCol - enPassantCol) == 1) {
                            Move epMove = new Move(fromRow, fromCol, enPassantRow, enPassantCol, true);
                            if (isValidMove(epMove)) {
                                System.out.println("En passant move generated for pawn at (" + fromRow + "," + fromCol + ") to (" + enPassantRow + "," + enPassantCol + ")");
                                moves.add(epMove);
                            }
                        }
                    }
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            
                            if (piece.getType() == PieceType.PAWN && enPassantRow != -1 && enPassantCol != -1 &&
                                toRow == enPassantRow && toCol == enPassantCol) {
                                continue;
                            }
                            Move move = new Move(fromRow, fromCol, toRow, toCol);
                            if (isValidMove(move)) {
                                
                                if (piece.getType() == PieceType.PAWN && 
                                    ((piece.getColor() == PieceColor.WHITE && toRow == 0) ||
                                     (piece.getColor() == PieceColor.BLACK && toRow == 7))) {
                                    
                                    moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.QUEEN));
                                    moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.ROOK));
                                    moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.BISHOP));
                                    moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.KNIGHT));
                                } else {
                                    moves.add(move);
                                }
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
    
    /**
     * Checks if the game is over (checkmate, stalemate, or draw).
     * @return true if the game is over, false otherwise
     */
    public boolean isGameOver() {
        return isCheckmate(PieceColor.WHITE) || 
               isCheckmate(PieceColor.BLACK) || 
               isDraw();
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
                    if (piece.getColor() == PieceColor.WHITE) {
                        whitePieces++;
                        if (piece.getType() == PieceType.PAWN) whiteHasPawn = true;
                        if (piece.getType() == PieceType.ROOK) whiteHasRook = true;
                        if (piece.getType() == PieceType.QUEEN) whiteHasQueen = true;
                    } else {
                        blackPieces++;
                        if (piece.getType() == PieceType.PAWN) blackHasPawn = true;
                        if (piece.getType() == PieceType.ROOK) blackHasRook = true;
                        if (piece.getType() == PieceType.QUEEN) blackHasQueen = true;
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
        char base;
        switch (piece.getType()) {
            case KING: base = 'K'; break;
            case QUEEN: base = 'Q'; break;
            case ROOK: base = 'R'; break;
            case BISHOP: base = 'B'; break;
            case KNIGHT: base = 'N'; break;
            case PAWN: base = 'P'; break;
            default: base = '?'; break;
        }
        return piece.getColor() == PieceColor.WHITE ? base : Character.toLowerCase(base);
    }
    
    public boolean isCastlingAvailable(PieceColor color, boolean kingside) {
        int kingRow = (color == PieceColor.WHITE) ? 7 : 0;
        int kingCol = 4;
        int kingDestCol = kingside ? 6 : 2;
        
        Move castlingMove = new Move(kingRow, kingCol, kingRow, kingDestCol);
        return isValidCastlingMove(color, castlingMove);
    }
    
    
    public int getEnPassantRow() {
        return enPassantRow;
    }
    
    public int getEnPassantCol() {
        return enPassantCol;
    }
   
    public boolean hasWhiteKingMoved() { return whiteKingMoved; }
    public boolean hasWhiteKingsideRookMoved() { return whiteKingsideRookMoved; }
    public boolean hasWhiteQueensideRookMoved() { return whiteQueensideRookMoved; }
    public boolean hasBlackKingMoved() { return blackKingMoved; }
    public boolean hasBlackKingsideRookMoved() { return blackKingsideRookMoved; }
    public boolean hasBlackQueensideRookMoved() { return blackQueensideRookMoved; }

    public boolean isSquareUnderAttack(int row, int col, PieceColor byColor) {
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece piece = getPiece(r, c);
                if (piece != null && piece.getColor() == byColor) {
                    Move move = new Move(r, c, row, col);
                    if (isValidMove(move)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}