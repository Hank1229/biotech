import java.awt.Graphics;

public abstract class GameObject {
    protected double x, y;   // current position
    protected double vx, vy; // velocity components
    protected int radius;    // radius for collision and drawing
    protected boolean sliced; // whether the object has been sliced by the player

    public GameObject(int x, int y, double vx, double vy, int radius) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.sliced = false;
    }

    // Update position based on velocity and gravity
    public void update(double speedFactor) {
        // speedFactor slows down motion if slow-motion effect is active
        x += vx * speedFactor;
        y += vy * speedFactor;
        // Apply gravity (pull downward)
        vy += GamePanel.GRAVITY * speedFactor;
    }

    // Check if object has moved off screen
    public boolean isOffScreen(int width, int height) {
        // Off screen if completely below bottom or far off sides
        return (y - radius > height) || (x + radius < 0) || (x - radius > width);
    }

    // Mark object as sliced
    public void setSliced(boolean sliced) {
        this.sliced = sliced;
    }
    public boolean isSliced() {
        return sliced;
    }

    // Check if a line segment (p1->p2) intersects this object's circle (collision detection for slicing)
    public boolean intersectsLine(int x1, int y1, int x2, int y2) {
        // Compute distance from this object's center to the line segment
        double cx = this.x;
        double cy = this.y;
        // Vector from p1 to p2
        double vx_line = x2 - x1;
        double vy_line = y2 - y1;
        // Vector from p1 to center
        double vx_center = cx - x1;
        double vy_center = cy - y1;
        // Project center vector onto line vector to find closest point
        double lineLenSq = vx_line * vx_line + vy_line * vy_line;
        double t = 0;
        if (lineLenSq > 0) {
            t = (vx_center * vx_line + vy_center * vy_line) / lineLenSq;
        }
        if (t < 0) {
            // Closest to p1
            cx = x1;
            cy = y1;
        } else if (t > 1) {
            // Closest to p2
            cx = x2;
            cy = y2;
        } else {
            // Projection falls on the segment
            cx = x1 + t * vx_line;
            cy = y1 + t * vy_line;
        }
        // Distance from this closest point to object center
        double dist = Math.hypot(this.x - cx, this.y - cy);
        return dist <= this.radius;
    }

    // Draw the object (to be implemented by subclasses for specific appearance)
    public abstract void draw(Graphics g);
}
