
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GamePanel extends JPanel implements KeyListener {

    private Path2D.Double raceTrack;
    private RaceTrack track;
    private Car car;
    private BotCar bot;
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private Timer timer;
    private CollisionDetection collisionDetection;
    private int knockbackFrames = 0;
    private int knockbackDuration = 30;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        track = new RaceTrack();
        car = new Car(200, 200, Color.RED);
        bot = new BotCar(200, 180, Color.BLUE);
        timer = new Timer(16, e -> {
            updateGame();
            repaint();
        });
        timer.start();
        collisionDetection = new CollisionDetection(car, bot);
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
        bot.draw(g2);
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

    private void applyKnockback() {
        double dx = car.getBounds().getCenterX() - bot.getBounds().getCenterX();
        double dy = car.getBounds().getCenterY() - bot.getBounds().getCenterY();
        ///Gets the vector that points from the bot toward the car(the direction the car should move away)
        double length = Math.sqrt(dx * dx + dy * dy);
        try {
            dx /= length;
            dy /= length;
        } catch (ArithmeticException e) {
            dx = 1;
            dy = 1;
            length = 1;
        }
        double knockbackSpeed = 0.5;
        ///How many pixels each car moves per frame
        car.knockback(dx * knockbackSpeed, dy * knockbackSpeed);
        bot.knockback(-dx * knockbackSpeed, -dy * knockbackSpeed);

    }

    private void updateGame() {
        if (knockbackFrames > 0) {
            applyKnockback();
            knockbackFrames--;
            return;
        }
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
        car.move();
        if (collisionDetection.checkCollision()) {
            knockbackFrames = knockbackDuration;
        }
        
    }

}
