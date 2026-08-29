package ai;

/**
 * An adapter that makes {@link ChessAI} usable as the "human-controlled" side.
 * The game loop calls {@link #getBestMove()} when it is the player's turn;
 * the value returned is the engine's best suggestion, which can be shown as a
 * hint or played automatically.
 */
public class AIPlayer {

    private final ChessAI ai;
    private BoardState board;

    public AIPlayer(ChessAI ai) {
        this.ai = ai;
    }

    /** Set (or change) the board this player looks at. */
    public void setBoard(BoardState board) {
        this.board = board;
    }

    /**
     * Return the best move the engine can find from the current position.
     * The engine is configured through the static fields of {@link AIConfig}.
     *
     * @return the best move in UCI notation, or {@code null} if something
     *         went wrong or no legal moves exist.
     */
    public String getBestMove() {
        if (board == null) return null;
        return ai.bestMove(board, AIConfig.HINT_PLY);
    }

    /**
     * Return the best move using an explicit depth, ignoring
     * {@link AIConfig#HINT_PLY}.
     */
    public String getBestMove(int depth) {
        if (board == null) return null;
        return ai.bestMove(board, depth);
    }

    /** Return the underlying engine instance. */
    public ChessAI getEngine() { return ai; }
}
