import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

TWIC_DIR = os.path.join(BASE_DIR, "data", "twic")
TWIC_START = 1450
TWIC_END = 1649
MIN_ELO = 2200

BATCH_SIZE = 512
EPOCHS = 100
LEARNING_RATE = 5e-4
WEIGHT_DECAY = 1e-4
LR_STEP = 20
LR_GAMMA = 0.5
LABEL_SMOOTHING = 0.1

TRAIN_SPLIT = 0.85
VAL_SPLIT = 0.10
TEST_SPLIT = 0.05

OUTPUT_DIR = os.path.join(BASE_DIR, "output")
MODEL_EXPORT = "policy_weights.json"
