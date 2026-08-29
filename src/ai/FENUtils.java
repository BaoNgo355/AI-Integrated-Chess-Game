package ai;

import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Minimal FEN utilities used outside the main {@link BoardState} parser.
 */
public final class FENUtils {

    /** Starting-position FEN. */
    public static final String START_FEN =
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private FENUtils() { }

    /** Load a {@link BoardState} from a FEN string (delegates to {@code BoardState}). */
    public static BoardState loadBoard(String fen) {
        return BoardState.fromFEN(fen);
    }

    /**
     * Return a human-readable string describing which squares attack the
     * given square.
     */
    public static String attacksOn(String fen, String square) {
        BoardState board = BoardState.fromFEN(fen);
        if (board == null) return "Invalid FEN";

        int col = "abcdefgh".indexOf(square.charAt(0));
        int row = square.charAt(1) - '1';
        if (col < 0 || row < 0 || col > 7 || row > 7) return "Invalid square";

        StringBuilder sb = new StringBuilder();
        if (CheckDetector.isSquareAttackedByWhite(board, col, row)) {
            sb.append("White attacks: ");
            for (int c = 0; c < 8; c++)
                for (int r = 0; r < 8; r++)
                    if (board.board[c][r] > 0
                        && CheckDetector.isSquareAttackedBy(board, c, r, true)
                        && c == col && r == row)
                        sb.append(PieceType.codeToChar(board.board[c][r]))
                          .append("(").append("abcdefgh".charAt(c)).append(r+1).append(") ");
        }
        if (CheckDetector.isSquareAttackedByBlack(board, col, row)) {
            sb.append("Black attacks: ");
            for (int c = 0; c < 8; c++)
                for (int r = 0; r < 8; r++)
                    if (board.board[c][r] < 0
                        && CheckDetector.isSquareAttackedBy(board, c, r, false)
                        && c == col && r == row)
                        sb.append(PieceType.codeToChar(board.board[c][r]))
                          .append("(").append("abcdefgh".charAt(c)).append(r+1).append(") ");
        }
        if (sb.length() == 0) sb.append("No attacks on ").append(square);
        return sb.toString();
    }
}
