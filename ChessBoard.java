import java.util.*;
import java.awt.Point;

public class ChessBoard implements Cloneable {
    private ChessPiece[][] board = new ChessPiece[8][8];

    // Castling state
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteKingsideRookMoved = false;
    private boolean whiteQueensideRookMoved = false;
    private boolean blackKingsideRookMoved = false;
    private boolean blackQueensideRookMoved = false;

    // En passant state
    private int enPassantRow = -1;
    private int enPassantCol = -1;

    // Cached king positions — updated in makeMove/unmakeMove
    private int whiteKingRow = 7, whiteKingCol = 4;
    private int blackKingRow = 0, blackKingCol = 4;

    // Zobrist hashing
    private long zobristHash = 0;
    private static final long[][][] ZOBRIST_TABLE = new long[2][6][64]; // [color][piece][square]
    private static final long ZOBRIST_SIDE; // XOR'd when it's black's turn
    private static final long[] ZOBRIST_CASTLING = new long[16];
    private static final long[] ZOBRIST_EN_PASSANT = new long[9]; // 8 cols + 1 for "none"

    static {
        Random rng = new Random(0xDEADBEEF); // fixed seed for reproducibility
        for (int c = 0; c < 2; c++)
            for (int p = 0; p < 6; p++)
                for (int sq = 0; sq < 64; sq++)
                    ZOBRIST_TABLE[c][p][sq] = rng.nextLong();
        ZOBRIST_SIDE = rng.nextLong();
        for (int i = 0; i < 16; i++)
            ZOBRIST_CASTLING[i] = rng.nextLong();
        for (int i = 0; i < 9; i++)
            ZOBRIST_EN_PASSANT[i] = rng.nextLong();
    }

    public ChessBoard() {
        setupBoard();
        zobristHash = computeFullHash();
    }

    public ChessBoard(ChessBoard other) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                this.board[row][col] = other.board[row][col]; // ChessPiece is immutable now
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
        this.whiteKingRow = other.whiteKingRow;
        this.whiteKingCol = other.whiteKingCol;
        this.blackKingRow = other.blackKingRow;
        this.blackKingCol = other.blackKingCol;
        this.zobristHash = other.zobristHash;
    }

    private void setupBoard() {
        for (int col = 0; col < 8; col++) {
            board[1][col] = new ChessPiece(PieceType.PAWN, PieceColor.BLACK);
            board[6][col] = new ChessPiece(PieceType.PAWN, PieceColor.WHITE);
        }
        PieceType[] backRow = { PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK };
        for (int col = 0; col < 8; col++) {
            board[0][col] = new ChessPiece(backRow[col], PieceColor.BLACK);
            board[7][col] = new ChessPiece(backRow[col], PieceColor.WHITE);
        }
        whiteKingRow = 7;
        whiteKingCol = 4;
        blackKingRow = 0;
        blackKingCol = 4;
    }

    @Override
    public ChessBoard clone() {
        return new ChessBoard(this);
    }

    // ---- Zobrist helpers ----

    private int zobristPieceIndex(ChessPiece p) {
        return p.getType().ordinal();
    }

    private int zobristColorIndex(ChessPiece p) {
        return p.getColor().ordinal();
    }

    private void zobristTogglePiece(ChessPiece p, int row, int col) {
        zobristHash ^= ZOBRIST_TABLE[zobristColorIndex(p)][zobristPieceIndex(p)][row * 8 + col];
    }

    private int castlingFlags() {
        int flags = 0;
        if (!whiteKingMoved && !whiteKingsideRookMoved)
            flags |= 1;
        if (!whiteKingMoved && !whiteQueensideRookMoved)
            flags |= 2;
        if (!blackKingMoved && !blackKingsideRookMoved)
            flags |= 4;
        if (!blackKingMoved && !blackQueensideRookMoved)
            flags |= 8;
        return flags;
    }

    private long computeFullHash() {
        long h = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
                if (p != null)
                    h ^= ZOBRIST_TABLE[p.getColor().ordinal()][p.getType().ordinal()][r * 8 + c];
            }
        h ^= ZOBRIST_CASTLING[castlingFlags()];
        h ^= ZOBRIST_EN_PASSANT[enPassantCol >= 0 ? enPassantCol : 8];
        return h;
    }

    public long getZobristHash() {
        return zobristHash;
    }

    // ---- Board access ----

    public ChessPiece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8)
            return null;
        return board[row][col];
    }

    // ---- Move validation ----

    public boolean isValidMove(Move move) {
        ChessPiece piece = getPiece(move.fromRow, move.fromCol);
        if (piece == null)
            return false;

        ChessPiece target = getPiece(move.toRow, move.toCol);
        if (target != null && target.getColor() == piece.getColor())
            return false;

        if (piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) == 2) {
            return isValidCastlingMove(piece.getColor(), move);
        }

        if (piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) <= 1
                && Math.abs(move.toRow - move.fromRow) <= 1) {
            if (wouldSquareBeUnderAttack(piece.getColor(), move.toRow, move.toCol)) {
                return false;
            }
        }

        if (wouldMoveExposeKing(piece.getColor(), move))
            return false;

        return isValidMoveForPiece(piece, move);
    }

    private boolean isValidCastlingMove(PieceColor color, Move move) {
        int kingRow = (color == PieceColor.WHITE) ? 7 : 0;
        if (move.fromRow != kingRow || move.fromCol != 4)
            return false;
        if ((color == PieceColor.WHITE && whiteKingMoved) || (color == PieceColor.BLACK && blackKingMoved))
            return false;
        if (isKingInCheck(color))
            return false;

        boolean kingside = move.toCol == 6;
        boolean queenside = move.toCol == 2;
        if (!kingside && !queenside)
            return false;

        int rookCol = kingside ? 7 : 0;
        ChessPiece rook = getPiece(kingRow, rookCol);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColor() != color)
            return false;

        if (kingside) {
            if ((color == PieceColor.WHITE && whiteKingsideRookMoved)
                    || (color == PieceColor.BLACK && blackKingsideRookMoved))
                return false;
        } else {
            if ((color == PieceColor.WHITE && whiteQueensideRookMoved)
                    || (color == PieceColor.BLACK && blackQueensideRookMoved))
                return false;
        }

        int startCol = Math.min(4, rookCol) + 1;
        int endCol = Math.max(4, rookCol);
        for (int col = startCol; col < endCol; col++) {
            if (getPiece(kingRow, col) != null)
                return false;
        }

        int[] checkSquares = kingside ? new int[] { 5, 6 } : new int[] { 3, 2 };
        for (int col : checkSquares) {
            if (wouldSquareBeUnderAttack(color, kingRow, col))
                return false;
        }
        return true;
    }

    private boolean wouldSquareBeUnderAttack(PieceColor kingColor, int row, int col) {
        ChessPiece originalPiece = board[row][col];
        board[row][col] = new ChessPiece(PieceType.KING, kingColor);

        boolean underAttack = false;
        PieceColor opponentColor = kingColor.opposite();

        for (int r = 0; r < 8 && !underAttack; r++) {
            for (int c = 0; c < 8 && !underAttack; c++) {
                ChessPiece piece = board[r][c];
                if (piece != null && piece.getColor() == opponentColor) {
                    if (isValidMoveForPiece(piece, new Move(r, c, row, col))) {
                        underAttack = true;
                    }
                }
            }
        }

        board[row][col] = originalPiece;
        return underAttack;
    }

    private boolean wouldMoveExposeKing(PieceColor color, Move move) {
        ChessPiece captured = board[move.toRow][move.toCol];
        ChessPiece moving = board[move.fromRow][move.fromCol];
        board[move.toRow][move.toCol] = moving;
        board[move.fromRow][move.fromCol] = null;

        // Temporarily update king position if king is moving
        int oldKingRow = -1, oldKingCol = -1;
        if (moving.getType() == PieceType.KING) {
            if (color == PieceColor.WHITE) {
                oldKingRow = whiteKingRow;
                oldKingCol = whiteKingCol;
                whiteKingRow = move.toRow;
                whiteKingCol = move.toCol;
            } else {
                oldKingRow = blackKingRow;
                oldKingCol = blackKingCol;
                blackKingRow = move.toRow;
                blackKingCol = move.toCol;
            }
        }

        boolean inCheck = isKingInCheck(color);

        // Restore
        board[move.fromRow][move.fromCol] = moving;
        board[move.toRow][move.toCol] = captured;
        if (moving.getType() == PieceType.KING) {
            if (color == PieceColor.WHITE) {
                whiteKingRow = oldKingRow;
                whiteKingCol = oldKingCol;
            } else {
                blackKingRow = oldKingRow;
                blackKingCol = oldKingCol;
            }
        }

        return inCheck;
    }

    public boolean isKingInCheck(PieceColor color) {
        int kingRow = (color == PieceColor.WHITE) ? whiteKingRow : blackKingRow;
        int kingCol = (color == PieceColor.WHITE) ? whiteKingCol : blackKingCol;

        PieceColor opponentColor = color.opposite();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null && piece.getColor() == opponentColor) {
                    if (isValidMoveForPiece(piece, new Move(row, col, kingRow, kingCol))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Point findKing(PieceColor color) {
        if (color == PieceColor.WHITE)
            return new Point(whiteKingRow, whiteKingCol);
        return new Point(blackKingRow, blackKingCol);
    }

    private boolean isValidMoveForPiece(ChessPiece piece, Move move) {
        int rowDiff = move.toRow - move.fromRow;
        int colDiff = move.toCol - move.fromCol;
        if (rowDiff == 0 && colDiff == 0)
            return false;

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
                if (Math.abs(colDiff) == 2 && rowDiff == 0)
                    return false;
                return Math.abs(rowDiff) <= 1 && Math.abs(colDiff) <= 1;
            case KNIGHT:
                return (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 1)
                        || (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 2);
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(ChessPiece piece, Move move, int rowDiff, int colDiff) {
        int direction = piece.getColor() == PieceColor.WHITE ? -1 : 1;

        if (colDiff == 0 && getPiece(move.toRow, move.toCol) == null) {
            if (rowDiff == direction)
                return true;
            if (rowDiff == 2 * direction) {
                if ((piece.getColor() == PieceColor.WHITE && move.fromRow == 6) ||
                        (piece.getColor() == PieceColor.BLACK && move.fromRow == 1)) {
                    return getPiece(move.fromRow + direction, move.fromCol) == null;
                }
            }
        } else if (Math.abs(colDiff) == 1 && rowDiff == direction) {
            ChessPiece target = getPiece(move.toRow, move.toCol);
            if (target != null && target.getColor() != piece.getColor())
                return true;

            if (target == null && move.toRow == enPassantRow && move.toCol == enPassantCol &&
                    enPassantRow != -1 && enPassantCol != -1) {
                ChessPiece capturedPawn = getPiece(move.fromRow, move.toCol);
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
            if (board[row][col] != null)
                return false;
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    // ---- UndoInfo for make/unmake ----

    public static class UndoInfo {
        final ChessPiece capturedPiece;
        final int capturedRow, capturedCol; // may differ from toRow/toCol for en passant
        final boolean prevWhiteKingMoved, prevBlackKingMoved;
        final boolean prevWKR, prevWQR, prevBKR, prevBQR; // rook moved flags
        final int prevEnPassantRow, prevEnPassantCol;
        final int prevWhiteKingRow, prevWhiteKingCol, prevBlackKingRow, prevBlackKingCol;
        final long prevHash;

        UndoInfo(ChessPiece capturedPiece, int capturedRow, int capturedCol,
                boolean wk, boolean bk, boolean wkr, boolean wqr, boolean bkr, boolean bqr,
                int epRow, int epCol, int wkRow, int wkCol, int bkRow, int bkCol, long hash) {
            this.capturedPiece = capturedPiece;
            this.capturedRow = capturedRow;
            this.capturedCol = capturedCol;
            this.prevWhiteKingMoved = wk;
            this.prevBlackKingMoved = bk;
            this.prevWKR = wkr;
            this.prevWQR = wqr;
            this.prevBKR = bkr;
            this.prevBQR = bqr;
            this.prevEnPassantRow = epRow;
            this.prevEnPassantCol = epCol;
            this.prevWhiteKingRow = wkRow;
            this.prevWhiteKingCol = wkCol;
            this.prevBlackKingRow = bkRow;
            this.prevBlackKingCol = bkCol;
            this.prevHash = hash;
        }
    }

    // ---- Make Move (returns UndoInfo) ----

    public UndoInfo makeMove(Move move) {
        // Save full state for undo
        ChessPiece captured = null;
        int capturedRow = move.toRow, capturedCol = move.toCol;
        UndoInfo undo = new UndoInfo(
                null, capturedRow, capturedCol, // captured filled below
                whiteKingMoved, blackKingMoved,
                whiteKingsideRookMoved, whiteQueensideRookMoved,
                blackKingsideRookMoved, blackQueensideRookMoved,
                enPassantRow, enPassantCol,
                whiteKingRow, whiteKingCol, blackKingRow, blackKingCol,
                zobristHash);

        // Remove old en passant from hash
        zobristHash ^= ZOBRIST_EN_PASSANT[enPassantCol >= 0 ? enPassantCol : 8];
        // Remove old castling from hash
        zobristHash ^= ZOBRIST_CASTLING[castlingFlags()];

        ChessPiece piece = board[move.fromRow][move.fromCol];

        // BUG FIX: Save captured piece BEFORE overwriting destination
        captured = board[move.toRow][move.toCol];
        if (captured != null && captured.getType() == PieceType.ROOK) {
            updateCastlingStateForCapturedRook(captured.getColor(), move.toRow, move.toCol);
        }

        enPassantRow = -1;
        enPassantCol = -1;

        updateCastlingState(piece, move);

        if (piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) == 2) {
            executeCastling(move, piece);
        } else {
            // Remove piece from source
            zobristTogglePiece(piece, move.fromRow, move.fromCol);
            // Remove captured piece from dest
            if (captured != null) {
                zobristTogglePiece(captured, move.toRow, move.toCol);
            }

            board[move.toRow][move.toCol] = board[move.fromRow][move.fromCol];
            board[move.fromRow][move.fromCol] = null;

            // En passant capture
            if (move.isEnPassant) {
                int captPawnRow = move.fromRow;
                int captPawnCol = move.toCol;
                ChessPiece epPawn = board[captPawnRow][captPawnCol];
                if (epPawn != null) {
                    zobristTogglePiece(epPawn, captPawnRow, captPawnCol);
                }
                captured = board[captPawnRow][captPawnCol];
                capturedRow = captPawnRow;
                capturedCol = captPawnCol;
                board[captPawnRow][captPawnCol] = null;
            }

            // Pawn promotion
            if (piece.getType() == PieceType.PAWN &&
                    ((piece.getColor() == PieceColor.WHITE && move.toRow == 0) ||
                            (piece.getColor() == PieceColor.BLACK && move.toRow == 7))) {
                PieceType promotionType = (move.promotion != null) ? move.promotion : PieceType.QUEEN;
                ChessPiece promoted = new ChessPiece(promotionType, piece.getColor());
                // Remove pawn hash, add promoted piece hash
                zobristTogglePiece(piece, move.toRow, move.toCol);
                board[move.toRow][move.toCol] = promoted;
                zobristTogglePiece(promoted, move.toRow, move.toCol);
            } else {
                // Add piece at destination
                zobristTogglePiece(piece, move.toRow, move.toCol);
            }

            // Set en passant target for double pawn push
            if (piece.getType() == PieceType.PAWN && Math.abs(move.toRow - move.fromRow) == 2) {
                enPassantRow = (move.fromRow + move.toRow) / 2;
                enPassantCol = move.fromCol;
            }
        }

        // Update king position cache
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == PieceColor.WHITE) {
                whiteKingRow = move.toRow;
                whiteKingCol = move.toCol;
            } else {
                blackKingRow = move.toRow;
                blackKingCol = move.toCol;
            }
        }

        // Add new castling + en passant to hash
        zobristHash ^= ZOBRIST_CASTLING[castlingFlags()];
        zobristHash ^= ZOBRIST_EN_PASSANT[enPassantCol >= 0 ? enPassantCol : 8];

        // Return UndoInfo with actual captured piece
        return new UndoInfo(
                captured, capturedRow, capturedCol,
                undo.prevWhiteKingMoved, undo.prevBlackKingMoved,
                undo.prevWKR, undo.prevWQR, undo.prevBKR, undo.prevBQR,
                undo.prevEnPassantRow, undo.prevEnPassantCol,
                undo.prevWhiteKingRow, undo.prevWhiteKingCol,
                undo.prevBlackKingRow, undo.prevBlackKingCol,
                undo.prevHash);
    }

    public void unmakeMove(Move move, UndoInfo undo) {
        ChessPiece piece = board[move.toRow][move.toCol];

        // Handle promotion — the piece at dest is the promoted piece, restore pawn
        if (move.promotion != null || (piece != null && piece.getType() != PieceType.PAWN &&
                board[move.toRow][move.toCol] != null)) {
            // Check if this was a promotion move
            ChessPiece originalPiece = board[move.toRow][move.toCol];
            if (move.promotion != null) {
                // Restore pawn
                board[move.toRow][move.toCol] = new ChessPiece(PieceType.PAWN, originalPiece.getColor());
                piece = board[move.toRow][move.toCol];
            }
        }

        // Handle castling unmake
        if (piece != null && piece.getType() == PieceType.KING && Math.abs(move.toCol - move.fromCol) == 2) {
            boolean kingside = move.toCol > move.fromCol;
            int kingRow = move.fromRow;
            // Move king back
            board[kingRow][move.fromCol] = board[kingRow][move.toCol];
            board[kingRow][move.toCol] = null;
            // Move rook back
            int rookNewCol = kingside ? move.toCol - 1 : move.toCol + 1;
            int rookOrigCol = kingside ? 7 : 0;
            board[kingRow][rookOrigCol] = board[kingRow][rookNewCol];
            board[kingRow][rookNewCol] = null;
        } else {
            // Normal move — move piece back
            board[move.fromRow][move.fromCol] = piece;
            board[move.toRow][move.toCol] = null;

            // Restore captured piece
            if (undo.capturedPiece != null) {
                board[undo.capturedRow][undo.capturedCol] = undo.capturedPiece;
            }
        }

        // Restore all state
        whiteKingMoved = undo.prevWhiteKingMoved;
        blackKingMoved = undo.prevBlackKingMoved;
        whiteKingsideRookMoved = undo.prevWKR;
        whiteQueensideRookMoved = undo.prevWQR;
        blackKingsideRookMoved = undo.prevBKR;
        blackQueensideRookMoved = undo.prevBQR;
        enPassantRow = undo.prevEnPassantRow;
        enPassantCol = undo.prevEnPassantCol;
        whiteKingRow = undo.prevWhiteKingRow;
        whiteKingCol = undo.prevWhiteKingCol;
        blackKingRow = undo.prevBlackKingRow;
        blackKingCol = undo.prevBlackKingCol;
        zobristHash = undo.prevHash;
    }

    private void updateCastlingState(ChessPiece piece, Move move) {
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == PieceColor.WHITE)
                whiteKingMoved = true;
            else
                blackKingMoved = true;
        } else if (piece.getType() == PieceType.ROOK) {
            if (piece.getColor() == PieceColor.WHITE) {
                if (move.fromRow == 7 && move.fromCol == 7)
                    whiteKingsideRookMoved = true;
                if (move.fromRow == 7 && move.fromCol == 0)
                    whiteQueensideRookMoved = true;
            } else {
                if (move.fromRow == 0 && move.fromCol == 7)
                    blackKingsideRookMoved = true;
                if (move.fromRow == 0 && move.fromCol == 0)
                    blackQueensideRookMoved = true;
            }
        }
    }

    private void updateCastlingStateForCapturedRook(PieceColor rookColor, int row, int col) {
        if (rookColor == PieceColor.WHITE) {
            if (row == 7 && col == 7)
                whiteKingsideRookMoved = true;
            if (row == 7 && col == 0)
                whiteQueensideRookMoved = true;
        } else {
            if (row == 0 && col == 7)
                blackKingsideRookMoved = true;
            if (row == 0 && col == 0)
                blackQueensideRookMoved = true;
        }
    }

    private void executeCastling(Move move, ChessPiece king) {
        int kingRow = move.fromRow;
        boolean kingside = move.toCol > move.fromCol;

        // Remove king from hash at old pos
        zobristTogglePiece(king, kingRow, move.fromCol);
        board[kingRow][move.toCol] = board[kingRow][move.fromCol];
        board[kingRow][move.fromCol] = null;
        // Add king at new pos
        zobristTogglePiece(king, kingRow, move.toCol);

        int rookCol = kingside ? 7 : 0;
        int newRookCol = kingside ? move.toCol - 1 : move.toCol + 1;
        ChessPiece rook = board[kingRow][rookCol];

        // Remove rook from hash at old pos
        zobristTogglePiece(rook, kingRow, rookCol);
        board[kingRow][newRookCol] = board[kingRow][rookCol];
        board[kingRow][rookCol] = null;
        // Add rook at new pos
        zobristTogglePiece(rook, kingRow, newRookCol);
    }

    // ---- Per-piece move generation ----

    public List<Move> getAllValidMoves(PieceColor color) {
        List<Move> moves = new ArrayList<>(64);

        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = board[fromRow][fromCol];
                if (piece == null || piece.getColor() != color)
                    continue;

                switch (piece.getType()) {
                    case PAWN:
                        generatePawnMoves(piece, fromRow, fromCol, moves);
                        break;
                    case KNIGHT:
                        generateKnightMoves(piece, fromRow, fromCol, moves);
                        break;
                    case BISHOP:
                        generateSlidingMoves(piece, fromRow, fromCol, moves, BISHOP_DIRS);
                        break;
                    case ROOK:
                        generateSlidingMoves(piece, fromRow, fromCol, moves, ROOK_DIRS);
                        break;
                    case QUEEN:
                        generateSlidingMoves(piece, fromRow, fromCol, moves, QUEEN_DIRS);
                        break;
                    case KING:
                        generateKingMoves(piece, fromRow, fromCol, moves);
                        break;
                }
            }
        }
        return moves;
    }

    private static final int[][] KNIGHT_OFFSETS = { { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 }, { 1, -2 }, { 1, 2 },
            { 2, -1 }, { 2, 1 } };
    private static final int[][] KING_OFFSETS = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 },
            { 1, 0 }, { 1, 1 } };
    private static final int[][] BISHOP_DIRS = { { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 } };
    private static final int[][] ROOK_DIRS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    private static final int[][] QUEEN_DIRS = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 },
            { 1, 0 }, { 1, 1 } };

    private void generatePawnMoves(ChessPiece piece, int row, int col, List<Move> moves) {
        int dir = piece.getColor() == PieceColor.WHITE ? -1 : 1;
        int startRow = piece.getColor() == PieceColor.WHITE ? 6 : 1;
        int promoRow = piece.getColor() == PieceColor.WHITE ? 0 : 7;

        // Forward one
        int nr = row + dir;
        if (nr >= 0 && nr < 8 && board[nr][col] == null) {
            addPawnMove(piece, row, col, nr, col, promoRow, moves);
            // Forward two from start
            int nr2 = row + 2 * dir;
            if (row == startRow && board[nr2][col] == null) {
                moves.add(new Move(row, col, nr2, col));
            }
        }

        // Captures
        for (int dc = -1; dc <= 1; dc += 2) {
            int nc = col + dc;
            if (nc < 0 || nc >= 8 || nr < 0 || nr >= 8)
                continue;
            ChessPiece target = board[nr][nc];
            if (target != null && target.getColor() != piece.getColor()) {
                addPawnMove(piece, row, col, nr, nc, promoRow, moves);
            }
            // En passant
            if (target == null && nr == enPassantRow && nc == enPassantCol && enPassantRow != -1) {
                ChessPiece epPawn = board[row][nc];
                if (epPawn != null && epPawn.getType() == PieceType.PAWN && epPawn.getColor() != piece.getColor()) {
                    Move epMove = new Move(row, col, nr, nc, true);
                    if (!wouldMoveExposeKing(piece.getColor(), epMove)) {
                        moves.add(epMove);
                    }
                }
            }
        }
    }

    private void addPawnMove(ChessPiece piece, int fromRow, int fromCol, int toRow, int toCol, int promoRow,
            List<Move> moves) {
        Move move = new Move(fromRow, fromCol, toRow, toCol);
        if (wouldMoveExposeKing(piece.getColor(), move))
            return;

        if (toRow == promoRow) {
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.QUEEN));
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.ROOK));
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.BISHOP));
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.KNIGHT));
        } else {
            moves.add(move);
        }
    }

    private void generateKnightMoves(ChessPiece piece, int row, int col, List<Move> moves) {
        for (int[] off : KNIGHT_OFFSETS) {
            int nr = row + off[0], nc = col + off[1];
            if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8)
                continue;
            ChessPiece target = board[nr][nc];
            if (target != null && target.getColor() == piece.getColor())
                continue;
            Move move = new Move(row, col, nr, nc);
            if (!wouldMoveExposeKing(piece.getColor(), move)) {
                moves.add(move);
            }
        }
    }

    private void generateSlidingMoves(ChessPiece piece, int row, int col, List<Move> moves, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                ChessPiece target = board[nr][nc];
                if (target != null && target.getColor() == piece.getColor())
                    break;
                Move move = new Move(row, col, nr, nc);
                if (!wouldMoveExposeKing(piece.getColor(), move)) {
                    moves.add(move);
                }
                if (target != null)
                    break; // captured, stop sliding
                nr += d[0];
                nc += d[1];
            }
        }
    }

    private void generateKingMoves(ChessPiece piece, int row, int col, List<Move> moves) {
        for (int[] off : KING_OFFSETS) {
            int nr = row + off[0], nc = col + off[1];
            if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8)
                continue;
            ChessPiece target = board[nr][nc];
            if (target != null && target.getColor() == piece.getColor())
                continue;
            Move move = new Move(row, col, nr, nc);
            if (!wouldSquareBeUnderAttack(piece.getColor(), nr, nc) && !wouldMoveExposeKing(piece.getColor(), move)) {
                moves.add(move);
            }
        }

        // Castling
        if (piece.getColor() == PieceColor.WHITE && row == 7 && col == 4) {
            Move ksMove = new Move(7, 4, 7, 6);
            if (isValidCastlingMove(PieceColor.WHITE, ksMove))
                moves.add(ksMove);
            Move qsMove = new Move(7, 4, 7, 2);
            if (isValidCastlingMove(PieceColor.WHITE, qsMove))
                moves.add(qsMove);
        } else if (piece.getColor() == PieceColor.BLACK && row == 0 && col == 4) {
            Move ksMove = new Move(0, 4, 0, 6);
            if (isValidCastlingMove(PieceColor.BLACK, ksMove))
                moves.add(ksMove);
            Move qsMove = new Move(0, 4, 0, 2);
            if (isValidCastlingMove(PieceColor.BLACK, qsMove))
                moves.add(qsMove);
        }
    }

    // ---- Game state queries ----

    public boolean isCheckmate(PieceColor color) {
        if (!isKingInCheck(color))
            return false;
        return getAllValidMoves(color).isEmpty();
    }

    public boolean isStalemate(PieceColor color) {
        if (isKingInCheck(color))
            return false;
        return getAllValidMoves(color).isEmpty();
    }

    public boolean isDraw() {
        if (hasInsufficientMaterial())
            return true;
        if (isStalemate(PieceColor.WHITE) || isStalemate(PieceColor.BLACK))
            return true;
        return false;
    }

    private boolean hasInsufficientMaterial() {
        int whitePieces = 0, blackPieces = 0;
        boolean whiteHasPawn = false, blackHasPawn = false;
        boolean whiteHasRook = false, blackHasRook = false;
        boolean whiteHasQueen = false, blackHasQueen = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null) {
                    if (piece.getColor() == PieceColor.WHITE) {
                        whitePieces++;
                        if (piece.getType() == PieceType.PAWN)
                            whiteHasPawn = true;
                        if (piece.getType() == PieceType.ROOK)
                            whiteHasRook = true;
                        if (piece.getType() == PieceType.QUEEN)
                            whiteHasQueen = true;
                    } else {
                        blackPieces++;
                        if (piece.getType() == PieceType.PAWN)
                            blackHasPawn = true;
                        if (piece.getType() == PieceType.ROOK)
                            blackHasRook = true;
                        if (piece.getType() == PieceType.QUEEN)
                            blackHasQueen = true;
                    }
                }
            }
        }
        if (whitePieces == 1 && blackPieces == 1)
            return true;
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
                ChessPiece piece = board[row][col];
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(getPieceChar(piece));
                }
            }
            if (emptyCount > 0)
                fen.append(emptyCount);
            if (row < 7)
                fen.append("/");
        }
        return fen.toString();
    }

    private char getPieceChar(ChessPiece piece) {
        char base;
        switch (piece.getType()) {
            case KING:
                base = 'K';
                break;
            case QUEEN:
                base = 'Q';
                break;
            case ROOK:
                base = 'R';
                break;
            case BISHOP:
                base = 'B';
                break;
            case KNIGHT:
                base = 'N';
                break;
            case PAWN:
                base = 'P';
                break;
            default:
                base = '?';
                break;
        }
        return piece.getColor() == PieceColor.WHITE ? base : Character.toLowerCase(base);
    }

    public boolean isCastlingAvailable(PieceColor color, boolean kingside) {
        int kingRow = (color == PieceColor.WHITE) ? 7 : 0;
        int kingDestCol = kingside ? 6 : 2;
        return isValidCastlingMove(color, new Move(kingRow, 4, kingRow, kingDestCol));
    }

    public int getEnPassantRow() {
        return enPassantRow;
    }

    public int getEnPassantCol() {
        return enPassantCol;
    }

    public boolean hasWhiteKingMoved() {
        return whiteKingMoved;
    }

    public boolean hasWhiteKingsideRookMoved() {
        return whiteKingsideRookMoved;
    }

    public boolean hasWhiteQueensideRookMoved() {
        return whiteQueensideRookMoved;
    }

    public boolean hasBlackKingMoved() {
        return blackKingMoved;
    }

    public boolean hasBlackKingsideRookMoved() {
        return blackKingsideRookMoved;
    }

    public boolean hasBlackQueensideRookMoved() {
        return blackQueensideRookMoved;
    }

    public boolean isSquareUnderAttack(int row, int col, PieceColor byColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece piece = board[r][c];
                if (piece != null && piece.getColor() == byColor) {
                    Move move = new Move(r, c, row, col);
                    if (isValidMoveForPiece(piece, move)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}