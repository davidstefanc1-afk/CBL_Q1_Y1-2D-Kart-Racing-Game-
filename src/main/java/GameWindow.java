import javax.swing.*;

/**
 * Top-level window that hosts the game panel.
 *
 * @author roire
 */
public class GameWindow extends JFrame {

    /**
     * Creates and displays the game window.
     */
    public GameWindow() {
        this.setTitle("Racing Game");
        this.setSize(900, 700);

        // Terminate the application when the window is closed.
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // The game panel fills the window's content area.
        GamePanel panel = new GamePanel();
        add(panel);

        this.setVisible(true);
    }

}
