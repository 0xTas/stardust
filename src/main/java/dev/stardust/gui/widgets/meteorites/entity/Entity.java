package dev.stardust.gui.widgets.meteorites.entity;

public class Entity {
    public double x, y, vx, vy;

    public void wrap(double width, double height) {
        if (this.x < 0) this.x += width;
        else if (this.x > width) this.x -= width;
        if (this.y < 0) this.y += height;
        else if (this.y > height) this.y -= height;
    }
}
