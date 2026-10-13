/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author roire
 */
public class Car {

    double x, y, angle, speed;
    int width, height;
    Color color;

    public Car(double startX, double startY, Color color) {
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

    public void move() {
        x += Math.cos(angle) * speed;
        y += Math.sin(angle) * speed;
    }

    public void accelerate() {
        if (speed < 5) {
            speed += 0.01;
        }
    }

    public void brake() {
        if (speed > -1) {
            speed -= 0.01;
        }
    }

    public void turnRight() {
        angle += 0.05;
    }

    public void turnLeft() {
        angle -= 0.05;
    }
    ///Returns the car's current hitbox as a rectangle
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }
    ///Shifts car position 
    public void knockback(double dx,double dy){
    x+=dx;
    y+=dy;}
}
