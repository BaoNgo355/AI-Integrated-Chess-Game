package core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SavePGNAction {
    private final GamePanel gp;

    public SavePGNAction(GamePanel gp) {
        this.gp = gp;
    }

    public void save() {
        if (gp.moveHistory.isEmpty()) {
            JOptionPane.showMessageDialog(gp, "No moves to save.", "Save PGN", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PGN");
        chooser.setFileFilter(new FileNameExtensionFilter("PGN files (*.pgn)", "pgn"));
        chooser.setSelectedFile(new File("chess_game.pgn"));

        int result = chooser.showSaveDialog(gp);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pgn")) {
            file = new File(file.getAbsolutePath() + ".pgn");
        }

        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(gp,
                "File already exists. Overwrite?", "Save PGN",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        String whiteName = JOptionPane.showInputDialog(gp, "White player name:", "Player 1");
        if (whiteName == null || whiteName.trim().isEmpty()) whiteName = "Player 1";

        String blackName = JOptionPane.showInputDialog(gp, "Black player name:", "Player 2");
        if (blackName == null || blackName.trim().isEmpty()) blackName = "Player 2";

        String pgn = gp.getHistoryManager().toPGN(whiteName.trim(), blackName.trim());

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(pgn);
            JOptionPane.showMessageDialog(gp,
                "PGN saved to:\n" + file.getAbsolutePath(),
                "Save PGN", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gp,
                "Error saving file:\n" + e.getMessage(),
                "Save PGN", JOptionPane.ERROR_MESSAGE);
        }
    }
}
