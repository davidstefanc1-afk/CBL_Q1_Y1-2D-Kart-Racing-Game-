import java.awt.*;
import java.awt.geom.Rectangle2D;
public class BotCar {
    double x,y, angle, speed;
    int width, height;
    Color color;
    
    public BotCar(double startX, double startY, Color color) {
        this.x = startX;
        this.y = startY;
        this.speed = 0;
        this.angle = 0;
        this.height = 15;
        this.width = 30;
        this.color = color;
    }
    
    public void draw(Graphics2D g2) {
        Graphics2D copy = (Graphics2D) g2.create();
        
        copy.rotate(angle, x, y);
        
        copy.setColor(color);
        
        copy.fillRect((int) x - 15, (int) y - 8, 30, 16);
        
        copy.dispose();        
    }
    ///Returns the bot's current hitbox as a rectangle
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }
    ///Shifts bot position 
    public void knockback(double dx,double dy){
    x+=dx;
    y+=dy;}
}
