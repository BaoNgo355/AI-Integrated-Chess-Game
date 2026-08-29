package ai;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.PieceType;
import core.MoveHistoryManager;

/**
 * Compact, stack-based representation of a chess position.
 * <p>
 * Pieces are stored in a {@code int[8][8]} array whose values follow the
 * same convention as {@link PieceType}: positive = White, negative = Black.
 * This class exposes lightweight, cloneable snapshots that can be pushed and
 * popped cheaply by the search.
 * </p>
 *
 * <h3>Usage pattern</h3>
 * <pre>
 *   BoardState root = BoardState.fromFEN(fen);
 *   Move m = ...;
 *   root.push(m);          // apply
 *   root.pop();            // restore
 * </pre>
 */
public final class BoardState {

    private static final String[] COL_NAMES = {"a","b","c","d","e","f","g","h"};

    // ── board data ───────────────────────────────────────────────────────

    /** 8×8 piece array – {@code board[col][row]}, 0-indexed. */
    int[][] board = new int[8][8];

    int whiteKingCol, whiteKingRow;
    int blackKingCol, blackKingRow;

    /** Side to move: {@code true}=White. */
    boolean whiteToMove;

    /** Castling availability – index order: WK, WQ, BK, BQ. */
    boolean[] castling = new boolean[4];

    /** En-passant target square, or {@code -1} if none. */
    int epCol = -1, epRow = -1;

    int halfmoveClock;
    int fullmoveNumber;

    /** Incrementally updated Zobrist hash. */
    long hash;

    /** Incrementally updated total material (centipawns, White positive). */
    int material;

    // ── undo record (pushed onto a stack inside MoveApplier) ─────────────

    int capturedPiece;
    boolean prevCastleRights;
    boolean prevEpAvailable;
    int     prevEpCol, prevEpRow;

    // ── construction / cloning ───────────────────────────────────────────

    public BoardState() { }

    /**
     * Deep-clone every mutable field so that {@code push}/{@code pop} on the
     * copy never affects the original.
     */
    public BoardState clone() {
        BoardState c   = new BoardState();
        c.board        = new int[8][8];
        for (int i = 0; i < 8; i++)
            System.arraycopy(board[i], 0, c.board[i], 0, 8);

        c.whiteKingCol = whiteKingCol;  c.whiteKingRow = whiteKingRow;
        c.blackKingCol = blackKingCol;  c.blackKingRow = blackKingRow;
        c.whiteToMove  = whiteToMove;

        c.castling  = castling.clone();
        c.epCol     = epCol;  c.epRow = epRow;
        c.halfmoveClock  = halfmoveClock;
        c.fullmoveNumber = fullmoveNumber;
        c.hash      = hash;
        c.material  = material;
        return c;
    }

    // ── FEN ──────────────────────────────────────────────────────────────

    /**
     * FEN parser that also rebuilds the incremental hash and material
     * total.
     */
    public static BoardState fromFEN(String fen) {
        if (fen == null || fen.trim().isEmpty()) return null;
        String[] parts = fen.trim().split("\\s+");

        BoardState s = new BoardState();

        // piece placement
        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) return null;

        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char ch : ranks[row].toCharArray()) {
                if (ch == '/') continue;
                if (Character.isDigit(ch)) {
                    col += ch - '0';
                } else {
                    s.board[col][7 - row] = charToCode(ch);
                    if (ch == 'K') { s.whiteKingCol = col; s.whiteKingRow = 7 - row; }
                    if (ch == 'k') { s.blackKingCol = col; s.blackKingRow = 7 - row; }
                    col++;
                }
            }
        }

        // side to move
        s.whiteToMove = parts.length < 2 || parts[1].equals("w");

        // castling
        if (parts.length >= 3) {
            String c = parts[2];
            s.castling[0] = c.contains("K");
            s.castling[1] = c.contains("Q");
            s.castling[2] = c.contains("k");
            s.castling[3] = c.contains("q");
        }

        // en-passant
        if (parts.length >= 4 && !parts[3].equals("-")) {
            s.epCol = colNameToIndex(parts[3].substring(0, 1));
            s.epRow = parts[3].charAt(1) - '1';
        } else {
            s.epCol = s.epRow = -1;
        }

        // clocks
        s.halfmoveClock  = (parts.length >= 5) ? Integer.parseInt(parts[4]) : 0;
        s.fullmoveNumber = (parts.length >= 6) ? Integer.parseInt(parts[5]) : 1;

        // rebuild incremental data
        s.hash     = Hasher.computeHash(s);
        s.material = computeMaterial(s);

        return s;
    }

    /** Export the current position as a FEN string. */
    public String toFEN() {
        StringBuilder sb = new StringBuilder();

        for (int row = 7; row >= 0; row--) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                int p = board[col][row];
                if (p == 0) { empty++; continue; }
                if (empty > 0) { sb.append(empty); empty = 0; }
                sb.append(codeToChar(p));
            }
            if (empty > 0) sb.append(empty);
            if (row > 0) sb.append('/');
        }

        sb.append(whiteToMove ? " w " : " b ");

        StringBuilder cb = new StringBuilder();
        if (castling[0]) cb.append('K');
        if (castling[1]) cb.append('Q');
        if (castling[2]) cb.append('k');
        if (castling[3]) cb.append('q');
        sb.append(cb.length() == 0 ? "-" : cb).append(' ');

        if (epCol >= 0) sb.append(COL_NAMES[epCol]).append(epRow + 1);
        else sb.append('-');

        sb.append(' ').append(halfmoveClock);
        sb.append(' ').append(fullmoveNumber);

        return sb.toString();
    }

    // ── debug: pretty-print board ────────────────────────────────────────

    public void printBoard() {
        System.out.println("\n  +---+---+---+---+---+---+---+---+");
        for (int row = 7; row >= 0; row--) {
            System.out.print((row + 1) + " |");
            for (int col = 0; col < 8; col++) {
                char c = (board[col][row] == 0) ? ' ' : codeToChar(board[col][row]);
                System.out.print(" " + c + " |");
            }
            System.out.println("\n  +---+---+---+---+---+---+---+---+");
        }
        System.out.println("    a   b   c   d   e   f   g   h\n");
    }

    // ── incremental update helpers (called by MoveApplier) ───────────────

    void updateHashRemovePiece(int col, int row) {
        int code = board[col][row];
        if (code != 0)
            hash ^= Hasher.randomNumber(col, row, code);
    }

    void updateHashAddPiece(int col, int row, int code) {
        if (code != 0)
            hash ^= Hasher.randomNumber(col, row, code);
    }

    void updateHashCastling(boolean[] oldRights) {
        for (int i = 0; i < 4; i++)
            if (castling[i] != oldRights[i])
                hash ^= Hasher.castlingRandomNumber(i);
    }

    void updateHashEP(int oldCol, int oldRow) {
        if (oldCol != epCol || oldRow != epRow)
            hash ^= Hasher.epRandomNumber(epCol);
    }

    void updateMaterialAdd(int code)    { material += MaterialTable.relativeScore(code); }
    void updateMaterialRemove(int code) { material -= MaterialTable.relativeScore(code); }

    void updateMaterialReplace(int removed, int added) {
        material -= MaterialTable.relativeScore(removed);
        material += MaterialTable.relativeScore(added);
    }

    void zeroKeys() { hash = 0; }

    // ── bounds queries ───────────────────────────────────────────────────

    boolean isWhite(int col, int row) { return board[col][row] > 0; }
    boolean isBlack(int col, int row) { return board[col][row] < 0; }
    boolean inBounds(int col, int row){ return col >= 0 && col < 8 && row >= 0 && row < 8; }
    boolean isEmpty(int col, int row) { return board[col][row] == 0; }

    boolean whiteToMove()            { return whiteToMove; }
    boolean whiteCanCastleKingside()  { return castling[0]; }
    boolean whiteCanCastleQueenside() { return castling[1]; }
    boolean blackCanCastleKingside()  { return castling[2]; }
    boolean blackCanCastleQueenside() { return castling[3]; }

    boolean hasEP() { return epCol >= 0; }

    int pieceAt(int col, int row) {
        if (!inBounds(col, row)) return 0;
        return board[col][row];
    }

    boolean isEnemy(int col, int row) {
        int p = pieceAt(col, row);
        return whiteToMove ? p < 0 : p > 0;
    }

    boolean isFriendly(int col, int row) {
        int p = pieceAt(col, row);
        return whiteToMove ? p > 0 : p < 0;
    }

    // ── utility ──────────────────────────────────────────────────────────

    private static final Pattern MVVLVA_MOVE_RE =
            Pattern.compile("^([a-h][1-8])([a-h][1-8])([qrbn])?$");

    public static int compareToRank(Move m1, Move m2) {
        return Integer.compare(rankOf(m2), rankOf(m1));
    }

    static int rankOf(Move m) {
        if (m == null) return Integer.MIN_VALUE;
        Matcher matcher = MVVLVA_MOVE_RE.matcher(m.toString());
        if (!matcher.matches()) return 0;
        int fromCol = colNameToIndex(matcher.group(1).substring(0, 1));
        int fromRow = matcher.group(1).charAt(1) - '1';
        int toCol   = colNameToIndex(matcher.group(2).substring(0, 1));
        int toRow   = matcher.group(2).charAt(1) - '1';
        int victim  = pieceAtStatic(null, toCol, toRow);
        int attacker = pieceAtStatic(null, fromCol, fromRow);
        return mvvlvaScore(attacker, victim);
    }

    private static int pieceAtStatic(BoardState b, int c, int r) {
        return (b != null) ? b.pieceAt(c, r) : 0;
    }

    static int mvvlvaScore(int attacker, int victim) {
        int aVal = Math.abs(attacker);
        int vVal = Math.abs(victim);
        if (aVal == 0 || vVal == 0) return 0;
        return vVal * 10 - aVal;
    }

    // ── piece-code helpers ───────────────────────────────────────────────

    private static int charToCode(char ch) {
        return switch (ch) {
            case 'P' ->  PieceType.WHITE_PAWN;
            case 'N' ->  PieceType.WHITE_KNIGHT;
            case 'B' ->  PieceType.WHITE_BISHOP;
            case 'R' ->  PieceType.WHITE_ROOK;
            case 'Q' ->  PieceType.WHITE_QUEEN;
            case 'K' ->  PieceType.WHITE_KING;
            case 'p' ->  PieceType.BLACK_PAWN;
            case 'n' ->  PieceType.BLACK_KNIGHT;
            case 'b' ->  PieceType.BLACK_BISHOP;
            case 'r' ->  PieceType.BLACK_ROOK;
            case 'q' ->  PieceType.BLACK_QUEEN;
            case 'k' ->  PieceType.BLACK_KING;
            default  ->  0;
        };
    }

    private static char codeToChar(int code) {
        return switch (code) {
            case PieceType.WHITE_PAWN   -> 'P';
            case PieceType.WHITE_KNIGHT -> 'N';
            case PieceType.WHITE_BISHOP -> 'B';
            case PieceType.WHITE_ROOK   -> 'R';
            case PieceType.WHITE_QUEEN  -> 'Q';
            case PieceType.WHITE_KING   -> 'K';
            case PieceType.BLACK_PAWN   -> 'p';
            case PieceType.BLACK_KNIGHT -> 'n';
            case PieceType.BLACK_BISHOP -> 'b';
            case PieceType.BLACK_ROOK   -> 'r';
            case PieceType.BLACK_QUEEN  -> 'q';
            case PieceType.BLACK_KING   -> 'k';
            default                     -> ' ';
        };
    }

    private static int colNameToIndex(String name) {
        return switch (name) {
            case "a" -> 0; case "b" -> 1; case "c" -> 2; case "d" -> 3;
            case "e" -> 4; case "f" -> 5; case "g" -> 6; case "h" -> 7;
            default  -> -1;
        };
    }

    /** Total material on the board in centipawns (White positive). */
    static int computeMaterial(BoardState s) {
        int total = 0;
        for (int c = 0; c < 8; c++)
            for (int r = 0; r < 8; r++)
                total += MaterialTable.relativeScore(s.board[c][r]);
        return total;
    }

    // ── conversion between BoardState ↔ Position ──────────────────────────

    /**
     * Returns all pseudo-legal moves (may leave own king in check).
     */
    public List<Move> generatePseudoLegalMoves() {
        return MoveGenerator.generateMoves(this, false);
    }

    /** Number of pieces of the given colour currently on the board. */
    public int pieceCount(boolean white) {
        int count = 0;
        int sign = white ? 1 : -1;
        for (int c = 0; c < 8; c++)
            for (int r = 0; r < 8; r++)
                if (Integer.signum(board[c][r]) == sign) count++;
        return count;
    }

    // ── conversion helpers (used by Evaluator and SQE) ───────────────────

    /** Convert 0-63 index to column (0-7). */
    public static int idxToCol(int idx) { return idx & 7; }
    /** Convert 0-63 index to row (0-7). */
    public static int idxToRow(int idx) { return idx >> 3; }
}
