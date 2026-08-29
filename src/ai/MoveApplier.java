package ai;

import static ai.Evaluator.*;

class MoveApplier {
    void applyMove(BoardState s, SMove m) {
        int piece = s.board[m.fr][m.fc];
        s.board[m.tr][m.tc] = piece;
        s.board[m.fr][m.fc] = EMPTY;

        if (Math.abs(piece) == W_KING) {
            if (piece > 0) { s.wKingRow = m.tr; s.wKingCol = m.tc; }
            else { s.bKingRow = m.tr; s.bKingCol = m.tc; }
        }

        if (Math.abs(piece) == W_KING) {
            if (piece > 0) { s.removeCastle(0); s.removeCastle(1); }
            else { s.removeCastle(2); s.removeCastle(3); }
        }
        if (m.fr == 7 && m.fc == 0) s.removeCastle(1);
        if (m.fr == 7 && m.fc == 7) s.removeCastle(0);
        if (m.tr == 7 && m.tc == 0) s.removeCastle(1);
        if (m.tr == 7 && m.tc == 7) s.removeCastle(0);
        if (m.fr == 0 && m.fc == 0) s.removeCastle(3);
        if (m.fr == 0 && m.fc == 7) s.removeCastle(2);
        if (m.tr == 0 && m.tc == 0) s.removeCastle(3);
        if (m.tr == 0 && m.tc == 7) s.removeCastle(2);

        if (m.isCastling) {
            s.board[m.rookTr][m.rookTc] = s.board[m.rookFr][m.rookFc];
            s.board[m.rookFr][m.rookFc] = EMPTY;
        }

        s.enPassantCol = -1;
        if (Math.abs(piece) == W_PAWN && Math.abs(m.tr - m.fr) == 2)
            s.enPassantCol = m.fc;

        if (m.isEnPassant) {
            int capRow = (piece > 0) ? m.tr + 1 : m.tr - 1;
            s.board[capRow][m.tc] = EMPTY;
        }

        if (m.promoteTo != 0)
            s.board[m.tr][m.tc] = m.promoteTo;

        if (Math.abs(piece) == W_PAWN || m.captured != 0)
            s.halfMoveClock = 0;
        else
            s.halfMoveClock++;

        s.whiteToMove = !s.whiteToMove;
    }

    // ── new API (used by ChessAI) ────────────────────────────────────────

    public static void apply(BoardState board, Move m) {
        // Save undo info
        board.capturedPiece = board.board[m.toCol()][m.toRow()];
        board.prevCastleRights = board.castling.clone()[0];
        board.prevEpAvailable = board.hasEP();
        board.prevEpCol = board.epCol;
        board.prevEpRow = board.epRow;

        board.updateHashRemovePiece(m.fromCol(), m.fromRow());
        board.updateMaterialRemove(board.board[m.toCol()][m.toRow()]);
        board.updateHashRemovePiece(m.toCol(), m.toRow());

        board.board[m.toCol()][m.toRow()] = board.board[m.fromCol()][m.fromRow()];
        board.board[m.fromCol()][m.fromRow()] = 0;

        board.updateHashAddPiece(m.toCol(), m.toRow(), board.board[m.toCol()][m.toRow()]);

        // Update king position
        if (Math.abs(board.board[m.toCol()][m.toRow()]) == Evaluator.W_KING) {
            if (board.board[m.toCol()][m.toRow()] > 0) {
                board.whiteKingCol = m.toCol();
                board.whiteKingRow = m.toRow();
            } else {
                board.blackKingCol = m.toCol();
                board.blackKingRow = m.toRow();
            }
        }

        board.whiteToMove = !board.whiteToMove;
        board.hash ^= Hasher.sideRandomNumber();

        board.epCol = board.epRow = -1;
        board.hash ^= Hasher.epRandomNumber(board.prevEpCol);
    }

    public static void unapply(BoardState board, Move m) {
        board.whiteToMove = !board.whiteToMove;
        board.hash ^= Hasher.sideRandomNumber();

        board.board[m.fromCol()][m.fromRow()] = board.board[m.toCol()][m.toRow()];
        board.board[m.toCol()][m.toRow()] = board.capturedPiece;

        board.updateMaterialAdd(board.capturedPiece);

        // Restore king position
        if (Math.abs(board.board[m.fromCol()][m.fromRow()]) == Evaluator.W_KING) {
            if (board.board[m.fromCol()][m.fromRow()] > 0) {
                board.whiteKingCol = m.fromCol();
                board.whiteKingRow = m.fromRow();
            } else {
                board.blackKingCol = m.fromCol();
                board.blackKingRow = m.fromRow();
            }
        }

        board.castling[0] = board.prevCastleRights;
        board.epCol = board.prevEpCol;
        board.epRow = board.prevEpRow;
    }
}
