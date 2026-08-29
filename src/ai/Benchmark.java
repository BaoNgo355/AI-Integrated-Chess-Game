package ai;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Runs the engine against a set of benchmark positions and reports the
 * best move, score and node count for each one.
 *
 * <p>Usage:</p>
 * <pre>
 *   java ai.Benchmark [positions_file] [depth]
 * </pre>
 *
 * If no arguments are given the default resource file
 * {@code res/benchmark_positions.txt} is used.
 */
public final class Benchmark {

    // ── entry-point ──────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {

        int depth  = AIConfig.MATCH_PLY;
        String file = "res/benchmark_positions.txt";

        if (args.length >= 1) file = args[0];
        if (args.length >= 2) depth = Integer.parseInt(args[1]);

        AIConfig.load("res/ai_config.properties");

        List<Position> positions = loadPositions(file);
        System.out.println("Loaded " + positions.size() + " benchmark positions\n");

        ChessAI ai = new ChessAI();
        long totalNodes = 0;

        for (Position pos : positions) {
            BoardState board = BoardState.fromFEN(pos.fen);
            if (board == null) {
                System.out.println("Invalid FEN: " + pos.fen);
                continue;
            }

            long t0     = System.currentTimeMillis();
            String move = ai.bestMove(board, depth);
            long elapsed = System.currentTimeMillis() - t0;
            int nodes   = ai.lastNodeCount();

            totalNodes += nodes;

            System.out.printf("%-35s  best=%-6s  score=%+5d  nodes=%7d  time=%4dms%n",
                              pos.name, move, ai.lastScore(), nodes, elapsed);
        }

        System.out.println("\nTotal nodes: " + totalNodes);
    }

    // ── position record ──────────────────────────────────────────────────

    private static final class Position {
        final String name, fen;
        Position(String n, String f) { name = n; fen = f; }
    }

    /** Parse a benchmark file.  Lines starting with '#' are comments. */
    static List<Position> loadPositions(String path) throws IOException {
        List<Position> out = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(path))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\s*;\\s*", 3);
            if (parts.length < 2) continue;

            String name = parts[0].trim();
            String fen  = parts[1].trim();

            // strip optional move hint (third field)
            out.add(new Position(name, fen));
        }
        return out;
    }
}
