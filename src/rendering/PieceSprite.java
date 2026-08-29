package rendering;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class PieceSprite {
    private static final int WHITE = 0, BLACK = 1;
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    private static BufferedImage load(String path) {
        if (cache.containsKey(path)) return cache.get(path);
        BufferedImage img = null;
        try {
            InputStream is = PieceSprite.class.getResourceAsStream(path + ".png");
            if (is != null) {
                img = ImageIO.read(is);
            }
        } catch (Exception e) {
            // fall through to filesystem fallback
        }
        if (img == null) {
            try {
                File f = new File("src" + path + ".png");
                if (f.exists()) {
                    img = ImageIO.read(f);
                }
            } catch (Exception e) {
                // fall through
            }
        }
        cache.put(path, img);
        if (img == null) {
            System.err.println("Resource not found: " + path + ".png");
        }
        return img;
    }

    public static BufferedImage get(int color, String pieceName) {
        String side = (color == WHITE) ? "w" : "b";
        return load("/res/image/" + side + "-" + pieceName);
    }
}
