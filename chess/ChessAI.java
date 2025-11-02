package chess;

import java.io.*;
import java.awt.Point;
import java.util.*;
public class ChessAI implements ChessStrategy {
    
    private static final String NAME = "Alpha-Beta Pruning";
    
    private final Map<Long, TranspositionEntry> transpositionTable;
    private static final int MAX_TABLE_SIZE = 1_000_000;
    private static final int MAX_ITERATIVE_DEPTH = 8;
    
    // Metrics tracking
    private long nodesEvaluated;
    private long searchStartTime;
    private int maxDepthReached;
    private double bestScore;
    
    public ChessAI() {
        this.transpositionTable = new HashMap<>();
        reset();
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public Move findBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs) {
        reset();
        searchStartTime = System.currentTimeMillis();
        
        // Use iterative deepening within the time limit
        Move bestMove = null;
        maxDepthReached = 0;
        
        for (int currentDepth = 1; currentDepth <= Math.min(depth, MAX_ITERATIVE_DEPTH); currentDepth++) {
            try {
                Move move = findBestMoveAtDepth(board, color, currentDepth, timeLimitMs);
                if (move != null) {
                    bestMove = move;
                    maxDepthReached = currentDepth;
                }
            } catch (SearchTimeoutException e) {
                break; // Time's up, return best move found so far
            }
            
            // Check if we've run out of time
            if (System.currentTimeMillis() - searchStartTime > timeLimitMs) {
                break;
            }
        }
        
        return bestMove;
    }
    
    private Move findBestMoveAtDepth(ChessBoard board, PieceColor color, int depth, long timeLimitMs) 
            throws SearchTimeoutException {
        List<Move> validMoves = board.getAllValidMoves(color);
        if (validMoves.isEmpty()) return null;
        
        // Clear transposition table if it's getting too big
        if (transpositionTable.size() > MAX_TABLE_SIZE) {
            transpositionTable.clear();
        }
        
        Move bestMove = null;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int bestScore = Integer.MIN_VALUE;
        
        // Sort moves for better alpha-beta pruning
        sortMoves(validMoves, board, color);
        
        for (Move move : validMoves) {
            checkTimeout(timeLimitMs);
            
            ChessBoard newBoard = board.clone();
            newBoard.makeMove(move);
            
            int score = -alphaBeta(newBoard, depth - 1, -beta, -alpha, 
                                 color.opposite(), timeLimitMs);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                this.bestScore = bestScore;
            }
            
            alpha = Math.max(alpha, bestScore);
            if (alpha >= beta) {
                break; // Beta cutoff
            }
        }
        
        return bestMove != null ? bestMove : validMoves.get(0);
    }
    
    private void checkTimeout(long timeLimitMs) throws SearchTimeoutException {
        if (System.currentTimeMillis() - searchStartTime > timeLimitMs) {
            throw new SearchTimeoutException("Search timed out");
        }
    }
    
    private void sortMoves(List<Move> moves, ChessBoard board, PieceColor color) {
        moves.sort((m1, m2) -> {
            int score1 = estimateMoveValue(m1, board, color);
            int score2 = estimateMoveValue(m2, board, color);
            return Integer.compare(score2, score1);
        });
    }
    
    @Override
    public SearchMetrics getMetrics() {
        return new SearchMetrics(
            nodesEvaluated,
            System.currentTimeMillis() - searchStartTime,
            maxDepthReached,
            bestScore
        );
    }
    
    @Override
    public void reset() {
        nodesEvaluated = 0;
        searchStartTime = 0;
        maxDepthReached = 0;
        bestScore = 0;
        transpositionTable.clear();
    }
    
    private int estimateMoveValue(Move move, ChessBoard board, PieceColor color) {
        int score = 0;
        
        ChessPiece target = board.getPiece(move.toRow, move.toCol);
        if (target != null) {
            score += getPieceValue(target.getType()) * 10;
        }
        
        ChessPiece piece = board.getPiece(move.fromRow, move.fromCol);
        if (piece != null && piece.getType() == PieceType.PAWN) {
            if (move.toCol >= 2 && move.toCol <= 5) {
                score += 50;
            }
        }
        
        if (piece != null && piece.getType() == PieceType.KING) {
            score -= 100;
        }
        
        return score;
    }
    
    private int alphaBeta(ChessBoard board, int depth, int alpha, int beta, 
                         PieceColor color, long timeLimitMs) throws SearchTimeoutException {
        checkTimeout(timeLimitMs);
        nodesEvaluated++;
        
        // Check for terminal state or max depth
        if (depth <= 0 || board.isGameOver()) {
            return evaluateBoard(board, color);
        }
        
        long hashKey = generateHashKey(board);
        
        TranspositionEntry entry = transpositionTable.get(hashKey);
        if (entry != null && entry.depth >= depth) {
            if (entry.flag == TranspositionFlag.EXACT) {
                return entry.score;
            } else if (entry.flag == TranspositionFlag.LOWERBOUND && entry.score <= alpha) {
                return entry.score;
            } else if (entry.flag == TranspositionFlag.UPPERBOUND && entry.score >= beta) {
                return entry.score;
            }
        }
        
        List<Move> validMoves = board.getAllValidMoves(color);
        if (validMoves.isEmpty()) {
            int score = evaluateBoard(board, color);
            storeTransposition(hashKey, score, depth, TranspositionFlag.EXACT);
            return score;
        }
        
        int originalAlpha = alpha;
        int bestScore = Integer.MIN_VALUE;
        TranspositionFlag flag = TranspositionFlag.UPPERBOUND;
        
        for (Move move : validMoves) {
            ChessBoard tempBoard = board.clone();
            tempBoard.makeMove(move);
            
            int score = -alphaBeta(tempBoard, depth - 1, -beta, -alpha, 
                                 color.opposite(), timeLimitMs);
            
            if (score > bestScore) {
                bestScore = score;
                flag = TranspositionFlag.EXACT;
            }
            
            alpha = Math.max(alpha, score);
            if (beta <= alpha) {
                flag = (score <= originalAlpha) ? TranspositionFlag.UPPERBOUND : TranspositionFlag.LOWERBOUND;
                break;
            }
        }
        
        storeTransposition(hashKey, bestScore, depth, flag);
        return bestScore;
    }
    
    private long generateHashKey(ChessBoard board) {
        long hash = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    int pieceIndex = piece.getType().ordinal();
                    int colorIndex = piece.getColor().ordinal();
                    hash ^= (pieceIndex + colorIndex * 6) << ((row * 8 + col) % 64);
                }
            }
        }
        return hash;
    }
    
    private void storeTransposition(long hashKey, int score, int depth, TranspositionFlag flag) {
        transpositionTable.put(hashKey, new TranspositionEntry(score, depth, flag));
    }
    
    private int evaluateBoard(ChessBoard board, PieceColor color) {
        if (board.isKingInCheck(color.opposite()) && isCheckmate(board, color.opposite())) {
            return Integer.MAX_VALUE - (MAX_ITERATIVE_DEPTH - maxDepthReached);
        }
        if (isStalemate(board, color) || board.isInsufficientMaterial() || board.isThreefoldRepetition()) {
            return 0;
        }
        
        int score = 0;
        
        // Material score
        score += getMaterialScore(board, color) - getMaterialScore(board, color.opposite());
        
        // Positional score
        score += evaluatePositionalScore(board, color) - evaluatePositionalScore(board, color.opposite());
        
        // Mobility
        score += (getAllValidMoves(board, color).size() - 
                 getAllValidMoves(board, color.opposite()).size()) * 2;
        
        // King safety
        score += evaluateKingSafety(board, color) - evaluateKingSafety(board, color.opposite());
        
        return score;
    }
    
    private boolean isCheckmate(ChessBoard board, PieceColor color) {
        if (!board.isKingInCheck(color)) {
            return false;
        }
        return isStalemate(board, color);
    }
    
    private boolean isStalemate(ChessBoard board, PieceColor color) {
        return !board.isKingInCheck(color) && getAllValidMoves(board, color).isEmpty();
    }
    
    private List<Move> getAllValidMoves(ChessBoard board, PieceColor color) {
        List<Move> validMoves = new ArrayList<>();
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = board.getPiece(fromRow, fromCol);
                if (piece != null && piece.getColor() == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            Move move = new Move(fromRow, fromCol, toRow, toCol);
                            if (board.isValidMove(move)) {
                                // Simulate the move
                                ChessBoard newBoard = board.clone();
                                newBoard.makeMove(move);
                                
                                // Only add if it doesn't leave the king in check
                                if (!newBoard.isKingInCheck(color)) {
                                    validMoves.add(move);
                                }
                            }
                        }
                    }
                }
            }
        }
        return validMoves;
    }
    
    private int evaluateKingSafety(ChessBoard board, PieceColor color) {
        Point kingPos = findKing(board, color);
        if (kingPos == null) return -1000; // King not found, very bad
        
        int safetyScore = 0;
        
        // Penalize being in check
        if (board.isKingInCheck(color)) {
            safetyScore -= 50;
        }
        
        // Reward castling rights
        if (color == PieceColor.WHITE) {
            // Check if kingside castling is still possible
            if (!board.hasWhiteKingMoved() && 
                board.getPiece(7, 7) != null && 
                board.getPiece(7, 7).getType() == PieceType.ROOK && 
                board.getPiece(7, 7).getColor() == PieceColor.WHITE) {
                safetyScore += 15;
            }
            // Check if queenside castling is still possible
            if (!board.hasWhiteKingMoved() && 
                board.getPiece(7, 0) != null && 
                board.getPiece(7, 0).getType() == PieceType.ROOK && 
                board.getPiece(7, 0).getColor() == PieceColor.WHITE) {
                safetyScore += 10;
            }
        } else {
            // Check if kingside castling is still possible for black
            if (!board.hasBlackKingMoved() && 
                board.getPiece(0, 7) != null && 
                board.getPiece(0, 7).getType() == PieceType.ROOK && 
                board.getPiece(0, 7).getColor() == PieceColor.BLACK) {
                safetyScore += 15;
            }
            // Check if queenside castling is still possible for black
            if (!board.hasBlackKingMoved() && 
                board.getPiece(0, 0) != null && 
                board.getPiece(0, 0).getType() == PieceType.ROOK && 
                board.getPiece(0, 0).getColor() == PieceColor.BLACK) {
                safetyScore += 10;
                      }
        }
        
        // Penalize having opponent pieces attacking squares around the king
        int attackWeight = 0;
        for (int row = Math.max(0, kingPos.x - 1); row <= Math.min(7, kingPos.x + 1); row++) {
            for (int col = Math.max(0, kingPos.y - 1); col <= Math.min(7, kingPos.y + 1); col++) {
                if (row == kingPos.x && col == kingPos.y) continue;
                
                // Check if any opponent piece can attack this square
                if (isSquareUnderAttack(board, color.opposite(), row, col)) {
                    attackWeight += 5;
                }
            }
        }
        safetyScore -= attackWeight;
        
        // Evaluate pawn shield and king mobility
        safetyScore += evaluatePawnShield(board, color, kingPos);
        safetyScore += evaluateKingMobility(board, color, kingPos);
        
        return safetyScore;
    }
    
    private boolean isSquareUnderAttack(ChessBoard board, PieceColor attackerColor, int row, int col) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ChessPiece piece = board.getPiece(r, c);
                if (piece != null && piece.getColor() == attackerColor) {
                    Move move = new Move(r, c, row, col);
                    if (board.isValidMove(move)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Point findKing(ChessBoard board, PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return new Point(row, col);
                }
            }
        }
        return null;
    }
    
    private int getMaterialScore(ChessBoard board, PieceColor color) {
        int score = 0;
        int opponentScore = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    int pieceValue = getPieceValue(piece.getType());
                    if (piece.getColor() == color) {
                        score += pieceValue;
                    } else {
                        opponentScore += pieceValue;
                    }
                }
            }
        }
        
        return score - opponentScore;
    }
    
    private int evaluatePositionalScore(ChessBoard board, PieceColor color) {
        int score = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == color) {
                    score += getPositionalBonus(piece, row, col);
                }
            }
        }
        
        return score;
    }
    
    private int getPositionalBonus(ChessPiece piece, int row, int col) {
        int bonus = 0;
        
        switch (piece.getType()) {
            case PAWN:
                bonus = getPawnPositionalBonus(piece.getColor(), row, col);
                break;
            case KNIGHT:
                bonus = getKnightPositionalBonus(row, col);
                break;
            case BISHOP:
                bonus = getBishopPositionalBonus(row, col);
                break;
            case ROOK:
                bonus = getRookPositionalBonus(piece.getColor(), row, col);
                break;
            case QUEEN:
                bonus = getQueenPositionalBonus(row, col);
                break;
            case KING:
                bonus = getKingPositionalBonus(piece.getColor(), row, col);
                break;
        }
        
        return piece.getColor() == PieceColor.WHITE ? bonus : -bonus;
    }
    
    private int getPawnPositionalBonus(PieceColor color, int row, int col) {
        int bonus = 0;
        
        
        if (col >= 2 && col <= 5) bonus += 10;
        
        
        if (color == PieceColor.WHITE) {
            bonus += (7 - row) * 5; 
        } else {
            bonus += row * 5;
        }
        
        return bonus;
    }
    
    private int getKnightPositionalBonus(int row, int col) {
        int bonus = 0;
        
        
        if (row >= 2 && row <= 5 && col >= 2 && col <= 5) {
            bonus += 20;
        }
        
        
        if (row == 0 || row == 7 || col == 0 || col == 7) {
            bonus -= 10;
        }
        
        return bonus;
    }
    
    private int getBishopPositionalBonus(int row, int col) {
        int bonus = 0;
        
        
        if (row == col || row + col == 7) {
            bonus += 15;
        }
        
        
        if (row >= 2 && row <= 5 && col >= 2 && col <= 5) {
            bonus += 10;
        }
        
        return bonus;
    }
    
    private int getRookPositionalBonus(PieceColor color, int row, int col) {
        int bonus = 0;
        
        
        if (color == PieceColor.WHITE && row == 1) {
            bonus += 15;
        } else if (color == PieceColor.BLACK && row == 6) {
            bonus += 15;
        }
        
        return bonus;
    }
    
    private int getQueenPositionalBonus(int row, int col) {
        int bonus = 0;
        
        
        if (row >= 2 && row <= 5 && col >= 2 && col <= 5) {
            bonus += 10;
        }
        
        
        if (row == 7 || row == 0) {
            bonus += 5;
        }
        
        return bonus;
    }
    
    private int getKingPositionalBonus(PieceColor color, int row, int col) {
        int bonus = 0;
        

        if (color == PieceColor.WHITE) {
            if (row >= 6) bonus += 20; 
            if (col >= 2 && col <= 5) bonus += 10; 
        } else {
            if (row <= 1) bonus += 20;
            if (col >= 2 && col <= 5) bonus += 10;
        }
        
        return bonus;
    }
    
    private boolean isKingUnderAttack(ChessBoard board, PieceColor color, Point kingPos) {
        PieceColor opponentColor = (color == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == opponentColor) {
                    Move attackMove = new Move(row, col, kingPos.x, kingPos.y);
                    if (board.isValidMove(attackMove)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private int evaluatePawnShield(ChessBoard board, PieceColor color, Point kingPos) {
        int score = 0;
        int direction = (color == PieceColor.WHITE) ? -1 : 1;
        
        
        for (int col = Math.max(0, kingPos.y - 1); col <= Math.min(7, kingPos.y + 1); col++) {
            ChessPiece pawn = board.getPiece(kingPos.x + direction, col);
            if (pawn != null && pawn.getType() == PieceType.PAWN && pawn.getColor() == color) {
                score += 10;
            }
        }
        
        return score;
    }
    
    private int evaluateKingMobility(ChessBoard board, PieceColor color, Point kingPos) {
        int mobility = 0;
        
        
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                
                int newRow = kingPos.x + dr;
                int newCol = kingPos.y + dc;
                
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    Move kingMove = new Move(kingPos.x, kingPos.y, newRow, newCol);
                    if (board.isValidMove(kingMove)) {
                        mobility++;
                    }
                }
            }
        }
        
        return mobility * 5;
    }
    
    private int evaluateMobility(ChessBoard board, PieceColor color) {
        List<Move> moves = board.getAllValidMoves(color);
        return moves.size() * 2; 
    }
    
    private int getPieceValue(PieceType type) {
        switch (type) {
            case PAWN: return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK: return 500;
            case QUEEN: return 900;
            case KING: return 20000;
            default: return 0;
        }
    }
} 