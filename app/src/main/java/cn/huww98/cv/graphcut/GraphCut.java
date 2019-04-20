package cn.huww98.cv.graphcut;

class ColorUtil {
    final static int byteMask = 0xFF;

    static byte toGray(byte r, byte g, byte b) {
        return (byte) (0.299 * (r & byteMask) + 0.587 * (g & byteMask) + 0.114 * (b & byteMask));
    }
}

class Histogram {
    private boolean addFinished;
    private double[] data = new double[256];

    void addGray(byte gray) {
        if (addFinished) {
            throw new IllegalStateException("finishAdd called");
        }

        data[gray & ColorUtil.byteMask]++;
    }

    void finishAdd() {
        this.addFinished = true;
        double sum = 0;
        for (double d : data) {
            sum += d;
        }
        double noise = sum * 0.0001;
        sum += noise * data.length;
        for (int i = 0; i < data.length; i++) {
            data[i] += noise;
            data[i] /= sum;
        }
    }

    double getProbability(byte gray) {
        return data[gray & ColorUtil.byteMask];
    }
}

public class GraphCut {

    final static int channel = 4;
    final static int rOffset = 1;
    final static int gOffset = 2;
    final static int bOffset = 3;

    final static double gamma = 50;
    final static double K = 4 * gamma + 1;

    final static int byteMask = 0xFF;

    private static double calcSquaredDiff(byte[] img, int i, int j) {
        int dr = img[channel * i + rOffset] & byteMask - img[channel * j + rOffset] & byteMask;
        int dg = img[channel * i + gOffset] & byteMask - img[channel * j + gOffset] & byteMask;
        int db = img[channel * i + bOffset] & byteMask - img[channel * j + bOffset] & byteMask;
        return (dr * dr + dg * dg + db * db);
    }

    private static double calcB(byte[] img, int i, int j, double beta) {
        return gamma * Math.exp(-beta * calcSquaredDiff(img, i, j));
    }

    private static double calcBeta(byte[] img, int width) {
        int height = img.length / width / channel;
        double diffSum = 0.0;
        int diffCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (x < width - 1) {
                    int j = i + 1;
                    diffSum += calcSquaredDiff(img, i, j);
                    diffCount++;
                }
                if (y < height - 1) {
                    int j = i + width;
                    diffSum += calcSquaredDiff(img, i, j);
                    diffCount++;
                }
            }
        }
        return 1.0 / (2 * diffSum / diffCount);
    }

    private static byte toGray(byte[] img, int i) {
        return ColorUtil.toGray(img[channel * i + rOffset], img[channel * i + gOffset], img[channel * i + bOffset]);
    }

    public static boolean[] graphCut(byte[] img, GraphCutClasses[] mask, int width) {
        if (img.length != mask.length * channel) {
            throw new IllegalArgumentException("img and mask should be at same length");
        }
        if (img.length % (width * channel) != 0) {
            throw new IllegalArgumentException();
        }

        Histogram foregroundHist = new Histogram();
        Histogram backgroundHist = new Histogram();
        for (int i = 0; i < mask.length; i++) {
            byte gray = toGray(img, i);
            switch (mask[i]) {
                case FOREGROUND:
                    foregroundHist.addGray(gray);
                    break;
                case BACKGROUND:
                    backgroundHist.addGray(gray);
                    break;
            }
        }
        foregroundHist.finishAdd();
        backgroundHist.finishAdd();

        int height = mask.length / width;
        GCGraph graph = new GCGraph(width, height);
        double beta = calcBeta(img, width);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                byte gray = toGray(img, i);
                switch (mask[i]) {
                    case UNKNOWN:
                        graph.addTermWeights(i,
                                -Math.log(backgroundHist.getProbability(gray)),
                                -Math.log(foregroundHist.getProbability(gray)));
                        break;
                    case FOREGROUND:
                        graph.addTermWeights(i, K, 0);
                        break;
                    case BACKGROUND:
                        graph.addTermWeights(i, 0, K);
                        break;
                }

                double[] weights = new double[2];
                if (x < width - 1) {
                    int j = i + 1;
                    weights[0] = calcB(img, i, j, beta);
                }
                if (y < height - 1) {
                    int j = i + width;
                    weights[1] = calcB(img, i, j, beta);
                }
                graph.setEdgeWeights(i, weights);
            }
        }

        VertexStatus[] status = graph.maxFlow();
        boolean[] result = new boolean[mask.length];
        for (int i = 0; i < mask.length; i++) {
            result[i] = status[i] == VertexStatus.Source;
        }
        return result;
    }
}
