import os
import time
import warnings
warnings.filterwarnings("ignore", category=UserWarning, module="chess")
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
from torch.amp import autocast, GradScaler
from config import BATCH_SIZE, EPOCHS, LEARNING_RATE, WEIGHT_DECAY, LR_STEP, LR_GAMMA, OUTPUT_DIR, MODEL_EXPORT, LABEL_SMOOTHING
from dataset import load_and_split
from model import PolicyNetwork
from export_binary import export_all

CHECKPOINT_EVERY = 1

try:
    from tqdm import tqdm
except ImportError:
    class tqdm:
        def __init__(self, iterable, **kwargs):
            self._it = iterable
        def __iter__(self):
            return iter(self._it)
        def set_postfix(self, **kwargs):
            pass

def top_k_accuracy(output, target, k=5):
    _, pred = output.topk(k, dim=1)
    correct = pred.eq(target.view(-1, 1))
    return correct.any(dim=1).float().mean().item()

def save_checkpoint(path, epoch, model, optimizer, scheduler, scaler, best_val_loss):
    torch.save({
        "epoch": epoch,
        "model_state": model.state_dict(),
        "optimizer_state": optimizer.state_dict(),
        "scheduler_state": scheduler.state_dict(),
        "scaler_state": scaler.state_dict(),
        "best_val_loss": best_val_loss,
    }, path)
    print(f"  [ckpt] Saved -> {os.path.basename(path)}", flush=True)

def load_checkpoint(path, model, optimizer, scheduler, scaler):
    ckpt = torch.load(path, map_location="cpu", weights_only=False)
    model.load_state_dict(ckpt["model_state"])
    optimizer.load_state_dict(ckpt["optimizer_state"])
    scheduler.load_state_dict(ckpt["scheduler_state"])
    if "scaler_state" in ckpt:
        scaler.load_state_dict(ckpt["scaler_state"])
    print(f"  [ckpt] Resumed from epoch {ckpt['epoch']} "
          f"(best_val_loss={ckpt['best_val_loss']:.4f})", flush=True)
    return ckpt["epoch"], ckpt["best_val_loss"]

def find_latest_checkpoint(output_dir):
    if not os.path.isdir(output_dir):
        return None
    best_epoch = -1
    best_path = None
    for fname in os.listdir(output_dir):
        if fname.startswith("checkpoint_epoch") and fname.endswith(".pt"):
            try:
                ep = int(fname.replace("checkpoint_epoch", "").replace(".pt", ""))
                if ep > best_epoch:
                    best_epoch = ep
                    best_path = os.path.join(output_dir, fname)
            except ValueError:
                pass
    return best_path

def train():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    model = PolicyNetwork().to(device)
    criterion = nn.CrossEntropyLoss(label_smoothing=LABEL_SMOOTHING)
    optimizer = optim.AdamW(model.parameters(), lr=LEARNING_RATE, weight_decay=WEIGHT_DECAY)
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=LR_STEP, gamma=LR_GAMMA)
    scaler = GradScaler('cuda')

    start_epoch = 1
    best_val_loss = float("inf")
    ckpt_path = find_latest_checkpoint(OUTPUT_DIR)
    if ckpt_path:
        start_epoch, best_val_loss = load_checkpoint(ckpt_path, model, optimizer, scheduler, scaler)
        start_epoch += 1
        if start_epoch > EPOCHS:
            print(f"Training already completed ({EPOCHS} epochs). "
                  f"Delete checkpoint files to retrain from scratch.", flush=True)
            return
        csv_mode = "a"
        print(f"Resuming training from epoch {start_epoch}/{EPOCHS}", flush=True)
    else:
        csv_mode = "w"
        print(f"Starting fresh training on {device}", flush=True)

    train_ds, val_ds, _ = load_and_split()

    train_loader = DataLoader(train_ds, batch_size=BATCH_SIZE,     num_workers=0)
    val_loader   = DataLoader(val_ds,   batch_size=BATCH_SIZE * 2, num_workers=0)

    csv_path = os.path.join(OUTPUT_DIR, "train_log.csv")
    if csv_mode == "w":
        with open(csv_path, "w") as f:
            f.write("epoch,train_loss,train_top1,train_top5,val_loss,val_top1,val_top5,lr\n")

    print(f"Training on {device}", flush=True)

    for epoch in range(start_epoch, EPOCHS + 1):
        t0 = time.time()

        model.train()
        train_loss = train_top1 = train_top5 = 0.0
        n_batches = 0
        pbar = tqdm(train_loader, desc=f"E{epoch:2d} train", leave=False)
        for inputs, labels in pbar:
            inputs, labels = inputs.to(device), labels.to(device)
            optimizer.zero_grad()
            with autocast('cuda'):
                outputs = model(inputs)
                loss = criterion(outputs, labels)
            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()

            train_loss += loss.item()
            train_top1 += top_k_accuracy(outputs, labels, 1)
            train_top5 += top_k_accuracy(outputs, labels, 5)
            n_batches += 1
            pbar.set_postfix(loss=train_loss / n_batches, top1=train_top1 / n_batches)

        scheduler.step()
        train_loss /= n_batches
        train_top1 /= n_batches
        train_top5 /= n_batches

        model.eval()
        val_loss = val_top1 = val_top5 = 0.0
        n_val_batches = 0
        pbar_val = tqdm(val_loader, desc=f"E{epoch:2d} val  ", leave=False)
        with torch.no_grad():
            for inputs, labels in pbar_val:
                inputs, labels = inputs.to(device), labels.to(device)
                with autocast('cuda'):
                    outputs = model(inputs)
                    loss = criterion(outputs, labels)
                val_loss += loss.item()
                val_top1 += top_k_accuracy(outputs, labels, 1)
                val_top5 += top_k_accuracy(outputs, labels, 5)
                n_val_batches += 1
                pbar_val.set_postfix(loss=val_loss / n_val_batches, top1=val_top1 / n_val_batches)

        val_loss /= n_val_batches
        val_top1 /= n_val_batches
        val_top5 /= n_val_batches

        elapsed = time.time() - t0
        lr_now = scheduler.get_last_lr()[0]
        print(f"E{epoch:2d}  train_loss={train_loss:.4f} top1={train_top1:.3f} top5={train_top5:.3f}  "
              f"val_loss={val_loss:.4f} top1={val_top1:.3f} top5={val_top5:.3f}  "
              f"lr={lr_now:.2e}  time={elapsed:.0f}s", flush=True)

        with open(csv_path, "a") as f:
            f.write(f"{epoch},{train_loss:.4f},{train_top1:.3f},{train_top5:.3f},"
                    f"{val_loss:.4f},{val_top1:.3f},{val_top5:.3f},{lr_now:.2e}\n")

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            torch.save(model.state_dict(), os.path.join(OUTPUT_DIR, "best_model.pt"))
            export_all(model,
                       os.path.join(OUTPUT_DIR, MODEL_EXPORT),
                       os.path.join(OUTPUT_DIR, "policy_weights.bin"))
            save_checkpoint(os.path.join(OUTPUT_DIR, "checkpoint_best.pt"),
                            epoch, model, optimizer, scheduler, scaler, best_val_loss)
            print(f"  [best] New best val_loss={best_val_loss:.4f}", flush=True)

        if epoch % CHECKPOINT_EVERY == 0:
            ckpt_name = os.path.join(OUTPUT_DIR, f"checkpoint_epoch{epoch:03d}.pt")
            save_checkpoint(ckpt_name, epoch, model, optimizer, scheduler, scaler, best_val_loss)

if __name__ == "__main__":
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    train()
