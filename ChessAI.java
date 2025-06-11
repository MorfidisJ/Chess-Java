import java.io.*;
import java.awt.Point;
import java.util.*;

public class ChessAI {
    
    private Map<Long, TranspositionEntry> transpositionTable;
    private static final int MAX_TABLE_SIZE = 1000000; 
    
    
    private static final int MAX_ITERATIVE_DEPTH = 8;
    private static final long TIME_LIMIT_MS = 5000; 
    
    public ChessAI() {
        transpositionTable = new HashMap<>();
    }
    
    public Move findBestMove(ChessBoard board, PieceColor color, int depth) {
        return findBestMoveBuiltIn(board, color, depth);
    }
    
    private Move findBestMoveBuiltIn(ChessBoard board, PieceColor color, int depth) {
        
        if (transpositionTable.size() > MAX_TABLE_SIZE) {
            transpositionTable.clear();
        }
        
        List<Move> validMoves = board.getAllValidMoves(color);
        if (validMoves.isEmpty()) return null;
        
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        long startTime = System.currentTimeMillis();
        
        
        int currentDepth = Math.min(depth, MAX_ITERATIVE_DEPTH);
        for (int d = 1; d <= currentDepth; d++) {
            Move currentBestMove = null;
            int currentBestScore = Integer.MIN_VALUE;
            
            
            sortMoves(validMoves, board, color);
            
            for (Move move : validMoves) {
                
                if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
                    break;
                }
                
                
                ChessBoard tempBoard = board.clone();
                tempBoard.makeMove(move);
                
                
                int score = alphaBeta(tempBoard, d - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, 
                                    color == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE, false, startTime);
                
                if (score > currentBestScore) {
                    currentBestScore = score;
                    currentBestMove = move;
                }
            }
            
            
            if (currentBestMove != null) {
                bestMove = currentBestMove;
                bestScore = currentBestScore;
            }
            
            
            if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
                break;
            }
        }
        
        return bestMove != null ? bestMove : validMoves.get(new Random().nextInt(validMoves.size()));
    }
    
    private void sortMoves(List<Move> moves, ChessBoard board, PieceColor color) {
    
        moves.sort((m1, m2) -> {
            int score1 = estimateMoveValue(m1, board, color);
            int score2 = estimateMoveValue(m2, board, color);
            return Integer.compare(score2, score1); 
        });
    }
    
    private int estimateMoveValue(Move move, ChessBoard board, PieceColor color) {
        int score = 0;
        
        
        ChessPiece target = board.getPiece(move.toRow, move.toCol);
        if (target != null) {
            score += getPieceValue(target.type) * 10;
        }
        
        
        ChessPiece piece = board.getPiece(move.fromRow, move.fromCol);
        if (piece != null && piece.type == PieceType.PAWN) {
            if (move.toCol >= 2 && move.toCol <= 5) {
                score += 50;
            }
        }
        
        
        if (piece != null && piece.type == PieceType.KING) {
            score -= 100;
        }
        
        return score;
    }
    
    private int alphaBeta(ChessBoard board, int depth, int alpha, int beta, PieceColor color, boolean maximizing, long startTime) {
        
        if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) {
            return 0; 
        }
        
        
        long hashKey = generateHashKey(board);
        
        
        TranspositionEntry entry = transpositionTable.get(hashKey);
        if (entry != null && entry.depth >= depth) {
            if (entry.flag == TranspositionFlag.EXACT) {
                return entry.score;
            } else if (entry.flag == TranspositionFlag.ALPHA && entry.score <= alpha) {
                return entry.score;
            } else if (entry.flag == TranspositionFlag.BETA && entry.score >= beta) {
                return entry.score;
            }
        }
        
        
        if (depth == 0 || board.isCheckmate(color) || board.isDraw()) {
            int score = evaluatePosition(board, color);
            storeTransposition(hashKey, score, depth, TranspositionFlag.EXACT);
            return score;
        }
        
        List<Move> validMoves = board.getAllValidMoves(color);
        if (validMoves.isEmpty()) {
            int score = evaluatePosition(board, color);
            storeTransposition(hashKey, score, depth, TranspositionFlag.EXACT);
            return score;
        }
        
        int originalAlpha = alpha;
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        TranspositionFlag flag = TranspositionFlag.ALPHA;
        
        for (Move move : validMoves) {
            ChessBoard tempBoard = board.clone();
            tempBoard.makeMove(move);
            
            int score = alphaBeta(tempBoard, depth - 1, alpha, beta, 
                                color == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE, !maximizing, startTime);
            
            if (maximizing) {
                if (score > bestScore) {
                    bestScore = score;
                    flag = TranspositionFlag.EXACT;
                }
                alpha = Math.max(alpha, score);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    flag = TranspositionFlag.EXACT;
                }
                beta = Math.min(beta, score);
            }
            
            
            if (beta <= alpha) {
                flag = (score <= originalAlpha) ? TranspositionFlag.ALPHA : TranspositionFlag.BETA;
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
                    int pieceIndex = piece.type.ordinal();
                    int colorIndex = piece.color.ordinal();
                    hash ^= (pieceIndex + colorIndex * 6) << ((row * 8 + col) % 64);
                }
            }
        }
        return hash;
    }
    
    private void storeTransposition(long hashKey, int score, int depth, TranspositionFlag flag) {
        transpositionTable.put(hashKey, new TranspositionEntry(score, depth, flag));
    }
    
    
    private static class TranspositionEntry {
        int score;
        int depth;
        TranspositionFlag flag;
        
        TranspositionEntry(int score, int depth, TranspositionFlag flag) {
            this.score = score;
            this.depth = depth;
            this.flag = flag;
        }
    }
    
    private enum TranspositionFlag {
        EXACT, ALPHA, BETA
    }
    
    private int evaluatePosition(ChessBoard board, PieceColor color) {
        int score = 0;
        
    
        score += evaluateMaterial(board, color);
        
        
        score += evaluatePositionalScore(board, color);
        
        
        score += evaluateKingSafety(board, color);
        
        
        score += evaluateMobility(board, color);
        
        return score;
    }
    
    private int evaluateMaterial(ChessBoard board, PieceColor color) {
        int score = 0;
        int opponentScore = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    int pieceValue = getPieceValue(piece.type);
                    if (piece.color == color) {
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
                if (piece != null && piece.color == color) {
                    score += getPositionalBonus(piece, row, col);
                }
            }
        }
        
        return score;
    }
    
    private int getPositionalBonus(ChessPiece piece, int row, int col) {
        int bonus = 0;
        
        switch (piece.type) {
            case PAWN:
                bonus = getPawnPositionalBonus(piece.color, row, col);
                break;
            case KNIGHT:
                bonus = getKnightPositionalBonus(row, col);
                break;
            case BISHOP:
                bonus = getBishopPositionalBonus(row, col);
                break;
            case ROOK:
                bonus = getRookPositionalBonus(piece.color, row, col);
                break;
            case QUEEN:
                bonus = getQueenPositionalBonus(row, col);
                break;
            case KING:
                bonus = getKingPositionalBonus(piece.color, row, col);
                break;
        }
        
        return piece.color == PieceColor.WHITE ? bonus : -bonus;
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
    
    private int evaluateKingSafety(ChessBoard board, PieceColor color) {
        int score = 0;
        
        
        Point kingPos = findKing(board, color);
        if (kingPos == null) return 0;
        
        
        if (isKingUnderAttack(board, color, kingPos)) {
            score -= 50;
        }
        
        
        score += evaluatePawnShield(board, color, kingPos);
        
        
        score += evaluateKingMobility(board, color, kingPos);
        
        return score;
    }
    
    private Point findKing(ChessBoard board, PieceColor color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return new Point(row, col);
                }
            }
        }
        return null;
    }
    
    private boolean isKingUnderAttack(ChessBoard board, PieceColor color, Point kingPos) {
        PieceColor opponentColor = (color == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.color == opponentColor) {
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
            if (pawn != null && pawn.type == PieceType.PAWN && pawn.color == color) {
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