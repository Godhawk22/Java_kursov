/** Танк игрока с номером, очками и собственными клавишами управления. */
public class Player extends Tank {
    final int id;
    int score = 0;

    Player(int id, int tileX, int tileY) {
        super(GameMap.tileToCenteredPixel(tileX, GameConfig.PLAYER_TANK_SIZE),
                GameMap.tileToCenteredPixel(tileY, GameConfig.PLAYER_TANK_SIZE),
                Direction.UP, GameConfig.PLAYER_TANK_SIZE, GameConfig.PLAYER_START_LIVES);
        this.id = id;
    }

    void resetToSpawn(int tileX, int tileY) {
        x = GameMap.tileToCenteredPixel(tileX, GameConfig.PLAYER_TANK_SIZE);
        y = GameMap.tileToCenteredPixel(tileY, GameConfig.PLAYER_TANK_SIZE);
        direction = Direction.UP;
        reload = 0;
        lives = GameConfig.PLAYER_START_LIVES;
        score = 0;
    }
}
