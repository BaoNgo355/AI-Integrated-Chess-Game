package piece;

import java.util.List;
import state.GameState;
import common.PieceType;

public class Piece {
    public PieceType type;
    public int x, y;
    public int col, row, preCol, preRow;
    public int color;
    public Piece hittingP;
    public boolean moved, twoStepped;

    public Piece(int color, int row, int col) {
        this.color = color;
        this.col = col;
        this.row = row;
        x = getX(col);
        y = getY(row);
        preCol = col;
        preRow = row;
    }

    public int getX(int col) {
        return (rendering.Board.blackPerspective ? 7 - col : col) * rendering.Board.SQUARE_SIZE;
    }

    public int getY(int row) {
        return (rendering.Board.blackPerspective ? 7 - row : row) * rendering.Board.SQUARE_SIZE;
    }

    public int getCol(int x) {
        int c = (x + rendering.Board.HALF_SQUARE_SIZE) / rendering.Board.SQUARE_SIZE;
        return rendering.Board.blackPerspective ? 7 - c : c;
    }

    public int getRow(int y) {
        int r = (y + rendering.Board.HALF_SQUARE_SIZE) / rendering.Board.SQUARE_SIZE;
        return rendering.Board.blackPerspective ? 7 - r : r;
    }

    public int getIndex(List<Piece> allPieces) {
        for (int i = 0; i < allPieces.size(); i++)
            if (allPieces.get(i) == this) return i;
        return -1;
    }

    public void updatePosition() {
        if (type == PieceType.PAWN && Math.abs(row - preRow) == 2)
            twoStepped = true;
        x = getX(col);
        y = getY(row);
        preCol = getCol(x);
        preRow = getRow(y);
        moved = true;
    }

    public void resetPosition() {
        col = preCol;
        row = preRow;
        x = getX(col);
        y = getY(row);
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> allPieces, GameState state) {
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public Piece getHittingP(int targetCol, int targetRow, List<Piece> allPieces) {
        for (Piece piece : allPieces)
            if (piece.col == targetCol && piece.row == targetRow && piece != this)
                return piece;
        return null;
    }

    public boolean isValidSquare(int targetCol, int targetRow, List<Piece> allPieces) {
        hittingP = getHittingP(targetCol, targetRow, allPieces);
        if (hittingP == null) return true;
        if (hittingP.color != color) return true;
        hittingP = null;
        return false;
    }

    public boolean pieceIsOnStraightLine(int targetCol, int targetRow, List<Piece> allPieces) {
        for (int c = preCol - 1; c > targetCol; c--)
            for (Piece piece : allPieces)
                if (piece.col == c && piece.row == targetRow) { hittingP = piece; return true; }
        for (int c = preCol + 1; c < targetCol; c++)
            for (Piece piece : allPieces)
                if (piece.col == c && piece.row == targetRow) { hittingP = piece; return true; }
        for (int r = preRow - 1; r > targetRow; r--)
            for (Piece piece : allPieces)
                if (piece.col == targetCol && piece.row == r) { hittingP = piece; return true; }
        for (int r = preRow + 1; r < targetRow; r++)
            for (Piece piece : allPieces)
                if (piece.col == targetCol && piece.row == r) { hittingP = piece; return true; }
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow, List<Piece> allPieces) {
        if (targetRow < preRow) {
            for (int c = preCol - 1; c > targetCol; c--) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : allPieces)
                    if (piece.col == c && piece.row == preRow - diff) { hittingP = piece; return true; }
            }
            for (int c = preCol + 1; c < targetCol; c++) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : allPieces)
                    if (piece.col == c && piece.row == preRow - diff) { hittingP = piece; return true; }
            }
        }
        if (targetRow > preRow) {
            for (int c = preCol - 1; c > targetCol; c--) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : allPieces)
                    if (piece.col == c && piece.row == preRow + diff) { hittingP = piece; return true; }
            }
            for (int c = preCol + 1; c < targetCol; c++) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : allPieces)
                    if (piece.col == c && piece.row == preRow + diff) { hittingP = piece; return true; }
            }
        }
        return false;
    }
}
