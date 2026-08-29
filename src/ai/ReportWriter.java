package ai;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ai.EngineCompetition.*;

public class ReportWriter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void writeTxtReport(List<GameRecord> records, EngineStats statsA, EngineStats statsB,
                                       int numGames, int hashMB, int depthW, int depthB,
                                       int eloW, int eloB,
                                       String filePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("====================================================");
            pw.println("  Engine Competition Report");
            pw.println("====================================================");
            pw.println();
            pw.println("Date       : " + LocalDateTime.now().format(DATE_FMT));
            pw.println("Engine A   : " + statsA.name);
            pw.println("Engine B   : " + statsB.name);
            pw.println();
            pw.println("Settings");
            pw.println("  Games    : " + numGames);
            pw.println("  Hash     : " + hashMB + " MB");
            pw.println("  Depth A  : " + depthW);
            pw.println("  Depth B  : " + depthB);
            pw.println("  Elo A    : " + eloW);
            pw.println("  Elo B    : " + eloB);
            pw.println();
            pw.println("----------------------------------------------------");
            pw.println("Game Results");
            pw.println("----------------------------------------------------");
            pw.printf("%-6s %-12s %-12s %-8s %-6s %-10s%n",
                      "Game", "White", "Black", "Winner", "Moves", "Duration");
            pw.println("----------------------------------------------------");

            double totalMoves = 0;
            double totalTime = 0;

            for (GameRecord rec : records) {
                pw.printf("%-6d %-12s %-12s %-8s %-6d %-10.1f%n",
                          rec.gameNumber, rec.whiteEngine, rec.blackEngine,
                          rec.winner, rec.totalMoves, rec.durationSec);
                totalMoves += rec.totalMoves;
                totalTime += rec.durationSec;
            }

            pw.println("----------------------------------------------------");
            pw.println();
            pw.println("----------------------------------------------------");
            pw.println("Moves");
            pw.println("----------------------------------------------------");
            for (GameRecord rec : records) {
                pw.printf("Game %d: ", rec.gameNumber);
                List<String> san = rec.movesSAN;
                for (int i = 0; i < san.size(); i++) {
                    if (i > 0 && i % 12 == 0) pw.printf("%n         ");
                    pw.print(san.get(i) + " ");
                }
                pw.println();
            }
            pw.println();
            pw.println("----------------------------------------------------");
            pw.println("Statistics");
            pw.println("----------------------------------------------------");

            printEngineStats(pw, statsA);
            pw.println();
            printEngineStats(pw, statsB);
            pw.println();

            pw.println("----------------------------------------------------");
            pw.println("Head-to-Head");
            pw.println("----------------------------------------------------");

            int whiteWinsA = 0, blackWinsA = 0;
            int whiteGamesA = 0, blackGamesA = 0;

            for (GameRecord rec : records) {
                String engineName = statsA.name;
                if (rec.whiteEngine.equals(engineName)) {
                    whiteGamesA++;
                    if (rec.winner.equals("White")) whiteWinsA++;
                }
                if (rec.blackEngine.equals(engineName)) {
                    blackGamesA++;
                    if (rec.winner.equals("Black")) blackWinsA++;
                }
            }

            double whiteWinRateA = whiteGamesA > 0 ? 100.0 * whiteWinsA / whiteGamesA : 0;
            double blackWinRateA = blackGamesA > 0 ? 100.0 * blackWinsA / blackGamesA : 0;
            double avgMoves = records.isEmpty() ? 0 : totalMoves / records.size();
            double avgTime = records.isEmpty() ? 0 : totalTime / records.size();

            pw.printf("%-20s : %.1f%%%n", statsA.name + " (White) Win %", whiteWinRateA);
            pw.printf("%-20s : %.1f%%%n", statsA.name + " (Black) Win %", blackWinRateA);
            pw.printf("%-20s : %.1f%n", "Average Moves", avgMoves);
            pw.printf("%-20s : %.1f s%n", "Average Game Time", avgTime);
            pw.println();
            pw.println("====================================================");
        }
    }

    private static void printEngineStats(PrintWriter pw, EngineStats st) {
        pw.println(st.name);
        pw.println("  Wins             : " + st.wins);
        pw.println("  Draws            : " + st.draws);
        pw.println("  Losses           : " + st.losses);
        pw.printf ("  Win Rate         : %.1f%%%n", st.getWinRate());
        pw.printf ("  Average Nodes    : %.0f%n", st.getAvgNodes());
        pw.printf ("  Average Time     : %.1f s%n", st.getAvgTime());
        pw.printf ("  Average Depth    : %.1f%n", st.getAvgDepth());
    }

    public static void writeCsvReport(List<GameRecord> records, String filePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("Game,White,Black,Winner,Moves,Duration,NodesWhite,NodesBlack,DepthWhite,DepthBlack,MovesSAN");
            for (GameRecord rec : records) {
                String sanStr = String.join(" ", rec.movesSAN);
                pw.printf("%d,%s,%s,%s,%d,%.3f,%d,%d,%d,%d,\"%s\"%n",
                          rec.gameNumber, rec.whiteEngine, rec.blackEngine,
                          rec.winner, rec.totalMoves, rec.durationSec,
                          rec.nodesWhite, rec.nodesBlack,
                          rec.depthWhite, rec.depthBlack,
                          sanStr);
            }
        }
    }

    public static String generateFilePath(String prefix, String dir) {
        String filename = prefix + "_" + LocalDateTime.now().format(DT_FMT);
        if (dir != null && !dir.isEmpty()) {
            return dir.replace('/', java.io.File.separatorChar)
                      .replace('\\', java.io.File.separatorChar)
                + java.io.File.separator + filename;
        }
        return filename;
    }
}
