package chess.ai;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.Move;
import chess.PieceColor;

import java.util.List;

/**
 * A basic Minimax AI implementation for chess.
 */
public class MinimaxAI extends BaseChessAI {
    private final String name;
    private final int maxDepth;

    public MinimaxAI(String name) {
        this(name, 3); // Default depth of 3
    }

    public MinimaxAI(String name, int maxDepth) {
        this.name = name;
        this.maxDepth = maxDepth;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected Move searchBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs) {
        updatePositionHistory(board);
        
        List<Move> legalMoves = getOrderedMoves(board, color);
        if (legalMoves.isEmpty()) {
            return null; // No legal moves
        }

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        searchStartTime = System.currentTimeMillis();

        for (Move move : legalMoves) {
            if (isTimeExpired(timeLimitMs)) {
                break;
            }
            
            // Make a copy of the board to simulate the move
            ChessBoard newBoard = board.clone();
            newBoard.makeMove(move);
            
            // Skip if this leads to threefold repetition
            if (isThreefoldRepetition(newBoard)) {
                continue;
            }
            
            int moveValue = minimax(newBoard, maxDepth - 1, false, color, timeLimitMs);
            nodesEvaluated++;

            if (moveValue > bestValue) {
                bestValue = moveValue;
                bestMove = move;
                recordKillerMove(move);
            }
        }

        // If no move was selected (e.g., all led to repetition), return first legal move
        return bestMove != null ? bestMove : legalMoves.get(0);
    }

    private int minimax(ChessBoard board, int depth, boolean isMaximizing, PieceColor maximizingColor, long timeLimitMs) {
        // Check terminal conditions
        if (depth == 0 || board.isGameOver() || isTimeExpired(timeLimitMs)) {
            return evaluateBoard(board, maximizingColor);
        }
        
        // Check for threefold repetition
        if (isThreefoldRepetition(board)) {
            return 0; // Draw by repetition
        }

        maxDepthReached = Math.max(maxDepthReached, maxDepth - depth);
        
        PieceColor currentColor = isMaximizing ? maximizingColor : maximizingColor.opposite();
        List<Move> legalMoves = getOrderedMoves(board, currentColor);
        
        if (legalMoves.isEmpty()) {
            // Check for checkmate or stalemate
            if (board.isKingInCheck(currentColor)) {
                return isMaximizing ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;
            }
            return 0; // Stalemate
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : legalMoves) {
                if (isTimeExpired(timeLimitMs)) {
                    break;
                }
                
                ChessBoard newBoard = board.clone();
                newBoard.makeMove(move);
                
                // Skip if this leads to threefold repetition
                if (isThreefoldRepetition(newBoard)) {
                    continue;
                }
                
                int eval = minimax(newBoard, depth - 1, false, maximizingColor, timeLimitMs);
                nodesEvaluated++;
                maxEval = Math.max(maxEval, eval);
                
                // Record killer move if this was a good capture
                if (board.getPiece(move.toRow, move.toCol) != null) {
                    recordKillerMove(move);
                }
                if (isTimeExpired(timeLimitMs)) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : legalMoves) {
                if (isTimeExpired(timeLimitMs)) {
                    break;
                }
                
                ChessBoard newBoard = board.clone();
                newBoard.makeMove(move);
                
                // Skip if this leads to threefold repetition
                if (isThreefoldRepetition(newBoard)) {
                    continue;
                }
                
                int eval = minimax(newBoard, depth - 1, true, maximizingColor, timeLimitMs);
                nodesEvaluated++;
                minEval = Math.min(minEval, eval);
                
                // Record killer move if this was a good capture
                if (board.getPiece(move.toRow, move.toCol) != null) {
                    recordKillerMove(move);
                }
            }
            return minEval == Integer.MAX_VALUE ? 0 : minEval;
        }
    }

    private int evaluateBoard(ChessBoard board, PieceColor color) {
        // Simple material evaluation
        int score = 0;
        score += calculateMaterial(board, color);
        score -= calculateMaterial(board, color.opposite());
        return score;
    }
    
    private int calculateMaterial(ChessBoard board, PieceColor color) {
        int material = 0;
        // Iterate through the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == color) {
                    // Add material value based on piece type
                    switch (piece.getType()) {
                        case PAWN: material += 100; break;
                        case KNIGHT: material += 320; break;
                        case BISHOP: material += 330; break;
                        case ROOK: material += 500; break;
                        case QUEEN: material += 900; break;
                        case KING: material += 20000; break; // King is very valuable
                    }
                }
            }
        }
        return material;
    }
}
