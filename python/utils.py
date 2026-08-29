import chess
import numpy as np

PIECE_MAP = {
    chess.PAWN: 0, chess.KNIGHT: 1, chess.BISHOP: 2,
    chess.ROOK: 3, chess.QUEEN: 4, chess.KING: 5
}

def board_to_tensor(board: chess.Board) -> np.ndarray:
    tensor = np.zeros((17, 8, 8), dtype=np.float32)
    for square in chess.SQUARES:
        piece = board.piece_at(square)
        if piece is None:
            continue
        row = square // 8
        col = square % 8
        plane = PIECE_MAP[piece.piece_type]
        if piece.color == chess.BLACK:
            plane += 6
        tensor[plane, row, col] = 1.0
    if board.turn == chess.WHITE:
        tensor[12, :, :] = 1.0
    if board.has_kingside_castling_rights(chess.WHITE):
        tensor[13, :, :] = 1.0
    if board.has_queenside_castling_rights(chess.WHITE):
        tensor[14, :, :] = 1.0
    if board.has_kingside_castling_rights(chess.BLACK):
        tensor[15, :, :] = 1.0
    if board.has_queenside_castling_rights(chess.BLACK):
        tensor[16, :, :] = 1.0
    return tensor

def move_to_index(move: chess.Move) -> int:
    return move.from_square * 64 + move.to_square

def index_to_move(idx: int) -> tuple:
    return (idx // 64, idx % 64)

def augment_position(board: chess.Board, move: chess.Move):
    board = board.mirror()
    move = chess.Move(
        move.from_square ^ 56,
        move.to_square ^ 56,
        promotion=move.promotion
    )
    return board, move
