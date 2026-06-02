import java.awt.Rectangle;

/** Базовая изменяемая сущность танка. */
public class Tank {
    int x, y;
    Direction direction;
    int reload = 0;
    int lives;
    final int size;

    Tank(int x, int y, Direction direction, int size, int lives) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.size = size;
        this.lives = lives;
    }

    Rectangle bounds() {
        return new Rectangle(x, y, size, size);
    }

    boolean isAlive() {
        return lives > 0;
    }
}
