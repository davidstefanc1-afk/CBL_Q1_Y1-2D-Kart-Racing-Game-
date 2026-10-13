/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import javax.swing.*; 
/**
 *
 * @author roire
 */
public class GameWindow extends JFrame{
    
    public GameWindow(){
        this.setTitle("Racing Game");
        this.setSize(900, 700);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        GamePanel panel = new GamePanel();
        add(panel);
        
        this.setVisible(true);
    }
    

    
    

}
