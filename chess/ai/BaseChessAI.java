package chess.ai;

import chess.ChessBoard;
import chess.Move;
import chess.PieceColor;
import chess.ChessPiece;
import chess.PieceType;

import java.util.*;

/**
 * Base class for chess AI implementations that provides common functionality.
 */
public abstract class BaseChessAI implements ChessAI {
    protected long nodesEvaluated;
    protected long searchStartTime;
    protected int maxDepthReached;
    protected String name;
    protected long searchTimeMs;
    protected Map<String, Integer> positionCount = new HashMap<>();
    protected List<Move> moveHistory = new ArrayList<>();
    protected int[][] killerMoves = new int[64][64]; // [from][to] move ordering
    
    @Override
    public final Move findBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs) {
        reset();
        searchStartTime = System.currentTimeMillis();
        
        try {
            return searchBestMove(board, color, depth, timeLimitMs);
        } finally {
            searchTimeMs = System.currentTimeMillis() - searchStartTime;
        }
    }
    
    protected abstract Move searchBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs);
    
    protected boolean isTimeExpired(long timeLimitMs) {
        return (System.currentTimeMillis() - searchStartTime) >= timeLimitMs;
    }
    
    @Override
    public String getMetrics() {
        double nodesPerSecond = (nodesEvaluated * 1000.0) / (searchTimeMs > 0 ? searchTimeMs : 1);
        return String.format("Nodes: %,d | Speed: %,.0f nodes/sec | Depth: %d | Time: %dms",
                nodesEvaluated, nodesPerSecond, maxDepthReached, searchTimeMs);
    }
    
    @Override
    public void reset() {
        nodesEvaluated = 0;
        maxDepthReached = 0;
        searchTimeMs = 0;
    }
    
    protected void updatePositionHistory(ChessBoard board) {
        // Create a simple position key based on piece positions
        StringBuilder positionKey = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null) {
                    positionKey.append(piece.getType().name().charAt(0));
                    positionKey.append(piece.getColor().name().charAt(0));
                } else {
                    positionKey.append('.');
                }
            }
        }
        String key = positionKey.toString();
        positionCount.put(key, positionCount.getOrDefault(key, 0) + 1);
    }
    
    protected boolean isThreefoldRepetition(ChessBoard board) {
        // Check if the current position has been seen 3 times
        return positionCount.values().stream().anyMatch(count -> count >= 3);
    }
    
    protected List<Move> getOrderedMoves(ChessBoard board, PieceColor color) {
        List<Move> moves = board.getAllValidMoves(color);
        
        // Simple move ordering: captures first, then non-captures
        moves.sort((m1, m2) -> {
            // Get the piece being captured (if any)
            ChessPiece target1 = board.getPiece(m1.toRow, m1.toCol);
            ChessPiece target2 = board.getPiece(m2.toRow, m2.toCol);
            
            // Prioritize captures
            int m1Value = (target1 != null) ? getPieceValue(target1.getType()) : 0;
            int m2Value = (target2 != null) ? getPieceValue(target2.getType()) : 0;
            
            // Add killer move bonus
            m1Value += killerMoves[m1.fromRow * 8 + m1.fromCol][m1.toRow * 8 + m1.toCol];
            m2Value += killerMoves[m2.fromRow * 8 + m2.fromCol][m2.toRow * 8 + m2.toCol];
            
            return m2Value - m1Value; // Sort in descending order
        });
        
        return moves;
    }
    
    private int getPieceValue(PieceType type) {
        return type.getValue();
    }
    
    protected void recordKillerMove(Move move) {
        // Update killer move history
        killerMoves[move.fromRow * 8 + move.fromCol][move.toRow * 8 + move.toCol] += 2;
        
        // Decay old killer moves
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
                if (killerMoves[i][j] > 0) {
                    killerMoves[i][j]--;
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
}
