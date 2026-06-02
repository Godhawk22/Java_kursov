import java.awt.Color;

/** Общие настройки карты, игроков, врагов, пуль, стен и текстур. */
public final class GameConfig {
    private GameConfig() {}

    // Размер одной клетки карты в пикселях.
    public static final int TILE_SIZE = 32;
    // Ширина карты в клетках.
    public static final int MAP_W = 20;
    // Высота карты в клетках: поле увеличено в два раза.
    public static final int MAP_H = 30;
    // Ширина окна и игрового поля в пикселях.
    public static final int WIDTH = MAP_W * TILE_SIZE;
    // Высота окна и игрового поля в пикселях.
    public static final int HEIGHT = MAP_H * TILE_SIZE;
    // Задержка между шагами игрового цикла в миллисекундах.
    public static final int TIMER_DELAY_MS = 16;

    // Значение пустой клетки карты.
    public static final int CELL_EMPTY = 0;
    // Значение стальной стены: неразрушаемое препятствие.
    public static final int CELL_STEEL = -1;
    // Полная прочность кирпичной стены: для разрушения нужны три попадания.
    public static final int BRICK_MAX_HEALTH = 3;
    // Размер откалываемого куска кирпича за одно попадание.
    public static final int BRICK_DAMAGE_STEP = TILE_SIZE / BRICK_MAX_HEALTH;
    // Шанс случайного появления кирпича в каждой внутренней клетке карты.
    public static final float BRICK_SPAWN_CHANCE = 0.15f;
    // Шанс случайного появления стального блока в каждой внутренней клетке карты.
    public static final float STEEL_SPAWN_CHANCE = 0.04f;

    // Стартовая клетка первого игрока по оси X.
    public static final int PLAYER_START_TILE_X = 2;
    // Стартовая клетка первого игрока по оси Y.
    public static final int PLAYER_START_TILE_Y = MAP_H - 2;
    // Стартовая клетка второго локального игрока по оси X.
    public static final int PLAYER_TWO_START_TILE_X = MAP_W - 3;
    // Стартовая клетка второго локального игрока по оси Y.
    public static final int PLAYER_TWO_START_TILE_Y = MAP_H - 2;
    // Размер танка игрока: уменьшен на 25% относительно клетки 32x32.
    public static final int PLAYER_TANK_SIZE = TILE_SIZE * 3 / 4;
    // Радиус стального ограждения вокруг спавна игрока в клетках.
    public static final int PLAYER_SPAWN_WALL_RADIUS = 2;
    // Скорость движения игрока в пикселях за шаг игрового цикла.
    public static final int PLAYER_SPEED = 2;
    // Начальное количество жизней игрока.
    public static final int PLAYER_START_LIVES = 3;
    // Длительность перезарядки игрока в шагах игрового цикла.
    public static final int PLAYER_RELOAD_TICKS = 16;
    // Цвет корпуса танка первого игрока.
    public static final Color PLAYER_COLOR = new Color(55, 180, 80);
    // Цвет корпуса танка второго локального игрока.
    public static final Color PLAYER_TWO_COLOR = new Color(70, 120, 220);

    // Размер танка врага в пикселях.
    public static final int ENEMY_TANK_SIZE = TILE_SIZE;
    // Стартовая строка спавна врагов в клетках.
    public static final int ENEMY_SPAWN_TILE_Y = 1;
    // Радиус нижнего стального ограждения спавна врагов в клетках.
    public static final int ENEMY_SPAWN_WALL_RADIUS = 1;
    // Количество врагов при новой игре или сбросе.
    public static final int INITIAL_ENEMY_COUNT = 2;
    // Максимальное количество врагов на карте одновременно.
    public static final int MAX_ENEMIES = 6;
    // Интервал спавна врагов в шагах игрового цикла.
    public static final int ENEMY_SPAWN_INTERVAL_TICKS = 180;
    // Количество попыток спавна врага перед пропуском заблокированной позиции.
    public static final int ENEMY_SPAWN_RETRIES = 10;
    // Скорость движения врага в пикселях за шаг игрового цикла.
    public static final int ENEMY_SPEED = 1;
    // Минимальная длительность случайного направления AI в шагах игрового цикла.
    public static final int ENEMY_AI_MIN_TICKS = 20;
    // Дополнительный случайный диапазон длительности направления AI в шагах игрового цикла.
    public static final int ENEMY_AI_RANDOM_TICKS = 40;
    // Шанс выстрела врага при выборе нового направления патрулирования.
    public static final float ENEMY_RANDOM_SHOT_CHANCE = 0.25f;
    // Длительность перезарядки врага в шагах игрового цикла.
    public static final int ENEMY_RELOAD_TICKS = 35;
    // Допуск линии видимости врага в пикселях для выравнивания с игроком.
    public static final int ENEMY_SIGHT_TOLERANCE = 8;
    // Очки за уничтожение одного врага.
    public static final int ENEMY_SCORE_REWARD = 100;
    // Линии спавна врагов в координатах клеток.
    public static final int[] ENEMY_SPAWN_LANES = {2, MAP_W / 2, MAP_W - 3};
    // Цвет корпуса вражеского танка.
    public static final Color ENEMY_COLOR = new Color(180, 60, 60);

    // Скорость пули в пикселях за шаг игрового цикла.
    public static final int BULLET_SPEED = 3;
    // Радиус пули в пикселях.
    public static final int BULLET_RADIUS = 3;
    // Цвет пули игрока.
    public static final Color BULLET_COLOR = Color.YELLOW;
    // Цвет пули вражеского бота.
    public static final Color ENEMY_BULLET_COLOR = Color.ORANGE;

    // Заглушка пути к JPG-текстуре танка игрока.
    public static final String PLAYER_TEXTURE_JPG = "assets/textures/player_tank.jpg";
    // Заглушка пути к BMP-текстуре танка игрока.
    public static final String PLAYER_TEXTURE_BMP = "assets/textures/player_tank.bmp";
    // Заглушка пути к JPG-текстуре вражеского танка.
    public static final String ENEMY_TEXTURE_JPG = "assets/textures/enemy_tank.jpg";
    // Заглушка пути к BMP-текстуре вражеского танка.
    public static final String ENEMY_TEXTURE_BMP = "assets/textures/enemy_tank.bmp";
    // Заглушка пути к JPG-текстуре кирпичной стены.
    public static final String BRICK_TEXTURE_JPG = "assets/textures/brick_wall.jpg";
    // Заглушка пути к BMP-текстуре стальной стены.
    public static final String STEEL_TEXTURE_BMP = "assets/textures/steel_wall.bmp";
    // Заглушка пути к JPG-текстуре пули.
    public static final String BULLET_TEXTURE_JPG = "assets/textures/bullet.jpg";

    // Цвет кирпичной стены.
    public static final Color BRICK_COLOR = new Color(178, 87, 34);
    // Цвет стальной стены.
    public static final Color STEEL_COLOR = Color.GRAY;
    // Цвет текста интерфейса.
    public static final Color HUD_COLOR = Color.WHITE;
    // Цвет текста окончания игры.
    public static final Color GAME_OVER_COLOR = new Color(255, 40, 40);
    // Размер шрифта текста окончания игры.
    public static final float GAME_OVER_FONT_SIZE = 42f;
}
