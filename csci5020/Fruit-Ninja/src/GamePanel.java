import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    // Constants for panel size and game physics
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final double GRAVITY = 0.5; // gravity acceleration (pixels per frame^2)
    private static final int INITIAL_SPAWN_INTERVAL = 100; // frames between spawns at start (approx 2 seconds at 20ms frame)
    
    private Timer timer;                // Swing timer for game loop (UIR2)
    private List<GameObject> objects;   // Active game objects (fruits, bombs, bonuses) on screen
    private Random rand;                // Random generator for spawning objects
    
    private boolean gameOver;           // Flag indicating if game is over (FR8)
    private int score;
    private int lives;
    
    // Swipe detection and combo tracking
    private boolean swipeActive;            // Is the player currently swiping (mouse pressed and held)
    private List<Point> swipePoints;        // Points of the current swipe trail (UIR1)
    private int currentSwipeFruitCount;     // Number of fruits sliced in current continuous swipe (for combos) (FR4)
    
    // Combo message display
    private String comboMessage;            // Message to display for combos or bonuses (UIR5)
    private int comboMessageTimer;          // Frames remaining to display the combo message
    
    // Bonus effects
    private boolean slowMotionActive;       // If true, slow-motion effect is active (from bonus or combo)
    private int slowMotionTimer;            // Frames remaining for slow motion effect
    
    // Buttons for game over options (UIR6)
    private JButton restartButton;
    private JButton exitButton;
    
    // Difficulty control
    private int spawnInterval;              // Current frames between spawns (decreases over time for difficulty, FR7)
    private int spawnCounter;               // Counter for frames since last spawn
    private int nextDifficultyScoreThreshold; // Score at which to next increase difficulty
    
    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);  // Background color for game canvas
        setLayout(null); // Use manual layout for overlay components (like buttons)
        
        // Initialize game state
        objects = new ArrayList<>();
        rand = new Random();
        gameOver = false;
        score = 0;
        lives = 3;
        swipeActive = false;
        swipePoints = new ArrayList<>();
        currentSwipeFruitCount = 0;
        comboMessage = "";
        comboMessageTimer = 0;
        slowMotionActive = false;
        slowMotionTimer = 0;
        
        spawnInterval = INITIAL_SPAWN_INTERVAL;
        spawnCounter = 0;
        nextDifficultyScoreThreshold = 50; // Increase difficulty at score 50, then 100, etc.
        
        // Setup mouse listeners for swipe detection (FR2)
        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        
        // Setup game over buttons (UIR6)
        restartButton = new JButton("Restart");
        exitButton = new JButton("Exit");
        // Position buttons at center of screen (they will be shown on game over)
        restartButton.setBounds(WIDTH/2 - 60, HEIGHT/2 - 10, 120, 30);
        exitButton.setBounds(WIDTH/2 - 60, HEIGHT/2 + 30, 120, 30);
        restartButton.setVisible(false);
        exitButton.setVisible(false);
        // Add buttons to panel
        add(restartButton);
        add(exitButton);
        // Action listeners for buttons
        restartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restartGame();
            }
        });
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Exit the game
                System.exit(0);
            }
        });
        
        // Start the game loop timer (UIR2 - smooth animations)
        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }
    
    // Game loop tick - called by timer every frame
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            // Spawn new objects at intervals (FR1)
            spawnCounter++;
            if (spawnCounter >= spawnInterval) {
                spawnObject(); // Launch a new fruit/bonus/bomb from bottom
                spawnCounter = 0;
            }
            
            // Update positions of all objects (UIR2 - smooth movement)
            double speedFactor = slowMotionActive ? 0.5 : 1.0; // If slow motion bonus active, move at half speed
            for (GameObject obj : objects) {
                obj.update(speedFactor);
            }
            
            // Apply gravity uniformly to objects (part of update)
            // Check for objects that went out of bounds
            Iterator<GameObject> it = objects.iterator();
            while (it.hasNext()) {
                GameObject obj = it.next();
                if (obj.isOffScreen(WIDTH, HEIGHT)) {
                    // If a fruit was missed (fell off bottom without being sliced), lose a life (like missing fruit in Fruit Ninja)
                    if (!obj.isSliced()) {
                        if (obj instanceof Fruit) {
                            loseLife(); // Player missed a fruit
                        }
                        // No penalty for missing bombs or bonuses; they simply disappear if not sliced
                    }
                    it.remove();
                }
            }
            
            // Decrease slow motion timer if active (FR4/FR5 slow-motion effect)
            if (slowMotionActive) {
                slowMotionTimer--;
                if (slowMotionTimer <= 0) {
                    slowMotionActive = false;
                }
            }
            
            // Decrease combo message display timer
            if (comboMessageTimer > 0) {
                comboMessageTimer--;
                if (comboMessageTimer == 0) {
                    comboMessage = "";
                }
            }
            
            // Increase difficulty as score grows (FR7)
            if (score >= nextDifficultyScoreThreshold) {
                // Increase difficulty: speed up spawns (reduce interval)
                spawnInterval = Math.max(20, spawnInterval - 10); // Faster spawn, not below 20 frames (~0.3s)
                nextDifficultyScoreThreshold += 50;  // Next threshold (increase every 50 points)
            }
        }
        
        // Redraw the game scene
        repaint();
    }
    
    // Launch a new object from the bottom (could be fruit, bomb, or bonus) (FR1)
    private void spawnObject() {
        int xPos = rand.nextInt(WIDTH - 100) + 50; // Spawn somewhere near bottom, avoiding extreme edges
        int yPos = HEIGHT + 10; // Just below bottom of screen
        // Random velocities for a nice arc
        double initVy = -(rand.nextDouble() * 5 + 15); // Upward velocity (negative y direction) ~[-15,-20]
        double initVx = rand.nextDouble() * 6 - 3;     // Horizontal velocity between -3 and 3
        // Randomly decide object type: mostly fruits, some bombs, some bonus
        double r = rand.nextDouble();
        GameObject newObj;
        if (r < 0.70) {
            // 70% chance fruit
            newObj = new Fruit(xPos, yPos, initVx, initVy);
        } else if (r < 0.85) {
            // 15% chance bomb
            newObj = new Bomb(xPos, yPos, initVx, initVy);
        } else {
            // 15% chance bonus item
            newObj = new BonusItem(xPos, yPos, initVx, initVy);
        }
        objects.add(newObj);
    }
    
    // Handle losing one life (common routine for bomb hit or missed fruit)
    private void loseLife() {
        lives--;
        if (lives <= 0) {
            lives = 0;
            endGame(); // Trigger game over if no lives left (FR8)
        }
    }
    
    // End the game and show Game Over screen (FR8, UIR6)
    private void endGame() {
        gameOver = true;
        timer.stop();
        // Update high score
        if (score > FruitNinjaGame.highScore) {
            FruitNinjaGame.highScore = score;
        }
        // Show game over options
        restartButton.setVisible(true);
        exitButton.setVisible(true);
        // Force repaint to draw "Game Over" text and scores
        repaint();
    }
    
    // Restart the game after game over
    private void restartGame() {
        // Reset game state
        score = 0;
        lives = 3;
        objects.clear();
        swipePoints.clear();
        swipeActive = false;
        currentSwipeFruitCount = 0;
        comboMessage = "";
        comboMessageTimer = 0;
        slowMotionActive = false;
        slowMotionTimer = 0;
        gameOver = false;
        // Reset difficulty
        spawnInterval = INITIAL_SPAWN_INTERVAL;
        spawnCounter = 0;
        nextDifficultyScoreThreshold = 50;
        // Hide game over buttons
        restartButton.setVisible(false);
        exitButton.setVisible(false);
        // Restart timer loop
        timer.start();
    }
    
    // Paint the game elements on the screen (called by Swing)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw all active game objects (UIR2)
        for (GameObject obj : objects) {
            obj.draw(g);
        }
        // Draw the swipe trail (UIR1)
        if (!swipePoints.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.WHITE);
            g2.setStroke(new java.awt.BasicStroke(3)); // Thicker line for the blade trail
            for (int i = 0; i < swipePoints.size() - 1; i++) {
                Point p1 = swipePoints.get(i);
                Point p2 = swipePoints.get(i + 1);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            // Reset stroke to default
            g2.setStroke(new java.awt.BasicStroke(1));
        }
        // Draw score and lives (UIR3, UIR4)
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
        // Draw lives as heart icons
        g.drawString("Lives:", 10, 40);
        for (int i = 0; i < lives; i++) {
            // Draw a heart symbol for each life (use Unicode heart)
            g.setColor(Color.RED);
            g.drawString("\u2665", 60 + i * 15, 40);
        }
        // Reset color for later drawings
        g.setColor(Color.WHITE);
        // Draw combo or bonus message if active (UIR5)
        if (comboMessage != null && !comboMessage.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            // Display combo message at center-top of screen
            Font originalFont = g.getFont();
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(comboMessage);
            g.drawString(comboMessage, (WIDTH - textWidth) / 2, 50);
            g.setFont(originalFont);
        }
        // Draw game over screen overlay (UIR6)
        if (gameOver) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 150)); // Semi-transparent dark overlay
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            String gameOverText = "Game Over";
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(gameOverText);
            g2.drawString(gameOverText, (WIDTH - textWidth) / 2, HEIGHT/2 - 80);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            String finalScoreText = "Final Score: " + score;
            String highScoreText = "High Score: " + FruitNinjaGame.highScore;
            g2.drawString(finalScoreText, WIDTH/2 - 80, HEIGHT/2 - 40);
            g2.drawString(highScoreText, WIDTH/2 - 80, HEIGHT/2 - 20);
            // Buttons (Restart and Exit) are already added and visible on top
        }
    }
    
    // MouseListener and MouseMotionListener implementations for swipe detection (FR2)
    @Override
    public void mousePressed(MouseEvent e) {
        swipeActive = true;
        currentSwipeFruitCount = 0;
        swipePoints.clear();
        // Record starting point of swipe
        swipePoints.add(new Point(e.getX(), e.getY()));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (swipeActive) {
            // Swipe ended, check for combo bonuses (FR4)
            if (currentSwipeFruitCount >= 3) {
                // Player sliced 3 or more fruits in one swipe -> combo
                int bonusPoints = currentSwipeFruitCount; // e.g., +N points for an N-fruit combo
                score += bonusPoints;
                String message = currentSwipeFruitCount + " Fruits Combo! +" + bonusPoints + " points";
                // Extra reward for large combos
                if (currentSwipeFruitCount >= 5) {
                    // Reward an extra life for combos of 5 or more (special reward as per FR4)
                    if (lives < 5) { // Cap max lives to 5
                        lives++;
                    }
                    message += " +1 Life!";
                }
                comboMessage = message;
                comboMessageTimer = 60; // Display message for ~60 frames (1 second)
            }
        }
        swipeActive = false;
        swipePoints.clear();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!swipeActive) return;
        // Add point to swipe trail
        swipePoints.add(new Point(e.getX(), e.getY()));
        // Check line segment from last point to new point for intersections with objects (FR2, FR3, FR5, FR6)
        if (swipePoints.size() >= 2) {
            int n = swipePoints.size();
            Point p1 = swipePoints.get(n - 2);
            Point p2 = swipePoints.get(n - 1);
            // Check each game object for collision with swipe line
            Iterator<GameObject> it = objects.iterator();
            while (it.hasNext()) {
                GameObject obj = it.next();
                if (!obj.isSliced() && obj.intersectsLine(p1.x, p1.y, p2.x, p2.y)) {
                    // Object is sliced by the swipe
                    obj.setSliced(true);
                    if (obj instanceof Fruit) {
                        Fruit fruit = (Fruit) obj;
                        score += fruit.getPointValue(); // Increase score based on fruit type (FR3)
                        currentSwipeFruitCount++;
                    } else if (obj instanceof Bomb) {
                        // Bomb sliced - lose a life (FR6)
                        loseLife();
                    } else if (obj instanceof BonusItem) {
                        BonusItem bonus = (BonusItem) obj;
                        score += bonus.getPointValue(); // Optional points for bonus
                        // Activate bonus effect (FR5)
                        if (bonus.getBonusType() == BonusItem.BonusType.EXTRA_LIFE) {
                            if (lives < 5) {
                                lives++;
                            }
                            comboMessage = "+1 Life!"; // Display life gain message (UIR5)
                            comboMessageTimer = 60;
                        } else if (bonus.getBonusType() == BonusItem.BonusType.SLOW_MOTION) {
                            slowMotionActive = true;
                            slowMotionTimer = 150; // Slow motion for 150 frames (~3 seconds)
                            comboMessage = "Slow Motion Activated!"; // Display slow-mo message
                            comboMessageTimer = 60;
                        }
                    }
                    // Remove the object from play
                    it.remove();
                }
            }
        }
        // Repaint to update screen (trail drawing and possibly object removal visual)
        repaint();
    }
    
    // Unused interface methods
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}
