package chess;

/**
 * Represents the type of score stored in a transposition table entry.
 * EXACT: The score is an exact evaluation
 * LOWERBOUND: The score is a lower bound (cutoff occurred)
 * UPPERBOUND: The score is an upper bound (cutoff occurred)
 */
public enum TranspositionFlag {
    EXACT,
    LOWERBOUND,
    UPPERBOUND
}
