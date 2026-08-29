package rendering;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class Board {
    final int MAX_COL = 8;
    final int MAX_ROW = 8;
    public static final int SQUARE_SIZE = 100;
    public static final int HALF_SQUARE_SIZE = SQUARE_SIZE/2;
    public static boolean blackPerspective = false;
    
    public void draw(Graphics2D g2){
        
        int c = 0;
        
        for(int row = 0; row < MAX_ROW; row++){
            for(int col = 0; col < MAX_COL; col ++){
                if(c == 0){
                    g2.setColor(new Color(238, 238, 205));
                    c = 1;
                }
                else{
                    g2.setColor(new Color(118, 150, 86));
                    c = 0;
                }
                g2.fillRect(col*SQUARE_SIZE, row*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
            if(c == 0){
                c = 1;
            }
            else{
                c = 0;
            }
        }
    }

    public void drawCoordinates(Graphics2D g2){
        Font font = new Font("SansSerif", Font.BOLD, 16);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        for(int col = 0; col < MAX_COL; col++){
            char file = blackPerspective
                ? (char)('h' - col)
                : (char)('a' + col);
            int x = col * SQUARE_SIZE + 4;
            int y = (MAX_ROW - 1) * SQUARE_SIZE + SQUARE_SIZE - 6;
            g2.setColor(getLabelColor(col, MAX_ROW - 1));
            g2.drawString(String.valueOf(file), x, y);
        }

        for(int row = 0; row < MAX_ROW; row++){
            int rank = blackPerspective ? (1 + row) : (8 - row);
            int x = 4;
            int y = row * SQUARE_SIZE + fm.getAscent() + 2;
            g2.setColor(getLabelColor(0, row));
            g2.drawString(String.valueOf(rank), x, y);
        }
    }

    private Color getLabelColor(int col, int row){
        boolean lightSquare = (row + col) % 2 == 0;
        return lightSquare ? new Color(50, 80, 30) : new Color(200, 200, 160);
    }
}
