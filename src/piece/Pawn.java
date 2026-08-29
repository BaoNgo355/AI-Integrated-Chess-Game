package piece;

import java.util.List;
import state.GameState;
import common.PieceType;

public class Pawn extends Piece {

    public Pawn(int color, int col, int row) {
        super(color, col, row);
        type = PieceType.PAWN;
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> allPieces, GameState state) {
        if (!isWithinBoard(targetCol, targetRow) || isSameSquare(targetCol, targetRow)) return false;

        int moveValue = color == GameState.WHITE ? -1 : 1;
        hittingP = getHittingP(targetCol, targetRow, allPieces);

        if (targetCol == preCol && targetRow == preRow + moveValue && hittingP == null)
            return true;

        if (targetCol == preCol && targetRow == preRow + moveValue * 2
            && hittingP == null && !moved
            && !pieceIsOnStraightLine(targetCol, targetRow, allPieces))
            return true;

        if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue
            && hittingP != null && hittingP.color != color)
            return true;

        if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
            for (Piece piece : allPieces) {
                if (piece.col == targetCol && piece.row == preRow
                    && piece.twoStepped && piece.color != color) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        return false;
    }
}
