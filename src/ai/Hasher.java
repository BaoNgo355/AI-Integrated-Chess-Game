package ai;

import java.util.Random;

public class Hasher {
    private static final long[][] PIECE_KEYS = new long[13][64];
    private static final long[] CASTLING_KEYS = new long[16];
    private static final long[] EP_KEYS = new long[9];
    private static final long SIDE_KEY;

    static {
        Random rng = new Random(0x12345678);
        for (int p = 0; p < 13; p++)
            for (int sq = 0; sq < 64; sq++)
                PIECE_KEYS[p][sq] = rng.nextLong();
        for (int i = 0; i < 16; i++)
            CASTLING_KEYS[i] = rng.nextLong();
        for (int i = 0; i < 9; i++)
            EP_KEYS[i] = rng.nextLong();
        SIDE_KEY = rng.nextLong();
    }

    private static int pieceIndex(int piece) {
        if (piece == 0) return 0;
        int absP = Math.abs(piece);
        if (absP >= 1 && absP <= 6) {
            int idx = (piece > 0) ? absP : absP + 6;
            return idx;
        }
        return 0;
    }

    private static int sqIndex(int row, int col) {
        return row * 8 + col;
    }

    public static long computeHash(int[][] board, boolean whiteToMove,
                                    int castlingRights, int enPassantCol) {
        long hash = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p != 0) {
                    int pi = pieceIndex(p);
                    hash ^= PIECE_KEYS[pi][sqIndex(r, c)];
                }
            }
        if (whiteToMove)
            hash ^= SIDE_KEY;
        hash ^= CASTLING_KEYS[castlingRights & 0xF];
        if (enPassantCol >= 0 && enPassantCol < 8)
            hash ^= EP_KEYS[enPassantCol + 1];
        return hash;
    }

    static long computeHash(BoardState s) {
        return computeHash(s.board, s.whiteToMove, s.castlingRights, s.enPassantCol);
    }

    public static long randomNumber(int col, int row, int code) {
        int pi = pieceIndex(code);
        return PIECE_KEYS[pi][sqIndex(row, col)];
    }

    public static long sideRandomNumber() {
        return SIDE_KEY;
    }

    public static long castlingRandomNumber(int idx) {
        return CASTLING_KEYS[idx];
    }

    public static long epRandomNumber(int col) {
        if (col < 0 || col >= 8) return 0;
        return EP_KEYS[col + 1];
    }
}
