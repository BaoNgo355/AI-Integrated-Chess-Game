package ai;

import java.io.*;
import java.util.Properties;

/**
 * Reads AI tuning parameters from {@code res/ai_config.properties}.
 * Every numeric key in that file is exposed as a static field so the rest of
 * the engine can read it without doing I/O themselves.
 *
 * <p>Reload at any time by calling {@link #load()} again.</p>
 */
public final class AIConfig {

    /** Search depth used when the user clicks "Hint". */
    public static int HINT_PLY = 15;

    /** Fixed depth used in engine-vs-engine matches. */
    public static int MATCH_PLY = 20;

    /** When true the engine assumes it is playing White. */
    public static boolean PLAY_WHITE = true;

    /** Multi-PV mode: 0 = single best, ≥1 = top-N lines. */
    public static int MULTI_PV = 0;

    /** Penalty in centipawns applied for every repetition of a position. */
    public static int REPETITION_PENALTY_CP = 0;

    /** Bonus (centipawns) given to the side-to-move when it has the initiative. */
    public static int TEMPO_BONUS_CP = 0;

    // ---------- Neural-net / policy parameters ----------

    /** Maximum ply the neural-net policy is allowed to look ahead. */
    public static int policyPlyLimit = 0;

    /** Number of top moves kept from the policy before statistical pruning. */
    public static int policyTopK = 5;

    // ---------- Static-search parameters ----------

    /** Killer-move bonus (centipawns) added in move ordering. */
    public static int killerBonus = 3000;

    /** Scaling factor applied to the history heuristic. */
    public static int historyScale = 5;

    /** Scaling factor for SEE (Static Exchange Evaluation) scores. */
    public static int seeScale = 100;

    /** Bonus (centipawns) for queen promotions. */
    public static int promotionBonus = 900;

    // ---------- Endgame criterion ----------

    /** Total on-board material (in centipawns) below which the game is endgame. */
    public static int endgameMaterialThreshold = 1300;

    /** King-proximity bonus multiplier for endgames. */
    public static int kingProximityBonus = 15;

    /** Scale applied to the NNUE-like evaluation blend. */
    public static double nnueScale = 1.0;

    private AIConfig() { }

    /** Convenience overload kept for backward compatibility. */
    public static void load(String filename) {
        load(new File(filename));
    }

    /**
     * Load configuration from the given properties file.
     * Values that are not present keep their compiled defaults.
     */
    public static void load(File file) {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            p.load(fis);
        } catch (FileNotFoundException e) {
            System.out.println("AI config file not found: " + file.getAbsolutePath()
                    + " – using defaults.");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // --- search parameters ---
        HINT_PLY            = getInt(p, "hint.ply.limit",            HINT_PLY);
        MATCH_PLY           = getInt(p, "match.ply.limit",           MATCH_PLY);
        PLAY_WHITE          = getBool(p, "play.white",               PLAY_WHITE);
        MULTI_PV            = getInt(p, "multi.pv",                  MULTI_PV);
        REPETITION_PENALTY_CP = getInt(p, "repetition.penalty.cp", REPETITION_PENALTY_CP);
        TEMPO_BONUS_CP      = getInt(p, "tempo.bonus.cp",           TEMPO_BONUS_CP);

        // --- neural-net / policy ---
        policyPlyLimit      = getInt(p, "policy.ply.limit",         policyPlyLimit);
        policyTopK          = getInt(p, "policy.top.k",             policyTopK);

        // --- static search ---
        killerBonus         = getInt(p, "killer.bonus",             killerBonus);
        historyScale        = getInt(p, "history.scale",            historyScale);
        seeScale            = getInt(p, "see.scale",                seeScale);
        promotionBonus      = getInt(p, "promotion.bonus",          promotionBonus);

        // --- endgame ---
        endgameMaterialThreshold = getInt(p, "endgame.material.threshold",
                                          endgameMaterialThreshold);
        kingProximityBonus  = getInt(p, "king.proximity.bonus",     kingProximityBonus);

        // --- NNUE / hybrid blend ---
        nnueScale           = getDouble(p, "nnue.scale",            nnueScale);
    }

    // ------------------------------------------------------------------ helpers

    private static int getInt(Properties p, String key, int dflt) {
        String v = p.getProperty(key);
        if (v == null) return dflt;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return dflt; }
    }

    private static double getDouble(Properties p, String key, double dflt) {
        String v = p.getProperty(key);
        if (v == null) return dflt;
        try { return Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { return dflt; }
    }

    private static boolean getBool(Properties p, String key, boolean dflt) {
        String v = p.getProperty(key);
        if (v == null) return dflt;
        return Boolean.parseBoolean(v.trim());
    }
}
