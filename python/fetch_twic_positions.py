"""
Extract benchmark positions from TWIC PGN games.
"""
import os
import sys
import random
import chess.pgn

from config import TWIC_DIR, TWIC_START, TWIC_END, MIN_ELO


def parse_args():
    args = {
        "count": 30,
        "min_elo": MIN_ELO,
        "min_ply": 10,
        "max_ply": 30,
    }
    for arg in sys.argv[1:]:
        if arg.startswith("--count="):
            args["count"] = int(arg.split("=", 1)[1])
        elif arg.startswith("--min-elo="):
            args["min_elo"] = int(arg.split("=", 1)[1])
        elif arg.startswith("--min-ply="):
            args["min_ply"] = int(arg.split("=", 1)[1])
        elif arg.startswith("--max-ply="):
            args["max_ply"] = int(arg.split("=", 1)[1])
    return args


def passes_elo(headers):
    try:
        white_elo = int(headers.get("WhiteElo", 0) or 0)
        black_elo = int(headers.get("BlackElo", 0) or 0)
    except (ValueError, TypeError):
        return False
    return white_elo >= MIN_ELO and black_elo >= MIN_ELO


def extract_positions(count, min_elo, min_ply, max_ply):
    positions = []
    rng = random.Random(42)

    pgn_files = []
    for num in range(TWIC_START, TWIC_END + 1):
        path = os.path.join(TWIC_DIR, f"twic{num}.pgn")
        if os.path.exists(path):
            pgn_files.append((path, num))

    rng.shuffle(pgn_files)

    for filepath, twic_num in pgn_files:
        if len(positions) >= count:
            break

        with open(filepath, encoding="utf-8", errors="ignore") as f:
            while True:
                game = chess.pgn.read_game(f)
                if game is None:
                    break

                headers = game.headers
                if not passes_elo(headers):
                    continue

                board = game.board()
                moves = list(game.mainline_moves())

                if len(moves) < max_ply:
                    continue

                ply = rng.randint(min_ply, max_ply)
                for i, move in enumerate(moves):
                    if i >= ply:
                        break
                    board.push(move)

                if ply >= len(moves):
                    continue

                best_move = moves[ply]
                fen = board.fen()

                white = headers.get("White", "?")
                black = headers.get("Black", "?")
                white_elo = headers.get("WhiteElo", "?")
                black_elo = headers.get("BlackElo", "?")

                positions.append({
                    "id": f"twic{twic_num}_{ply}",
                    "fen": fen,
                    "best_move": best_move.uci(),
                    "game": f"twic{twic_num}",
                    "ply": ply,
                    "white": white,
                    "black": black,
                    "white_elo": white_elo,
                    "black_elo": black_elo,
                })

                if len(positions) >= count:
                    break

    return positions


def write_positions(positions, output_path):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("# TWIC game positions + Lichess puzzle benchmark\n")
        f.write("# Format: name ; FEN ; best_move (UCI)\n\n")

        for p in positions:
            f.write(f"# {p['id']}: {p['game']} ply={p['ply']} | "
                    f"{p['white']}({p['white_elo']}) vs {p['black']}({p['black_elo']})\n")
            f.write(f"{p['id']} ; {p['fen']} ; {p['best_move']}\n")
            f.write("\n")


def main():
    args = parse_args()
    print(f"Extracting {args['count']} positions from TWIC games...")
    print(f"  Min Elo: {args['min_elo']}, Ply range: {args['min_ply']}-{args['max_ply']}")

    positions = extract_positions(args['count'], args['min_elo'], args['min_ply'], args['max_ply'])
    print(f"  Found {len(positions)} positions")

    output_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "res", "benchmark_positions.txt")
    write_positions(positions, output_path)
    print(f"  Written to: {output_path}")


if __name__ == "__main__":
    main()
