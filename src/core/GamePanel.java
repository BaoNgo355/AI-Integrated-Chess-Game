package core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import network.NetworkPeer;
import network.Message;
import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;
import ai.AIConfig;
import ai.BoardState;
import ai.ChessAI;
import ai.Hasher;
import ai.Move;
import common.PieceType;
import common.Config;
import rendering.Board;
import input.Mouse;
import state.GameState;

public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = Config.WINDOW_WIDTH;
    public static final int HEIGHT = Config.WINDOW_HEIGHT;

    Thread gameThread;
    public Board board = new Board();
    public Mouse mouse = new Mouse();
    public GameState state = new GameState();

    public ArrayList<Piece> pieces = new ArrayList<>();
    public ArrayList<Piece> simPieces = new ArrayList<>();
    public ArrayList<Piece> promoPieces = new ArrayList<>();
    public Piece activeP;
    public boolean canMove;
    public boolean validSquare;
    public int lastMoveFromCol = -1;
    public int lastMoveFromRow = -1;
    public int lastMoveToCol = -1;
    public int lastMoveToRow = -1;
    public volatile boolean promotion;
    public volatile boolean gameOver;
    public volatile String winner;
    public volatile boolean gameOverDialogShown;

    public int gameMode;
    public int playerColor;
    private ChessAI chessAI;
    private volatile boolean aiThinking;
    private int aiDepth;
    public NetworkPeer networkPeer;

    public ArrayList<MovePair> moveHistory = new ArrayList<>();
    public ArrayList<BoardSnapshot> boardHistory = new ArrayList<>();
    public ArrayList<Long> positionHashes = new ArrayList<>();
    public boolean viewingMode;
    public int viewingMoveIdx = -1;
    public ArrayList<Piece> savedPieces = new ArrayList<>();
    public int savedColor;
    public boolean savedGameOver;

    public int moveHistoryScrollY = 0;
    private boolean mouseWasPressed;
    public Rectangle toStartBounds = new Rectangle();
    public Rectangle undoBounds = new Rectangle();
    public Rectangle redoBounds = new Rectangle();
    public Rectangle toEndBounds = new Rectangle();
    public Rectangle copyBounds = new Rectangle();
    public Rectangle savePGNBounds = new Rectangle();
    public int hoveredButton = -1;
    public int hoveredActionBtn = -1;
    public int hoveredCellRow = -1;
    public int hoveredCellCol = -1;
    public Rectangle undoMoveBounds = new Rectangle();
    public Rectangle drawOfferBounds = new Rectangle();
    public Rectangle resignBounds = new Rectangle();
    public Rectangle gameOverPlayAgainBounds = new Rectangle();
    public Rectangle gameOverExitBounds = new Rectangle();
    public int halfMoveClock;
    volatile boolean drawOffered;
    private int drawOfferColor;
    public int pendingFromCol = -1, pendingFromRow = -1;
    public int pendingToCol = -1, pendingToRow = -1;
    public int pendingPromoType = -1;
    private int promoFromCol, promoFromRow;
    private boolean promoWasCapture;
    private Piece capturedPiece;

    private final GameRenderer renderer;
    private final MoveHistoryManager historyMgr;
    private final NetworkController networkCtrl;
    private common.MenuLauncher menuLauncher;

    public void setMenuLauncher(common.MenuLauncher launcher) {
        this.menuLauncher = launcher;
    }

    public MoveHistoryManager getHistoryManager() {
        return historyMgr;
    }

    public static class MovePair {
        public int number;
        public String white;
        public String black;
        public MovePair(int number, String white) {
            this.number = number;
            this.white = white;
        }
    }

    public static class PieceData {
        public PieceType type; public int color, col, row; public boolean moved, twoStepped;
        public PieceData(Piece p) {
            type = p.type; color = p.color;
            col = p.col; row = p.row;
            moved = p.moved; twoStepped = p.twoStepped;
        }
    }

    public static class BoardSnapshot {
        public ArrayList<PieceData> pieceData = new ArrayList<>();
        public int currentColor;
        public int fromCol, fromRow, toCol, toRow;
        public BoardSnapshot(List<Piece> ps, int color, int fromCol, int fromRow, int toCol, int toRow) {
            currentColor = color;
            this.fromCol = fromCol; this.fromRow = fromRow;
            this.toCol = toCol; this.toRow = toRow;
            for (Piece p : ps) pieceData.add(new PieceData(p));
        }
    }

    public GamePanel(int gameMode, int playerColor, NetworkPeer peer, int aiDepth) {
        this.gameMode = gameMode;
        this.playerColor = playerColor;
        this.aiDepth = aiDepth;

        Board.blackPerspective = (playerColor == GameState.BLACK);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        addMouseWheelListener(e -> {
            int totalPixels = moveHistory.size() * Config.MOVE_HISTORY_ROW_HEIGHT;
            int availablePixels = Config.SCROLL_AREA_BOTTOM - Config.SCROLL_AREA_TOP;
            int maxScroll = Math.max(0, totalPixels - availablePixels);
            moveHistoryScrollY += e.getWheelRotation() * Config.MOVE_HISTORY_ROW_HEIGHT;
            moveHistoryScrollY = Math.max(0, Math.min(maxScroll, moveHistoryScrollY));
            repaint();
        });

        this.networkPeer = peer;

        renderer = new GameRenderer(this);
        historyMgr = new MoveHistoryManager(this);
        networkCtrl = new NetworkController(this);

        setPieces();
        copyPieces(pieces, simPieces);

        if (gameMode == Config.GAME_MODE_AI) {
            AIConfig cfg = new AIConfig();
            cfg.load("res/ai_config.properties");
            chessAI = new ChessAI(cfg);
            if (playerColor != GameState.WHITE) {
                mouse.setEnabled(false);
                triggerAI();
            }
        } else if (gameMode == Config.GAME_MODE_LAN) {
            if (playerColor != GameState.WHITE) {
                mouse.setEnabled(false);
            }
        }
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stopGame() {
        gameThread = null;
    }

    public void setPieces() {
        pieces.clear();
        for (int c = 0; c < 8; c++) pieces.add(new Pawn(GameState.WHITE, 6, c));
        pieces.add(new Rook(GameState.WHITE, 7, 0));
        pieces.add(new Rook(GameState.WHITE, 7, 7));
        pieces.add(new Knight(GameState.WHITE, 7, 1));
        pieces.add(new Knight(GameState.WHITE, 7, 6));
        pieces.add(new Bishop(GameState.WHITE, 7, 2));
        pieces.add(new Bishop(GameState.WHITE, 7, 5));
        pieces.add(new Queen(GameState.WHITE, 7, 3));
        pieces.add(new King(GameState.WHITE, 7, 4));

        for (int c = 0; c < 8; c++) pieces.add(new Pawn(GameState.BLACK, 1, c));
        pieces.add(new Rook(GameState.BLACK, 0, 0));
        pieces.add(new Rook(GameState.BLACK, 0, 7));
        pieces.add(new Knight(GameState.BLACK, 0, 1));
        pieces.add(new Knight(GameState.BLACK, 0, 6));
        pieces.add(new Bishop(GameState.BLACK, 0, 2));
        pieces.add(new Bishop(GameState.BLACK, 0, 5));
        pieces.add(new Queen(GameState.BLACK, 0, 3));
        pieces.add(new King(GameState.BLACK, 0, 4));
    }

    public void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        target.addAll(source);
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / Config.GAME_FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void update() {
        if (gameOver && !gameOverDialogShown) {
            gameOverDialogShown = true;
            return;
        }

        if (mouse.pressed && !mouseWasPressed) {
            if (toStartBounds.contains(mouse.x, mouse.y))
                { historyMgr.navigateView(0); mouseWasPressed = true; repaint(); return; }
            if (undoBounds.contains(mouse.x, mouse.y))
                { historyMgr.navigateView(viewingMode ? viewingMoveIdx - 1 : boardHistory.size() - 2);
                  mouseWasPressed = true; repaint(); return; }
            if (redoBounds.contains(mouse.x, mouse.y)) {
                if (viewingMode && viewingMoveIdx + 1 < boardHistory.size()) {
                    int next = viewingMoveIdx + 1;
                    if (next == boardHistory.size() - 1)
                        exitViewMode();
                    else
                        historyMgr.navigateView(next);
                }
                mouseWasPressed = true; repaint(); return;
            }
            if (toEndBounds.contains(mouse.x, mouse.y))
                { exitViewMode(); mouseWasPressed = true; repaint(); return; }
            if (copyBounds.contains(mouse.x, mouse.y))
                { historyMgr.copyMovesToClipboard(); mouseWasPressed = true; return; }
            if (savePGNBounds.contains(mouse.x, mouse.y))
                { new SavePGNAction(this).save(); mouseWasPressed = true; return; }

            if (undoMoveBounds.contains(mouse.x, mouse.y)
                    && gameMode == 0 && boardHistory.size() >= 2) {
                historyMgr.undoLastMoveAI();
                mouseWasPressed = true; repaint(); return;
            }
            if (drawOfferBounds.contains(mouse.x, mouse.y)) {
                handleDrawOffer();
                mouseWasPressed = true; repaint(); return;
            }
            if (resignBounds.contains(mouse.x, mouse.y)) {
                handleResign();
                mouseWasPressed = true; repaint(); return;
            }

            if (gameOver) {
                if (gameOverPlayAgainBounds.contains(mouse.x, mouse.y)) {
                    restartGame();
                    mouseWasPressed = true; repaint(); return;
                }
                if (gameOverExitBounds.contains(mouse.x, mouse.y)) {
                    exitToMenu();
                    mouseWasPressed = true; return;
                }
            }

            int row = historyMgr.hitTestMoveRow(mouse.y);
            if (row >= 0) {
                MovePair mp = moveHistory.get(row);
                int boxX = 800 + (WIDTH - 800 - 286) / 2;
                int colX[] = {815, 860, 980};
                if (mouse.x < colX[1]) { mouseWasPressed = true; return; }
                int snapIdx = -1;
                if (mouse.x >= colX[1] && mouse.x < colX[2] && mp.white != null)
                    snapIdx = row * 2;
                else if (mouse.x >= colX[2] && mouse.x <= boxX + 286 && mp.black != null)
                    snapIdx = row * 2 + 1;
                if (snapIdx < 0 || snapIdx >= boardHistory.size()) { mouseWasPressed = true; return; }
                if (viewingMode && snapIdx == boardHistory.size() - 1) {
                    exitViewMode();
                } else {
                    historyMgr.navigateView(snapIdx);
                }
                mouseWasPressed = true;
                repaint();
                return;
            }
        }
        mouseWasPressed = mouse.pressed;

        if (gameOver || state.isGameOver()) return;
        if (viewingMode) return;

        if (gameMode == Config.GAME_MODE_AI) {
            if (aiThinking) return;
            if (state.getCurrentColor() != playerColor) {
                triggerAI();
                return;
            }
            mouse.setEnabled(true);
        }

        if (gameMode == Config.GAME_MODE_LAN) {
            if (state.getCurrentColor() != playerColor) {
                mouse.setEnabled(false);
                return;
            } else {
                mouse.setEnabled(true);
            }
        }

        if (promotion) {
            promoting();
            return;
        }

        if (mouse.pressed) {
            if (activeP == null) {
                int mc = screenToBoardCol(mouse.x);
                int mr = screenToBoardRow(mouse.y);
                for (Piece piece : simPieces) {
                    if (piece.color == state.getCurrentColor()
                            && piece.col == mc && piece.row == mr)
                        activeP = piece;
                }
            } else {
                simulate();
            }
        }

        if (!mouse.pressed && activeP != null) {
            if (validSquare) {
                int origCol = activeP.preCol;
                int origRow = activeP.preRow;

                copyPieces(simPieces, pieces);
                activeP.updatePosition();
                adjustCastlingRook();

                if (canPromote()) {
                    promoFromCol = origCol;
                    promoFromRow = origRow;
                    promoWasCapture = capturedPiece != null;
                    promotion = true;
                } else {
                    int destCol = activeP.col;
                    int destRow = activeP.row;
                    boolean wasCapture = capturedPiece != null;
                    state.setCastlingRook(null);
                    state.changePlayer(pieces);
                    historyMgr.recordMove(activeP.color, origCol, origRow, destCol, destRow, wasCapture, 0);
                    recordPositionHash();
                    if (activeP.type == PieceType.PAWN || wasCapture)
                        halfMoveClock = 0;
                    else
                        halfMoveClock++;
                    checkDrawConditions();
                    lastMoveFromCol = -1; lastMoveFromRow = -1;
                    lastMoveToCol = -1; lastMoveToRow = -1;
                    gameOver = state.isGameOver();
                    winner = state.getWinner();
                    networkCtrl.sendMoveToNetwork(origCol, origRow, destCol, destRow, 0);
                    activeP = null;
                    validSquare = false;
                }
            } else {
                copyPieces(pieces, simPieces);
                activeP.resetPosition();
                activeP = null;
            }
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;
        capturedPiece = null;
        if (activeP != null) {
            activeP.hittingP = null;
        }

        copyPieces(pieces, simPieces);

        Piece cr = state.getCastlingRook();
        if (cr != null) {
            cr.col = cr.preCol;
            cr.x = cr.getX(cr.col);
            state.setCastlingRook(null);
        }

        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        if (activeP.canMove(activeP.col, activeP.row, simPieces, state)) {
            canMove = true;

            if (activeP.hittingP != null) {
                capturedPiece = activeP.hittingP;
                simPieces.remove(activeP.hittingP);
            }

            if (state.isKingInCheck(state.getCurrentColor(), simPieces)) {
                capturedPiece = null;
                canMove = false;
            } else {
                validSquare = true;
            }
        }
    }

    public boolean isCheckmate(int kingColor) {
        return state.isCheckmate(kingColor, simPieces);
    }

    public boolean isStalemate(int kingColor) {
        return state.isStalemate(kingColor, simPieces);
    }

    public boolean canMoveLegally(Piece p, int targetCol, int targetRow) {
        int savedCol = p.col;
        int savedRow = p.row;
        Piece savedCastlingRook = state.getCastlingRook();

        p.col = p.preCol;
        p.row = p.preRow;

        if (!p.canMove(targetCol, targetRow, simPieces, state)) {
            p.col = savedCol;
            p.row = savedRow;
            state.setCastlingRook(savedCastlingRook);
            return false;
        }

        ArrayList<Piece> backup = new ArrayList<>(simPieces);

        p.col = targetCol;
        p.row = targetRow;

        if (p.hittingP != null)
            simPieces.remove(p.hittingP);

        boolean check = state.isKingInCheck(state.getCurrentColor(), simPieces);
        copyPieces(backup, simPieces);

        p.col = savedCol;
        p.row = savedRow;
        state.setCastlingRook(savedCastlingRook);

        return !check;
    }

    private void adjustCastlingRook() {
        Piece cr = state.getCastlingRook();
        if (cr != null) {
            if (cr.col == 0) cr.col += 3;
            else if (cr.col == 7) cr.col -= 2;
            cr.updatePosition();
        }
    }

    private boolean canPromote() {
        if (activeP != null && activeP.type == PieceType.PAWN) {
            if ((state.getCurrentColor() == GameState.WHITE && activeP.row == 0)
                || (state.getCurrentColor() == GameState.BLACK && activeP.row == 7)) {
                promoPieces.clear();
                int startX = 800 + (WIDTH - 800 - Board.SQUARE_SIZE) / 2;
                int startY = 200;
                int step = Board.SQUARE_SIZE;
                Piece r = new Rook(state.getCurrentColor(), 0, 0);
                r.x = startX; r.y = startY;
                promoPieces.add(r);
                Piece n = new Knight(state.getCurrentColor(), 0, 0);
                n.x = startX; n.y = startY + step;
                promoPieces.add(n);
                Piece b = new Bishop(state.getCurrentColor(), 0, 0);
                b.x = startX; b.y = startY + 2 * step;
                promoPieces.add(b);
                Piece q = new Queen(state.getCurrentColor(), 0, 0);
                q.x = startX; q.y = startY + 3 * step;
                promoPieces.add(q);
                return true;
            }
        }
        return false;
    }

    private void promoting() {
        if (mouse.pressed) {
            for (Piece piece : promoPieces) {
                if (mouse.x >= piece.x && mouse.x <= piece.x + Board.SQUARE_SIZE
                        && mouse.y >= piece.y && mouse.y <= piece.y + Board.SQUARE_SIZE) {

                    int origCol = promoFromCol;
                    int origRow = promoFromRow;
                    int destCol = activeP.col;
                    int destRow = activeP.row;
                    int promoType = promoTypeFor(piece.type);

                    Piece newPiece;
                    switch (piece.type) {
                        case ROOK:   newPiece = new Rook(state.getCurrentColor(), activeP.row, activeP.col); break;
                        case KNIGHT: newPiece = new Knight(state.getCurrentColor(), activeP.row, activeP.col); break;
                        case BISHOP: newPiece = new Bishop(state.getCurrentColor(), activeP.row, activeP.col); break;
                        default:     newPiece = new Queen(state.getCurrentColor(), activeP.row, activeP.col); break;
                    }
                    simPieces.add(newPiece);
                    simPieces.remove(activeP);
                    boolean promoCapture = promoWasCapture;
                    copyPieces(simPieces, pieces);
                    activeP = null;
                    promotion = false;
                    state.setCastlingRook(null);
                    state.changePlayer(pieces);
                    historyMgr.recordMove(
                        state.getCurrentColor() == GameState.WHITE ? GameState.BLACK : GameState.WHITE,
                        origCol, origRow, destCol, destRow, promoCapture, promoType);
                    recordPositionHash();
                    halfMoveClock = 0;
                    checkDrawConditions();
                    lastMoveFromCol = -1; lastMoveFromRow = -1;
                    lastMoveToCol = -1; lastMoveToRow = -1;
                    gameOver = state.isGameOver();
                    winner = state.getWinner();
                    networkCtrl.sendMoveToNetwork(origCol, origRow, destCol, destRow, promoType);
                }
            }
        }
    }

    private static int promoTypeFor(PieceType t) {
        if (t == PieceType.QUEEN) return 5;
        if (t == PieceType.ROOK) return 4;
        if (t == PieceType.BISHOP) return 3;
        if (t == PieceType.KNIGHT) return 2;
        return 0;
    }

    private static int screenToBoardCol(int sx) {
        int c = sx / Board.SQUARE_SIZE;
        return Board.blackPerspective ? 7 - c : c;
    }

    private static int screenToBoardRow(int sy) {
        int r = sy / Board.SQUARE_SIZE;
        return Board.blackPerspective ? 7 - r : r;
    }

    private void recordPositionHash() {
        int[][] boardArray = state.buildBoardArray(pieces);
        int castlingRights = state.buildCastlingRights(pieces);
        int enPassantCol = state.getEnPassantCol(pieces);
        long hash = Hasher.computeHash(boardArray, state.getCurrentColor() == GameState.WHITE, castlingRights, enPassantCol);
        positionHashes.add(hash);
    }

    private void triggerAI() {
        aiThinking = true;
        mouse.setEnabled(false);

        new Thread(() -> {
            try {
                int[][] boardArray = state.buildBoardArray(pieces);
                int castlingRights = state.buildCastlingRights(pieces);
                int enPassantCol = state.getEnPassantCol(pieces);

                BoardState boardState = new BoardState();
                for (int r = 0; r < 8; r++)
                    System.arraycopy(boardArray[r], 0, boardState.board[r], 0, 8);
                boardState.whiteToMove = (state.getCurrentColor() == 0);
                boardState.castlingRights = castlingRights;
                boardState.enPassantCol = enPassantCol;
                boardState.wKingRow = state.findKingRow(true, pieces);
                boardState.wKingCol = state.findKingCol(true, pieces);
                boardState.bKingRow = state.findKingRow(false, pieces);
                boardState.bKingCol = state.findKingCol(false, pieces);
                boardState.halfMoveClock = halfMoveClock;
                boardState.gameHistory = positionHashes;

                chessAI.setSearchDepth(aiDepth);
                Move bestMove = chessAI.search(boardState);

                SwingUtilities.invokeLater(() -> {
                    if (bestMove != null && !gameOver && !state.isGameOver()) {
                        makeMove(bestMove.fromCol, bestMove.fromRow,
                                 bestMove.toCol, bestMove.toRow,
                                 bestMove.promotionPieceType);
                    }
                    aiThinking = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                aiThinking = false;
            }
        }).start();
    }

    public void setNetworkPeer(NetworkPeer peer) {
        this.networkPeer = peer;
    }

    public void onNetworkMessage(String msg) {
        networkCtrl.onNetworkMessage(msg);
    }

    public boolean makeMove(int fromCol, int fromRow, int toCol, int toRow, int promotionType) {
        if (gameOver || state.isGameOver()) return false;
        if (viewingMode) {
            pendingFromCol = fromCol; pendingFromRow = fromRow;
            pendingToCol = toCol; pendingToRow = toRow;
            pendingPromoType = promotionType;
            return false;
        }

        Piece movingPiece = null;
        for (Piece p : pieces) {
            if (p.col == fromCol && p.row == fromRow && p.color == state.getCurrentColor()) {
                movingPiece = p;
                break;
            }
        }
        if (movingPiece == null) return false;

        copyPieces(pieces, simPieces);

        Piece simPiece = null;
        for (Piece p : simPieces) {
            if (p == movingPiece) {
                simPiece = p;
                break;
            }
        }
        if (simPiece == null) return false;

        Piece cr = state.getCastlingRook();
        if (cr != null) {
            cr.col = cr.preCol;
            cr.x = cr.getX(cr.col);
            state.setCastlingRook(null);
        }

        simPiece.x = simPiece.getX(toCol);
        simPiece.y = simPiece.getY(toRow);
        simPiece.col = toCol;
        simPiece.row = toRow;

        if (!simPiece.canMove(toCol, toRow, simPieces, state)) {
            copyPieces(pieces, simPieces);
            return false;
        }

        if (simPiece.hittingP != null)
            simPieces.remove(simPiece.hittingP);

        Piece savedCastlingRook = state.getCastlingRook();
        if (savedCastlingRook != null) {
            if (savedCastlingRook.col == 0) savedCastlingRook.col += 3;
            else if (savedCastlingRook.col == 7) savedCastlingRook.col -= 2;
            savedCastlingRook.x = savedCastlingRook.getX(savedCastlingRook.col);
        }

        if (state.isKingInCheck(state.getCurrentColor(), simPieces)) {
            copyPieces(pieces, simPieces);
            if (savedCastlingRook != null) {
                savedCastlingRook.col = savedCastlingRook.preCol;
                savedCastlingRook.x = savedCastlingRook.getX(savedCastlingRook.col);
                state.setCastlingRook(null);
            }
            return false;
        }

        copyPieces(simPieces, pieces);
        movingPiece.updatePosition();
        if (savedCastlingRook != null) {
            savedCastlingRook.updatePosition();
        }

        lastMoveFromCol = fromCol;
        lastMoveFromRow = fromRow;
        lastMoveToCol = toCol;
        lastMoveToRow = toRow;

        boolean wasCapture = (simPiece.hittingP != null);

        activeP = movingPiece;
        if (canPromote()) {
            autoPromote(promotionType, fromCol, fromRow, wasCapture);
        } else {
            activeP = null;
            state.setCastlingRook(null);
            state.changePlayer(pieces);
            historyMgr.recordMove(
                state.getCurrentColor() == GameState.WHITE ? GameState.BLACK : GameState.WHITE,
                fromCol, fromRow, toCol, toRow, wasCapture, 0);
            recordPositionHash();
            if (movingPiece.type == PieceType.PAWN || wasCapture)
                halfMoveClock = 0;
            else
                halfMoveClock++;
            checkDrawConditions();
            gameOver = state.isGameOver();
            winner = state.getWinner();
        }

        return true;
    }

    private void autoPromote(int promotionType, int fromCol, int fromRow, boolean wasCapture) {
        if (activeP == null || activeP.type != PieceType.PAWN) return;

        int row = activeP.row;
        int col = activeP.col;

        pieces.remove(activeP);

        Piece promoted;
        switch (promotionType) {
            case 2:  promoted = new Knight(state.getCurrentColor(), row, col); break;
            case 3:  promoted = new Bishop(state.getCurrentColor(), row, col); break;
            case 4:  promoted = new Rook(state.getCurrentColor(), row, col); break;
            default: promoted = new Queen(state.getCurrentColor(), row, col); break;
        }

        pieces.add(promoted);
        copyPieces(pieces, simPieces);
        activeP = null;
        promotion = false;
        state.setCastlingRook(null);
        state.changePlayer(pieces);
        historyMgr.recordMove(
            state.getCurrentColor() == GameState.WHITE ? GameState.BLACK : GameState.WHITE,
            fromCol, fromRow, col, row, wasCapture, promotionType);
        recordPositionHash();
        halfMoveClock = 0;
        checkDrawConditions();
        gameOver = state.isGameOver();
        winner = state.getWinner();
    }

    private void handleDrawOffer() {
        if (gameMode == 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Ban co muon de xuat hoa?", "De xuat hoa",
                JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;

            gameOver = true;
            winner = "Draw (Agreement)";
            state.setGameOver(true);
        } else if (gameMode == 1) {
            if (networkPeer != null) {
                networkPeer.send(Message.drawOffer().serialize());
                drawOffered = true;
                drawOfferColor = state.getCurrentColor();
            }
        }
    }

    private void handleResign() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Ban chac chan muon dau hang?", "Dau hang",
            JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        gameOver = true;
        winner = (playerColor == GameState.WHITE) ? "Black wins" : "White wins";
        state.setGameOver(true);

        if (gameMode == 1 && networkPeer != null) {
            networkPeer.send(Message.resign().serialize());
        }
    }

    private void checkDrawConditions() {
        if (halfMoveClock >= Config.FIFTY_MOVE_LIMIT) {
            state.setGameOver(true);
            state.setWinner("Draw (Fifty-Move Rule)");
            return;
        }
        if (historyMgr.isThreefoldRepetition()) {
            state.setGameOver(true);
            state.setWinner("Draw (Threefold Repetition)");
        }
    }

    public void showGameOverDialog() {
        if (SwingUtilities.isEventDispatchThread()) {
            showGameOverDialogDirect();
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> showGameOverDialogDirect());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showGameOverDialogDirect() {
        String msg = (winner != null) ? winner + "!" : "Game Over!";
        String[] opts = {"Play Again", "Exit to Menu"};
        int choice = JOptionPane.showOptionDialog(
            this, msg, "Game Over",
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, opts, opts[0]);
        if (choice == 0) restartGame();
        else exitToMenu();
    }

    private void restartGame() {
        moveHistory.clear();
        positionHashes.clear();
        moveHistoryScrollY = 0;
        gameOverDialogShown = false;
        lastMoveFromCol = lastMoveFromRow = -1;
        lastMoveToCol = lastMoveToRow = -1;
        gameOver = false;
        winner = null;
        activeP = null;
        canMove = false;
        validSquare = false;
        promotion = false;
        halfMoveClock = 0;
        drawOffered = false;
        state = new GameState();
        pieces.clear();
        setPieces();
        copyPieces(pieces, simPieces);
        if (gameMode == 0 && playerColor != GameState.WHITE) {
            mouse.setEnabled(false);
            triggerAI();
        }
    }

    private void exitToMenu() {
        stopGame();
        if (networkPeer != null) {
            networkPeer.stop();
            networkPeer = null;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.dispose();
        if (menuLauncher != null) menuLauncher.showMenu();
    }

    public void exitViewMode() {
        historyMgr.exitViewMode();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g);
    }
}
