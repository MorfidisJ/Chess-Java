import java.awt.Point;
import java.util.*;

public class ChessAI {

    // Array-based transposition table (fixed size, index by hash)
    private static final int TT_SIZE = 1 << 20; // ~1M entries
    private static final int TT_MASK = TT_SIZE - 1;
    private final TranspositionEntry[] transpositionTable = new TranspositionEntry[TT_SIZE];

    private static final int MAX_ITERATIVE_DEPTH = 8;
    private static final long TIME_LIMIT_MS = 5000;
    private boolean timeUp;

    public ChessAI() {
    }

    public Move findBestMove(ChessBoard board, PieceColor color, int depth) {
        timeUp = false;
        long startTime = System.currentTimeMillis();

        List<Move> validMoves = board.getAllValidMoves(color);
        if (validMoves.isEmpty())
            return null;

        // Sort moves initially
        sortMoves(validMoves, board, color);

        Move bestMove = validMoves.get(0);

        int currentDepth = Math.min(depth, MAX_ITERATIVE_DEPTH);

        for (int d = 1; d <= currentDepth; d++) {
            Move currentBestMove = null;
            int currentBestScore = Integer.MIN_VALUE;

            for (Move move : validMoves) {
                if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
                    timeUp = true;
                    break;
                }

                ChessBoard.UndoInfo undo = board.makeMove(move);
                int score = -alphaBeta(board, d - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1,
                        color.opposite(), startTime);
                board.unmakeMove(move, undo);

                if (score > currentBestScore) {
                    currentBestScore = score;
                    currentBestMove = move;
                }
            }

            if (currentBestMove != null) {
                bestMove = currentBestMove;

                // Reorder: put best move first for next iteration
                validMoves.remove(currentBestMove);
                validMoves.add(0, currentBestMove);
            }

            if (timeUp || System.currentTimeMillis() - startTime > TIME_LIMIT_MS)
                break;
        }

        return bestMove;
    }

    private void sortMoves(List<Move> moves, ChessBoard board, PieceColor color) {
        moves.sort((m1, m2) -> {
            int s1 = estimateMoveValue(m1, board);
            int s2 = estimateMoveValue(m2, board);
            return Integer.compare(s2, s1);
        });
    }

    private int estimateMoveValue(Move move, ChessBoard board) {
        int score = 0;

        // MVV-LVA: Most Valuable Victim - Least Valuable Attacker
        ChessPiece target = board.getPiece(move.toRow, move.toCol);
        ChessPiece attacker = board.getPiece(move.fromRow, move.fromCol);

        if (target != null && attacker != null) {
            score += getPieceValue(target.getType()) * 10 - getPieceValue(attacker.getType());
        }

        // Promotion bonus
        if (move.promotion != null) {
            score += getPieceValue(move.promotion);
        }

        // Center control for pawns
        if (attacker != null && attacker.getType() == PieceType.PAWN) {
            if (move.toCol >= 2 && move.toCol <= 5)
                score += 30;
        }

        return score;
    }

    /**
     * Negamax alpha-beta with transposition table.
     * Always scores from the perspective of 'color' (the side to move).
     */
    private int alphaBeta(ChessBoard board, int depth, int alpha, int beta,
            PieceColor color, long startTime) {
        if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
            timeUp = true;
            return 0;
        }

        // Transposition table lookup
        long hash = board.getZobristHash();
        int ttIndex = (int) (hash & TT_MASK);
        TranspositionEntry entry = transpositionTable[ttIndex];
        if (entry != null && entry.hashKey == hash && entry.depth >= depth) {
            if (entry.flag == TranspositionFlag.EXACT)
                return entry.score;
            if (entry.flag == TranspositionFlag.ALPHA && entry.score <= alpha)
                return alpha;
            if (entry.flag == TranspositionFlag.BETA && entry.score >= beta)
                return beta;
        }

        // Terminal / leaf node
        List<Move> validMoves = board.getAllValidMoves(color);
        if (validMoves.isEmpty()) {
            if (board.isKingInCheck(color))
                return -100000 - depth; // checkmate (deeper = worse for us)
            return 0; // stalemate
        }

        if (depth <= 0) {
            return quiescence(board, alpha, beta, color, startTime, 4);
        }

        // Sort moves for better pruning
        sortMoves(validMoves, board, color);

        int origAlpha = alpha;
        int bestScore = Integer.MIN_VALUE + 1;

        for (Move move : validMoves) {
            ChessBoard.UndoInfo undo = board.makeMove(move);
            int score = -alphaBeta(board, depth - 1, -beta, -alpha, color.opposite(), startTime);
            board.unmakeMove(move, undo);

            if (timeUp)
                return 0;

            if (score > bestScore)
                bestScore = score;
            if (score > alpha)
                alpha = score;
            if (alpha >= beta)
                break; // beta cutoff
        }

        // Store in transposition table (depth-preferred replacement)
        TranspositionFlag flag;
        if (bestScore <= origAlpha)
            flag = TranspositionFlag.ALPHA;
        else if (bestScore >= beta)
            flag = TranspositionFlag.BETA;
        else
            flag = TranspositionFlag.EXACT;

        if (entry == null || entry.depth <= depth) {
            transpositionTable[ttIndex] = new TranspositionEntry(hash, bestScore, depth, flag);
        }

        return bestScore;
    }

    /**
     * Quiescence search — only search captures to avoid horizon effect.
     */
    private int quiescence(ChessBoard board, int alpha, int beta,
            PieceColor color, long startTime, int maxDepth) {
        if (timeUp || System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
            timeUp = true;
            return 0;
        }

        int standPat = evaluatePosition(board, color);
        if (standPat >= beta)
            return beta;
        if (standPat > alpha)
            alpha = standPat;

        if (maxDepth <= 0)
            return alpha;

        // Generate only captures
        List<Move> allMoves = board.getAllValidMoves(color);
        List<Move> captures = new ArrayList<>();
        for (Move m : allMoves) {
            if (board.getPiece(m.toRow, m.toCol) != null || m.isEnPassant || m.promotion != null) {
                captures.add(m);
            }
        }

        sortMoves(captures, board, color);

        for (Move move : captures) {
            ChessBoard.UndoInfo undo = board.makeMove(move);
            int score = -quiescence(board, -beta, -alpha, color.opposite(), startTime, maxDepth - 1);
            board.unmakeMove(move, undo);

            if (timeUp)
                return 0;

            if (score >= beta)
                return beta;
            if (score > alpha)
                alpha = score;
        }

        return alpha;
    }

    // ---- Transposition table ----

    private static class TranspositionEntry {
        long hashKey; // full hash key for collision detection
        int score;
        int depth;
        TranspositionFlag flag;

        TranspositionEntry(long hashKey, int score, int depth, TranspositionFlag flag) {
            this.hashKey = hashKey;
            this.score = score;
            this.depth = depth;
            this.flag = flag;
        }
    }

    private enum TranspositionFlag {
        EXACT, ALPHA, BETA
    }

    // ---- Evaluation ----

    private int evaluatePosition(ChessBoard board, PieceColor color) {
        int score = 0;
        score += evaluateMaterial(board, color);
        score += evaluatePositionalScore(board, color);
        score += evaluateKingSafety(board, color);
        score += evaluateMobilityCheap(board, color);
        return score;
    }

    private int evaluateMaterial(ChessBoard board, PieceColor color) {
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    int val = getPieceValue(piece.getType());
                    if (piece.getColor() == color)
                        score += val;
                    else
                        score -= val;
                }
            }
        }
        return score;
    }

    private int evaluatePositionalScore(ChessBoard board, PieceColor color) {
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    int bonus = getPositionalBonus(piece, row, col);
                    if (piece.getColor() == color)
                        score += bonus;
                    else
                        score -= bonus;
                }
            }
        }
        return score;
    }

    /**
     * Positional bonus — always returns a POSITIVE value for good positions.
     * The sign is handled by the caller based on color.
     */
    private int getPositionalBonus(ChessPiece piece, int row, int col) {
        switch (piece.getType()) {
            case PAWN:
                return getPawnPositionalBonus(piece.getColor(), row, col);
            case KNIGHT:
                return getKnightPositionalBonus(row, col);
            case BISHOP:
                return getBishopPositionalBonus(row, col);
            case ROOK:
                return getRookPositionalBonus(piece.getColor(), row, col);
            case QUEEN:
                return getQueenPositionalBonus(row, col);
            case KING:
                return getKingPositionalBonus(piece.getColor(), row, col);
            default:
                return 0;
        }
    }

    private int getPawnPositionalBonus(PieceColor color, int row, int col) {
        int bonus = 0;
        if (col >= 2 && col <= 5)
            bonus += 10;
        if (color == PieceColor.WHITE)
            bonus += (7 - row) * 5;
        else
            bonus += row * 5;
        return bonus;
    }

    private int getKnightPositionalBonus(int row, int col) {
        int bonus = 0;
        if (row >= 2 && row <= 5 && col >= 2 && col <= 5)
            bonus += 20;
        if (row == 0 || row == 7 || col == 0 || col == 7)
            bonus -= 10;
        return bonus;
    }

    private int getBishopPositionalBonus(int row, int col) {
        int bonus = 0;
        if (row == col || row + col == 7)
            bonus += 15;
        if (row >= 2 && row <= 5 && col >= 2 && col <= 5)
            bonus += 10;
        return bonus;
    }

    private int getRookPositionalBonus(PieceColor color, int row, int col) {
        int bonus = 0;
        if (color == PieceColor.WHITE && row == 1)
            bonus += 15;
        else if (color == PieceColor.BLACK && row == 6)
            bonus += 15;
        return bonus;
    }

    private int getQueenPositionalBonus(int row, int col) {
        int bonus = 0;
        if (row >= 2 && row <= 5 && col >= 2 && col <= 5)
            bonus += 10;
        return bonus;
    }

    private int getKingPositionalBonus(PieceColor color, int row, int col) {
        int bonus = 0;
        if (color == PieceColor.WHITE) {
            if (row >= 6)
                bonus += 20;
            if (col >= 2 && col <= 5)
                bonus += 10;
        } else {
            if (row <= 1)
                bonus += 20;
            if (col >= 2 && col <= 5)
                bonus += 10;
        }
        return bonus;
    }

    private int evaluateKingSafety(ChessBoard board, PieceColor color) {
        int score = 0;
        Point kingPos = board.findKing(color);
        if (kingPos == null)
            return 0;

        if (board.isKingInCheck(color))
            score -= 50;

        score += evaluatePawnShield(board, color, kingPos);
        return score;
    }

    private int evaluatePawnShield(ChessBoard board, PieceColor color, Point kingPos) {
        int score = 0;
        int direction = (color == PieceColor.WHITE) ? -1 : 1;
        int shieldRow = kingPos.x + direction;
        if (shieldRow < 0 || shieldRow >= 8)
            return 0;

        for (int col = Math.max(0, kingPos.y - 1); col <= Math.min(7, kingPos.y + 1); col++) {
            ChessPiece pawn = board.getPiece(shieldRow, col);
            if (pawn != null && pawn.getType() == PieceType.PAWN && pawn.getColor() == color) {
                score += 10;
            }
        }
        return score;
    }

    /**
     * Cheap mobility estimate — counts pseudo-legal attack squares
     * without generating full legal moves.
     */
    private int evaluateMobilityCheap(ChessBoard board, PieceColor color) {
        int mobility = 0;
        int opponentMobility = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece == null)
                    continue;

                int count = countPseudoLegalMoves(board, piece, row, col);
                if (piece.getColor() == color)
                    mobility += count;
                else
                    opponentMobility += count;
            }
        }

        return (mobility - opponentMobility);
    }

    private int countPseudoLegalMoves(ChessBoard board, ChessPiece piece, int row, int col) {
        int count = 0;
        switch (piece.getType()) {
            case PAWN: {
                int dir = piece.getColor() == PieceColor.WHITE ? -1 : 1;
                int nr = row + dir;
                if (nr >= 0 && nr < 8) {
                    if (board.getPiece(nr, col) == null)
                        count++;
                    if (col > 0 && board.getPiece(nr, col - 1) != null)
                        count++;
                    if (col < 7 && board.getPiece(nr, col + 1) != null)
                        count++;
                }
                break;
            }
            case KNIGHT:
                for (int[] off : new int[][] { { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 }, { 1, -2 }, { 1, 2 },
                        { 2, -1 }, { 2, 1 } }) {
                    int nr = row + off[0], nc = col + off[1];
                    if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                        ChessPiece t = board.getPiece(nr, nc);
                        if (t == null || t.getColor() != piece.getColor())
                            count++;
                    }
                }
                break;
            case BISHOP:
                count = countSlidingMoves(board, piece, row, col,
                        new int[][] { { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 } });
                break;
            case ROOK:
                count = countSlidingMoves(board, piece, row, col,
                        new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } });
                break;
            case QUEEN:
                count = countSlidingMoves(board, piece, row, col, new int[][] { { -1, -1 }, { -1, 0 }, { -1, 1 },
                        { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } });
                break;
            case KING:
                for (int[] off : new int[][] { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 },
                        { 1, 0 }, { 1, 1 } }) {
                    int nr = row + off[0], nc = col + off[1];
                    if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                        ChessPiece t = board.getPiece(nr, nc);
                        if (t == null || t.getColor() != piece.getColor())
                            count++;
                    }
                }
                break;
        }
        return count;
    }

    private int countSlidingMoves(ChessBoard board, ChessPiece piece, int row, int col, int[][] dirs) {
        int count = 0;
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                ChessPiece t = board.getPiece(nr, nc);
                if (t != null && t.getColor() == piece.getColor())
                    break;
                count++;
                if (t != null)
                    break;
                nr += d[0];
                nc += d[1];
            }
        }
        return count;
    }

    public int getPieceValue(PieceType type) {
        switch (type) {
            case PAWN:
                return 100;
            case KNIGHT:
                return 320;
            case BISHOP:
                return 330;
            case ROOK:
                return 500;
            case QUEEN:
                return 900;
            case KING:
                return 20000;
            default:
                return 0;
        }
    }
}