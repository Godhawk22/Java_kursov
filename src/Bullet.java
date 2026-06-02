/** Сущность пули с позицией, скоростью и владельцем. */
public class Bullet {
    int x, y;
    final int dx, dy;
    final Tank owner;

    Bullet(int x, int y, int dx, int dy, Tank owner) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.owner = owner;
    }
}
