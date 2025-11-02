package chess.ai;

import chess.ChessBoard;
import chess.Move;
import chess.PieceColor;
import chess.ChessPiece;
import chess.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * An improved Minimax AI implementation with Alpha-Beta pruning for chess.
 * This is more efficient than basic Minimax as it prunes unnecessary branches.
 */
public class AlphaBetaAI extends BaseChessAI implements TrackedChessAI {
    private final String name;
    private final int maxDepth;
    private long lastMoveNodesEvaluated = 0;

    public AlphaBetaAI(String name) {
        this(name, 3); // Default depth of 3
    }

    public AlphaBetaAI(String name, int maxDepth) {
        this.name = name;
        this.maxDepth = maxDepth;
    }

    @Override
    public long getLastMoveNodesEvaluated() {
        return lastMoveNodesEvaluated;
    }

    @Override
    protected Move searchBestMove(ChessBoard board, PieceColor color, int depth, long timeLimitMs) {
        lastMoveNodesEvaluated = 0;
        List<Move> legalMoves = generateLegalMoves(board, color);
        if (legalMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move move : legalMoves) {
            lastMoveNodesEvaluated++;  // Count this move
            ChessBoard newBoard = board.clone();
            newBoard.makeMove(move);
            
            int moveValue = minimax(newBoard, maxDepth - 1, alpha, beta, false, color.opposite());
            lastMoveNodesEvaluated++;  // Count the minimax call

            if (moveValue > bestValue) {
                bestValue = moveValue;
                bestMove = move;
            }
            
            alpha = Math.max(alpha, bestValue);
            if (beta <= alpha) {
                break; // Beta cut-off
            }
        }

        return bestMove;
    }

    private int minimax(ChessBoard board, int depth, int alpha, int beta, boolean isMaximizing, PieceColor currentColor) {
        if (depth == 0 || isGameOver(board)) {
            return evaluate(board, currentColor);
        }

        List<Move> legalMoves = generateLegalMoves(board, currentColor);
        
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : legalMoves) {
                ChessBoard newBoard = board.clone();
                newBoard.makeMove(move);
                
                int eval = minimax(newBoard, depth - 1, alpha, beta, false, currentColor.opposite());
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                
                if (beta <= alpha) {
                    break; // Beta cut-off
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : legalMoves) {
                ChessBoard newBoard = board.clone();
                newBoard.makeMove(move);
                
                int eval = minimax(newBoard, depth - 1, alpha, beta, true, currentColor.opposite());
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                
                if (beta <= alpha) {
                    break; // Alpha cut-off
                }
            }
            return minEval;
        }
    }

    private List<Move> generateLegalMoves(ChessBoard board, PieceColor color) {
        List<Move> legalMoves = new ArrayList<>();
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = board.getPiece(fromRow, fromCol);
                if (piece != null && piece.getColor() == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            Move move = new Move(fromRow, fromCol, toRow, toCol);
                            if (board.isValidMove(move)) {
                                legalMoves.add(move);
                            }
                        }
                    }
                }
            }
        }
        return legalMoves;
    }

    private boolean isGameOver(ChessBoard board) {
        // Check if the game is over by checking if either king is missing
        boolean hasWhiteKing = false;
        boolean hasBlackKing = false;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.getType() == PieceType.KING) {
                    if (piece.getColor() == PieceColor.WHITE) {
                        hasWhiteKing = true;
                    } else {
                        hasBlackKing = true;
                    }
                }
            }
        }
        return !hasWhiteKing || !hasBlackKing;
    }

    private int evaluate(ChessBoard board, PieceColor color) {
        return calculateMaterial(board, color);
    }

    private int calculateMaterial(ChessBoard board, PieceColor color) {
        int material = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == color) {
                    switch (piece.getType()) {
                        case PAWN: material += 100; break;
                        case KNIGHT: material += 320; break;
                        case BISHOP: material += 330; break;
                        case ROOK: material += 500; break;
                        case QUEEN: material += 900; break;
                        case KING: material += 20000; break;
                    }
                }
            }
        }
        return material;
    }

    @Override
    public String getName() {
        return name + " (AB, Depth: " + maxDepth + ")";
    }
}
