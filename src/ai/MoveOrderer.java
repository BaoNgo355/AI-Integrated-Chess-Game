package ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static ai.Evaluator.*;

class MoveOrderer {
    private static final int TT_ZONE = 10_000_000;
    private static final int WINNING_CAPTURE_ZONE = 5_000_000;
    private static final int NON_CAPTURE_ZONE = 0;
    private static final int LOSING_CAPTURE_ZONE = -1_000_000;
    private static final int CHECK_ORDERING_BONUS = 1000;

    private final CheckDetector checkDetector;
    private final MoveApplier moveApplier;

    MoveOrderer(CheckDetector checkDetector, MoveApplier moveApplier) {
        this.checkDetector = checkDetector;
        this.moveApplier = moveApplier;
    }

    void orderMoves(List<SMove> moves, BoardState s) {
        moves.sort((a, b) -> moveScore(b, s, null) - moveScore(a, s, null));
    }

    void orderMoves(List<SMove> moves, MoveOrderingContext ctx) {
        Map<SMove, Integer> ranks = ctx.policyRanks;
        int depthForKillers = Math.max(ctx.depth, ctx.ply);
        int[] kMoves = ctx.killerMoves != null ? ctx.killerMoves.getKillers(depthForKillers) : null;
        moves.sort((a, b) -> moveScoreWithContext(b, ctx, ranks, kMoves)
                           - moveScoreWithContext(a, ctx, ranks, kMoves));
    }

    private int moveScore(SMove m, BoardState s, MoveOrderingContext ctx) {
        int score = 0;
        if (ctx != null && ctx.ttBestMove != null && movesEqual(m, ctx.ttBestMove))
            score += 10_000_000;

        if (m.promoteTo != 0)
            score += 5000 + pieceValue(Math.abs(m.promoteTo));

        if (m.captured != EMPTY) {
            int victim = pieceValue(Math.abs(m.captured));
            int attacker = pieceValue(Math.abs(s.board[m.fr][m.fc]));
            score += victim * 10 - attacker;
        }

        if (m.captured != EMPTY)
            score += see(s, m.fr, m.fc, m.tr, m.tc) * 2;

        if (m.isCastling) score += 50;

        int piece = s.board[m.fr][m.fc];
        int absPiece = Math.abs(piece);
        if (absPiece == W_KNIGHT || absPiece == W_BISHOP) {
            int backRank = (piece > 0) ? 7 : 0;
            if (m.fr == backRank)
                score += DEVELOPMENT_MOVE_BONUS;
        }

        BoardState tmp = s.copy();
        moveApplier.applyMove(tmp, m);
        if (checkDetector.isInCheck(tmp, s.whiteToMove ? BLACK : WHITE))
            score += CHECK_ORDERING_BONUS;

        return score;
    }

    private int moveScoreWithContext(SMove m, MoveOrderingContext ctx,
                                      Map<SMove, Integer> policyRanks, int[] killerMoves) {
        BoardState s = ctx.state;
        AIConfig cfg = ctx.config;

        if (ctx.ttBestMove != null && movesEqual(m, ctx.ttBestMove))
            return TT_ZONE;

        int rawSee = m.captured != EMPTY ? see(s, m.fr, m.fc, m.tr, m.tc) : 0;

        if (m.captured != EMPTY && rawSee >= 0) {
            int victim = pieceValue(Math.abs(m.captured));
            int attacker = pieceValue(Math.abs(s.board[m.fr][m.fc]));
            int score = WINNING_CAPTURE_ZONE + victim * 10 - attacker;
            if (m.promoteTo != 0) score += 1000;
            return score;
        }

        if (m.captured != EMPTY && rawSee < 0) {
            int scaledSee = rawSee * cfg.seeScale / 100;
            int score = LOSING_CAPTURE_ZONE + scaledSee;
            return score;
        }

        int score = NON_CAPTURE_ZONE;

        if (m.promoteTo != 0)
            score += 50000 + pieceValue(Math.abs(m.promoteTo));

        if (killerMoves != null) {
            int fromSq = m.fr * 8 + m.fc;
            int toSq = m.tr * 8 + m.tc;
            int moveKey = fromSq * 64 + toSq;
            for (int i = 0; i < killerMoves.length; i++) {
                if (killerMoves[i] == moveKey) {
                    score += (i == 0) ? cfg.killerBonus : cfg.killerBonus * 2 / 3;
                    break;
                }
            }
        }

        if (policyRanks != null && m.captured == EMPTY && m.promoteTo == 0
            && ctx.ply <= cfg.policyPlyLimit) {
            Integer rank = policyRanks.get(m);
            if (rank != null && rank <= cfg.policyTopK) {
                int weight = getPolicyWeight(ctx);
                score += weight / rank;
            }
        }

        if (ctx.historyTable != null) {
            int fromSq = m.fr * 8 + m.fc;
            int toSq = m.tr * 8 + m.tc;
            score += ctx.historyTable.get(fromSq, toSq) * cfg.historyScale;
        }

        if (m.isCastling) score += 1000;

        if (ctx.counterMoveTable != null && ctx.prevMovePiece != 0) {
            Move counter = ctx.counterMoveTable.get(ctx.prevMovePiece, ctx.prevMoveToSq);
            if (counter != null && movesEqual(m, counter)) {
                score += 8000;
            }
        }

        int piece = s.board[m.fr][m.fc];
        int absPiece = Math.abs(piece);
        if (absPiece == W_KNIGHT || absPiece == W_BISHOP) {
            int backRank = (piece > 0) ? 7 : 0;
            if (m.fr == backRank)
                score += 500;
        }

        BoardState tmp = s.copy();
        moveApplier.applyMove(tmp, m);
        if (checkDetector.isInCheck(tmp, s.whiteToMove ? BLACK : WHITE))
            score += 3000;

        return score;
    }

    private int getPolicyWeight(MoveOrderingContext ctx) {
        double phase = Evaluator.gamePhaseRatioStatic(ctx.state);
        if (phase > 0.7) return ctx.config.policyWeightOpening;
        if (phase > 0.3) return ctx.config.policyWeightMiddlegame;
        return ctx.config.policyWeightEndgame;
    }

    private boolean movesEqual(SMove m, Move mv) {
        return m.fc == mv.fromCol && m.fr == mv.fromRow
            && m.tc == mv.toCol && m.tr == mv.toRow;
    }

    int see(BoardState s, int fr, int fc, int tr, int tc) {
        BoardState copy = s.copy();

        int attackerPiece = copy.board[fr][fc];
        int victimPiece = copy.board[tr][tc];

        copy.board[tr][tc] = attackerPiece;
        copy.board[fr][fc] = EMPTY;

        ArrayList<Integer> victims = new ArrayList<>();
        victims.add(pieceValue(Math.abs(victimPiece)));

        int side = (attackerPiece > 0) ? BLACK : WHITE;

        while (true) {
            int[] attacker = findLeastValuableAttacker(copy, tr, tc, side);
            if (attacker == null) break;

            int curDefender = copy.board[tr][tc];
            victims.add(pieceValue(Math.abs(curDefender)));

            copy.board[tr][tc] = copy.board[attacker[0]][attacker[1]];
            copy.board[attacker[0]][attacker[1]] = EMPTY;

            side = (side == WHITE) ? BLACK : WHITE;
        }

        int value = 0;
        for (int i = victims.size() - 1; i >= 0; i--)
            value = victims.get(i) - value;

        return value;
    }

    private int[] findLeastValuableAttacker(BoardState s, int tr, int tc, int side) {
        int bestVal = Integer.MAX_VALUE;
        int[] result = null;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = s.board[r][c];
                if (piece == EMPTY) continue;
                if ((side == WHITE && piece < 0) || (side == BLACK && piece > 0)) continue;
                if (Math.abs(piece) == W_KING) continue;
                if (r == tr && c == tc) continue;

                if (checkDetector.doesPieceAttackSquare(s, r, c, piece, tr, tc)) {
                    int val = pieceValue(Math.abs(piece));
                    if (val < bestVal) {
                        bestVal = val;
                        result = new int[]{r, c};
                    }
                }
            }
        }

        return result;
    }

    private int pieceValue(int absType) {
        switch (absType) {
            case W_PAWN:   return PAWN_VAL;
            case W_KNIGHT: return KNIGHT_VAL;
            case W_BISHOP: return BISHOP_VAL;
            case W_ROOK:   return ROOK_VAL;
            case W_QUEEN:  return QUEEN_VAL;
            default: return 0;
        }
    }

    // ── new API (used by ChessAI) ────────────────────────────────────────

    public static void orderMoves(BoardState board, List<Move> moves,
                                   KillerMoves[] killers, HistoryTable history, int ply) {
        moves.sort((a, b) -> {
            int sa = scoreMove(a, killers, history, ply);
            int sb = scoreMove(b, killers, history, ply);
            return Integer.compare(sb, sa);
        });
    }

    private static int scoreMove(Move m, KillerMoves[] killers, HistoryTable history, int ply) {
        int score = 0;
        if (m.isCapture()) score += 1000000;
        if (m.isPromotion()) score += 500000;
        if (ply < killers.length) {
            if (m.equals(killers[ply].getFirst()))  score += 900000;
            if (m.equals(killers[ply].getSecond())) score += 800000;
        }
        score += history.get(m);
        return score;
    }
}
