import java.awt.Color;
import java.awt.Graphics;

public class Fruit extends GameObject {
    // Different fruit types with distinct point values and colors (FR3)
    public enum FruitType {
        APPLE(5, Color.RED),
        ORANGE(10, Color.ORANGE),
        BANANA(15, Color.YELLOW);

        private int points;
        private Color color;
        FruitType(int points, Color color) {
            this.points = points;
            this.color = color;
        }
        public int getPoints() { return points; }
        public Color getColor() { return color; }
    }

    private FruitType type;
    
    public Fruit(int x, int y, double vx, double vy) {
        super(x, y, vx, vy, 20);
        // Randomly assign a type of fruit for point value and color
        FruitType[] types = FruitType.values();
        this.type = types[(int)(Math.random() * types.length)];
    }

    public int getPointValue() {
        // Score points for this fruit (based on type, FR3)
        return type.getPoints();
    }

    @Override
    public void draw(Graphics g) {
        // Draw fruit as a colored circle
        g.setColor(type.getColor());
        g.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
        // Optionally, draw an outline
        g.setColor(Color.BLACK);
        g.drawOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
    }
}
