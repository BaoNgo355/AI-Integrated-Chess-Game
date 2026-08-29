package ai;

public class TranspositionTable {
    private final TTEntry[] entries;
    private final int mask;

    public TranspositionTable(int sizeMB) {
        int targetEntries = (sizeMB * 1024 * 1024) / 48;
        int size = 1;
        while (size < targetEntries) size <<= 1;
        size = Math.max(size, 1 << 16);
        entries = new TTEntry[size];
        mask = size - 1;
    }

    public void put(long hash, int depth, int score, byte flag, int bestMove) {
        int idx = (int)(hash & mask);
        TTEntry existing = entries[idx];
        if (existing == null || depth >= existing.depth) {
            entries[idx] = new TTEntry(hash, depth, score, flag, bestMove);
        }
    }

    public TTEntry get(long hash) {
        TTEntry e = entries[(int)(hash & mask)];
        if (e != null && e.hash == hash) return e;
        return null;
    }

    public void clear() {
        for (int i = 0; i < entries.length; i++)
            entries[i] = null;
    }

    // ── new API (used by ChessAI) ────────────────────────────────────────

    public TranspositionTable() {
        this(64);
    }

    public void incAge() { }

    public TTEntry probe(long hash) {
        return get(hash);
    }

    public Move bestMove(long hash) {
        TTEntry e = get(hash);
        if (e == null) return null;
        int packed = e.bestMove;
        if (packed == 0) return null;
        int fromSq = TTEntry.unpackFromSq(packed);
        int toSq   = TTEntry.unpackToSq(packed);
        int promo  = TTEntry.unpackPromo(packed);
        return new Move(fromSq / 8, fromSq % 8, toSq / 8, toSq % 8, promo);
    }

    public void store(long hash, int depth, int score, int flag, Move bestMove) {
        int packed = 0;
        if (bestMove != null) {
            int fromSq = bestMove.fromRow() * 8 + bestMove.fromCol();
            int toSq   = bestMove.toRow()   * 8 + bestMove.toCol();
            packed = TTEntry.packMove(fromSq, toSq, bestMove.promotionPiece());
        }
        put(hash, depth, score, (byte) flag, packed);
    }
}
