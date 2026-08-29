import os
import chess
import chess.pgn
import numpy as np
from config import TWIC_DIR, TWIC_START, TWIC_END, MIN_ELO
from utils import board_to_tensor, move_to_index, augment_position
from torch.utils.data import IterableDataset
import random

class GameRef:
    """Con tro den 1 game trong file PGN. Chi ~100 bytes, khong parse game tree."""
    __slots__ = ("filepath", "offset")
    def __init__(self, filepath: str, offset: int):
        self.filepath = filepath
        self.offset = offset

    def __repr__(self):
        return f"GameRef({os.path.basename(self.filepath)}:{self.offset})"


def game_from_ref(ref: GameRef) -> chess.pgn.Game:
    """Doc dung 1 game tu offset da luu."""
    with open(ref.filepath, encoding="utf-8", errors="ignore") as f:
        f.seek(ref.offset)
        return chess.pgn.read_game(f)


def passes_elo(headers: dict) -> bool:
    """Kiem tra ca 2 nguoi choi co Elo >= MIN_ELO."""
    try:
        white_elo = int(headers.get("WhiteElo", 0) or 0)
        black_elo = int(headers.get("BlackElo", 0) or 0)
    except (ValueError, TypeError):
        return False
    return white_elo >= MIN_ELO and black_elo >= MIN_ELO


def scan_games() -> dict:
    all_refs = []
    for num in range(TWIC_START, TWIC_END + 1):
        path = os.path.join(TWIC_DIR, f"twic{num}.pgn")
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8", errors="ignore") as f:
            while True:
                offset = f.tell()
                headers = chess.pgn.read_headers(f)
                if headers is None:
                    break
                if passes_elo(headers):
                    all_refs.append(GameRef(path, offset))

    rng = np.random.default_rng(42)
    rng.shuffle(all_refs)

    n = len(all_refs)
    n_train = int(n * 0.85)
    n_val = int(n * 0.10)
    print(f"Scanned {n} games (Elo>={MIN_ELO}). "
          f"Train={n_train} Val={n_val} Test={n-n_train-n_val}", flush=True)
    return {
        "train": all_refs[:n_train],
        "val": all_refs[n_train:n_train + n_val],
        "test": all_refs[n_train + n_val:],
    }


class ChessStreamDataset(IterableDataset):
    """Streaming dataset: doc PGN on-the-fly, khong giu positions trong RAM."""
    def __init__(self, game_refs: list, shuffle_buffer: int = 50000,
                 augment: bool = False):
        self.game_refs = game_refs
        self.shuffle_buffer = shuffle_buffer
        self.augment = augment

    def __len__(self):
        return len(self.game_refs) * 90

    def __iter__(self):
        refs = list(self.game_refs)
        random.shuffle(refs)

        if self.shuffle_buffer == 0:
            for ref in refs:
                try:
                    game = game_from_ref(ref)
                except Exception:
                    continue
                board = game.board()
                for move in game.mainline_moves():
                    if not board.is_legal(move):
                        board.push(move)
                        continue
                    if self.augment and random.random() < 0.5:
                        aug_board, aug_move = augment_position(board.copy(), move)
                        yield board_to_tensor(aug_board), move_to_index(aug_move)
                    else:
                        yield board_to_tensor(board), move_to_index(move)
                    board.push(move)
        else:
            buf = []
            for ref in refs:
                try:
                    game = game_from_ref(ref)
                except Exception:
                    continue
                board = game.board()
                for move in game.mainline_moves():
                    if not board.is_legal(move):
                        board.push(move)
                        continue
                    if self.augment and random.random() < 0.5:
                        aug_board, aug_move = augment_position(board.copy(), move)
                        sample = (board_to_tensor(aug_board), move_to_index(aug_move))
                    else:
                        sample = (board_to_tensor(board), move_to_index(move))
                    if len(buf) < self.shuffle_buffer:
                        buf.append(sample)
                    else:
                        idx = random.randint(0, len(buf) - 1)
                        yield buf[idx]
                        buf[idx] = sample
                    board.push(move)
            random.shuffle(buf)
            yield from buf


def load_and_split(shuffle_seed: int = 42):
    game_splits = scan_games()
    train_ds = ChessStreamDataset(game_splits["train"], shuffle_buffer=50000, augment=True)
    val_ds = ChessStreamDataset(game_splits["val"], augment=False)
    test_ds = ChessStreamDataset(game_splits["test"], augment=False)
    return train_ds, val_ds, test_ds
