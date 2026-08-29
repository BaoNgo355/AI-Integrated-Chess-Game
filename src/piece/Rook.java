package piece;

import java.util.List;
import state.GameState;
import common.PieceType;

public class Rook extends Piece {

    public Rook(int color, int col, int row) {
        super(color, col, row);
        type = PieceType.ROOK;
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> allPieces, GameState state) {
        if (!isWithinBoard(targetCol, targetRow) || isSameSquare(targetCol, targetRow)) return false;
        if (targetCol == preCol || targetRow == preRow) {
            if (isValidSquare(targetCol, targetRow, allPieces)
                && !pieceIsOnStraightLine(targetCol, targetRow, allPieces))
                return true;
        }
        return false;
    }
}
