import java.awt.Color;
import java.awt.Graphics;

public class BonusItem extends GameObject {
    public enum BonusType {
        EXTRA_LIFE, SLOW_MOTION
    }
    private BonusType bonusType;
    private int pointValue;
    
    public BonusItem(int x, int y, double vx, double vy) {
        super(x, y, vx, vy, 20);
        // Randomly choose type of bonus
        bonusType = Math.random() < 0.5 ? BonusType.EXTRA_LIFE : BonusType.SLOW_MOTION;
        // Assign appearance and points based on type
        if (bonusType == BonusType.EXTRA_LIFE) {
            pointValue = 0; // no points, just a life reward
        } else if (bonusType == BonusType.SLOW_MOTION) {
            pointValue = 5; // slicing slow-motion bonus gives some points
        }
    }
    
    public BonusType getBonusType() {
        return bonusType;
    }
    public int getPointValue() {
        return pointValue;
    }
    
    @Override
    public void draw(Graphics g) {
        if (bonusType == BonusType.EXTRA_LIFE) {
            // Draw life bonus as a pink circle with "+1"
            g.setColor(Color.PINK);
            g.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
            g.setColor(Color.WHITE);
            g.drawString("+1", (int)x - 6, (int)y + 4);
        } else if (bonusType == BonusType.SLOW_MOTION) {
            // Draw slow-motion bonus as a cyan circle with "S"
            g.setColor(Color.CYAN);
            g.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
            g.setColor(Color.BLUE.darker());
            g.drawString("S", (int)x - 4, (int)y + 5);
        }
    }
}
