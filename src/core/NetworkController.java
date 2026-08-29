package core;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import network.NetworkPeer;
import network.Message;
import state.GameState;

public class NetworkController {
    private final GamePanel gp;

    public NetworkController(GamePanel gp) {
        this.gp = gp;
    }

    public void onNetworkMessage(String msg) {
        if (msg.startsWith("MOVE:")) {
            String[] parts = msg.split(":");
            try {
                int fc = Integer.parseInt(parts[1]);
                int fr = Integer.parseInt(parts[2]);
                int tc = Integer.parseInt(parts[3]);
                int tr = Integer.parseInt(parts[4]);
                int pt = (parts.length > 5) ? Integer.parseInt(parts[5]) : 0;
                int finalPt = pt;
                SwingUtilities.invokeLater(() -> {
                    gp.makeMove(fc, fr, tc, tr, finalPt);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg.equals("RESIGN")) {
            SwingUtilities.invokeLater(() -> {
                gp.gameOver = true;
                gp.winner = (gp.playerColor == GameState.WHITE) ? "White wins" : "Black wins";
                gp.state.setGameOver(true);
                gp.gameOverDialogShown = true;
                gp.showGameOverDialog();
            });
        } else if (msg.equals("DRAW_OFFER")) {
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(gp,
                    "Doi thu de xuat hoa. Chap nhan?",
                    "De xuat hoa", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    if (gp.networkPeer != null)
                        gp.networkPeer.send(Message.drawAccept().serialize());
                    gp.gameOver = true;
                    gp.winner = "Draw (Agreement)";
                    gp.state.setGameOver(true);
                    gp.gameOverDialogShown = true;
                    gp.showGameOverDialog();
                } else {
                    if (gp.networkPeer != null)
                        gp.networkPeer.send(Message.drawDecline().serialize());
                }
            });
        } else if (msg.equals("DRAW_ACCEPT")) {
            SwingUtilities.invokeLater(() -> {
                if (!gp.gameOver) {
                    gp.gameOver = true;
                    gp.winner = "Draw (Agreement)";
                    gp.state.setGameOver(true);
                    gp.gameOverDialogShown = true;
                    gp.showGameOverDialog();
                }
            });
        } else if (msg.equals("DRAW_DECLINE")) {
            SwingUtilities.invokeLater(() -> {
                gp.drawOffered = false;
                JOptionPane.showMessageDialog(gp,
                    "Doi thu tu choi hoa.",
                    "Tu choi hoa", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    public void sendMoveToNetwork(int fromCol, int fromRow, int toCol, int toRow, int promoType) {
        if (gp.gameMode == 1 && gp.networkPeer != null) {
            gp.networkPeer.send("MOVE:" + fromCol + ":" + fromRow + ":" + toCol + ":" + toRow + ":" + promoType);
        }
    }
}
