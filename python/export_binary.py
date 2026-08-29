"""Export PyTorch model weights to a simple binary format readable by Java."""
import json
import struct
import torch
from model import PolicyNetwork

BIN_MAGIC = b"CHESSNET"
BIN_VERSION = 1

def export_binary(model: PolicyNetwork, path: str):
    model.eval()
    state = model.state_dict()
    with open(path, "wb") as f:
        f.write(BIN_MAGIC)
        f.write(struct.pack("<I", BIN_VERSION))
        f.write(struct.pack("<I", len(state)))
        for name, tensor in state.items():
            data = tensor.cpu().numpy().ravel().astype("float32").tobytes()
            name_bytes = name.encode("utf-8")
            f.write(struct.pack("<I", len(name_bytes)))
            f.write(name_bytes)
            shape = list(tensor.shape)
            f.write(struct.pack("<I", len(shape)))
            for dim in shape:
                f.write(struct.pack("<I", dim))
            f.write(struct.pack("<I", len(data) // 4))
            f.write(data)

def export_json_human(model: PolicyNetwork, path: str):
    export_weights(model, path)

def export_weights(model, path):
    state = model.state_dict()
    export = {}
    for name, tensor in state.items():
        export[name] = tensor.cpu().numpy().tolist()
    with open(path, "w") as f:
        json.dump(export, f, separators=(",", ":"))

def export_all(model, json_path: str, bin_path: str):
    export_weights(model, json_path)
    export_binary(model, bin_path)
