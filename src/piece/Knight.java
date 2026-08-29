package piece;

import java.util.List;
import state.GameState;
import common.PieceType;

public class Knight extends Piece {

    public Knight(int color, int col, int row) {
        super(color, col, row);
        type = PieceType.KNIGHT;
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> allPieces, GameState state) {
        if (!isWithinBoard(targetCol, targetRow)) return false;
        if (Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 2) {
            if (isValidSquare(targetCol, targetRow, allPieces))
                return true;
        }
        return false;
    }
}
