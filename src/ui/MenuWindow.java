package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import core.GamePanel;
import state.GameState;
import network.LANController;
import network.NetworkPeer;
import common.MenuLauncher;

public class MenuWindow extends JFrame implements LANController.Callback, MenuLauncher {

    private JRadioButton rbAI, rbLAN;
    private JRadioButton rbWhite, rbBlack;
    private JSpinner depthSpinner;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    private JTextField tfRoomCode;
    private JLabel lblStatus;
    private JButton btnHost, btnJoin, btnCancel;

    private LANController lanController;
    private volatile GamePanel gamePanel;

    public MenuWindow() {
        setTitle("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(420, 320);
        setLocationRelativeTo(null);

        lanController = new LANController();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel.add(createMenuPanel(), "menu");
        mainPanel.add(createLANPanel(), "lan");

        add(mainPanel);
        cardLayout.show(mainPanel, "menu");

        setVisible(true);
    }

    @Override
    public void showMenu() {
        SwingUtilities.invokeLater(() -> {
            new MenuWindow();
        });
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("CHESS GAME", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 2; gbc.gridy = 1;
        JLabel modeLabel = new JLabel("Che do choi:");
        panel.add(modeLabel, gbc);

        rbAI = new JRadioButton("Choi voi AI", true);
        rbLAN = new JRadioButton("Choi mang LAN (PvP)");

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(rbAI);
        modeGroup.add(rbLAN);

        rbAI.addItemListener(e -> enableColorChoice(true));
        rbLAN.addItemListener(e -> enableColorChoice(false));

        gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.gridx = 0; panel.add(rbAI, gbc);
        gbc.gridx = 1; panel.add(rbLAN, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 3;
        JLabel colorLabel = new JLabel("Mau quan cua ban:");
        panel.add(colorLabel, gbc);

        rbWhite = new JRadioButton("Trang (di truoc)", true);
        rbBlack = new JRadioButton("Den");

        ButtonGroup colorGroup = new ButtonGroup();
        colorGroup.add(rbWhite);
        colorGroup.add(rbBlack);

        gbc.gridy = 4; gbc.gridwidth = 1;
        gbc.gridx = 0; panel.add(rbWhite, gbc);
        gbc.gridx = 1; panel.add(rbBlack, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 5;
        JLabel diffLabel = new JLabel("Do sau tim kiem:");
        panel.add(diffLabel, gbc);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 3, 10, 1);
        depthSpinner = new JSpinner(spinnerModel);
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        spinnerPanel.add(new JLabel("Depth "));
        spinnerPanel.add(depthSpinner);
        gbc.gridy = 6;
        panel.add(spinnerPanel, gbc);

        gbc.gridy = 7;
        JButton btnStart = new JButton("BAT DAU");
        btnStart.addActionListener(e -> onStart());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnStart);
        panel.add(btnPanel, gbc);

        return panel;
    }

    private void enableColorChoice(boolean enable) {
        rbWhite.setEnabled(enable);
        rbBlack.setEnabled(enable);
        enableDifficultyChoice(enable);
    }

    private void enableDifficultyChoice(boolean enable) {
        depthSpinner.setEnabled(enable);
    }

    private JPanel createLANPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("KET NOI MANG LAN", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        JLabel hint = new JLabel("Mot nguoi tao phong, nguoi kia ghep tran", SwingConstants.CENTER);
        gbc.gridy = 1; panel.add(hint, gbc);

        gbc.gridwidth = 2; gbc.gridy = 2;
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JLabel roomLabel = new JLabel("Ma phong:");
        roomPanel.add(roomLabel);
        tfRoomCode = new JTextField(8);
        tfRoomCode.setHorizontalAlignment(JTextField.CENTER);
        roomPanel.add(tfRoomCode);
        panel.add(roomPanel, gbc);

        gbc.gridwidth = 1;
        btnHost = new JButton("TAO PHONG");
        gbc.gridy = 3; gbc.gridx = 0;
        btnHost.addActionListener(e -> onHost());
        panel.add(btnHost, gbc);

        btnJoin = new JButton("GHEP TRAN");
        gbc.gridx = 1;
        btnJoin.addActionListener(e -> onJoin());
        panel.add(btnJoin, gbc);

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(lblStatus, gbc);

        btnCancel = new JButton("HUY");
        btnCancel.setVisible(false);
        gbc.gridy = 5;
        btnCancel.addActionListener(e -> lanController.cancel());
        panel.add(btnCancel, gbc);

        JButton btnBack = new JButton("QUAY LAI");
        gbc.gridy = 6;
        btnBack.addActionListener(e -> {
            lanController.cancel();
            cardLayout.show(mainPanel, "menu");
        });
        panel.add(btnBack, gbc);

        return panel;
    }

    @Override
    public void onStatus(String text) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    @Override
    public void onGameStart(String color, NetworkPeer peer) {
        SwingUtilities.invokeLater(() -> startGameLAN(color, peer));
    }

    @Override
    public void onMessage(String msg) {
        GamePanel gp = gamePanel;
        if (gp != null) gp.onNetworkMessage(msg);
    }

    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(error);
            btnHost.setEnabled(true);
            btnJoin.setEnabled(true);
            btnCancel.setVisible(false);
        });
    }

    @Override
    public void onCancel() {
        SwingUtilities.invokeLater(() -> {
            btnHost.setEnabled(true);
            btnJoin.setEnabled(true);
            btnCancel.setVisible(false);
            lblStatus.setText(" ");
        });
    }

    private void onStart() {
        if (rbLAN.isSelected()) {
            cardLayout.show(mainPanel, "lan");
        } else {
            String color = rbWhite.isSelected() ? "WHITE" : "BLACK";
            startGameAI(color);
        }
    }

    private void onHost() {
        String roomCode = tfRoomCode.getText().trim();
        if (roomCode.isEmpty()) {
            lblStatus.setText("Vui long nhap ma phong!");
            return;
        }

        btnHost.setEnabled(false);
        btnJoin.setEnabled(false);
        btnCancel.setVisible(true);

        lanController.host(roomCode, this);
    }

    private void onJoin() {
        String roomCode = tfRoomCode.getText().trim();
        if (roomCode.isEmpty()) {
            lblStatus.setText("Vui long nhap ma phong!");
            return;
        }

        btnHost.setEnabled(false);
        btnJoin.setEnabled(false);
        btnCancel.setVisible(true);

        lanController.join(roomCode, this);
    }

    private void startGameAI(String color) {
        dispose();
        int playerColor = color.equals("WHITE") ? GameState.WHITE : GameState.BLACK;
        int depth = (int) depthSpinner.getValue();
        JFrame window = new JFrame("Chess Game - AI Mode");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gp = new GamePanel(0, playerColor, null, depth);
        gp.setMenuLauncher(this);
        window.add(gp);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gp.launchGame();
    }

    private void startGameLAN(String myColor, NetworkPeer peer) {
        dispose();
        int playerColor = myColor.equals("WHITE") ? GameState.WHITE : GameState.BLACK;
        JFrame window = new JFrame("Chess Game - LAN Mode");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gp = new GamePanel(1, playerColor, peer, 3);
        gp.setMenuLauncher(this);
        window.add(gp);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gp.launchGame();

        gamePanel = gp;
    }
}
