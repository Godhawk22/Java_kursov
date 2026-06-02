import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Главная игровая панель: цикл матча, ввод, танки, пули и отрисовка. */
public class Tanky1990Game extends JPanel {
    private final GameMap map;
    private final Player playerOne;
    private final Player playerTwo;
    private final List<Player> players = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final boolean[] keys = new boolean[256];
    private final Random random = new Random();
    private final boolean localMultiplayer;

    private int enemySpawnTimer = 0;
    private boolean gameOver = false;

    public Tanky1990Game() {
        this(false);
    }

    public Tanky1990Game(boolean localMultiplayer) {
        this.localMultiplayer = localMultiplayer;
        setPreferredSize(new Dimension(GameConfig.WIDTH, GameConfig.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        map = new GameMap(random);
        map.init();

        playerOne = new Player(1, GameConfig.PLAYER_START_TILE_X, GameConfig.PLAYER_START_TILE_Y);
        playerTwo = localMultiplayer
                ? new Player(2, GameConfig.PLAYER_TWO_START_TILE_X, GameConfig.PLAYER_TWO_START_TILE_Y)
                : null;
        players.add(playerOne);
        if (playerTwo != null) players.add(playerTwo);

        spawnInitialEnemies();
        setupControls();

        Timer timer = new Timer(GameConfig.TIMER_DELAY_MS, this::gameLoop);
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    private void setupControls() {
        bindMovementKey(KeyEvent.VK_W);
        bindMovementKey(KeyEvent.VK_A);
        bindMovementKey(KeyEvent.VK_S);
        bindMovementKey(KeyEvent.VK_D);
        bindMovementKey(KeyEvent.VK_UP);
        bindMovementKey(KeyEvent.VK_DOWN);
        bindMovementKey(KeyEvent.VK_LEFT);
        bindMovementKey(KeyEvent.VK_RIGHT);

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "shoot_p1");
        actionMap.put("shoot_p1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameOver) shoot(playerOne);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "shoot_p2");
        actionMap.put("shoot_p2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameOver && playerTwo != null) shoot(playerTwo);
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

    private void gameLoop(ActionEvent ignored) {
        if (!gameOver) {
            handlePlayersInput();
            updateEnemies();
            updateBullets();
            maybeSpawnEnemy();
            updateReloads();
            gameOver = players.stream().noneMatch(Player::isAlive);
        }
        repaint();
    }

    private void handlePlayersInput() {
        handlePlayerInput(playerOne, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D);
        if (playerTwo != null) {
            handlePlayerInput(playerTwo, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT);
        } else {
            handlePlayerInput(playerOne, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT);
        }
    }

    private void handlePlayerInput(Player player, int up, int down, int left, int right) {
        if (!player.isAlive()) return;
        int dx = 0;
        int dy = 0;
        Direction dir = player.direction;

        if (keys[up]) {
            dy = -GameConfig.PLAYER_SPEED;
            dir = Direction.UP;
        } else if (keys[down]) {
            dy = GameConfig.PLAYER_SPEED;
            dir = Direction.DOWN;
        } else if (keys[left]) {
            dx = -GameConfig.PLAYER_SPEED;
            dir = Direction.LEFT;
        } else if (keys[right]) {
            dx = GameConfig.PLAYER_SPEED;
            dir = Direction.RIGHT;
        }

        player.direction = dir;
        moveTank(player, dx, dy);
    }

    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            Player target = nearestAlivePlayer(enemy);
            if (target != null && enemySeesTank(enemy, target)) {
                enemy.direction = directionToTarget(enemy, target);
                if (enemy.reload == 0) shoot(enemy);
                continue;
            }

            enemy.aiTimer--;
            if (enemy.aiTimer <= 0) {
                enemy.aiTimer = GameConfig.ENEMY_AI_MIN_TICKS + random.nextInt(GameConfig.ENEMY_AI_RANDOM_TICKS);
                enemy.direction = Direction.values()[random.nextInt(Direction.values().length)];
                if (random.nextFloat() < GameConfig.ENEMY_RANDOM_SHOT_CHANCE) shoot(enemy);
            }

            int[] step = stepForDirection(enemy.direction, GameConfig.ENEMY_SPEED);
            if (!moveTank(enemy, step[0], step[1])) {
                if (target != null) enemy.direction = directionToTarget(enemy, target);
                step = stepForDirection(enemy.direction, GameConfig.ENEMY_SPEED);
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

            if (map.hitWall(b)) {
                it.remove();
                continue;
            }

            if (hitPlayers(b) || hitEnemies(b)) {
                it.remove();
            }
        }
    }

    private boolean hitPlayers(Bullet bullet) {
        for (Player player : players) {
            if (bullet.owner == player || !player.isAlive()) continue;
            if (player.bounds().contains(bullet.x, bullet.y)) {
                player.lives--;
                if (player.lives > 0) respawnPlayer(player);
                return true;
            }
        }
        return false;
    }

    private boolean hitEnemies(Bullet bullet) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (bullet.owner == enemy) continue;
            if (enemy.bounds().contains(bullet.x, bullet.y)) {
                enemies.remove(i);
                if (bullet.owner instanceof Player owner) {
                    owner.score += GameConfig.ENEMY_SCORE_REWARD;
                }
                return true;
            }
        }
        return false;
    }

    private void maybeSpawnEnemy() {
        enemySpawnTimer++;
        if (enemySpawnTimer >= GameConfig.ENEMY_SPAWN_INTERVAL_TICKS && enemies.size() < GameConfig.MAX_ENEMIES) {
            enemySpawnTimer = 0;
            spawnEnemy();
        }
    }

    private void spawnInitialEnemies() {
        for (int i = 0; i < GameConfig.INITIAL_ENEMY_COUNT; i++) {
            spawnEnemy();
        }
    }

    private void spawnEnemy() {
        for (int i = 0; i < GameConfig.ENEMY_SPAWN_RETRIES; i++) {
            int lane = GameConfig.ENEMY_SPAWN_LANES[random.nextInt(GameConfig.ENEMY_SPAWN_LANES.length)];
            int x = lane * GameConfig.TILE_SIZE;
            int y = GameConfig.ENEMY_SPAWN_TILE_Y * GameConfig.TILE_SIZE;
            Enemy enemy = new Enemy(x, y);
            if (canSpawn(enemy)) {
                enemies.add(enemy);
                return;
            }
        }
    }

    private boolean canSpawn(Tank tank) {
        if (map.blocks(tank.bounds())) return false;
        for (Player player : players) {
            if (player.isAlive() && player.bounds().intersects(tank.bounds())) return false;
        }
        for (Enemy enemy : enemies) {
            if (enemy.bounds().intersects(tank.bounds())) return false;
        }
        return true;
    }

    private Player nearestAlivePlayer(Tank enemy) {
        Player best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Player player : players) {
            if (!player.isAlive()) continue;
            int dx = player.x - enemy.x;
            int dy = player.y - enemy.y;
            int distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private Direction directionToTarget(Tank source, Tank target) {
        int dx = target.x - source.x;
        int dy = target.y - source.y;
        if (Math.abs(dx) > Math.abs(dy)) return dx < 0 ? Direction.LEFT : Direction.RIGHT;
        return dy < 0 ? Direction.UP : Direction.DOWN;
    }

    private boolean enemySeesTank(Tank enemy, Tank target) {
        int enemyCx = enemy.x + enemy.size / 2;
        int enemyCy = enemy.y + enemy.size / 2;
        int targetCx = target.x + target.size / 2;
        int targetCy = target.y + target.size / 2;

        if (Math.abs(enemyCx - targetCx) <= GameConfig.ENEMY_SIGHT_TOLERANCE) {
            for (int y = Math.min(enemyCy, targetCy); y <= Math.max(enemyCy, targetCy); y += GameConfig.BULLET_SPEED) {
                if (map.wallContainsPoint(enemyCx, y)) return false;
            }
            return !map.wallContainsPoint(enemyCx, Math.max(enemyCy, targetCy));
        }

        if (Math.abs(enemyCy - targetCy) <= GameConfig.ENEMY_SIGHT_TOLERANCE) {
            for (int x = Math.min(enemyCx, targetCx); x <= Math.max(enemyCx, targetCx); x += GameConfig.BULLET_SPEED) {
                if (map.wallContainsPoint(x, enemyCy)) return false;
            }
            return !map.wallContainsPoint(Math.max(enemyCx, targetCx), enemyCy);
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

    private boolean moveTank(Tank tank, int dx, int dy) {
        if (dx == 0 && dy == 0) return true;
        Rectangle next = new Rectangle(tank.x + dx, tank.y + dy, tank.size, tank.size);
        if (!map.isInsideTankArea(next) || map.blocks(next)) return false;

        for (Player player : players) {
            if (player != tank && player.isAlive() && player.bounds().intersects(next)) return false;
        }
        for (Enemy enemy : enemies) {
            if (enemy != tank && enemy.bounds().intersects(next)) return false;
        }

        tank.x = next.x;
        tank.y = next.y;
        return true;
    }

    private void shoot(Tank tank) {
        if (tank.reload > 0 || !tank.isAlive()) return;
        int bx = tank.x + tank.size / 2;
        int by = tank.y + tank.size / 2;
        int dx = 0, dy = 0;
        switch (tank.direction) {
            case UP -> {
                by = tank.y - 1;
                dy = -GameConfig.BULLET_SPEED;
            }
            case DOWN -> {
                by = tank.y + tank.size;
                dy = GameConfig.BULLET_SPEED;
            }
            case LEFT -> {
                bx = tank.x - 1;
                dx = -GameConfig.BULLET_SPEED;
            }
            case RIGHT -> {
                bx = tank.x + tank.size;
                dx = GameConfig.BULLET_SPEED;
            }
        }
        bullets.add(new Bullet(bx, by, dx, dy, tank));
        tank.reload = tank instanceof Enemy ? GameConfig.ENEMY_RELOAD_TICKS : GameConfig.PLAYER_RELOAD_TICKS;
        System.out.printf("SHOT: %s direction=%s bullet=(%d,%d) tank=(%d,%d)%n",
                tank instanceof Enemy ? "bot" : "player", tank.direction, bx, by, tank.x, tank.y);
    }

    private void resetGame() {
        gameOver = false;
        bullets.clear();
        enemies.clear();
        map.init();
        playerOne.resetToSpawn(GameConfig.PLAYER_START_TILE_X, GameConfig.PLAYER_START_TILE_Y);
        if (playerTwo != null) {
            playerTwo.resetToSpawn(GameConfig.PLAYER_TWO_START_TILE_X, GameConfig.PLAYER_TWO_START_TILE_Y);
        }
        enemySpawnTimer = 0;
        spawnInitialEnemies();
    }

    private void respawnPlayer(Player player) {
        int tileX = player.id == 1 ? GameConfig.PLAYER_START_TILE_X : GameConfig.PLAYER_TWO_START_TILE_X;
        int tileY = player.id == 1 ? GameConfig.PLAYER_START_TILE_Y : GameConfig.PLAYER_TWO_START_TILE_Y;
        player.x = GameMap.tileToCenteredPixel(tileX, player.size);
        player.y = GameMap.tileToCenteredPixel(tileY, player.size);
        player.direction = Direction.UP;
        player.reload = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        map.draw(g2);
        drawTank(g2, playerOne, GameConfig.PLAYER_COLOR);
        if (playerTwo != null) drawTank(g2, playerTwo, GameConfig.PLAYER_TWO_COLOR);
        for (Enemy e : enemies) drawTank(g2, e, GameConfig.ENEMY_COLOR);

        g2.setColor(GameConfig.BULLET_COLOR);
        for (Bullet b : bullets) {
            g2.fillOval(b.x - GameConfig.BULLET_RADIUS, b.y - GameConfig.BULLET_RADIUS,
                    GameConfig.BULLET_RADIUS * 2, GameConfig.BULLET_RADIUS * 2);
        }

        g2.setColor(GameConfig.HUD_COLOR);
        g2.drawString(hudText(), 12, 18);

        if (gameOver) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, GameConfig.GAME_OVER_FONT_SIZE));
            String text = "GAME OVER";
            int w = g2.getFontMetrics().stringWidth(text);
            g2.setColor(GameConfig.GAME_OVER_COLOR);
            g2.drawString(text, (GameConfig.WIDTH - w) / 2, GameConfig.HEIGHT / 2);
        }
    }

    private String hudText() {
        if (playerTwo == null) {
            return "P1 Score: " + playerOne.score + "   Lives: " + playerOne.lives + "   R - restart";
        }
        return "P1 Lives: " + playerOne.lives + " Score: " + playerOne.score
                + "   P2 Lives: " + playerTwo.lives + " Score: " + playerTwo.score + "   R - restart";
    }

    private void updateReloads() {
        for (Player player : players) if (player.reload > 0) player.reload--;
        for (Enemy e : enemies) if (e.reload > 0) e.reload--;
    }

    private void drawTank(Graphics2D g2, Tank tank, Color body) {
        if (!tank.isAlive()) return;
        int x = tank.x;
        int y = tank.y;
        int size = tank.size;
        int padding = Math.max(3, size / 8);
        int barrelWidth = Math.max(4, size / 5);
        int barrelLength = Math.max(12, size / 2);

        g2.setColor(body);
        g2.fillRect(x + padding, y + padding, size - padding * 2, size - padding * 2);

        g2.setColor(body.darker());
        switch (tank.direction) {
            case UP -> g2.fillRect(x + size / 2 - barrelWidth / 2, y - barrelLength / 3, barrelWidth, barrelLength);
            case DOWN -> g2.fillRect(x + size / 2 - barrelWidth / 2, y + size - barrelLength * 2 / 3, barrelWidth, barrelLength);
            case LEFT -> g2.fillRect(x - barrelLength / 3, y + size / 2 - barrelWidth / 2, barrelLength, barrelWidth);
            case RIGHT -> g2.fillRect(x + size - barrelLength * 2 / 3, y + size / 2 - barrelWidth / 2, barrelLength, barrelWidth);
        }
    }

    public static void main(String[] args) {
        GameClient.main(args);
    }
}
