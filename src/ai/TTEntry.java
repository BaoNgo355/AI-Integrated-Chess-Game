package ai;

public class TTEntry {
    public static final byte EXACT = 0;
    public static final byte LOWER_BOUND = 1;
    public static final byte UPPER_BOUND = 2;

    public static final int LOWER = 1;
    public static final int UPPER = 2;

    public long hash;
    public int depth;
    public int score;
    public byte flag;
    public int bestMove; // packed: fromSq(6) | toSq(6) | promo(4)

    public TTEntry(long hash, int depth, int score, byte flag, int bestMove) {
        this.hash = hash;
        this.depth = depth;
        this.score = score;
        this.flag = flag;
        this.bestMove = bestMove;
    }

    public boolean isExact() { return flag == EXACT; }

    public static int packMove(int fromSq, int toSq, int promo) {
        return (fromSq << 10) | (toSq << 4) | promo;
    }

    public static int unpackFromSq(int packed) { return packed >> 10; }
    public static int unpackToSq(int packed) { return (packed >> 4) & 0x3F; }
    public static int unpackPromo(int packed) { return packed & 0xF; }
}
