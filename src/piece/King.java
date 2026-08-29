package piece;

import java.util.List;
import state.GameState;
import common.PieceType;

public class King extends Piece {

    public King(int color, int col, int row) {
        super(color, col, row);
        type = PieceType.KING;
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> allPieces, GameState state) {
        if (!isWithinBoard(targetCol, targetRow)) return false;

        if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1
            || Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1) {
            if (isValidSquare(targetCol, targetRow, allPieces))
                return true;
        }

        if (moved) return false;

        if (targetCol == preCol + 2 && targetRow == preRow
            && !pieceIsOnStraightLine(targetCol, targetRow, allPieces)) {
            boolean destEmpty = true;
            for (Piece p : allPieces)
                if (p != this && p.col == preCol + 2 && p.row == preRow) { destEmpty = false; break; }
            if (destEmpty) {
                for (Piece piece : allPieces) {
                    if (piece.col == preCol + 3 && piece.row == preRow && !piece.moved) {
                        if (!state.isKingInCheck(color, allPieces)) {
                                int saveCol = this.col;
                                this.col = preCol + 1;
                                boolean check5 = state.isKingInCheck(color, allPieces);
                                this.col = preCol + 2;
                                boolean check6 = state.isKingInCheck(color, allPieces);
                                this.col = saveCol;
                                if (!check5 && !check6) {
                                    state.setCastlingRook(piece);
                                    return true;
                                }
                        }
                    }
                }
            }
        }

        if (targetCol == preCol - 2 && targetRow == preRow
            && !pieceIsOnStraightLine(targetCol, targetRow, allPieces)) {
            Piece p0 = null, p1 = null, p2 = null;
            for (Piece piece : allPieces) {
                if (piece.col == preCol - 3 && piece.row == targetRow) p0 = piece;
                if (piece != this && piece.col == preCol - 2 && piece.row == targetRow) p1 = piece;
                if (piece.col == preCol - 4 && piece.row == targetRow) p2 = piece;
            }
            if (p0 == null && p1 == null && p2 != null && !p2.moved) {
                if (!state.isKingInCheck(color, allPieces)) {
                        int saveCol = this.col;
                        this.col = preCol - 1;
                        boolean check3 = state.isKingInCheck(color, allPieces);
                        this.col = preCol - 2;
                        boolean check2 = state.isKingInCheck(color, allPieces);
                        this.col = saveCol;
                        if (!check3 && !check2) {
                            state.setCastlingRook(p2);
                            return true;
                        }
                }
            }
        }

        return false;
    }
}
