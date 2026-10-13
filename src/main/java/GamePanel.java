/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author roire
 */
public class GamePanel extends JPanel implements KeyListener {

    private Path2D.Double raceTrack;
    private RaceTrack track;
    private Car car;
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private Timer timer;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        track = new RaceTrack();
        car = new Car(200, 200, Color.RED);

        timer = new Timer(16, e -> {updateGame(); repaint();});
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(70, 110, 70));
        g.fillRect(0, 0, getWidth(), getHeight());

        if (raceTrack == null) {
            raceTrack = track.drawSmoothHull(track.generateRandomPoints(getWidth(), getHeight()));
        }
        Graphics2D g2 = (Graphics2D) g;
        if (raceTrack != null) {

            g2.setStroke(new BasicStroke(60));
            g2.setColor(Color.GRAY);
            g2.draw(this.raceTrack);
        }

        car.draw(g2);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) {
            upPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_S) {
            downPressed = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_A) {
            leftPressed = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_D) {
            rightPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) {
            upPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_S) {
            downPressed = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_A) {
            leftPressed = false;
        }

        if (e.getKeyCode() == KeyEvent.VK_D) {
            rightPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
    
    private void updateGame() {

    if (upPressed) {
        car.accelerate();
    }

    if (downPressed) {
        car.brake();
    }

    if (leftPressed) {
        car.turnLeft();
    }

    if (rightPressed) {
        car.turnRight();
    }
    
    if (!upPressed && !downPressed){
        car.slowDown();
    }

    car.move();
}

}
