/** Вражеский бот с таймером AI и стандартными параметрами врага. */
public class Enemy extends Tank {
    int aiTimer = 0;

    Enemy(int x, int y) {
        super(x, y, Direction.DOWN, GameConfig.ENEMY_TANK_SIZE, 1);
    }
}
