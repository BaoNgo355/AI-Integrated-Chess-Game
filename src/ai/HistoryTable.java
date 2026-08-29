package ai;

import java.util.*;

/**
 * History heuristic table: indexed by move, accumulates a score for
 * quiet moves that produced a beta cutoff at a given depth.
 */
public final class HistoryTable {

    private final Map<Move, Integer> table = new HashMap<>();

    /** Add {@code depth} to the entry for {@code m}. */
    public void increment(Move m, int depth) {
        table.merge(m, depth, Integer::sum);
    }

    /** Return the stored score for {@code m} (0 if absent). */
    public int get(Move m) {
        return table.getOrDefault(m, 0);
    }

    /** Halve all entries (called periodically to avoid overflow). */
    public void age() {
        for (Map.Entry<Move, Integer> e : table.entrySet())
            e.setValue(e.getValue() >> 1);
    }
}
