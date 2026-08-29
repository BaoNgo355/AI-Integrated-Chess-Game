package ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

public class StockfishEngine implements AIPlayer {

    private Process process;
    private BufferedReader reader;
    private OutputStreamWriter writer;
    private boolean running;

    private int elo = 1800;
    private int hashMB = 64;
    private int threads = 1;
    private int searchDepth = -1;
    private int moveTimeMs = -1;
    private boolean limitStrength = true;

    private static final String DEFAULT_PATH = "../stockfish-windows-x86-64-avx2/stockfish/stockfish-windows-x86-64-avx2.exe";

    private String enginePath;
    private long totalNodes;
    private int lastDepthReached;

    public StockfishEngine() {
        this(DEFAULT_PATH);
    }

    public StockfishEngine(String enginePath) {
        this.enginePath = enginePath;
    }

    public void setEnginePath(String path) { this.enginePath = path; }

    public void start() throws IOException {
        if (running) return;
        ProcessBuilder pb = new ProcessBuilder(enginePath);
        pb.redirectErrorStream(true);
        process = pb.start();
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = new OutputStreamWriter(process.getOutputStream());
        running = true;

        send("uci");
        waitFor("uciok", 5000);

        if (limitStrength) {
            send("setoption name UCI_LimitStrength value true");
            send("setoption name UCI_Elo value " + elo);
        } else {
            send("setoption name UCI_LimitStrength value false");
        }
        send("setoption name Hash value " + hashMB);
        send("setoption name Threads value " + threads);

        send("isready");
        waitFor("readyok", 5000);
    }

    public void stop() {
        running = false;
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
        reader = null;
        writer = null;
    }

    public boolean isRunning() { return running; }

    public void setElo(int elo) { this.elo = elo; this.limitStrength = true; }
    public int getElo() { return elo; }

    public void useFixedDepth() { this.limitStrength = false; }

    public void setHash(int mb) { this.hashMB = mb; }
    public int getHash() { return hashMB; }

    public void setThreads(int threads) { this.threads = threads; }
    public int getThreads() { return threads; }

    public void setDepth(int depth) { this.searchDepth = depth; this.moveTimeMs = -1; }
    public void setMoveTime(int ms) { this.moveTimeMs = ms; this.searchDepth = -1; }

    public int getSearchDepth() { return searchDepth; }
    public long getTotalNodes() { return totalNodes; }
    public int getLastDepthReached() { return lastDepthReached; }

    private void send(String cmd) throws IOException {
        if (!running || process == null) return;
        writer.write(cmd + "\n");
        writer.flush();
    }

    private void waitFor(String target, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        StringBuilder buf = new StringBuilder();
        while (System.currentTimeMillis() < deadline) {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line == null) return;
                buf.append(line).append('\n');
                if (line.contains(target)) return;
            }
            try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    @Override
    public Move search(BoardState board) {
        if (!running) {
            try { start(); } catch (IOException e) {
                System.err.println("[Stockfish] Failed to start: " + e.getMessage());
                return null;
            }
        }

        try {
            totalNodes = 0;
            lastDepthReached = 0;

            String fen = FENUtils.toFEN(board);
            send("ucinewgame");
            send("position fen " + fen);

            if (searchDepth > 0) {
                send("go depth " + searchDepth);
            } else if (moveTimeMs > 0) {
                send("go movetime " + moveTimeMs);
            } else {
                send("go depth 5");
            }

            String bestMoveStr = null;
            long deadline = System.currentTimeMillis() + 600000;

            while (System.currentTimeMillis() < deadline) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (line == null) {
                        return null;
                    }
                    if (line.startsWith("info")) {
                        parseInfo(line);
                    } else if (line.startsWith("bestmove")) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 2) {
                            bestMoveStr = parts[1];
                        }
                        break;
                    }
                }
                if (bestMoveStr != null) break;
                try { Thread.sleep(1); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            if (bestMoveStr == null || bestMoveStr.equals("(none)")) {
                return null;
            }

            return FENUtils.fromUCI(bestMoveStr);

        } catch (IOException e) {
            System.err.println("[Stockfish] IO error: " + e.getMessage());
            return null;
        }
    }

    private void parseInfo(String line) {
        String[] tokens = line.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals("nodes") && i + 1 < tokens.length) {
                try { totalNodes = Long.parseLong(tokens[i + 1]); } catch (NumberFormatException ignored) {}
            }
            if (tokens[i].equals("depth") && i + 1 < tokens.length) {
                try { lastDepthReached = Integer.parseInt(tokens[i + 1]); } catch (NumberFormatException ignored) {}
            }
        }
    }

    @Override
    public String getName() { return "Stockfish"; }
}
