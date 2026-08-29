package ai;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NeuralNet {

    public static final int INPUT_CHANNELS = 17;
    public static final int BOARD_SIZE = 8;
    public static final int NUM_OUTPUTS = 4096;
    private static final int NUM_RES_BLOCKS = 3;
    private static final int CHANNELS = 64;

    // Stem: Conv2d(17->64, 3x3, no bias) + BN
    private float[][][][] stemWeight;
    private float[] stemBnGamma, stemBnBeta, stemBnMean, stemBnVar;

    // 3 ResBlocks: each has BN1, Conv1, BN2, Conv2
    private float[][] rbBn1Gamma, rbBn1Beta, rbBn1Mean, rbBn1Var;
    private float[][][][][] rbConv1Weight;
    private float[][] rbBn2Gamma, rbBn2Beta, rbBn2Mean, rbBn2Var;
    private float[][][][][] rbConv2Weight;

    // Head: BN(64) -> Conv(64->128) -> BN(128)
    private float[] headBnGamma, headBnBeta, headBnMean, headBnVar;
    private float[][][][] headConvWeight;
    private float[] headBnPostGamma, headBnPostBeta, headBnPostMean, headBnPostVar;

    // FC: 8192->1024->4096
    private float[][] fc0Weight;
    private float[] fc0Bias;
    private float[][] fc1Weight;
    private float[] fc1Bias;

    // Pre-allocated zero bias for conv layers (no bias)
    private final float[] zeroBias64 = new float[64];
    private final float[] zeroBias128 = new float[128];

    public NeuralNet(String path) throws IOException {
        if (path.endsWith(".bin")) {
            Map<String, Object> data = loadBinary(path);
            initFromMap(data);
        } else {
            Map<String, Object> data = parseJsonFile(path);
            initFromMap(data);
        }
    }

    @SuppressWarnings("unchecked")
    private void initFromMap(Map<String, Object> data) {
        // Stem
        stemWeight = to4d(data.get("stem.0.weight"), 64, 17, 3, 3);
        stemBnGamma = to1d(data.get("stem.1.weight"), 64);
        stemBnBeta = to1d(data.get("stem.1.bias"), 64);
        stemBnMean = to1d(data.get("stem.1.running_mean"), 64);
        stemBnVar = to1d(data.get("stem.1.running_var"), 64);

        // 3 ResBlocks
        rbBn1Gamma = new float[NUM_RES_BLOCKS][];
        rbBn1Beta = new float[NUM_RES_BLOCKS][];
        rbBn1Mean = new float[NUM_RES_BLOCKS][];
        rbBn1Var = new float[NUM_RES_BLOCKS][];
        rbConv1Weight = new float[NUM_RES_BLOCKS][][][][];
        rbBn2Gamma = new float[NUM_RES_BLOCKS][];
        rbBn2Beta = new float[NUM_RES_BLOCKS][];
        rbBn2Mean = new float[NUM_RES_BLOCKS][];
        rbBn2Var = new float[NUM_RES_BLOCKS][];
        rbConv2Weight = new float[NUM_RES_BLOCKS][][][][];

        for (int i = 0; i < NUM_RES_BLOCKS; i++) {
            rbBn1Gamma[i] = to1d(data.get("res_blocks." + i + ".bn1.weight"), 64);
            rbBn1Beta[i] = to1d(data.get("res_blocks." + i + ".bn1.bias"), 64);
            rbBn1Mean[i] = to1d(data.get("res_blocks." + i + ".bn1.running_mean"), 64);
            rbBn1Var[i] = to1d(data.get("res_blocks." + i + ".bn1.running_var"), 64);
            rbConv1Weight[i] = to4d(data.get("res_blocks." + i + ".conv1.weight"), 64, 64, 3, 3);
            rbBn2Gamma[i] = to1d(data.get("res_blocks." + i + ".bn2.weight"), 64);
            rbBn2Beta[i] = to1d(data.get("res_blocks." + i + ".bn2.bias"), 64);
            rbBn2Mean[i] = to1d(data.get("res_blocks." + i + ".bn2.running_mean"), 64);
            rbBn2Var[i] = to1d(data.get("res_blocks." + i + ".bn2.running_var"), 64);
            rbConv2Weight[i] = to4d(data.get("res_blocks." + i + ".conv2.weight"), 64, 64, 3, 3);
        }

        // Head
        headBnGamma = to1d(data.get("head.0.weight"), 64);
        headBnBeta = to1d(data.get("head.0.bias"), 64);
        headBnMean = to1d(data.get("head.0.running_mean"), 64);
        headBnVar = to1d(data.get("head.0.running_var"), 64);
        headConvWeight = to4d(data.get("head.2.weight"), 128, 64, 3, 3);
        headBnPostGamma = to1d(data.get("head.3.weight"), 128);
        headBnPostBeta = to1d(data.get("head.3.bias"), 128);
        headBnPostMean = to1d(data.get("head.3.running_mean"), 128);
        headBnPostVar = to1d(data.get("head.3.running_var"), 128);

        // FC
        fc0Weight = to2d(data.get("head.6.weight"), 1024, 8192);
        fc0Bias = to1d(data.get("head.6.bias"), 1024);
        fc1Weight = to2d(data.get("head.8.weight"), 4096, 1024);
        fc1Bias = to1d(data.get("head.8.bias"), 4096);
    }

    public float[] forward(float[][][] input8x8x17) {
        float[][][] x = input8x8x17;

        // Stem: Conv(17->64, no bias) -> BN -> ReLU
        x = conv2d(x, stemWeight, zeroBias64);
        x = batchNorm(x, stemBnGamma, stemBnBeta, stemBnMean, stemBnVar);
        x = relu(x);

        // 3x ResBlock: BN -> ReLU -> Conv -> BN -> ReLU -> Conv + skip
        for (int i = 0; i < NUM_RES_BLOCKS; i++) {
            float[][][] residual = x;
            x = batchNorm(x, rbBn1Gamma[i], rbBn1Beta[i], rbBn1Mean[i], rbBn1Var[i]);
            x = relu(x);
            x = conv2d(x, rbConv1Weight[i], zeroBias64);
            x = batchNorm(x, rbBn2Gamma[i], rbBn2Beta[i], rbBn2Mean[i], rbBn2Var[i]);
            x = relu(x);
            x = conv2d(x, rbConv2Weight[i], zeroBias64);
            x = add3d(x, residual);
        }

        // Head: BN(64) -> ReLU -> Conv(64->128) -> BN(128) -> ReLU
        x = batchNorm(x, headBnGamma, headBnBeta, headBnMean, headBnVar);
        x = relu(x);
        x = conv2d(x, headConvWeight, zeroBias128);
        x = batchNorm(x, headBnPostGamma, headBnPostBeta, headBnPostMean, headBnPostVar);
        x = relu(x);

        // FC: flatten -> 1024 -> 4096
        float[] flat = flatten(x);
        float[] h = linear(flat, fc0Weight, fc0Bias);
        h = relu1d(h);
        return linear(h, fc1Weight, fc1Bias);
    }

    // ---- operations ----

    private static float[][][] conv2d(float[][][] input, float[][][][] weight, float[] bias) {
        int och = weight.length;
        int ich = input.length;
        int h = input[0].length, w = input[0][0].length;
        int k = weight[0][0].length;
        int p = k / 2;
        float[][][] out = new float[och][h][w];
        for (int o = 0; o < och; o++) {
            for (int i = 0; i < ich; i++) {
                for (int r = 0; r < h; r++) {
                    for (int c = 0; c < w; c++) {
                        float sum = 0;
                        for (int kr = 0; kr < k; kr++) {
                            int sr = r + kr - p;
                            if (sr < 0 || sr >= h) continue;
                            for (int kc = 0; kc < k; kc++) {
                                int sc = c + kc - p;
                                if (sc < 0 || sc >= w) continue;
                                sum += input[i][sr][sc] * weight[o][i][kr][kc];
                            }
                        }
                        out[o][r][c] += sum;
                    }
                }
            }
            for (int r = 0; r < h; r++)
                for (int c = 0; c < w; c++)
                    out[o][r][c] += bias[o];
        }
        return out;
    }

    private static float[][][] batchNorm(float[][][] input, float[] gamma, float[] beta,
                                          float[] mean, float[] var) {
        int ch = input.length;
        int h = input[0].length, w = input[0][0].length;
        float[][][] out = new float[ch][h][w];
        for (int c = 0; c < ch; c++) {
            float scale = gamma[c] / (float) Math.sqrt(var[c] + 1e-5f);
            float shift = beta[c] - scale * mean[c];
            for (int r = 0; r < h; r++)
                for (int cc = 0; cc < w; cc++)
                    out[c][r][cc] = input[c][r][cc] * scale + shift;
        }
        return out;
    }

    private static float[][][] relu(float[][][] input) {
        int ch = input.length, h = input[0].length, w = input[0][0].length;
        float[][][] out = new float[ch][h][w];
        for (int c = 0; c < ch; c++)
            for (int r = 0; r < h; r++)
                for (int cc = 0; cc < w; cc++)
                    out[c][r][cc] = Math.max(0, input[c][r][cc]);
        return out;
    }

    private static float[] relu1d(float[] input) {
        float[] out = new float[input.length];
        for (int i = 0; i < input.length; i++)
            out[i] = Math.max(0, input[i]);
        return out;
    }

    private static float[][][] add3d(float[][][] a, float[][][] b) {
        int ch = a.length, h = a[0].length, w = a[0][0].length;
        float[][][] out = new float[ch][h][w];
        for (int c = 0; c < ch; c++)
            for (int r = 0; r < h; r++)
                for (int cc = 0; cc < w; cc++)
                    out[c][r][cc] = a[c][r][cc] + b[c][r][cc];
        return out;
    }

    private static float[] flatten(float[][][] input) {
        int ch = input.length, h = input[0].length, w = input[0][0].length;
        float[] out = new float[ch * h * w];
        int idx = 0;
        for (int c = 0; c < ch; c++)
            for (int r = 0; r < h; r++)
                for (int cc = 0; cc < w; cc++)
                    out[idx++] = input[c][r][cc];
        return out;
    }

    private static float[] linear(float[] input, float[][] weight, float[] bias) {
        int outDim = bias.length;
        float[] out = new float[outDim];
        for (int o = 0; o < outDim; o++) {
            float sum = 0;
            for (int i = 0; i < input.length; i++)
                sum += input[i] * weight[o][i];
            out[o] = sum + bias[o];
        }
        return out;
    }

    // ---- binary loader ----

    private static Map<String, Object> loadBinary(String path) throws IOException {
        byte[] raw;
        try (InputStream is = NeuralNet.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                raw = readAllBytes(is);
            } else {
                try (FileInputStream fis = new FileInputStream(path)) {
                    raw = readAllBytes(fis);
                }
            }
        }
        ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);

        byte[] magic = new byte[8];
        bb.get(magic);
        if (!new String(magic, StandardCharsets.US_ASCII).equals("CHESSNET"))
            throw new IOException("Bad binary format: missing magic header");

        int version = bb.getInt();
        if (version != 1) throw new IOException("Unsupported binary version: " + version);

        int numTensors = bb.getInt();
        Map<String, Object> data = new LinkedHashMap<>();

        for (int t = 0; t < numTensors; t++) {
            int nameLen = bb.getInt();
            byte[] nameBytes = new byte[nameLen];
            bb.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            int rank = bb.getInt();
            int[] shape = new int[rank];
            for (int d = 0; d < rank; d++) shape[d] = bb.getInt();

            int dataLen = bb.getInt();
            float[] values = new float[dataLen];
            for (int i = 0; i < dataLen; i++) values[i] = bb.getFloat();

            if (name.endsWith(".num_batches_tracked")) continue;
            if (rank == 0) continue;

            data.put(name, shapeAndValuesToNested(shape, values));
        }

        return data;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static Object shapeAndValuesToNested(int[] shape, float[] values) {
        if (shape.length == 1) {
            List<Object> list = new ArrayList<>(shape[0]);
            for (int i = 0; i < shape[0]; i++)
                list.add(values[i]);
            return list;
        }
        int block = 1;
        for (int d = 1; d < shape.length; d++) block *= shape[d];
        List<Object> outer = new ArrayList<>(shape[0]);
        int[] innerShape = Arrays.copyOfRange(shape, 1, shape.length);
        for (int i = 0; i < shape[0]; i++) {
            float[] slice = Arrays.copyOfRange(values, i * block, (i + 1) * block);
            outer.add(shapeAndValuesToNested(innerShape, slice));
        }
        return outer;
    }

    // ---- JSON parser ----

    private static Map<String, Object> parseJsonFile(String path) throws IOException {
        String content = readFile(path);
        return (Map<String, Object>) parseValue(content, 0).value;
    }

    private static String readFile(String path) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (InputStream is = NeuralNet.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                byte[] tmp = new byte[8192];
                int n;
                while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
            } else {
                try (FileInputStream fis = new FileInputStream(path)) {
                    byte[] tmp = new byte[8192];
                    int n;
                    while ((n = fis.read(tmp)) != -1) buf.write(tmp, 0, n);
                }
            }
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    private static class ParseResult {
        Object value;
        int pos;
        ParseResult(Object v, int p) { value = v; pos = p; }
    }

    private static ParseResult parseValue(String s, int pos) {
        pos = skipWs(s, pos);
        char c = s.charAt(pos);
        if (c == '{') return parseObject(s, pos);
        if (c == '[') return parseArray(s, pos);
        if (c == '"') return parseString(s, pos);
        return parseNumber(s, pos);
    }

    private static ParseResult parseObject(String s, int pos) {
        pos = skipWs(s, pos + 1);
        Map<String, Object> obj = new LinkedHashMap<>();
        while (s.charAt(pos) != '}') {
            pos = skipWs(s, pos);
            ParseResult key = parseString(s, pos);
            pos = skipWs(s, key.pos);
            if (s.charAt(pos) == ':') pos++;
            pos = skipWs(s, pos);
            ParseResult val = parseValue(s, pos);
            obj.put((String) key.value, val.value);
            pos = skipWs(s, val.pos);
            if (s.charAt(pos) == ',') pos++;
            else if (s.charAt(pos) == '}') break;
        }
        return new ParseResult(obj, pos + 1);
    }

    private static ParseResult parseArray(String s, int pos) {
        pos = skipWs(s, pos + 1);
        List<Object> arr = new ArrayList<>();
        while (s.charAt(pos) != ']') {
            pos = skipWs(s, pos);
            ParseResult val = parseValue(s, pos);
            arr.add(val.value);
            pos = skipWs(s, val.pos);
            if (s.charAt(pos) == ',') pos++;
            else if (s.charAt(pos) == ']') break;
        }
        return new ParseResult(arr, pos + 1);
    }

    private static ParseResult parseString(String s, int pos) {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (s.charAt(pos) != '"') {
            sb.append(s.charAt(pos));
            pos++;
        }
        return new ParseResult(sb.toString(), pos + 1);
    }

    private static ParseResult parseNumber(String s, int pos) {
        int start = pos;
        if (s.charAt(pos) == '-') pos++;
        while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.' || s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
            if ((s.charAt(pos) == '+' || s.charAt(pos) == '-') && (s.charAt(pos-1) != 'e' && s.charAt(pos-1) != 'E')) break;
            if (s.charAt(pos) == '+' || s.charAt(pos) == '-') break;
            pos++;
        }
        if (pos + 1 < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-') && (s.charAt(pos-1) == 'e' || s.charAt(pos-1) == 'E')) {
            pos++;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
        }
        return new ParseResult(Float.parseFloat(s.substring(start, pos)), pos);
    }

    private static int skipWs(String s, int pos) {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        return pos;
    }

    // ---- tensor conversion ----

    @SuppressWarnings("unchecked")
    private static float[][][][] to4d(Object obj, int d0, int d1, int d2, int d3) {
        List<Object> l0 = (List<Object>) obj;
        float[][][][] out = new float[d0][d1][d2][d3];
        for (int i0 = 0; i0 < d0; i0++) {
            List<Object> l1 = (List<Object>) l0.get(i0);
            for (int i1 = 0; i1 < d1; i1++) {
                List<Object> l2 = (List<Object>) l1.get(i1);
                for (int i2 = 0; i2 < d2; i2++) {
                    List<Object> l3 = (List<Object>) l2.get(i2);
                    for (int i3 = 0; i3 < d3; i3++)
                        out[i0][i1][i2][i3] = ((Number) l3.get(i3)).floatValue();
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static float[][] to2d(Object obj, int d0, int d1) {
        List<Object> l0 = (List<Object>) obj;
        float[][] out = new float[d0][d1];
        for (int i0 = 0; i0 < d0; i0++) {
            List<Object> l1 = (List<Object>) l0.get(i0);
            for (int i1 = 0; i1 < d1; i1++)
                out[i0][i1] = ((Number) l1.get(i1)).floatValue();
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static float[] to1d(Object obj, int d0) {
        List<Object> l0 = (List<Object>) obj;
        float[] out = new float[d0];
        for (int i0 = 0; i0 < d0; i0++)
            out[i0] = ((Number) l0.get(i0)).floatValue();
        return out;
    }

    // ---- singleton ----

    private static NeuralNet instance;

    public static NeuralNet getInstance() {
        return instance;
    }

    public static void setInstance(NeuralNet net) {
        instance = net;
    }

    public String bestMove(BoardState board, int plyLimit) {
        return null; // placeholder
    }
}
