package ai;

import java.util.Map;

public class MoveOrderingContext {
    public int ply;
    public int depth;
    public Move ttBestMove;
    public Map<SMove, Integer> policyRanks;
    public KillerMoves killerMoves;
    public HistoryTable historyTable;
    public CounterMoveTable counterMoveTable;
    public BoardState state;
    public AIConfig config;
    public int prevMovePiece;
    public int prevMoveToSq;

    public MoveOrderingContext(BoardState state, AIConfig config) {
        this.state = state;
        this.config = config;
    }
}
