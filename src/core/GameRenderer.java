package core;

import java.awt.BasicStroke;
import rendering.Board;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import piece.Piece;
import common.PieceType;
import common.Config;
import rendering.PieceSprite;

public class GameRenderer {
    private final GamePanel gp;

    public GameRenderer(GamePanel gp) {
        this.gp = gp;
    }

    public void render(java.awt.Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        gp.board.draw(g2);
        gp.board.drawCoordinates(g2);
        drawLastMoveHighlight(g2);
        drawCheckHighlight(g2);
        drawLegalMoveDots(g2);
        drawPieces(g2);
        drawActivePieceBorder(g2);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (gp.promotion) {
            drawPromotionOverlay(g2);
            return;
        }
        drawMoveHistoryPanel(g2);
        if (gp.gameOver) drawGameOverOverlay(g2);
    }

    private void drawLastMoveHighlight(Graphics2D g2) {
        if (gp.lastMoveFromCol >= 0 && gp.lastMoveFromRow >= 0) {
            int fx = gp.lastMoveFromCol * Board.SQUARE_SIZE;
            int fy = gp.lastMoveFromRow * Board.SQUARE_SIZE;
            if (Board.blackPerspective) {
                fx = (7 - gp.lastMoveFromCol) * Board.SQUARE_SIZE;
                fy = (7 - gp.lastMoveFromRow) * Board.SQUARE_SIZE;
            }
            g2.setColor(new Color(255, 180, 50, 120));
            g2.fillRect(fx, fy, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
        }
        if (gp.lastMoveToCol >= 0 && gp.lastMoveToRow >= 0) {
            int tx = gp.lastMoveToCol * Board.SQUARE_SIZE;
            int ty = gp.lastMoveToRow * Board.SQUARE_SIZE;
            if (Board.blackPerspective) {
                tx = (7 - gp.lastMoveToCol) * Board.SQUARE_SIZE;
                ty = (7 - gp.lastMoveToRow) * Board.SQUARE_SIZE;
            }
            g2.setColor(new Color(255, 180, 50, 120));
            g2.fillRect(tx, ty, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
        }
    }

    private void drawCheckHighlight(Graphics2D g2) {
        if (gp.state.isChecking()) {
            Piece king = gp.state.getKing(gp.state.getCurrentColor(), gp.simPieces);
            if (king != null) {
                g2.setColor(new Color(255, 0, 0, 100));
                g2.fillRect(king.x, king.y, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
            }
        }
    }

    private void drawLegalMoveDots(Graphics2D g2) {
        Piece localActiveP = gp.activeP;
        if (localActiveP != null && !gp.promotion) {
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (gp.canMoveLegally(localActiveP, c, r)) {
                        if (c == localActiveP.col && r == localActiveP.row)
                            continue;
                        int dotSize = 16;
                        int dx = localActiveP.getX(c) + (Board.SQUARE_SIZE - dotSize) / 2;
                        int dy = localActiveP.getY(r) + (Board.SQUARE_SIZE - dotSize) / 2;
                        g2.setColor(new Color(64, 64, 64, 180));
                        g2.fillOval(dx, dy, dotSize, dotSize);
                    }
                }
            }
        }
    }

    private void drawPieces(Graphics2D g2) {
        ArrayList<Piece> drawPieces = new ArrayList<>(gp.simPieces);
        for (Piece p : drawPieces) {
            if (p != gp.activeP)
                drawPiece(g2, p);
        }
        if (gp.activeP != null && gp.activeP.hittingP != null && !gp.simPieces.contains(gp.activeP.hittingP))
            drawPiece(g2, gp.activeP.hittingP);
    }

    private void drawPiece(Graphics2D g2, Piece p) {
        String name = p.type.name().toLowerCase();
        java.awt.image.BufferedImage img = PieceSprite.get(p.color, name);
        if (img != null) {
            g2.drawImage(img, p.x, p.y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
        }
    }

    private void drawActivePieceBorder(Graphics2D g2) {
        Piece localActiveP = gp.activeP;
        if (localActiveP != null && gp.canMove) {
            int bx = localActiveP.getX(localActiveP.col);
            int by = localActiveP.getY(localActiveP.row);
            boolean darkSquare = (localActiveP.col + localActiveP.row) % 2 == 1;
            g2.setColor(darkSquare ? new Color(238, 238, 205) : new Color(118, 150, 86));
            g2.setStroke(new BasicStroke(3));
            int inset = 3;
            g2.drawRect(bx + inset, by + inset,
                    Board.SQUARE_SIZE - inset * 2, Board.SQUARE_SIZE - inset * 2);
        }
        if (gp.activeP != null) {
            drawPiece(g2, gp.activeP);
        }
    }

    private void drawPromotionOverlay(Graphics2D g2) {
        g2.setFont(new Font("Book Antiqua", Font.PLAIN, 28));
        g2.setColor(Color.white);
        drawCentered(g2, "Promote to:", 150);
        for (Piece piece : gp.promoPieces) {
            String name = piece.type.name().toLowerCase();
            java.awt.image.BufferedImage img = PieceSprite.get(piece.color, name);
            if (img != null) {
                g2.drawImage(img, piece.x, piece.y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            }
        }
    }

    private void drawMoveHistoryPanel(Graphics2D g2) {
        int boxX = 800 + (Config.WINDOW_WIDTH - 800 - 286) / 2, boxY = Config.TABLE_TOP - 22;
        int boxW = 286, boxH = Config.TABLE_BOTTOM - boxY + 8;
        boolean showUndo = (gp.gameMode == 0);
        int extraBottom = showUndo ? 44 : 37;
        boxH += extraBottom;

        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.setColor(new Color(70, 70, 70));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        updateHoverStates();

        int drawOffsetY = Config.TABLE_TOP + 32 - gp.moveHistoryScrollY;
        int colX[] = {815, 860, 980};
        int colW[] = {30, 115, 110};

        g2.setFont(new Font("Book Antiqua", Font.PLAIN, 16));
        g2.setColor(Color.white);
        drawCentered(g2, "Moves", Config.TABLE_TOP - 18);

        String headers[] = {"#", "White", "Black"};
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 3; i++) {
            int hw = g2.getFontMetrics().stringWidth(headers[i]);
            int x = colX[i] + (colW[i] - hw) / 2;
            g2.drawString(headers[i], x, Config.TABLE_TOP + 14);
        }

        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        int viewedRow = -1, viewedCol = -1;
        if (gp.viewingMode && gp.viewingMoveIdx >= 0) {
            viewedRow = gp.viewingMoveIdx / 2;
            viewedCol = gp.viewingMoveIdx % 2;
        }
        for (int i = 0; i < gp.moveHistory.size(); i++) {
            int y = drawOffsetY + i * Config.MOVE_HISTORY_ROW_HEIGHT;
            if (y + Config.MOVE_HISTORY_ROW_HEIGHT < Config.SCROLL_AREA_TOP || y > Config.SCROLL_AREA_BOTTOM)
                continue;
            GamePanel.MovePair mp = gp.moveHistory.get(i);

            int whiteW = mp.white != null ? g2.getFontMetrics().stringWidth(mp.white) : 0;
            int blackW = mp.black != null ? g2.getFontMetrics().stringWidth(mp.black) : 0;

            if (gp.viewingMode && i == viewedRow) {
                g2.setColor(new Color(255, 200, 50, 80));
                if (viewedCol == 0 && mp.white != null) {
                    int hx = colX[1] + (colW[1] - whiteW) / 2 - 3;
                    g2.fillRect(hx, y - Config.MOVE_HISTORY_ROW_HEIGHT + 4, whiteW + 6, Config.MOVE_HISTORY_ROW_HEIGHT);
                } else if (viewedCol == 1 && mp.black != null) {
                    int hx = colX[2] + (colW[2] - blackW) / 2 - 3;
                    g2.fillRect(hx, y - Config.MOVE_HISTORY_ROW_HEIGHT + 4, blackW + 6, Config.MOVE_HISTORY_ROW_HEIGHT);
                }
            }
            if (i == gp.hoveredCellRow) {
                boolean sameCell = gp.viewingMode && i == viewedRow && gp.hoveredCellCol == viewedCol;
                if (!sameCell) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    if (gp.hoveredCellCol == 0 && mp.white != null) {
                        int hx = colX[1] + (colW[1] - whiteW) / 2 - 3;
                        g2.fillRect(hx, y - Config.MOVE_HISTORY_ROW_HEIGHT + 4, whiteW + 6, Config.MOVE_HISTORY_ROW_HEIGHT);
                    } else if (gp.hoveredCellCol == 1 && mp.black != null) {
                        int hx = colX[2] + (colW[2] - blackW) / 2 - 3;
                        g2.fillRect(hx, y - Config.MOVE_HISTORY_ROW_HEIGHT + 4, blackW + 6, Config.MOVE_HISTORY_ROW_HEIGHT);
                    }
                }
            }

            g2.setColor(new Color(215, 215, 215));
            String num = mp.number + ".";
            int numW = g2.getFontMetrics().stringWidth(num);
            g2.drawString(num, colX[0] + (colW[0] - numW) / 2, y);
            if (mp.white != null) g2.drawString(mp.white, colX[1] + (colW[1] - whiteW) / 2, y);
            if (mp.black != null) g2.drawString(mp.black, colX[2] + (colW[2] - blackW) / 2, y);
        }

        drawNavigationButtons(g2, boxX, boxW);
        if (gp.viewingMode) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(new Color(255, 200, 50, 180));
            g2.drawString("Viewing move " + (gp.viewingMoveIdx + 1), boxX + 8, Config.TABLE_BOTTOM - 12 - 18);
        }

        drawActionButtons(g2, boxX, boxW, boxY, boxH, showUndo);
    }

    private void updateHoverStates() {
        int mx = gp.mouse.x, my = gp.mouse.y;
        gp.hoveredButton = -1;
        gp.hoveredActionBtn = -1;
        Rectangle[] allBtnBounds = {gp.toStartBounds, gp.undoBounds, gp.redoBounds, gp.toEndBounds, gp.copyBounds, gp.savePGNBounds};
        for (int i = 0; i < 6; i++) {
            if (allBtnBounds[i].contains(mx, my)) { gp.hoveredButton = i; break; }
        }
        if (gp.hoveredButton == -1) {
            if (gp.gameOverPlayAgainBounds.contains(mx, my)) { gp.hoveredButton = 6; }
            else if (gp.gameOverExitBounds.contains(mx, my)) { gp.hoveredButton = 7; }
        }
        if (gp.hoveredButton == -1) {
            boolean showUndo = (gp.gameMode == 0);
            if (showUndo) {
                Rectangle[] actionBounds = {gp.undoMoveBounds, gp.drawOfferBounds, gp.resignBounds};
                for (int i = 0; i < 3; i++) {
                    if (actionBounds[i].contains(mx, my)) { gp.hoveredActionBtn = i; break; }
                }
            } else {
                Rectangle[] actionBounds = {gp.drawOfferBounds, gp.resignBounds};
                for (int i = 0; i < 2; i++) {
                    if (actionBounds[i].contains(mx, my)) { gp.hoveredActionBtn = i; break; }
                }
            }
        }

        int boxX = 800 + (Config.WINDOW_WIDTH - 800 - 286) / 2;
        int boxW = 286;
        gp.hoveredCellRow = -1;
        gp.hoveredCellCol = -1;
        if (mx >= boxX && mx <= boxX + boxW) {
            int drawOffsetY = Config.TABLE_TOP + 32 - gp.moveHistoryScrollY;
            int colX[] = {815, 860, 980};
            for (int i = 0; i < gp.moveHistory.size(); i++) {
                int y = drawOffsetY + i * Config.MOVE_HISTORY_ROW_HEIGHT;
                if (my >= y - Config.MOVE_HISTORY_ROW_HEIGHT / 2 && my <= y + Config.MOVE_HISTORY_ROW_HEIGHT / 2
                    && y + Config.MOVE_HISTORY_ROW_HEIGHT >= Config.SCROLL_AREA_TOP && y <= Config.SCROLL_AREA_BOTTOM) {
                    GamePanel.MovePair mp = gp.moveHistory.get(i);
                    if (mx >= colX[1] && mx < colX[2] && mp.white != null) {
                        gp.hoveredCellRow = i; gp.hoveredCellCol = 0;
                    } else if (mx >= colX[2] && mx <= boxX + boxW && mp.black != null) {
                        gp.hoveredCellRow = i; gp.hoveredCellCol = 1;
                    }
                    break;
                }
            }
        }
    }

    private void drawNavigationButtons(Graphics2D g2, int boxX, int boxW) {
        int btnY = Config.TABLE_BOTTOM - 12;
        String[] labels = {"<<", "<", ">", ">>", "Copy", "Save PGN"};
        Rectangle[] btnBounds = {gp.toStartBounds, gp.undoBounds, gp.redoBounds, gp.toEndBounds, gp.copyBounds, gp.savePGNBounds};
        int totalBtnW = 0;
        int[] btnW = new int[6];
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        for (int i = 0; i < 6; i++) {
            btnW[i] = g2.getFontMetrics().stringWidth(labels[i]) + 16;
            totalBtnW += btnW[i];
        }
        int spacing = 6;
        totalBtnW += spacing * 5;
        int btnStartX = boxX + (boxW - totalBtnW) / 2;

        for (int i = 0; i < 6; i++) {
            int bx = btnStartX;
            for (int j = 0; j < i; j++) bx += btnW[j] + spacing;
            btnBounds[i].setBounds(bx, btnY - 14, btnW[i], 18);

            boolean hover = gp.hoveredButton == i;
            g2.setColor(hover ? Color.white : new Color(150, 150, 150));
            g2.drawString(labels[i], bx + btnW[i] / 2 - g2.getFontMetrics().stringWidth(labels[i]) / 2, btnY);
        }
    }

    private void drawActionButtons(Graphics2D g2, int boxX, int boxW, int boxY, int boxH, boolean showUndo) {
        int sepY = Config.TABLE_BOTTOM - 12 + 12;
        g2.setColor(new Color(70, 70, 70));
        g2.drawLine(boxX + 10, sepY, boxX + boxW - 10, sepY);

        int boxBottom = boxY + boxH;
        int actionY = sepY + (boxBottom - sepY) / 2;
        String[] actionLabels;
        if (showUndo) {
            actionLabels = new String[]{"<-", "Draw", "Resign"};
        } else {
            actionLabels = new String[]{"Draw", "Resign"};
        }
        int actionCount = actionLabels.length;
        int actionSpacing = 10;
        int actionTotalW = boxW - 20;
        int actionBtnW = (actionTotalW - (actionCount - 1) * actionSpacing) / actionCount;
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));

        int actionBtnH = 10;
        for (int i = 0; i < actionCount; i++) {
            int abx = boxX + 10 + i * (actionBtnW + actionSpacing);
            int aby = actionY - actionBtnH / 2;

            java.awt.Rectangle r = null;
            if (showUndo) {
                if (i == 0) r = gp.undoMoveBounds;
                else if (i == 1) r = gp.drawOfferBounds;
                else if (i == 2) r = gp.resignBounds;
            } else {
                if (i == 0) r = gp.drawOfferBounds;
                else if (i == 1) r = gp.resignBounds;
            }
            if (r != null) r.setBounds(abx, aby, actionBtnW, actionBtnH);

            boolean enabled = true;
            if (showUndo && i == 0) {
                enabled = (gp.gameMode == 0 && gp.boardHistory.size() >= 2);
            }
            if (!enabled) {
                g2.setColor(new Color(80, 80, 80));
            } else if (gp.hoveredActionBtn == i) {
                g2.setColor(Color.white);
            } else {
                g2.setColor(new Color(150, 150, 150));
            }

            if (showUndo && i == 0) {
                g2.drawString("<-", abx + actionBtnW / 2 - g2.getFontMetrics().stringWidth("<-") / 2, actionY);
            } else if (i == (showUndo ? 1 : 0)) {
                g2.drawString("Draw", abx + actionBtnW / 2 - g2.getFontMetrics().stringWidth("Draw") / 2, actionY);
            } else {
                g2.drawString("Resign", abx + actionBtnW / 2 - g2.getFontMetrics().stringWidth("Resign") / 2, actionY);
            }
        }
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        int cx = 400, cy = 300;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, 800, 800);

        g2.setFont(new Font("Book Antiqua", Font.BOLD, 48));
        g2.setColor(Color.white);
        String msg = (gp.winner != null) ? gp.winner : "Game Over!";
        FontMetrics fm = g2.getFontMetrics();
        int tx = cx - fm.stringWidth(msg) / 2;
        g2.drawString(msg, tx, cy - 60);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 20));
        String playAgain = "Play Again";
        String exit = "Exit to Menu";
        int btnW = 220, btnH = 50, btnGap = 30;
        int totalW = btnW * 2 + btnGap;
        int bx = cx - totalW / 2;
        int by = cy + 40;

        int pax = bx, pay = by;
        gp.gameOverPlayAgainBounds.setBounds(pax, pay, btnW, btnH);
        g2.setColor(gp.hoveredButton == 6 ? new Color(80, 180, 80) : new Color(50, 130, 50));
        g2.fillRect(pax, pay, btnW, btnH);
        g2.setColor(Color.white);
        fm = g2.getFontMetrics();
        g2.drawString(playAgain, pax + btnW / 2 - fm.stringWidth(playAgain) / 2, pay + btnH / 2 + 7);

        int ex = bx + btnW + btnGap, ey = by;
        gp.gameOverExitBounds.setBounds(ex, ey, btnW, btnH);
        g2.setColor(gp.hoveredButton == 7 ? new Color(180, 80, 80) : new Color(130, 50, 50));
        g2.fillRect(ex, ey, btnW, btnH);
        g2.setColor(Color.white);
        g2.drawString(exit, ex + btnW / 2 - fm.stringWidth(exit) / 2, ey + btnH / 2 + 7);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int cx = 800 + (Config.WINDOW_WIDTH - 800) / 2;
        int x = cx - fm.stringWidth(text) / 2;
        g2.drawString(text, x, y);
    }
}
