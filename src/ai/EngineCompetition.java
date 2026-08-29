package ai;

/**
 * Engine-vs-engine match driver.
 *
 * <p>Two {@link ChessAI} instances play a fixed number of games against each
 * other, alternating colours each game.  Results are reported via
 * {@link ReportWriter}.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   java ai.EngineCompetition [games] [depth]
 * </pre>
 */
public final class EngineCompetition {

    public static void main(String[] args) {
        int games = 2;
        int depth = AIConfig.MATCH_PLY;

        if (args.length >= 1) games = Integer.parseInt(args[0]);
        if (args.length >= 2) depth = Integer.parseInt(args[1]);

        AIConfig.load("res/ai_config.properties");

        ChessAI engine1 = new ChessAI();
        ChessAI engine2 = new ChessAI();

        int score1 = 0, score2 = 0;

        for (int g = 0; g < games; g++) {
            boolean engine1White = (g % 2 == 0);
            System.out.printf("Game %d/%d  —  %s plays White%n",
                              g + 1, games, engine1White ? "Engine1" : "Engine2");

            String result = playGame(engine1White ? engine1 : engine2,
                                     engine1White ? engine2 : engine1,
                                     depth);

            switch (result) {
                case "1-0" -> { score1 += (engine1White ? 1 : 0);
                                score2 += (engine1White ? 0 : 1); }
                case "0-1" -> { score1 += (engine1White ? 0 : 1);
                                score2 += (engine1White ? 1 : 0); }
                default    -> { score1 += 1; score2 += 1; }  // draw
            }

            ReportWriter.append(String.format("Game %d  result=%s", g + 1, result));
        }

        System.out.printf("%nFinal: Engine1=%.1f  Engine2=%.1f%n",
                          score1, score2);
        ReportWriter.append(String.format("Final: Engine1=%.1f  Engine2=%.1f", score1, score2));
    }

    /** Play a single game; return PGN result string. */
    private static String playGame(ChessAI white, ChessAI black, int depth) {
        BoardState board = BoardState.fromFEN(BoardState.STARTING_FEN);
        int moveCount = 0;

        while (moveCount < 300) {
            ChessAI current = board.whiteToMove ? white : black;
            String uci = current.bestMove(board, depth);

            if (uci == null) {
                return board.whiteToMove ? "0-1" : "1-0";
            }

            Move m = Move.fromUCI(uci, board);
            if (m == null) return board.whiteToMove ? "0-1" : "1-0";

            MoveApplier.apply(board, m);
            moveCount++;
        }
        return "1/2-1/2";   // 50-move / repetition draw
    }
}
