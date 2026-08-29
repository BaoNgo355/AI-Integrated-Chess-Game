package ai;

import piece.*;

/**
 * Fast check-detection utilities.
 *
 * <p>Every method answers the same question from a given side's point of view:
 * <em>"Is the specified king currently in check?"</em></p>
 */
public final class CheckDetector {

    private CheckDetector() { }

    /** Is the side-to-move's king in check right now? */
    public static boolean isKingInCheck(BoardState board) {
        return board.whiteToMove
                ? isSquareAttackedByBlack(board, board.whiteKingCol, board.whiteKingRow)
                : isSquareAttackedByWhite(board, board.blackKingCol, board.blackKingRow);
    }

    /** Is White attacking the given square? */
    public static boolean isSquareAttackedByWhite(BoardState board, int col, int row) {
        return isAttackedByWhitePawn  (board, col, row)
            || isAttackedByWhiteKnight(board, col, row)
            || isAttackedByWhiteDiag   (board, col, row)
            || isAttackedByWhiteOrtho  (board, col, row)
            || isAttackedByKing        (board, col, row, true);
    }

    /** Is Black attacking the given square? */
    public static boolean isSquareAttackedByBlack(BoardState board, int col, int row) {
        return isAttackedByBlackPawn  (board, col, row)
            || isAttackedByBlackKnight(board, col, row)
            || isAttackedByBlackDiag   (board, col, row)
            || isAttackedByBlackOrtho  (board, col, row)
            || isAttackedByKing        (board, col, row, false);
    }

    /** Is the given square attacked by any side (not used in mainline search)? */
    public static boolean isSquareAttacked(BoardState board, int col, int row,
                                           boolean byWhite) {
        return byWhite
                ? isSquareAttackedByWhite(board, col, row)
                : isSquareAttackedByBlack(board, col, row);
    }

    // ── pawn attacks ─────────────────────────────────────────────────────

    private static boolean isAttackedByWhitePawn(BoardState b, int col, int row) {
        if (row == 0) return false;
        if (b.inBounds(col - 1, row - 1) && b.board[col - 1][row - 1] == PieceType.WHITE_PAWN) return true;
        return b.inBounds(col + 1, row - 1) && b.board[col + 1][row - 1] == PieceType.WHITE_PAWN;
    }

    private static boolean isAttackedByBlackPawn(BoardState b, int col, int row) {
        if (row == 7) return false;
        if (b.inBounds(col - 1, row + 1) && b.board[col - 1][row + 1] == PieceType.BLACK_PAWN) return true;
        return b.inBounds(col + 1, row + 1) && b.board[col + 1][row + 1] == PieceType.BLACK_PAWN;
    }

    // ── knight attacks ───────────────────────────────────────────────────

    private static final int[][] KNIGHT_OFFSETS = {
        {-2, -1}, {-1, -2}, {1, -2}, {2, -1},
        {-2,  1}, {-1,  2}, {1,  2}, {2,  1}
    };

    private static boolean isAttackedByWhiteKnight(BoardState b, int col, int row) {
        for (int[] off : KNIGHT_OFFSETS) {
            int c2 = col + off[0], r2 = row + off[1];
            if (b.inBounds(c2, r2) && b.board[c2][r2] == PieceType.WHITE_KNIGHT) return true;
        }
        return false;
    }

    private static boolean isAttackedByBlackKnight(BoardState b, int col, int row) {
        for (int[] off : KNIGHT_OFFSETS) {
            int c2 = col + off[0], r2 = row + off[1];
            if (b.inBounds(c2, r2) && b.board[c2][r2] == PieceType.BLACK_KNIGHT) return true;
        }
        return false;
    }

    // ── diagonal (bishop/queen) ──────────────────────────────────────────

    private static final int[][] DIAG_OFFSETS = {{1,1},{1,-1},{-1,1},{-1,-1}};

    private static boolean isAttackedByWhiteDiag(BoardState b, int col, int row) {
        for (int[] off : DIAG_OFFSETS) {
            int c = col + off[0], r = row + off[1];
            while (b.inBounds(c, r)) {
                int p = b.board[c][r];
                if (p != 0) {
                    if (p == PieceType.WHITE_BISHOP || p == PieceType.WHITE_QUEEN) return true;
                    break;
                }
                c += off[0]; r += off[1];
            }
        }
        return false;
    }

    private static boolean isAttackedByBlackDiag(BoardState b, int col, int row) {
        for (int[] off : DIAG_OFFSETS) {
            int c = col + off[0], r = row + off[1];
            while (b.inBounds(c, r)) {
                int p = b.board[c][r];
                if (p != 0) {
                    if (p == PieceType.BLACK_BISHOP || p == PieceType.BLACK_QUEEN) return true;
                    break;
                }
                c += off[0]; r += off[1];
            }
        }
        return false;
    }

    // ── orthogonal (rook/queen) ──────────────────────────────────────────

    private static final int[][] ORTHO_OFFSETS = {{1,0},{-1,0},{0,1},{0,-1}};

    private static boolean isAttackedByWhiteOrtho(BoardState b, int col, int row) {
        for (int[] off : ORTHO_OFFSETS) {
            int c = col + off[0], r = row + off[1];
            while (b.inBounds(c, r)) {
                int p = b.board[c][r];
                if (p != 0) {
                    if (p == PieceType.WHITE_ROOK || p == PieceType.WHITE_QUEEN) return true;
                    break;
                }
                c += off[0]; r += off[1];
            }
        }
        return false;
    }

    private static boolean isAttackedByBlackOrtho(BoardState b, int col, int row) {
        for (int[] off : ORTHO_OFFSETS) {
            int c = col + off[0], r = row + off[1];
            while (b.inBounds(c, r)) {
                int p = b.board[c][r];
                if (p != 0) {
                    if (p == PieceType.BLACK_ROOK || p == PieceType.BLACK_QUEEN) return true;
                    break;
                }
                c += off[0]; r += off[1];
            }
        }
        return false;
    }

    // ── king attacks ─────────────────────────────────────────────────────

    private static final int[][] KING_OFFSETS = {
        {-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}
    };

    private static boolean isAttackedByKing(BoardState b, int col, int row,
                                            boolean whiteKing) {
        int target = whiteKing ? PieceType.WHITE_KING : PieceType.BLACK_KING;
        for (int[] off : KING_OFFSETS) {
            int c2 = col + off[0], r2 = row + off[1];
            if (b.inBounds(c2, r2) && b.board[c2][r2] == target) return true;
        }
        return false;
    }
}
