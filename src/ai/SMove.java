package ai;

class SMove {
    int fr, fc, tr, tc;
    int captured;
    boolean isCastling, isEnPassant;
    int rookFr, rookFc, rookTr, rookTc;
    int promoteTo;

    SMove(int fr, int fc, int tr, int tc) {
        this.fr = fr; this.fc = fc; this.tr = tr; this.tc = tc;
    }
}
