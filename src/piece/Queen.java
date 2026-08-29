package piece;

import java.util.List;
import state.GameState;
import common.PieceType;

public class Queen extends Piece {

    public Queen(int color, int col, int row) {
        super(color, col, row);
        type = PieceType.QUEEN;
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> allPieces, GameState state) {
        if (!isWithinBoard(targetCol, targetRow) || isSameSquare(targetCol, targetRow)) return false;
        if (targetCol == preCol || targetRow == preRow) {
            if (isValidSquare(targetCol, targetRow, allPieces)
                && !pieceIsOnStraightLine(targetCol, targetRow, allPieces))
                return true;
        }
        if (Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
            if (isValidSquare(targetCol, targetRow, allPieces)
                && !pieceIsOnDiagonalLine(targetCol, targetRow, allPieces))
                return true;
        }
        return false;
    }
}
