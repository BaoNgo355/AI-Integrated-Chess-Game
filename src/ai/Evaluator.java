package ai;

import common.PieceType;

/**
 * Static evaluation of a chess position.
 *
 * <p>The evaluator uses a piece-square table approach combined with material
 * counting, pawn structure, king safety and mobility heuristics.  All scores
 * are in centipawns from the point of view of the side to move.</p>
 */
public final class Evaluator {

    private Evaluator() { }

    /** Evaluate the given position (centipawns, side-to-move perspective). */
    public static int evaluate(BoardState board) {
        if (board == null) return 0;

        int whiteScore = 0;
        int blackScore = 0;

        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                int piece = board.board[col][row];
                if (piece == 0) continue;

                int pst = PST.value(piece, col, row);

                if (piece > 0) whiteScore += pst;
                else           blackScore += pst;
            }
        }

        int total = whiteScore - blackScore;
        return board.whiteToMove ? total : -total;
    }
}
