import java.awt.Color;
import java.awt.Graphics;

public class Bomb extends GameObject {
    public Bomb(int x, int y, double vx, double vy) {
        super(x, y, vx, vy, 20);
    }
    @Override
    public void draw(Graphics g) {
        // Draw bomb as a black circle with a white "B" on it
        g.setColor(Color.BLACK);
        g.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
        g.setColor(Color.WHITE);
        g.drawString("B", (int)x - 4, (int)y + 4);
    }
}
