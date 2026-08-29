package ai;

/**
 * Holds the two best quiet moves (killer moves) for each ply of the search.
 * Moves that caused a beta cutoff but were not captures are stored here so
 * that they can be tried early in sibling nodes at the same depth.
 */
public final class KillerMoves {

    private Move first;
    private Move second;

    /**
     * Try to add {@code m} as a killer move.
     * Duplicate moves are ignored; at most two are kept.
     */
    public void add(Move m) {
        if (m == null) return;
        if (m.equals(first))  return;
        if (m.equals(second)) return;

        second = first;
        first  = m;
    }

    /** Return the first (most recent) killer, or {@code null}. */
    public Move getFirst()  { return first; }

    /** Return the second killer, or {@code null}. */
    public Move getSecond() { return second; }

    /** Reset both slots. */
    public void clear() { first = second = null; }
}
