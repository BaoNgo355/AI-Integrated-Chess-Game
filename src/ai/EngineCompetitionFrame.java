package ai;

import javax.swing.*;
import java.awt.*;

/**
 * Simple Swing UI for running an engine-vs-engine match and displaying
 * the live score and move list.
 */
public class EngineCompetitionFrame extends JFrame {

    private final JTextArea logArea = new JTextArea(20, 50);
    private final JLabel statusLabel = new JLabel("Ready");

    public EngineCompetitionFrame() {
        super("Engine Competition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    public void appendLine(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setStatus(String s) { statusLabel.setText(s); }

    /** Launch a competition in a background thread. */
    public void startMatch(int games, int depth) {
        new Thread(() -> {
            AIConfig.load("res/ai_config.properties");
            ChessAI engine1 = new ChessAI();
            ChessAI engine2 = new ChessAI();

            int s1 = 0, s2 = 0;
            for (int g = 0; g < games; g++) {
                boolean e1White = (g % 2 == 0);
                appendLine("Game " + (g + 1) + "/" + games + "  "
                           + (e1White ? "Engine1" : "Engine2") + " plays White");

                String result = runGame(e1White ? engine1 : engine2,
                                        e1White ? engine2 : engine1, depth);

                switch (result) {
                    case "1-0" -> { s1 += (e1White ? 1 : 0); s2 += (e1White ? 0 : 1); }
                    case "0-1" -> { s1 += (e1White ? 0 : 1); s2 += (e1White ? 1 : 0); }
                    default    -> { s1++; s2++; }
                }
                appendLine("  Result: " + result);
            }
            appendLine(String.format("Final: Engine1=%.1f  Engine2=%.1f", s1, s2));
            setStatus("Finished");
        }).start();
    }

    private String runGame(ChessAI white, ChessAI black, int depth) {
        BoardState board = BoardState.fromFEN(BoardState.STARTING_FEN);
        int moves = 0;
        while (moves < 300) {
            ChessAI cur = board.whiteToMove ? white : black;
            String uci = cur.bestMove(board, depth);
            if (uci == null) return board.whiteToMove ? "0-1" : "1-0";
            Move m = Move.fromUCI(uci, board);
            if (m == null) return board.whiteToMove ? "0-1" : "1-0";
            MoveApplier.apply(board, m);
            moves++;
        }
        return "1/2-1/2";
    }
}
