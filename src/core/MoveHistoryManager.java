package core;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import piece.Piece;
import common.PieceType;
import common.Config;
import state.GameState;

public class MoveHistoryManager {
    private final GamePanel gp;

    public MoveHistoryManager(GamePanel gp) {
        this.gp = gp;
    }

    public String toSAN(PieceType pieceType, int color, int fromCol, int fromRow,
                        int toCol, int toRow, boolean capture, int promoType, List<Piece> boardPieces) {
        if (pieceType == PieceType.KING && Math.abs(toCol - fromCol) == 2)
            return toCol > fromCol ? "O-O" : "O-O-O";

        StringBuilder san = new StringBuilder();
        if (pieceType != PieceType.PAWN) {
            san.append(pieceLetter(pieceType));
            String disambig = getDisambiguation(pieceType, color, fromCol, fromRow, toCol, toRow, boardPieces);
            san.append(disambig);
        }
        if (capture && pieceType == PieceType.PAWN) {
            san.append((char)('a' + fromCol));
            san.append('x');
        } else if (capture) {
            san.append('x');
        }
        san.append((char)('a' + toCol));
        san.append(8 - toRow);
        if (promoType > 0) {
            san.append('=');
            san.append(pieceLetter(promoTypeToType(promoType)));
        }
        return san.toString();
    }

    private String getDisambiguation(PieceType type, int color, int fromCol, int fromRow,
                                      int toCol, int toRow, List<Piece> boardPieces) {
        boolean needFile = false;
        boolean needRank = false;
        ArrayList<Piece> temp = new ArrayList<>(boardPieces);
        for (int i = temp.size() - 1; i >= 0; i--) {
            Piece p = temp.get(i);
            if (p.col == toCol && p.row == toRow) {
                temp.remove(i);
                break;
            }
        }
        for (Piece p : temp) {
            if (p.type != type || p.color != color) continue;
            int savedCol = p.col;
            int savedRow = p.row;
            Piece savedHit = p.hittingP;
            p.col = p.preCol;
            p.row = p.preRow;
            boolean canAlsoReach = p.canMove(toCol, toRow, temp, gp.state);
            p.col = savedCol;
            p.row = savedRow;
            p.hittingP = savedHit;
            if (canAlsoReach) {
                if (p.preCol == fromCol) needRank = true;
                if (p.preRow == fromRow) needFile = true;
            }
        }
        if (needFile && needRank) return "" + (char)('a' + fromCol) + (8 - fromRow);
        if (needFile) return "" + (char)('a' + fromCol);
        if (needRank) return "" + (8 - fromRow);
        return "";
    }

    private static char pieceLetter(PieceType t) {
        switch (t) {
            case KING: return 'K';
            case QUEEN: return 'Q';
            case ROOK: return 'R';
            case BISHOP: return 'B';
            case KNIGHT: return 'N';
            default: return ' ';
        }
    }

    public static PieceType promoTypeToType(int pt) {
        switch (pt) {
            case 2: return PieceType.KNIGHT;
            case 3: return PieceType.BISHOP;
            case 4: return PieceType.ROOK;
            case 5: return PieceType.QUEEN;
            default: return PieceType.QUEEN;
        }
    }

    public void recordMove(int color, int fromCol, int fromRow, int toCol, int toRow,
                           boolean capture, int promoType) {
        PieceType pieceType;
        if (promoType > 0) pieceType = PieceType.PAWN;
        else {
            Piece p = findPieceAt(toCol, toRow, gp.pieces);
            pieceType = p != null ? p.type : PieceType.PAWN;
        }
        boolean givesCheck = gp.state.isChecking();
        boolean givesCheckmate = false;
        if (givesCheck) {
            givesCheckmate = gp.state.isCheckmate(gp.state.getCurrentColor(), gp.pieces);
        }
        String san = toSAN(pieceType, color, fromCol, fromRow, toCol, toRow, capture, promoType, gp.pieces);
        if (givesCheckmate) san += "#";
        else if (givesCheck) san += "+";
        if (color == GameState.WHITE) {
            gp.moveHistory.add(new GamePanel.MovePair(gp.moveHistory.size() + 1, san));
        } else {
            if (!gp.moveHistory.isEmpty()) {
                GamePanel.MovePair last = gp.moveHistory.get(gp.moveHistory.size() - 1);
                last.black = san;
            }
        }
        int totalPixels = gp.moveHistory.size() * Config.MOVE_HISTORY_ROW_HEIGHT;
        int availablePixels = Config.SCROLL_AREA_BOTTOM - Config.SCROLL_AREA_TOP;
        int maxScroll = Math.max(0, totalPixels - availablePixels);
        gp.moveHistoryScrollY = maxScroll;
        gp.boardHistory.add(new GamePanel.BoardSnapshot(gp.pieces, gp.state.getCurrentColor(), fromCol, fromRow, toCol, toRow));
    }

    private Piece findPieceAt(int col, int row, List<Piece> list) {
        for (Piece p : list)
            if (p.col == col && p.row == row) return p;
        return null;
    }

    public void undoLastMoveAI() {
        if (gp.gameMode != 0 || gp.boardHistory.size() < 2) return;
        if (gp.viewingMode) gp.exitViewMode();
        if (gp.gameOver || gp.state.isGameOver()) {
            gp.gameOver = false;
            gp.state.setGameOver(false);
            gp.winner = null;
            gp.gameOverDialogShown = false;
        }

        gp.boardHistory.remove(gp.boardHistory.size() - 1);
        gp.boardHistory.remove(gp.boardHistory.size() - 1);

        if (gp.moveHistory.size() >= 1) {
            gp.moveHistory.remove(gp.moveHistory.size() - 1);
        }

        int totalPixels = gp.moveHistory.size() * Config.MOVE_HISTORY_ROW_HEIGHT;
        int availablePixels = Config.SCROLL_AREA_BOTTOM - Config.SCROLL_AREA_TOP;
        gp.moveHistoryScrollY = Math.min(gp.moveHistoryScrollY, Math.max(0, totalPixels - availablePixels));

        if (gp.positionHashes.size() >= 2) {
            gp.positionHashes.remove(gp.positionHashes.size() - 1);
            gp.positionHashes.remove(gp.positionHashes.size() - 1);
        }

        int restoreIdx = gp.boardHistory.size() - 1;
        if (!gp.boardHistory.isEmpty()) {
            if (gp.boardHistory.get(restoreIdx).currentColor != gp.playerColor && restoreIdx > 0)
                restoreIdx--;
            GamePanel.BoardSnapshot snap = gp.boardHistory.get(restoreIdx);
            restoreBoardFromSnapshot(snap);
        } else {
            gp.moveHistory.clear();
            gp.state = new GameState();
            gp.pieces.clear();
            gp.setPieces();
            gp.copyPieces(gp.pieces, gp.simPieces);
        }

        gp.state.setGameOver(false);
        gp.gameOver = false;
        gp.winner = null;
        gp.activeP = null;
        gp.canMove = false;
        gp.validSquare = false;
        gp.lastMoveFromCol = -1;
        gp.lastMoveFromRow = -1;
        gp.lastMoveToCol = -1;
        gp.lastMoveToRow = -1;
        gp.halfMoveClock = 0;

        if (gp.gameMode == 0 && gp.state.getCurrentColor() != gp.playerColor) {
            gp.mouse.setEnabled(false);
        }
        gp.repaint();
    }

    public void navigateView(int idx) {
        if (idx < 0 || idx >= gp.boardHistory.size()) return;
        if (!gp.viewingMode) {
            gp.savedPieces.clear();
            for (Piece p : gp.pieces) gp.savedPieces.add(p);
            gp.savedColor = gp.state.getCurrentColor();
            gp.savedGameOver = gp.state.isGameOver();
        }
        gp.viewingMode = true;
        gp.viewingMoveIdx = idx;
        restoreBoardFromSnapshot(gp.boardHistory.get(idx));
        GamePanel.BoardSnapshot snap = gp.boardHistory.get(idx);
        gp.lastMoveFromCol = snap.fromCol;
        gp.lastMoveFromRow = snap.fromRow;
        gp.lastMoveToCol = snap.toCol;
        gp.lastMoveToRow = snap.toRow;
    }

    public void exitViewMode() {
        if (!gp.viewingMode) return;
        gp.viewingMode = false;
        gp.viewingMoveIdx = -1;
        gp.activeP = null;
        gp.canMove = false;
        gp.validSquare = false;
        gp.lastMoveFromCol = -1;
        gp.lastMoveFromRow = -1;
        gp.lastMoveToCol = -1;
        gp.lastMoveToRow = -1;
        gp.pieces.clear();
        gp.pieces.addAll(gp.savedPieces);
        gp.copyPieces(gp.pieces, gp.simPieces);
        gp.state.setCurrentColor(gp.savedColor);
        gp.state.setGameOver(gp.savedGameOver);
        gp.gameOver = gp.savedGameOver;
        if (gp.pendingFromCol >= 0) {
            gp.makeMove(gp.pendingFromCol, gp.pendingFromRow, gp.pendingToCol, gp.pendingToRow, gp.pendingPromoType);
            gp.pendingFromCol = -1;
        }
        gp.mouse.setEnabled(true);
        if (gp.gameMode == 0 && gp.state.getCurrentColor() != gp.playerColor) {
            gp.mouse.setEnabled(false);
        }
        gp.repaint();
    }

    public void restoreBoardFromSnapshot(GamePanel.BoardSnapshot snap) {
        gp.pieces.clear();
        for (GamePanel.PieceData pd : snap.pieceData) {
            Piece p;
            switch (pd.type) {
                case PAWN:   p = new piece.Pawn(pd.color, pd.row, pd.col); break;
                case ROOK:   p = new piece.Rook(pd.color, pd.row, pd.col); break;
                case KNIGHT: p = new piece.Knight(pd.color, pd.row, pd.col); break;
                case BISHOP: p = new piece.Bishop(pd.color, pd.row, pd.col); break;
                case QUEEN:  p = new piece.Queen(pd.color, pd.row, pd.col); break;
                default:     p = new piece.King(pd.color, pd.row, pd.col); break;
            }
            p.moved = pd.moved;
            p.twoStepped = pd.twoStepped;
            gp.pieces.add(p);
        }
        gp.copyPieces(gp.pieces, gp.simPieces);
        gp.state.setCurrentColor(snap.currentColor);
        gp.state.setGameOver(false);
        gp.gameOver = false;
        gp.mouse.setEnabled(true);
        gp.repaint();
    }

    public String toPGN(String whiteName, String blackName) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Event \"Chess Game\"]\n");
        sb.append("[Site \"Java Chess AI\"]\n");
        sb.append("[Date \"").append(java.time.LocalDate.now()).append("\"]\n");
        sb.append("[White \"").append(whiteName).append("\"]\n");
        sb.append("[Black \"").append(blackName).append("\"]\n");
        String result = "*";
        if (gp.gameOver || gp.state.isGameOver()) {
            if (gp.winner == null || gp.winner.isEmpty()) result = "1/2-1/2";
            else if (gp.winner.contains("White")) result = "1-0";
            else if (gp.winner.contains("Black")) result = "0-1";
            else result = "1/2-1/2";
        }
        sb.append("[Result \"").append(result).append("\"]\n\n");
        for (GamePanel.MovePair mp : gp.moveHistory) {
            sb.append(mp.number).append(". ");
            if (mp.white != null) sb.append(mp.white);
            if (mp.black != null) sb.append(" ").append(mp.black);
            sb.append(" ");
        }
        sb.append(result);
        return sb.toString();
    }

    public void copyMovesToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (GamePanel.MovePair mp : gp.moveHistory) {
            sb.append(mp.number).append(". ");
            if (mp.white != null) sb.append(mp.white);
            if (mp.black != null) sb.append(" ").append(mp.black);
            sb.append("\n");
        }
        StringSelection sel = new StringSelection(sb.toString().trim());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }

    public int hitTestMoveRow(int my) {
        int availablePixels = Config.SCROLL_AREA_BOTTOM - Config.SCROLL_AREA_TOP;
        int totalPixels = gp.moveHistory.size() * Config.MOVE_HISTORY_ROW_HEIGHT;
        int drawOffsetY = Config.TABLE_TOP + 32 - gp.moveHistoryScrollY;
        for (int i = 0; i < gp.moveHistory.size(); i++) {
            int y = drawOffsetY + i * Config.MOVE_HISTORY_ROW_HEIGHT;
            if (my >= y - Config.MOVE_HISTORY_ROW_HEIGHT / 2 && my <= y + Config.MOVE_HISTORY_ROW_HEIGHT / 2) return i;
        }
        return -1;
    }

    public boolean isThreefoldRepetition() {
        if (gp.positionHashes.isEmpty()) return false;
        long lastHash = gp.positionHashes.get(gp.positionHashes.size() - 1);
        int count = 0;
        for (long h : gp.positionHashes) {
            if (h == lastHash) count++;
        }
        return count >= Config.THREEFOLD_REPETITION;
    }
}
