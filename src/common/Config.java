package common;

public class Config {
    public static final int BOARD_SIZE = 8;
    public static final int SQUARE_SIZE = 100;
    public static final int HALF_SQUARE_SIZE = SQUARE_SIZE / 2;

    public static final int WINDOW_WIDTH = 1100;
    public static final int WINDOW_HEIGHT = 800;

    public static final int GAME_FPS = 90;

    public static final int DISCOVERY_PORT = 5001;
    public static final int GAME_PORT = 5000;
    public static final int DISCOVERY_TIMEOUT = 15000;
    public static final int PING_INTERVAL = 3000;
    public static final int TIMEOUT_MS = 10000;
    public static final int CONNECT_TIMEOUT = 5000;

    public static final int AI_DEPTH_EASY = 3;
    public static final int AI_DEPTH_MEDIUM = 4;
    public static final int AI_DEPTH_HARD = 5;

    public static final int MOVE_HISTORY_ROW_HEIGHT = 22;
    public static final int TABLE_TOP = 287;
    public static final int TABLE_BOTTOM = 527;
    public static final int SCROLL_AREA_TOP = TABLE_TOP + 32;
    public static final int SCROLL_AREA_BOTTOM = TABLE_BOTTOM - 30;

    public static final int FIFTY_MOVE_LIMIT = 100;
    public static final int THREEFOLD_REPETITION = 3;

    public static final int GAME_MODE_AI = 0;
    public static final int GAME_MODE_LAN = 1;
}
