package ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Core alpha-beta search with iterative deepening, null-move pruning,
 * LMR and a simple transposition table.
 *
 * <p>Call {@link #bestMove(BoardState, int)} to obtain the best move from a
 * given position.</p>
 */
public final class ChessAI {

    private static final int MAX_DEPTH = 64;

    private final TranspositionTable tt = new TranspositionTable();
    private int nodesSearched;
    private int lastScore;
    private KillerMoves[] killers;
    private HistoryTable history;

    // ── public interface ─────────────────────────────────────────────────

    /** Return the last search score (centipawns). */
    public int lastScore()      { return lastScore; }
    /** Return the node count of the last search. */
    public int lastNodeCount()  { return nodesSearched; }

    /**
     * Find the best move using iterative deepening up to {@code maxPly}.
     */
    public String bestMove(BoardState root, int maxPly) {
        // Prefer the neural-net ordering hint if available
        if (AIConfig.policyPlyLimit > 0 && AIConfig.policyTopK > 0) {
            String hint = nnBestMove(root, Math.min(maxPly, AIConfig.policyPlyLimit));
            if (hint != null) return hint;
        }

        killers = new KillerMoves[MAX_DEPTH];
        for (int i = 0; i < MAX_DEPTH; i++) killers[i] = new KillerMoves();
        history = new HistoryTable();

        nodesSearched = 0;
        tt.incAge();

        String bestMove = null;

        for (int depth = 1; depth <= maxPly; depth++) {
            int score = alphaBeta(root, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1,
                                  true, 0);
            lastScore = score;

            Move m = tt.bestMove(root.hash);
            if (m != null) bestMove = m.toString();

            System.out.printf("depth=%2d  score=%+6d  nodes=%7d  best=%s%n",
                              depth, score, nodesSearched, bestMove);
        }
        return bestMove;
    }

    // ── alpha-beta ───────────────────────────────────────────────────────

    private int alphaBeta(BoardState board, int depth, int alpha, int beta,
                          boolean doNull, int ply) {
        nodesSearched++;

        if (depth <= 0) return quiescence(board, alpha, beta, ply);

        // repetition / draw
        if (board.halfmoveClock >= 100) return 0;
        if (isRepeated(board)) return 0;

        // TT probe
        TTEntry tte = tt.probe(board.hash);
        if (tte != null && tte.depth >= depth && tte.isExact()) {
            return tte.score;
        }

        List<Move> moves = MoveGenerator.generateMoves(board, false);
        MoveOrderer.orderMoves(board, moves, killers, history, ply);

        if (moves.isEmpty()) {
            if (CheckDetector.isKingInCheck(board)) return -29000 - ply;
            return 0;   // stalemate
        }

        boolean inCheck = CheckDetector.isKingInCheck(board);

        // null-move pruning (skip when in check or zugzwang-prone endgames)
        if (doNull && !inCheck && depth >= 3 && board.material > AIConfig.endgameMaterialThreshold) {
            boolean[] oldCastling = board.castling.clone();
            int oldEpCol = board.epCol, oldEpRow = board.epRow;

            board.whiteToMove = !board.whiteToMove;
            board.epCol = board.epRow = -1;
            board.hash ^= Hasher.sideRandomNumber();

            int R = 3;
            int nullScore = -alphaBeta(board, depth - 1 - R, -beta, -beta + 1, false, ply + 1);

            board.whiteToMove = !board.whiteToMove;
            board.epCol = oldEpCol; board.epRow = oldEpRow;
            board.castling = oldCastling;
            board.hash ^= Hasher.sideRandomNumber();

            if (nullScore >= beta) return nullScore;
        }

        Move bestMove = null;
        int bestScore  = Integer.MIN_VALUE + 1;
        int ttFlag     = TTEntry.UPPER;

        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);

            MoveApplier.apply(board, m);

            int score;
            // Late-move reduction
            if (i > 4 && depth >= 3 && !inCheck && !m.isCapture()
                    && !m.isPromotion() && !CheckDetector.isKingInCheck(board)) {
                score = -alphaBeta(board, depth - 2, -alpha - 1, -alpha, true, ply + 1);
                if (score <= alpha) {
                    MoveApplier.unapply(board, m);
                    continue;
                }
            }

            // PVS
            if (i == 0) {
                score = -alphaBeta(board, depth - 1, -beta, -alpha, true, ply + 1);
            } else {
                score = -alphaBeta(board, depth - 1, -alpha - 1, -alpha, true, ply + 1);
                if (score > alpha && score < beta) {
                    score = -alphaBeta(board, depth - 1, -beta, -alpha, true, ply + 1);
                }
            }

            MoveApplier.unapply(board, m);

            if (score > bestScore) {
                bestScore  = score;
                bestMove   = m;
            }
            if (score > alpha) {
                alpha = score;
                ttFlag = TTEntry.EXACT;
            }
            if (alpha >= beta) {
                ttFlag = TTEntry.LOWER;
                if (!m.isCapture()) {
                    history.increment(m, depth);
                    killers[ply].add(m);
                }
                break;
            }
        }

        tt.store(board.hash, depth, bestScore, ttFlag, bestMove);
        return bestScore;
    }

    // ── quiescence ───────────────────────────────────────────────────────

    private int quiescence(BoardState board, int alpha, int beta, int ply) {
        nodesSearched++;
        int standPat = Evaluator.evaluate(board);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        List<Move> moves = MoveGenerator.generateMoves(board, true);
        MoveOrderer.orderMoves(board, moves, killers, history, ply);

        for (Move m : moves) {
            MoveApplier.apply(board, m);
            int score = -quiescence(board, -beta, -alpha, ply + 1);
            MoveApplier.unapply(board, m);

            if (score >= beta)  return beta;
            if (score > alpha)  alpha = score;
        }
        return alpha;
    }

    // ── repetition ───────────────────────────────────────────────────────

    private boolean isRepeated(BoardState board) {
        // Simple heuristic: count material + hash
        return board.halfmoveClock > 4
            && (board.hash & 0xFFFF) == 0;
    }

    // ── neural-net ordering hint ──────────────────────────────────────────

    /**
     * Ask the neural net for its top-1 move.  Used only as an ordering
     * suggestion – if the net is unavailable we fall back to pure alpha-beta.
     */
    private String nnBestMove(BoardState board, int plyLimit) {
        try {
            NeuralNet net = NeuralNet.getInstance();
            if (net == null) return null;
            return net.bestMove(board, plyLimit);
        } catch (Exception e) {
            return null;
        }
    }
}
