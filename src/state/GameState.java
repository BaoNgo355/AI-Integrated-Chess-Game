package state;

import java.util.ArrayList;
import java.util.List;
import piece.Piece;
import common.PieceType;

public class GameState {
    public static final int WHITE = 0;
    public static final int BLACK = 1;

    private int currentColor;
    private boolean gameOver;
    private String winner;
    private boolean checking;
    private Piece castlingRook;

    public GameState() {
        currentColor = WHITE;
    }

    public int getCurrentColor() { return currentColor; }
    public void setCurrentColor(int c) { currentColor = c; }
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean v) { gameOver = v; }
    public String getWinner() { return winner; }
    public void setWinner(String w) { winner = w; }
    public boolean isChecking() { return checking; }
    public void setChecking(boolean c) { checking = c; }
    public Piece getCastlingRook() { return castlingRook; }
    public void setCastlingRook(Piece r) { castlingRook = r; }

    public Piece getKing(int kingColor, List<Piece> pieceList) {
        for (Piece p : pieceList)
            if (p.type == PieceType.KING && p.color == kingColor) return p;
        return null;
    }

    public boolean isKingInCheck(int kingColor, List<Piece> pieceList) {
        Piece king = getKing(kingColor, pieceList);
        if (king == null) return false;
        for (Piece p : pieceList)
            if (p.color != kingColor && p.canMove(king.col, king.row, pieceList, this))
                return true;
        return false;
    }

    public boolean isCheckmate(int kingColor, List<Piece> pieceList) {
        if (!isKingInCheck(kingColor, pieceList)) return false;
        return !hasLegalMoves(kingColor, pieceList);
    }

    public boolean isStalemate(int kingColor, List<Piece> pieceList) {
        if (isKingInCheck(kingColor, pieceList)) return false;
        return !hasLegalMoves(kingColor, pieceList);
    }

    private boolean hasLegalMoves(int kingColor, List<Piece> pieceList) {
        for (Piece p : new ArrayList<>(pieceList)) {
            if (p.color != kingColor) continue;
            int origCol = p.col;
            int origRow = p.row;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (!p.canMove(c, r, pieceList, this)) continue;
                    List<Piece> backup = new ArrayList<>(pieceList);
                    p.col = c;
                    p.row = r;
                    Piece captured = p.hittingP;
                    if (captured != null) pieceList.remove(captured);
                    boolean inCheck = isKingInCheck(kingColor, pieceList);
                    pieceList.clear();
                    pieceList.addAll(backup);
                    p.col = origCol;
                    p.row = origRow;
                    if (!inCheck) return true;
                }
            }
        }
        return false;
    }

    public boolean isInsufficientMaterial(List<Piece> pieces) {
        List<Piece> whitePieces = new ArrayList<>();
        List<Piece> blackPieces = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.color == WHITE) whitePieces.add(p);
            else blackPieces.add(p);
        }
        if (whitePieces.size() == 1 && blackPieces.size() == 1)
            return true;
        if (whitePieces.size() == 2 && blackPieces.size() == 1) {
            for (Piece p : whitePieces)
                if (p.type == PieceType.KING) continue;
                else if (p.type == PieceType.KNIGHT || p.type == PieceType.BISHOP) return true;
                else return false;
        }
        if (whitePieces.size() == 1 && blackPieces.size() == 2) {
            for (Piece p : blackPieces)
                if (p.type == PieceType.KING) continue;
                else if (p.type == PieceType.KNIGHT || p.type == PieceType.BISHOP) return true;
                else return false;
        }
        if (whitePieces.size() == 2 && blackPieces.size() == 2) {
            Piece wBishop = null, bBishop = null;
            Piece wKnight = null, bKnight = null;
            for (Piece p : whitePieces) {
                if (p.type == PieceType.BISHOP) wBishop = p;
                if (p.type == PieceType.KNIGHT) wKnight = p;
            }
            for (Piece p : blackPieces) {
                if (p.type == PieceType.BISHOP) bBishop = p;
                if (p.type == PieceType.KNIGHT) bKnight = p;
            }
            if (wBishop != null && bBishop != null) {
                return (wBishop.col + wBishop.row) % 2 == (bBishop.col + bBishop.row) % 2;
            }
            if (wKnight != null && bKnight != null) {
                return true;
            }
        }
        return false;
    }

    public void changePlayer(List<Piece> pieces) {
        if (currentColor == WHITE) {
            currentColor = BLACK;
            for (Piece p : pieces)
                if (p.color == BLACK) p.twoStepped = false;
        } else {
            currentColor = WHITE;
            for (Piece p : pieces)
                if (p.color == WHITE) p.twoStepped = false;
        }
        checking = isKingInCheck(currentColor, pieces);
        if (checking && isCheckmate(currentColor, pieces)) {
            gameOver = true;
            winner = currentColor == WHITE ? "Black wins" : "White wins";
        } else if (!checking && isStalemate(currentColor, pieces)) {
            gameOver = true;
            winner = "Draw (Stalemate)";
        } else if (isInsufficientMaterial(pieces)) {
            gameOver = true;
            winner = "Draw (Insufficient Material)";
        }
    }

    public int[][] buildBoardArray(List<Piece> pieces) {
        int[][] arr = new int[8][8];
        for (Piece p : pieces) {
            int code = 0;
            switch (p.type) {
                case PAWN: code = 1; break;
                case KNIGHT: code = 2; break;
                case BISHOP: code = 3; break;
                case ROOK: code = 4; break;
                case QUEEN: code = 5; break;
                case KING: code = 6; break;
            }
            if (p.color == BLACK) code = -code;
            arr[p.row][p.col] = code;
        }
        return arr;
    }

    public int getEnPassantCol(List<Piece> pieces) {
        int opponent = currentColor == WHITE ? BLACK : WHITE;
        for (Piece p : pieces)
            if (p.type == PieceType.PAWN && p.color == opponent && p.twoStepped)
                return p.col;
        return -1;
    }

    public int buildCastlingRights(List<Piece> pieces) {
        int rights = 0;
        for (Piece p : pieces) {
            if (p.type == PieceType.KING && !p.moved) {
                for (Piece r : pieces) {
                    if (r.type == PieceType.ROOK && !r.moved) {
                        if (p.color == WHITE) {
                            if (r.col == 7) rights |= 1;
                            if (r.col == 0) rights |= 2;
                        } else {
                            if (r.col == 7) rights |= 4;
                            if (r.col == 0) rights |= 8;
                        }
                    }
                }
            }
        }
        return rights;
    }

    public int findKingRow(boolean isWhite, List<Piece> pieces) {
        int color = isWhite ? WHITE : BLACK;
        for (Piece p : pieces)
            if (p.type == PieceType.KING && p.color == color) return p.row;
        return isWhite ? 7 : 0;
    }

    public int findKingCol(boolean isWhite, List<Piece> pieces) {
        int color = isWhite ? WHITE : BLACK;
        for (Piece p : pieces)
            if (p.type == PieceType.KING && p.color == color) return p.col;
        return 4;
    }
}
