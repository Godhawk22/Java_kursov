import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Main game panel: owns the map, game loop, input bindings, rendering,
 * player tank, enemy tanks, and bullets.
 */
public class Tanky1990Game extends JPanel {
    // Size of one map tile in pixels.
    private static final int TILE_SIZE = 32;
    // Map width in tiles.
    private static final int MAP_W = 20;
    // Map height in tiles.
    private static final int MAP_H = 15;
    // Window/game field width in pixels.
    private static final int WIDTH = MAP_W * TILE_SIZE;
    // Window/game field height in pixels.
    private static final int HEIGHT = MAP_H * TILE_SIZE;
    // Delay between game-loop ticks in milliseconds.
    private static final int TIMER_DELAY_MS = 16;

    // Empty map cell value.
    private static final int CELL_EMPTY = 0;
    // Steel wall map cell value: indestructible obstacle.
    private static final int CELL_STEEL = -1;
    // Full brick wall health: each brick needs three bullet hits to disappear.
    private static final int BRICK_MAX_HEALTH = 3;
    // Random brick generation chance for each inner map tile.
    private static final float BRICK_SPAWN_CHANCE = 0.15f;
    // First tile of the safe player spawn area on X axis.
    private static final int PLAYER_SAFE_AREA_X1 = 1;
    // Last tile (exclusive) of the safe player spawn area on X axis.
    private static final int PLAYER_SAFE_AREA_X2 = 5;
    // First tile of the safe player spawn area on Y axis, counted from the bottom.
    private static final int PLAYER_SAFE_AREA_BOTTOM_OFFSET = 3;

    // Player starting tile on X axis.
    private static final int PLAYER_START_TILE_X = 2;
    // Player starting tile on Y axis.
    private static final int PLAYER_START_TILE_Y = MAP_H - 2;
    // Player movement speed in pixels per game-loop tick.
    private static final int PLAYER_SPEED = 2;
    // Player starting lives.
    private static final int PLAYER_START_LIVES = 3;
    // Player reload duration in game-loop ticks.
    private static final int PLAYER_RELOAD_TICKS = 16;
    // Player body color.
    private static final Color PLAYER_COLOR = new Color(55, 180, 80);

    // Number of enemies spawned at a new game/reset.
    private static final int INITIAL_ENEMY_COUNT = 2;
    // Maximum enemies allowed on the map at once.
    private static final int MAX_ENEMIES = 6;
    // Enemy spawn interval in game-loop ticks.
    private static final int ENEMY_SPAWN_INTERVAL_TICKS = 180;
    // Enemy spawn retry count before skipping a blocked spawn.
    private static final int ENEMY_SPAWN_RETRIES = 10;
    // Enemy movement speed in pixels per game-loop tick.
    private static final int ENEMY_SPEED = 1;
    // Minimum random AI direction duration in game-loop ticks.
    private static final int ENEMY_AI_MIN_TICKS = 20;
    // Extra random AI direction duration range in game-loop ticks.
    private static final int ENEMY_AI_RANDOM_TICKS = 40;
    // Enemy chance to fire when choosing a new patrol direction.
    private static final float ENEMY_RANDOM_SHOT_CHANCE = 0.25f;
    // Enemy reload duration in game-loop ticks.
    private static final int ENEMY_RELOAD_TICKS = 35;
    // Enemy line-of-sight tolerance in pixels for aligning with the player.
    private static final int ENEMY_SIGHT_TOLERANCE = 8;
    // Score awarded for destroying one enemy.
    private static final int ENEMY_SCORE_REWARD = 100;
    // Enemy spawn lanes in tile coordinates.
    private static final int[] ENEMY_SPAWN_LANES = {2, MAP_W / 2, MAP_W - 3};
    // Enemy body color.
    private static final Color ENEMY_COLOR = new Color(180, 60, 60);

    // Bullet speed in pixels per game-loop tick.
    private static final int BULLET_SPEED = 3;
    // Bullet radius in pixels.
    private static final int BULLET_RADIUS = 3;
    // Bullet color.
    private static final Color BULLET_COLOR = Color.YELLOW;

    // Brick wall color.
    private static final Color BRICK_COLOR = new Color(178, 87, 34);
    // Steel wall color.
    private static final Color STEEL_COLOR = Color.GRAY;
    // HUD text color.
    private static final Color HUD_COLOR = Color.WHITE;
    // Game-over text color.
    private static final Color GAME_OVER_COLOR = new Color(255, 40, 40);
    // Game-over font size.
    private static final float GAME_OVER_FONT_SIZE = 42f;

    // Map grid: 0 is empty, -1 is steel, 1..3 is brick health.
    private final int[][] map = new int[MAP_H][MAP_W];
    // Player tank instance.
    private final Tank player;
    // Active enemy tank list.
    private final List<Tank> enemies = new ArrayList<>();
    // Active bullet list.
    private final List<Bullet> bullets = new ArrayList<>();
    // Current pressed/released state for movement keys.
    private final boolean[] keys = new boolean[256];
    // Random source for map generation, enemy spawning, and AI decisions.
    private final Random random = new Random();

    // Counts ticks until the next enemy spawn attempt.
    private int enemySpawnTimer = 0;
    // Player score.
    private int score = 0;
    // Remaining player lives.
    private int lives = PLAYER_START_LIVES;
    // True after the player loses all lives.
    private boolean gameOver = false;

    public Tanky1990Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        initMap();

        player = new Tank(PLAYER_START_TILE_X * TILE_SIZE, PLAYER_START_TILE_Y * TILE_SIZE, Direction.UP, true);
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

    private void initMap() {
        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                map[y][x] = CELL_EMPTY;
            }
        }

        for (int x = 0; x < MAP_W; x++) {
            map[0][x] = CELL_STEEL;
            map[MAP_H - 1][x] = CELL_STEEL;
        }
        for (int y = 0; y < MAP_H; y++) {
            map[y][0] = CELL_STEEL;
            map[y][MAP_W - 1] = CELL_STEEL;
        }

        for (int y = 2; y < MAP_H - 2; y++) {
            for (int x = 2; x < MAP_W - 2; x++) {
                if (random.nextFloat() < BRICK_SPAWN_CHANCE) {
                    map[y][x] = BRICK_MAX_HEALTH;
                }
            }
        }

        for (int y = MAP_H - PLAYER_SAFE_AREA_BOTTOM_OFFSET; y < MAP_H - 1; y++) {
            for (int x = PLAYER_SAFE_AREA_X1; x < PLAYER_SAFE_AREA_X2; x++) {
                map[y][x] = CELL_EMPTY;
            }
        }
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
            if (map[ty][tx] > CELL_EMPTY) {
                map[ty][tx]--;
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
        Rectangle r = new Rectangle(t.x, t.y, TILE_SIZE, TILE_SIZE);
        return r.contains(b.x, b.y);
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
            int y = TILE_SIZE;
            if (canSpawnAt(x, y)) {
                enemies.add(new Tank(x, y, Direction.DOWN, false));
                return;
            }
        }
    }

    private boolean canSpawnAt(int x, int y) {
        Rectangle spawn = new Rectangle(x, y, TILE_SIZE, TILE_SIZE);
        if (new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE).intersects(spawn)) return false;
        for (Tank e : enemies) {
            if (new Rectangle(e.x, e.y, TILE_SIZE, TILE_SIZE).intersects(spawn)) return false;
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
        int enemyCx = enemy.x + TILE_SIZE / 2;
        int enemyCy = enemy.y + TILE_SIZE / 2;
        int playerCx = player.x + TILE_SIZE / 2;
        int playerCy = player.y + TILE_SIZE / 2;

        if (Math.abs(enemyCx - playerCx) <= ENEMY_SIGHT_TOLERANCE) {
            int x = enemyCx / TILE_SIZE;
            int y1 = Math.min(enemyCy, playerCy) / TILE_SIZE;
            int y2 = Math.max(enemyCy, playerCy) / TILE_SIZE;
            for (int y = y1; y <= y2; y++) {
                if (map[y][x] != CELL_EMPTY) return false;
            }
            return true;
        }

        if (Math.abs(enemyCy - playerCy) <= ENEMY_SIGHT_TOLERANCE) {
            int y = enemyCy / TILE_SIZE;
            int x1 = Math.min(enemyCx, playerCx) / TILE_SIZE;
            int x2 = Math.max(enemyCx, playerCx) / TILE_SIZE;
            for (int x = x1; x <= x2; x++) {
                if (map[y][x] != CELL_EMPTY) return false;
            }
            return true;
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

        Rectangle next = new Rectangle(nx, ny, TILE_SIZE, TILE_SIZE);
        if (nx < TILE_SIZE || ny < TILE_SIZE || nx + TILE_SIZE >= WIDTH - TILE_SIZE || ny + TILE_SIZE >= HEIGHT - TILE_SIZE) {
            return false;
        }

        int left = next.x / TILE_SIZE;
        int right = (next.x + TILE_SIZE - 1) / TILE_SIZE;
        int top = next.y / TILE_SIZE;
        int bottom = (next.y + TILE_SIZE - 1) / TILE_SIZE;

        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if (map[y][x] != CELL_EMPTY) return false;
            }
        }

        if (t.isPlayer) {
            for (Tank e : enemies) {
                if (new Rectangle(e.x, e.y, TILE_SIZE, TILE_SIZE).intersects(next)) return false;
            }
        } else {
            if (new Rectangle(player.x, player.y, TILE_SIZE, TILE_SIZE).intersects(next)) return false;
            for (Tank e : enemies) {
                if (e != t && new Rectangle(e.x, e.y, TILE_SIZE, TILE_SIZE).intersects(next)) return false;
            }
        }

        t.x = nx;
        t.y = ny;
        return true;
    }

    private void shoot(Tank t) {
        if (t.reload > 0) return;
        int bx = t.x + TILE_SIZE / 2;
        int by = t.y + TILE_SIZE / 2;
        int dx = 0, dy = 0;
        switch (t.direction) {
            case UP -> {
                by = t.y - 1;
                dy = -BULLET_SPEED;
            }
            case DOWN -> {
                by = t.y + TILE_SIZE;
                dy = BULLET_SPEED;
            }
            case LEFT -> {
                bx = t.x - 1;
                dx = -BULLET_SPEED;
            }
            case RIGHT -> {
                bx = t.x + TILE_SIZE;
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
        player.x = PLAYER_START_TILE_X * TILE_SIZE;
        player.y = PLAYER_START_TILE_Y * TILE_SIZE;
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
                    drawBrick(g2, x, y, cell);
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

    private void drawBrick(Graphics2D g2, int tileX, int tileY, int health) {
        int visibleWidth = TILE_SIZE * health / BRICK_MAX_HEALTH;
        g2.setColor(BRICK_COLOR);
        g2.fillRect(tileX * TILE_SIZE, tileY * TILE_SIZE, visibleWidth, TILE_SIZE);
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
        g2.setColor(body);
        g2.fillRect(x + 4, y + 4, TILE_SIZE - 8, TILE_SIZE - 8);

        g2.setColor(body.darker());
        switch (t.direction) {
            case UP -> g2.fillRect(x + TILE_SIZE / 2 - 3, y - 6, 6, 18);
            case DOWN -> g2.fillRect(x + TILE_SIZE / 2 - 3, y + TILE_SIZE - 12, 6, 18);
            case LEFT -> g2.fillRect(x - 6, y + TILE_SIZE / 2 - 3, 18, 6);
            case RIGHT -> g2.fillRect(x + TILE_SIZE - 12, y + TILE_SIZE / 2 - 3, 18, 6);
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

    /** Direction in which a tank is facing and shooting. */
    private enum Direction {UP, DOWN, LEFT, RIGHT}

    /** Mutable tank entity used for both the player and enemy bots. */
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

    /** Bullet entity with position, velocity, and ownership flag. */
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
