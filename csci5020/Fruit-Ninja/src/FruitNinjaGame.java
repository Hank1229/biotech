import javax.swing.JFrame;

public class FruitNinjaGame {
    // Static high score tracker (persists between games)
    public static int highScore = 0;

    public static void main(String[] args) {
        // Create game window (JFrame)
        JFrame frame = new JFrame("Fruit Ninja Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and add the game panel (where gameplay happens)
        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);
        frame.pack();             // size frame to fit panel
        frame.setLocationRelativeTo(null); // center on screen
        frame.setVisible(true);

        // The GamePanel will handle game loop and interactions
    }
}
