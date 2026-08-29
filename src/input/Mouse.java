package input;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter{
    public volatile int x, y;
    public volatile boolean pressed;
    private volatile boolean enabled = true;
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void mousePressed(MouseEvent e){
        pressed = true;
    }
    
    @Override
    public void mouseReleased(MouseEvent e){
        pressed = false;
    }
    
    @Override
    public void mouseDragged(MouseEvent e){
        if (enabled) {
            x = e.getX();
            y = e.getY();
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e){
        x = e.getX();
        y = e.getY(); 
    }
}
