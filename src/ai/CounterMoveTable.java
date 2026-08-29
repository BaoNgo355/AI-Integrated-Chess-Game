package ai;

import java.util.HashMap;
import java.util.Map;

/**
 * Counter-move (relative/history) table: for each (piece-type, to-square)
 * pair, stores the last move that caused a beta cutoff.
 */
public final class CounterMoveTable {

    private final Map<Long, Move> table = new HashMap<>();

    private static long key(int pieceType, int toCol, int toRow) {
        return ((long) pieceType << 6) | (toRow << 3) | toCol;
    }

    /** Store a counter-move. */
    public void put(int pieceType, int toCol, int toRow, Move m) {
        table.put(key(pieceType, toCol, toRow), m);
    }

    /** Retrieve a counter-move, or {@code null}. */
    public Move get(int pieceType, int toCol, int toRow) {
        return table.get(key(pieceType, toCol, toRow));
    }

    public void clear() { table.clear(); }
}
