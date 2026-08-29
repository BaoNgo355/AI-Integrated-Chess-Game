package ai;

import java.util.ArrayList;
import java.util.List;
import static ai.Evaluator.*;

class MoveGenerator {
    private final CheckDetector checkDetector;
    private final MoveApplier moveApplier;

    MoveGenerator(CheckDetector checkDetector, MoveApplier moveApplier) {
        this.checkDetector = checkDetector;
        this.moveApplier = moveApplier;
    }

    List<SMove> generateLegalMoves(BoardState s) {
        List<SMove> pseudo = generatePseudoLegalMoves(s);
        List<SMove> legal = new ArrayList<>();
        for (SMove m : pseudo) {
            BoardState copy = s.copy();
            moveApplier.applyMove(copy, m);
            if (!checkDetector.isInCheck(copy, s.whiteToMove ? WHITE : BLACK))
                legal.add(m);
        }
        return legal;
    }

    private List<SMove> generatePseudoLegalMoves(BoardState s) {
        List<SMove> out = new ArrayList<>();
        int turn = s.whiteToMove ? WHITE : BLACK;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int p = s.board[r][c];
                if ((turn == WHITE && p > 0) || (turn == BLACK && p < 0))
                    addPseudoPieceMoves(s, p, r, c, out);
            }
        }
        return out;
    }

    private void addPseudoPieceMoves(BoardState s, int piece, int r, int c, List<SMove> out) {
        switch (Math.abs(piece)) {
            case W_PAWN:   addPseudoPawnMoves(s, piece, r, c, out); break;
            case W_KNIGHT: addPseudoKnightMoves(s, piece, r, c, out); break;
            case W_BISHOP: addPseudoSlidingMoves(s, piece, r, c, out, true, false); break;
            case W_ROOK:   addPseudoSlidingMoves(s, piece, r, c, out, false, true); break;
            case W_QUEEN:  addPseudoSlidingMoves(s, piece, r, c, out, true, true); break;
            case W_KING:   addPseudoKingMoves(s, piece, r, c, out); break;
        }
    }

    private void addPseudoMove(SMove m, List<SMove> out) {
        out.add(m);
    }

    private void addPseudoPawnMoves(BoardState s, int piece, int r, int c, List<SMove> out) {
        int dir = (piece > 0) ? -1 : 1;
        int startRow = (piece > 0) ? 6 : 1;
        int promoRow = (piece > 0) ? 0 : 7;
        boolean isWhite = piece > 0;

        int nr = r + dir;
        if (checkDetector.inBounds(nr, c) && s.board[nr][c] == EMPTY) {
            if (nr == promoRow) {
                addPseudoPromoMoves(c, r, c, nr, 0, isWhite, out);
            } else {
                addPseudoMove(new SMove(r, c, nr, c), out);
            }
        }
        if (r == startRow) {
            nr = r + 2 * dir;
            if (checkDetector.inBounds(nr, c) && s.board[nr][c] == EMPTY && s.board[r + dir][c] == EMPTY)
                addPseudoMove(new SMove(r, c, nr, c), out);
        }
        for (int dc = -1; dc <= 1; dc += 2) {
            int nc = c + dc;
            int tr = r + dir;
            if (!checkDetector.inBounds(tr, nc)) continue;
            int target = s.board[tr][nc];
            if (target != EMPTY && (isWhite ? target < 0 : target > 0)) {
                if (tr == promoRow) {
                    addPseudoPromoMoves(c, r, nc, tr, target, isWhite, out);
                } else {
                    SMove m = new SMove(r, c, tr, nc);
                    m.captured = target;
                    addPseudoMove(m, out);
                }
            }
            if (tr == (isWhite ? 2 : 5) && nc == s.enPassantCol) {
                SMove m = new SMove(r, c, tr, nc);
                m.isEnPassant = true;
                m.captured = s.board[r][nc];
                out.add(m);
            }
        }
    }

    private void addPseudoPromoMoves(int fc, int fr, int tc, int tr, int captured, boolean isWhite, List<SMove> out) {
        int[] types = isWhite
            ? new int[]{W_QUEEN, W_ROOK, W_BISHOP, W_KNIGHT}
            : new int[]{B_QUEEN, B_ROOK, B_BISHOP, B_KNIGHT};
        for (int t : types) {
            SMove m = new SMove(fr, fc, tr, tc);
            m.promoteTo = t;
            m.captured = captured;
            addPseudoMove(m, out);
        }
    }

    private void addPseudoKnightMoves(BoardState s, int piece, int r, int c, List<SMove> out) {
        int[][] d = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] dd : d) {
            int nr = r + dd[0], nc = c + dd[1];
            if (!checkDetector.inBounds(nr, nc)) continue;
            int target = s.board[nr][nc];
            if (target == EMPTY || (piece > 0 ? target < 0 : target > 0)) {
                SMove m = new SMove(r, c, nr, nc);
                m.captured = target;
                addPseudoMove(m, out);
            }
        }
    }

    private void addPseudoSlidingMoves(BoardState s, int piece, int r, int c, List<SMove> out, boolean diag, boolean straight) {
        if (diag) {
            int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
            for (int[] d : dirs) addPseudoRay(s, piece, r, c, d[0], d[1], out);
        }
        if (straight) {
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) addPseudoRay(s, piece, r, c, d[0], d[1], out);
        }
    }

    private void addPseudoRay(BoardState s, int piece, int r, int c, int dr, int dc, List<SMove> out) {
        int nr = r + dr, nc = c + dc;
        while (checkDetector.inBounds(nr, nc)) {
            int target = s.board[nr][nc];
            if (target == EMPTY) {
                addPseudoMove(new SMove(r, c, nr, nc), out);
            } else {
                if ((piece > 0 ? target < 0 : target > 0)) {
                    SMove m = new SMove(r, c, nr, nc);
                    m.captured = target;
                    addPseudoMove(m, out);
                }
                break;
            }
            nr += dr; nc += dc;
        }
    }

    private void addPseudoKingMoves(BoardState s, int piece, int r, int c, List<SMove> out) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (!checkDetector.inBounds(nr, nc)) continue;
                int target = s.board[nr][nc];
                if (target == EMPTY || (piece > 0 ? target < 0 : target > 0)) {
                    SMove m = new SMove(r, c, nr, nc);
                    m.captured = target;
                    addPseudoMove(m, out);
                }
            }
        }
        int idx = (piece > 0) ? 0 : 2;
        int backRank = (piece > 0) ? 7 : 0;
        int rook = (piece > 0) ? W_ROOK : B_ROOK;
        int color = (piece > 0) ? WHITE : BLACK;

        if (s.canCastle(idx)
            && s.board[backRank][5] == EMPTY && s.board[backRank][6] == EMPTY
            && s.board[backRank][7] == rook) {
            if (!checkDetector.isInCheck(s, color)) {
                BoardState t1 = s.copy();
                t1.board[backRank][5] = piece; t1.board[backRank][4] = EMPTY;
                if (!checkDetector.isInCheck(t1, color)) {
                    BoardState t2 = s.copy();
                    t2.board[backRank][6] = piece; t2.board[backRank][4] = EMPTY;
                    if (!checkDetector.isInCheck(t2, color)) {
                        SMove m = new SMove(backRank, 4, backRank, 6);
                        m.isCastling = true;
                        m.rookFr = backRank; m.rookFc = 7;
                        m.rookTr = backRank; m.rookTc = 5;
                        addPseudoMove(m, out);
                    }
                }
            }
        }
        if (s.canCastle(idx + 1)
            && s.board[backRank][1] == EMPTY && s.board[backRank][2] == EMPTY
            && s.board[backRank][3] == EMPTY && s.board[backRank][0] == rook) {
            if (!checkDetector.isInCheck(s, color)) {
                BoardState t1 = s.copy();
                t1.board[backRank][3] = piece; t1.board[backRank][4] = EMPTY;
                if (!checkDetector.isInCheck(t1, color)) {
                    BoardState t2 = s.copy();
                    t2.board[backRank][2] = piece; t2.board[backRank][4] = EMPTY;
                    if (!checkDetector.isInCheck(t2, color)) {
                        SMove m = new SMove(backRank, 4, backRank, 2);
                        m.isCastling = true;
                        m.rookFr = backRank; m.rookFc = 0;
                        m.rookTr = backRank; m.rookTc = 3;
                        addPseudoMove(m, out);
                    }
                }
            }
        }
    }

    // ── new API (used by ChessAI) ────────────────────────────────────────

    public static List<Move> generateMoves(BoardState board, boolean capturesOnly) {
        List<SMove> pseudo = new MoveGenerator(new CheckDetector(), new MoveApplier())
                .generatePseudoLegalMoves(board);
        List<Move> result = new ArrayList<>();
        for (SMove sm : pseudo) {
            if (capturesOnly && sm.captured == EMPTY) continue;
            result.add(new Move(sm.fc, sm.fr, sm.tc, sm.tr));
        }
        return result;
    }
}
