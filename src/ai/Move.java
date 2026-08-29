package ai;

/**
 * Immutable record of a single chess move.
 *
 * <p>A {@code Move} encodes the origin square, destination square and an
 * optional promotion piece in a compact {@code short} field so that it can be
 * copied, hashed and compared cheaply.</p>
 *
 * <p><b>UCI encoding</b></p>
 * <pre>
 *   move.toString() == "e2e4"       (quiet)
 *   move.toString() == "e7e8q"      (promotion to queen)
 * </pre>
 */
public final class Move {

    private static final String COL_NAMES = "abcdefgh";

    /** Internal packed representation (16 bits). */
    final short value;

    // bit layout  [15..13] = promo piece (0 none, 1=Q, 2=R, 3=B, 4=N)
    //             [12..9 ] = to   square (0-63)
    //             [ 8..5 ] = from square (0-63)
    //             [ 4..0 ] = reserved / flags

    // ── constructors ─────────────────────────────────────────────────────

    public Move(int fromCol, int fromRow, int toCol, int toRow) {
        this(fromCol, fromRow, toCol, toRow, 0);
    }

    public Move(int fromCol, int fromRow, int toCol, int toRow, int promoPiece) {
        int from = fromRow * 8 + fromCol;
        int to   = toRow   * 8 + toCol;
        int promo = promoPiece;                    // 0 = none
        value = (short) ((promo << 13) | (to << 6) | from);
    }

    // ── factory from UCI ─────────────────────────────────────────────────

    public static Move fromUCI(String uci, BoardState board) {
        if (uci == null || uci.length() < 4) return null;
        int fromCol = COL_NAMES.indexOf(uci.charAt(0));
        int fromRow = uci.charAt(1) - '1';
        int toCol   = COL_NAMES.indexOf(uci.charAt(2));
        int toRow   = uci.charAt(3) - '1';

        int promo = 0;
        if (uci.length() == 5) {
            promo = switch (uci.charAt(4)) {
                case 'q' -> 1; case 'r' -> 2; case 'b' -> 3; case 'n' -> 4;
                default  -> 0;
            };
        }
        return new Move(fromCol, fromRow, toCol, toRow, promo);
    }

    // ── accessors ────────────────────────────────────────────────────────

    public int fromCol() { return value & 7; }
    public int fromRow() { return (value >> 3) & 7; }
    public int toCol()   { return (value >> 6) & 7; }
    public int toRow()   { return (value >> 9) & 7; }

    public int promotionPiece() { return (value >> 13) & 7; }

    public boolean isPromotion() { return promotionPiece() != 0; }

    public boolean isCapture() {
        // caller must check the board; this flag is set by MoveApplier
        return (value & (1 << 12)) != 0;
    }

    /** Set/clear the capture flag (used by MoveApplier). */
    Move setCapture(boolean c) {
        if (c) value |=  (1 << 12);
        else   value &= ~(1 << 12);
        return this;
    }

    // ── display ──────────────────────────────────────────────────────────

    /** UCI string, e.g. {@code "e2e4"} or {@code "e7e8q"}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(5);
        sb.append(COL_NAMES.charAt(fromCol()));
        sb.append(fromRow() + 1);
        sb.append(COL_NAMES.charAt(toCol()));
        sb.append(toRow() + 1);
        if (isPromotion()) {
            sb.append(switch (promotionPiece()) {
                case 1 -> 'q'; case 2 -> 'r';
                case 3 -> 'b'; case 4 -> 'n';
                default -> ' ';
            });
        }
        return sb.toString();
    }

    // ── equals / hashCode (Map-friendly) ─────────────────────────────────

    @Override
    public boolean equals(Object o) {
        return o instanceof Move m && m.value == this.value;
    }

    @Override
    public int hashCode() { return value; }
}
