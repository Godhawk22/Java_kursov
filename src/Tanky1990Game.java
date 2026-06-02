import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Главная игровая панель: хранит карту, игровой цикл, привязки клавиш,
 * отрисовку, танк игрока, танки врагов и пули.
 */
public class Tanky1990Game extends JPanel {
    // Размер одной клетки карты в пикселях.
    private static final int TILE_SIZE = 32;
    // Ширина карты в клетках.
    private static final int MAP_W = 20;
    // Высота карты в клетках: поле увеличено в два раза по сравнению с базовой версией.
    private static final int MAP_H = 30;
    // Ширина окна и игрового поля в пикселях.
    private static final int WIDTH = MAP_W * TILE_SIZE;
    // Высота окна и игрового поля в пикселях.
    private static final int HEIGHT = MAP_H * TILE_SIZE;
    // Задержка между шагами игрового цикла в миллисекундах.
    private static final int TIMER_DELAY_MS = 16;

    // Значение пустой клетки карты.
    private static final int CELL_EMPTY = 0;
    // Значение стальной стены: неразрушаемое препятствие.
    private static final int CELL_STEEL = -1;
    // Полная прочность кирпичной стены: для разрушения нужны три попадания.
    private static final int BRICK_MAX_HEALTH = 3;
    // Размер откалываемого куска кирпича за одно попадание.
    private static final int BRICK_DAMAGE_STEP = TILE_SIZE / BRICK_MAX_HEALTH;
    // Шанс случайного появления кирпича в каждой внутренней клетке карты.
    private static final float BRICK_SPAWN_CHANCE = 0.15f;
    // Шанс случайного появления стального блока в каждой внутренней клетке карты.
    private static final float STEEL_SPAWN_CHANCE = 0.04f;
    // Первая клетка безопасной зоны спавна игрока по оси X.
    private static final int PLAYER_SAFE_AREA_X1 = 1;
    // Последняя клетка безопасной зоны спавна игрока по оси X (не включительно).
    private static final int PLAYER_SAFE_AREA_X2 = 5;
    // Смещение первой клетки безопасной зоны спавна игрока по оси Y от нижнего края.
    private static final int PLAYER_SAFE_AREA_BOTTOM_OFFSET = 3;

    // Стартовая клетка игрока по оси X.
    private static final int PLAYER_START_TILE_X = 2;
    // Стартовая клетка игрока по оси Y.
    private static final int PLAYER_START_TILE_Y = MAP_H - 2;
    // Размер танка игрока: уменьшен на 25% относительно клетки 32x32.
    private static final int PLAYER_TANK_SIZE = TILE_SIZE * 3 / 4;
    // Радиус стального ограждения вокруг спавна игрока в клетках.
    private static final int PLAYER_SPAWN_WALL_RADIUS = 2;
    // Скорость движения игрока в пикселях за шаг игрового цикла.
    private static final int PLAYER_SPEED = 2;
    // Начальное количество жизней игрока.
    private static final int PLAYER_START_LIVES = 3;
    // Длительность перезарядки игрока в шагах игрового цикла.
    private static final int PLAYER_RELOAD_TICKS = 16;
    // Цвет корпуса танка игрока.
    private static final Color PLAYER_COLOR = new Color(55, 180, 80);

    // Размер танка врага в пикселях.
    private static final int ENEMY_TANK_SIZE = TILE_SIZE;
    // Стартовая строка спавна врагов в клетках.
    private static final int ENEMY_SPAWN_TILE_Y = 1;
    // Радиус нижнего стального ограждения спавна врагов в клетках.
    private static final int ENEMY_SPAWN_WALL_RADIUS = 1;
    // Количество врагов при новой игре или сбросе.
    private static final int INITIAL_ENEMY_COUNT = 2;
    // Максимальное количество врагов на карте одновременно.
    private static final int MAX_ENEMIES = 6;
    // Интервал спавна врагов в шагах игрового цикла.
    private static final int ENEMY_SPAWN_INTERVAL_TICKS = 180;
    // Количество попыток спавна врага перед пропуском заблокированной позиции.
    private static final int ENEMY_SPAWN_RETRIES = 10;
    // Скорость движения врага в пикселях за шаг игрового цикла.
    private static final int ENEMY_SPEED = 1;
    // Минимальная длительность случайного направления AI в шагах игрового цикла.
    private static final int ENEMY_AI_MIN_TICKS = 20;
    // Дополнительный случайный диапазон длительности направления AI в шагах игрового цикла.
    private static final int ENEMY_AI_RANDOM_TICKS = 40;
    // Шанс выстрела врага при выборе нового направления патрулирования.
    private static final float ENEMY_RANDOM_SHOT_CHANCE = 0.25f;
    // Длительность перезарядки врага в шагах игрового цикла.
    private static final int ENEMY_RELOAD_TICKS = 35;
    // Допуск линии видимости врага в пикселях для выравнивания с игроком.
    private static final int ENEMY_SIGHT_TOLERANCE = 8;
    // Очки за уничтожение одного врага.
    private static final int ENEMY_SCORE_REWARD = 100;
    // Линии спавна врагов в координатах клеток.
    private static final int[] ENEMY_SPAWN_LANES = {2, MAP_W / 2, MAP_W - 3};
    // Цвет корпуса вражеского танка.
    private static final Color ENEMY_COLOR = new Color(180, 60, 60);

    // Скорость пули в пикселях за шаг игрового цикла.
    private static final int BULLET_SPEED = 3;
    // Радиус пули в пикселях.
    private static final int BULLET_RADIUS = 3;
    // Цвет пули.
    private static final Color BULLET_COLOR = Color.YELLOW;

    // Заглушка пути к JPG-текстуре танка игрока.
    private static final String PLAYER_TEXTURE_JPG = "assets/textures/player_tank.jpg";
    // Заглушка пути к BMP-текстуре танка игрока.
    private static final String PLAYER_TEXTURE_BMP = "assets/textures/player_tank.bmp";
    // Заглушка пути к JPG-текстуре вражеского танка.
    private static final String ENEMY_TEXTURE_JPG = "assets/textures/enemy_tank.jpg";
    // Заглушка пути к BMP-текстуре вражеского танка.
    private static final String ENEMY_TEXTURE_BMP = "assets/textures/enemy_tank.bmp";
    // Заглушка пути к JPG-текстуре кирпичной стены.
    private static final String BRICK_TEXTURE_JPG = "assets/textures/brick_wall.jpg";
    // Заглушка пути к BMP-текстуре стальной стены.
    private static final String STEEL_TEXTURE_BMP = "assets/textures/steel_wall.bmp";
    // Заглушка пути к JPG-текстуре пули.
    private static final String BULLET_TEXTURE_JPG = "assets/textures/bullet.jpg";

    // Цвет кирпичной стены.
    private static final Color BRICK_COLOR = new Color(178, 87, 34);
    // Цвет стальной стены.
    private static final Color STEEL_COLOR = Color.GRAY;
    // Цвет текста интерфейса.
    private static final Color HUD_COLOR = Color.WHITE;
    // Цвет текста окончания игры.
    private static final Color GAME_OVER_COLOR = new Color(255, 40, 40);
    // Размер шрифта текста окончания игры.
    private static final float GAME_OVER_FONT_SIZE = 42f;

    // Сетка карты: 0 — пусто, -1 — сталь, 1..3 — прочность кирпича.
    private final int[][] map = new int[MAP_H][MAP_W];
    // Физические и визуальные границы оставшейся части кирпича внутри клетки.
    private final Rectangle[][] brickBounds = new Rectangle[MAP_H][MAP_W];
    // Экземпляр танка игрока.
    private final Tank player;
    // Список активных вражеских танков.
    private final List<Tank> enemies = new ArrayList<>();
    // Список активных пуль.
    private final List<Bullet> bullets = new ArrayList<>();
    // Текущее состояние нажатия клавиш движения.
    private final boolean[] keys = new boolean[256];
    // Генератор случайных чисел для карты, спавна врагов и решений AI.
    private final Random random = new Random();

    // Счётчик шагов до следующей попытки спавна врага.
    private int enemySpawnTimer = 0;
    // Счёт игрока.
    private int score = 0;
    // Оставшиеся жизни игрока.
    private int lives = PLAYER_START_LIVES;
    // Становится true, когда игрок теряет все жизни.
    private boolean gameOver = false;

    public Tanky1990Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        initMap();

        player = new Tank(tileToCenteredPixel(PLAYER_START_TILE_X, PLAYER_TANK_SIZE),
                tileToCenteredPixel(PLAYER_START_TILE_Y, PLAYER_TANK_SIZE), Direction.UP, true);
        spawnInitialEnemies();

        setupControls();

        Timer timer = new Timer(TIMER_DELAY_MS, this::gameLoop);
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    private void setupControls() {
        bindMovementKey(KeyEvent.VK_UP);
        bindMovementKey(KeyEvent.VK_DOWN);
        bindMovementKey(KeyEvent.VK_LEFT);
        bindMovementKey(KeyEvent.VK_RIGHT);
        bindMovementKey(KeyEvent.VK_W);
        bindMovementKey(KeyEvent.VK_A);
        bindMovementKey(KeyEvent.VK_S);
        bindMovementKey(KeyEvent.VK_D);

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "shoot");
        actionMap.put("shoot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameOver) {
                    shoot(player);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "reset");
        actionMap.put("reset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });
    }

    private void bindMovementKey(int keyCode) {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        String pressedName = "pressed_" + keyCode;
        String releasedName = "released_" + keyCode;

        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressedName);
        actionMap.put(pressedName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keys[keyCode] = true;
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), releasedName);
        actionMap.put(releasedName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keys[keyCode] = false;
            }
        });
    }

    private void setEmptyCell(int x, int y) {
        map[y][x] = CELL_EMPTY;
        brickBounds[y][x] = null;
    }

    private void setSteelCell(int x, int y) {
        if (!isInsideMap(x, y)) return;
        map[y][x] = CELL_STEEL;
        brickBounds[y][x] = null;
    }

    private void setBrickCell(int x, int y) {
        map[y][x] = BRICK_MAX_HEALTH;
        brickBounds[y][x] = new Rectangle(0, 0, TILE_SIZE, TILE_SIZE);
    }

    private boolean isInsideMap(int x, int y) {
        return x >= 0 && y >= 0 && x < MAP_W && y < MAP_H;
    }

    private int tileToCenteredPixel(int tile, int size) {
        return tile * TILE_SIZE + (TILE_SIZE - size) / 2;
    }

    private int tankSize(Tank tank) {
        return tank.isPlayer ? PLAYER_TANK_SIZE : ENEMY_TANK_SIZE;
    }

    private Rectangle tankBounds(Tank tank) {
        int size = tankSize(tank);
        return new Rectangle(tank.x, tank.y, size, size);
    }

    private boolean isReservedSpawnArea(int x, int y) {
        if (Math.abs(x - PLAYER_START_TILE_X) <= PLAYER_SPAWN_WALL_RADIUS
                && Math.abs(y - PLAYER_START_TILE_Y) <= PLAYER_SPAWN_WALL_RADIUS) {
            return true;
        }

        for (int lane : ENEMY_SPAWN_LANES) {
            if (Math.abs(x - lane) <= ENEMY_SPAWN_WALL_RADIUS
                    && y >= ENEMY_SPAWN_TILE_Y
                    && y <= ENEMY_SPAWN_TILE_Y + ENEMY_SPAWN_WALL_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private void clearEnemySpawnAreas() {
        for (int lane : ENEMY_SPAWN_LANES) {
            for (int x = lane - ENEMY_SPAWN_WALL_RADIUS; x <= lane + ENEMY_SPAWN_WALL_RADIUS; x++) {
                setEmptyCell(x, ENEMY_SPAWN_TILE_Y);
            }
        }
    }

    private void buildEnemySpawnWalls() {
        int wallY = ENEMY_SPAWN_TILE_Y + 1;
        for (int lane : ENEMY_SPAWN_LANES) {
            for (int x = lane - ENEMY_SPAWN_WALL_RADIUS; x <= lane + ENEMY_SPAWN_WALL_RADIUS; x++) {
                setSteelCell(x, wallY);
            }
        }
    }

    private void clearPlayerSafeArea() {
        for (int y = MAP_H - PLAYER_SAFE_AREA_BOTTOM_OFFSET; y < MAP_H - 1; y++) {
            for (int x = PLAYER_SAFE_AREA_X1; x < PLAYER_SAFE_AREA_X2; x++) {
                setEmptyCell(x, y);
            }
        }
    }

    private void buildPlayerSpawnWalls() {
        for (int y = PLAYER_START_TILE_Y - PLAYER_SPAWN_WALL_RADIUS;
             y <= PLAYER_START_TILE_Y + PLAYER_SPAWN_WALL_RADIUS; y++) {
            for (int x = PLAYER_START_TILE_X - PLAYER_SPAWN_WALL_RADIUS;
                 x <= PLAYER_START_TILE_X + PLAYER_SPAWN_WALL_RADIUS; x++) {
                if (!isInsideMap(x, y)) continue;

                boolean perimeter = Math.abs(x - PLAYER_START_TILE_X) == PLAYER_SPAWN_WALL_RADIUS
                        || Math.abs(y - PLAYER_START_TILE_Y) == PLAYER_SPAWN_WALL_RADIUS;
                boolean exitGate = x == PLAYER_START_TILE_X && y == PLAYER_START_TILE_Y - PLAYER_SPAWN_WALL_RADIUS;
                if (perimeter && !exitGate) {
                    setSteelCell(x, y);
                } else {
                    setEmptyCell(x, y);
                }
            }
        }
    }

    private void initMap() {
        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                setEmptyCell(x, y);
            }
        }

        for (int x = 0; x < MAP_W; x++) {
            setSteelCell(x, 0);
            setSteelCell(x, MAP_H - 1);
        }
        for (int y = 0; y < MAP_H; y++) {
            setSteelCell(0, y);
            setSteelCell(MAP_W - 1, y);
        }

        for (int y = 2; y < MAP_H - 2; y++) {
            for (int x = 2; x < MAP_W - 2; x++) {
                if (isReservedSpawnArea(x, y)) {
                    continue;
                }

                float roll = random.nextFloat();
                if (roll < STEEL_SPAWN_CHANCE) {
                    setSteelCell(x, y);
                } else if (roll < STEEL_SPAWN_CHANCE + BRICK_SPAWN_CHANCE) {
                    setBrickCell(x, y);
                }
            }
        }

        clearEnemySpawnAreas();
        buildEnemySpawnWalls();
        clearPlayerSafeArea();
        buildPlayerSpawnWalls();
    }

    private void gameLoop(ActionEvent ignored) {
        if (!gameOver) {
            handleInput();
            updateEnemies();
            updateBullets();
            maybeSpawnEnemy();
            updateReloads();
        }
        repaint();
    }

    private void handleInput() {
        int dx = 0;
        int dy = 0;
        Direction dir = player.direction;

        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
            dy = -PLAYER_SPEED;
            dir = Direction.UP;
        } else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
            dy = PLAYER_SPEED;
            dir = Direction.DOWN;
        } else if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
            dx = -PLAYER_SPEED;
            dir = Direction.LEFT;
        } else if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
            dx = PLAYER_SPEED;
            dir = Direction.RIGHT;
        }

        player.direction = dir;
        moveTank(player, dx, dy);
    }

    private void updateEnemies() {
        for (Tank enemy : enemies) {
            if (enemySeesPlayer(enemy)) {
                enemy.direction = directionToPlayer(enemy);
                if (enemy.reload == 0) {
                    shoot(enemy);
                }
                continue;
            }

            enemy.aiTimer--;
            if (enemy.aiTimer <= 0) {
                enemy.aiTimer = ENEMY_AI_MIN_TICKS + random.nextInt(ENEMY_AI_RANDOM_TICKS);
                enemy.direction = Direction.values()[random.nextInt(Direction.values().length)];
                if (random.nextFloat() < ENEMY_RANDOM_SHOT_CHANCE) shoot(enemy);
            }

            int[] step = stepForDirection(enemy.direction, ENEMY_SPEED);
            boolean moved = moveTank(enemy, step[0], step[1]);
            if (!moved) {
                enemy.direction = directionToPlayer(enemy);
                step = stepForDirection(enemy.direction, ENEMY_SPEED);
                if (!moveTank(enemy, step[0], step[1])) {
                    enemy.direction = Direction.values()[random.nextInt(Direction.values().length)];
                }
            }
        }
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.x += b.dx;
            b.y += b.dy;

            if (b.x < 0 || b.y < 0 || b.x >= WIDTH || b.y >= HEIGHT) {
                it.remove();
                continue;
            }

            int tx = b.x / TILE_SIZE;
            int ty = b.y / TILE_SIZE;
            if (map[ty][tx] > CELL_EMPTY && brickContainsPoint(tx, ty, b.x, b.y)) {
                damageBrick(tx, ty, b);
                it.remove();
                continue;
            }
            if (map[ty][tx] == CELL_STEEL) {
                it.remove();
                continue;
            }

            if (hitTank(b, player)) {
                if (!b.fromPlayer) {
                    lives--;
                    if (lives <= 0) gameOver = true;
                    player.x = PLAYER_START_TILE_X * TILE_SIZE;
                    player.y = PLAYER_START_TILE_Y * TILE_SIZE;
                }
                it.remove();
                continue;
            }

            boolean enemyHit = false;
            for (int i = 0; i < enemies.size(); i++) {
                Tank e = enemies.get(i);
                if (hitTank(b, e)) {
                    if (b.fromPlayer) {
                        enemies.remove(i);
                        score += ENEMY_SCORE_REWARD;
                    }
                    enemyHit = true;
                    break;
                }
            }
            if (enemyHit) {
                it.remove();
            }
        }
    }

    private boolean hitTank(Bullet b, Tank t) {
        return tankBounds(t).contains(b.x, b.y);
    }

    private void maybeSpawnEnemy() {
        enemySpawnTimer++;
        if (enemySpawnTimer >= ENEMY_SPAWN_INTERVAL_TICKS && enemies.size() < MAX_ENEMIES) {
            enemySpawnTimer = 0;
            spawnEnemy();
        }
    }

    private void spawnInitialEnemies() {
        for (int i = 0; i < INITIAL_ENEMY_COUNT; i++) {
            spawnEnemy();
        }
    }

    private void spawnEnemy() {
        for (int i = 0; i < ENEMY_SPAWN_RETRIES; i++) {
            int lane = ENEMY_SPAWN_LANES[random.nextInt(ENEMY_SPAWN_LANES.length)];
            int x = lane * TILE_SIZE;
            int y = ENEMY_SPAWN_TILE_Y * TILE_SIZE;
            if (canSpawnAt(x, y)) {
                enemies.add(new Tank(x, y, Direction.DOWN, false));
                return;
            }
        }
    }

    private boolean canSpawnAt(int x, int y) {
        Rectangle spawn = new Rectangle(x, y, ENEMY_TANK_SIZE, ENEMY_TANK_SIZE);
        if (tankBounds(player).intersects(spawn)) return false;
        for (Tank e : enemies) {
            if (tankBounds(e).intersects(spawn)) return false;
        }
        return true;
    }

    private Direction directionToPlayer(Tank enemy) {
        int dx = player.x - enemy.x;
        int dy = player.y - enemy.y;
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? Direction.LEFT : Direction.RIGHT;
        }
        return dy < 0 ? Direction.UP : Direction.DOWN;
    }

    private boolean enemySeesPlayer(Tank enemy) {
        int enemyCx = enemy.x + tankSize(enemy) / 2;
        int enemyCy = enemy.y + tankSize(enemy) / 2;
        int playerCx = player.x + tankSize(player) / 2;
        int playerCy = player.y + tankSize(player) / 2;

        if (Math.abs(enemyCx - playerCx) <= ENEMY_SIGHT_TOLERANCE) {
            for (int y = Math.min(enemyCy, playerCy); y <= Math.max(enemyCy, playerCy); y += BULLET_SPEED) {
                if (wallContainsPoint(enemyCx, y)) return false;
            }
            return !wallContainsPoint(enemyCx, Math.max(enemyCy, playerCy));
        }

        if (Math.abs(enemyCy - playerCy) <= ENEMY_SIGHT_TOLERANCE) {
            for (int x = Math.min(enemyCx, playerCx); x <= Math.max(enemyCx, playerCx); x += BULLET_SPEED) {
                if (wallContainsPoint(x, enemyCy)) return false;
            }
            return !wallContainsPoint(Math.max(enemyCx, playerCx), enemyCy);
        }

        return false;
    }

    private int[] stepForDirection(Direction direction, int speed) {
        return switch (direction) {
            case UP -> new int[]{0, -speed};
            case DOWN -> new int[]{0, speed};
            case LEFT -> new int[]{-speed, 0};
            case RIGHT -> new int[]{speed, 0};
        };
    }

    private boolean moveTank(Tank t, int dx, int dy) {
        if (dx == 0 && dy == 0) return true;
        int nx = t.x + dx;
        int ny = t.y + dy;

        int size = tankSize(t);
        Rectangle next = new Rectangle(nx, ny, size, size);
        if (nx < TILE_SIZE || ny < TILE_SIZE || nx + size >= WIDTH - TILE_SIZE || ny + size >= HEIGHT - TILE_SIZE) {
            return false;
        }

        int left = next.x / TILE_SIZE;
        int right = (next.x + size - 1) / TILE_SIZE;
        int top = next.y / TILE_SIZE;
        int bottom = (next.y + size - 1) / TILE_SIZE;

        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if (map[y][x] == CELL_STEEL) return false;
                if (map[y][x] > CELL_EMPTY && brickIntersects(x, y, next)) return false;
            }
        }

        if (t.isPlayer) {
            for (Tank e : enemies) {
                if (tankBounds(e).intersects(next)) return false;
            }
        } else {
            if (tankBounds(player).intersects(next)) return false;
            for (Tank e : enemies) {
                if (e != t && tankBounds(e).intersects(next)) return false;
            }
        }

        t.x = nx;
        t.y = ny;
        return true;
    }

    private void shoot(Tank t) {
        if (t.reload > 0) return;
        int size = tankSize(t);
        int bx = t.x + size / 2;
        int by = t.y + size / 2;
        int dx = 0, dy = 0;
        switch (t.direction) {
            case UP -> {
                by = t.y - 1;
                dy = -BULLET_SPEED;
            }
            case DOWN -> {
                by = t.y + size;
                dy = BULLET_SPEED;
            }
            case LEFT -> {
                bx = t.x - 1;
                dx = -BULLET_SPEED;
            }
            case RIGHT -> {
                bx = t.x + size;
                dx = BULLET_SPEED;
            }
        }
        bullets.add(new Bullet(bx, by, dx, dy, t.isPlayer));
        t.reload = t.isPlayer ? PLAYER_RELOAD_TICKS : ENEMY_RELOAD_TICKS;
        System.out.printf("SHOT: %s direction=%s bullet=(%d,%d) tank=(%d,%d)%n",
                t.isPlayer ? "player" : "bot", t.direction, bx, by, t.x, t.y);
    }

    private void resetGame() {
        score = 0;
        lives = PLAYER_START_LIVES;
        gameOver = false;
        bullets.clear();
        enemies.clear();
        initMap();
        player.x = tileToCenteredPixel(PLAYER_START_TILE_X, PLAYER_TANK_SIZE);
        player.y = tileToCenteredPixel(PLAYER_START_TILE_Y, PLAYER_TANK_SIZE);
        player.direction = Direction.UP;
        player.reload = 0;
        enemySpawnTimer = 0;
        spawnInitialEnemies();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                int cell = map[y][x];
                if (cell > CELL_EMPTY) {
                    drawBrick(g2, x, y);
                } else if (cell == CELL_STEEL) {
                    g2.setColor(STEEL_COLOR);
                    g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        drawTank(g2, player, PLAYER_COLOR);
        for (Tank e : enemies) drawTank(g2, e, ENEMY_COLOR);

        g2.setColor(BULLET_COLOR);
        for (Bullet b : bullets) {
            g2.fillOval(b.x - BULLET_RADIUS, b.y - BULLET_RADIUS, BULLET_RADIUS * 2, BULLET_RADIUS * 2);
        }

        g2.setColor(HUD_COLOR);
        g2.drawString("Score: " + score + "   Lives: " + lives + "   R - restart", 12, 18);

        if (gameOver) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, GAME_OVER_FONT_SIZE));
            String text = "GAME OVER";
            int w = g2.getFontMetrics().stringWidth(text);
            g2.setColor(GAME_OVER_COLOR);
            g2.drawString(text, (WIDTH - w) / 2, HEIGHT / 2);
        }
    }

    private void drawBrick(Graphics2D g2, int tileX, int tileY) {
        Rectangle bounds = brickBounds[tileY][tileX];
        if (bounds == null) return;

        g2.setColor(BRICK_COLOR);
        g2.fillRect(tileX * TILE_SIZE + bounds.x, tileY * TILE_SIZE + bounds.y, bounds.width, bounds.height);
    }

    private void damageBrick(int tileX, int tileY, Bullet bullet) {
        map[tileY][tileX]--;
        if (map[tileY][tileX] <= CELL_EMPTY) {
            map[tileY][tileX] = CELL_EMPTY;
            brickBounds[tileY][tileX] = null;
            return;
        }

        Rectangle bounds = brickBounds[tileY][tileX];
        if (bounds == null) {
            bounds = new Rectangle(0, 0, TILE_SIZE, TILE_SIZE);
            brickBounds[tileY][tileX] = bounds;
        }

        if (Math.abs(bullet.dx) >= Math.abs(bullet.dy)) {
            if (bullet.dx > 0) {
                removeBrickFromLeft(bounds);
            } else {
                removeBrickFromRight(bounds);
            }
        } else if (bullet.dy > 0) {
            removeBrickFromTop(bounds);
        } else {
            removeBrickFromBottom(bounds);
        }
    }

    private void removeBrickFromLeft(Rectangle bounds) {
        int removed = Math.min(BRICK_DAMAGE_STEP, bounds.width);
        bounds.x += removed;
        bounds.width -= removed;
    }

    private void removeBrickFromRight(Rectangle bounds) {
        bounds.width = Math.max(0, bounds.width - BRICK_DAMAGE_STEP);
    }

    private void removeBrickFromTop(Rectangle bounds) {
        int removed = Math.min(BRICK_DAMAGE_STEP, bounds.height);
        bounds.y += removed;
        bounds.height -= removed;
    }

    private void removeBrickFromBottom(Rectangle bounds) {
        bounds.height = Math.max(0, bounds.height - BRICK_DAMAGE_STEP);
    }

    private boolean brickIntersects(int tileX, int tileY, Rectangle target) {
        Rectangle bounds = getBrickWorldBounds(tileX, tileY);
        return bounds != null && bounds.intersects(target);
    }

    private boolean brickContainsPoint(int tileX, int tileY, int worldX, int worldY) {
        Rectangle bounds = getBrickWorldBounds(tileX, tileY);
        return bounds != null && bounds.contains(worldX, worldY);
    }

    private boolean wallContainsPoint(int worldX, int worldY) {
        if (worldX < 0 || worldY < 0 || worldX >= WIDTH || worldY >= HEIGHT) return true;

        int tileX = worldX / TILE_SIZE;
        int tileY = worldY / TILE_SIZE;
        if (map[tileY][tileX] == CELL_STEEL) return true;
        return map[tileY][tileX] > CELL_EMPTY && brickContainsPoint(tileX, tileY, worldX, worldY);
    }

    private Rectangle getBrickWorldBounds(int tileX, int tileY) {
        Rectangle bounds = brickBounds[tileY][tileX];
        if (bounds == null) return null;
        return new Rectangle(tileX * TILE_SIZE + bounds.x, tileY * TILE_SIZE + bounds.y, bounds.width, bounds.height);
    }

    private void updateReloads() {
        if (player.reload > 0) player.reload--;
        for (Tank e : enemies) {
            if (e.reload > 0) e.reload--;
        }
    }

    private void drawTank(Graphics2D g2, Tank t, Color body) {
        int x = t.x;
        int y = t.y;
        int size = tankSize(t);
        int padding = Math.max(3, size / 8);
        int barrelWidth = Math.max(4, size / 5);
        int barrelLength = Math.max(12, size / 2);

        g2.setColor(body);
        g2.fillRect(x + padding, y + padding, size - padding * 2, size - padding * 2);

        g2.setColor(body.darker());
        switch (t.direction) {
            case UP -> g2.fillRect(x + size / 2 - barrelWidth / 2, y - barrelLength / 3, barrelWidth, barrelLength);
            case DOWN -> g2.fillRect(x + size / 2 - barrelWidth / 2, y + size - barrelLength * 2 / 3, barrelWidth, barrelLength);
            case LEFT -> g2.fillRect(x - barrelLength / 3, y + size / 2 - barrelWidth / 2, barrelLength, barrelWidth);
            case RIGHT -> g2.fillRect(x + size - barrelLength * 2 / 3, y + size / 2 - barrelWidth / 2, barrelLength, barrelWidth);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tanky 1990 - Java");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new Tanky1990Game());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /** Направление, в которое танк смотрит и стреляет. */
    private enum Direction {UP, DOWN, LEFT, RIGHT}

    /** Изменяемая сущность танка для игрока и вражеских ботов. */
    private static class Tank {
        int x, y;
        Direction direction;
        final boolean isPlayer;
        int aiTimer = 0;
        int reload = 0;

        Tank(int x, int y, Direction direction, boolean isPlayer) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.isPlayer = isPlayer;
        }
    }

    /** Сущность пули с позицией, скоростью и признаком владельца. */
    private static class Bullet {
        int x, y;
        final int dx, dy;
        final boolean fromPlayer;

        Bullet(int x, int y, int dx, int dy, boolean fromPlayer) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.fromPlayer = fromPlayer;
        }
    }
}
