import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Tanky1990Game extends JPanel {
    private static final int TILE_SIZE = 32;
    private static final int MAP_W = 20;
    private static final int MAP_H = 15;
    private static final int WIDTH = MAP_W * TILE_SIZE;
    private static final int HEIGHT = MAP_H * TILE_SIZE;

    private final int[][] map = new int[MAP_H][MAP_W]; // 0-empty,1-brick,2-steel
    private final Tank player;
    private final List<Tank> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final boolean[] keys = new boolean[256];
    private final Random random = new Random();

    private int enemySpawnTimer = 0;
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;

    public Tanky1990Game() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        initMap();

        player = new Tank(2 * TILE_SIZE, (MAP_H - 2) * TILE_SIZE, Direction.UP, true);
        spawnEnemy();
        spawnEnemy();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k < keys.length) keys[k] = true;
                if (k == KeyEvent.VK_SPACE && !gameOver) {
                    shoot(player);
                }
                if (k == KeyEvent.VK_R) {
                    resetGame();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                if (k < keys.length) keys[k] = false;
            }
        });

        Timer timer = new Timer(16, this::gameLoop);
        timer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    private void initMap() {
        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                map[y][x] = 0;
            }
        }

        for (int x = 0; x < MAP_W; x++) {
            map[0][x] = 2;
            map[MAP_H - 1][x] = 2;
        }
        for (int y = 0; y < MAP_H; y++) {
            map[y][0] = 2;
            map[y][MAP_W - 1] = 2;
        }

        for (int y = 2; y < MAP_H - 2; y++) {
            for (int x = 2; x < MAP_W - 2; x++) {
                if (random.nextFloat() < 0.15f) {
                    map[y][x] = 1;
                }
            }
        }

        for (int y = MAP_H - 3; y < MAP_H - 1; y++) {
            for (int x = 1; x < 5; x++) {
                map[y][x] = 0;
            }
        }
    }

    private void gameLoop(ActionEvent ignored) {
        if (!gameOver) {
            handleInput();
            updateEnemies();
            updateBullets();
            maybeSpawnEnemy();
        }
        repaint();
    }

    private void handleInput() {
        int dx = 0;
        int dy = 0;
        Direction dir = player.direction;

        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) {
            dy = -2;
            dir = Direction.UP;
        } else if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]) {
            dy = 2;
            dir = Direction.DOWN;
        } else if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) {
            dx = -2;
            dir = Direction.LEFT;
        } else if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) {
            dx = 2;
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
                enemy.aiTimer = 20 + random.nextInt(40);
                enemy.direction = Direction.values()[random.nextInt(4)];
                if (random.nextFloat() < 0.25f) shoot(enemy);
            }

            int dx = 0;
            int dy = 0;
            switch (enemy.direction) {
                case UP -> dy = -1;
                case DOWN -> dy = 1;
                case LEFT -> dx = -1;
                case RIGHT -> dx = 1;
            }

            boolean moved = moveTank(enemy, dx, dy);
            if (!moved) {
                enemy.direction = directionToPlayer(enemy);
                if (!moveTank(enemy, dx, dy)) {
                    enemy.direction = Direction.values()[random.nextInt(4)];
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
            if (map[ty][tx] == 1) {
                map[ty][tx] = 0;
                it.remove();
                continue;
            }
            if (map[ty][tx] == 2) {
                it.remove();
                continue;
            }

            if (hitTank(b, player)) {
                if (!b.fromPlayer) {
                    lives--;
                    if (lives <= 0) gameOver = true;
                    player.x = 2 * TILE_SIZE;
                    player.y = (MAP_H - 2) * TILE_SIZE;
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
                        score += 100;
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
        if (enemySpawnTimer >= 180 && enemies.size() < 6) {
            enemySpawnTimer = 0;
            spawnEnemy();
        }
    }

    private void spawnEnemy() {
        int[] lanes = {2, MAP_W / 2, MAP_W - 3};
        for (int i = 0; i < 10; i++) {
            int lane = lanes[random.nextInt(lanes.length)];
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

        if (Math.abs(enemyCx - playerCx) <= 8) {
            int x = enemyCx / TILE_SIZE;
            int y1 = Math.min(enemyCy, playerCy) / TILE_SIZE;
            int y2 = Math.max(enemyCy, playerCy) / TILE_SIZE;
            for (int y = y1; y <= y2; y++) {
                if (map[y][x] != 0) return false;
            }
            return true;
        }

        if (Math.abs(enemyCy - playerCy) <= 8) {
            int y = enemyCy / TILE_SIZE;
            int x1 = Math.min(enemyCx, playerCx) / TILE_SIZE;
            int x2 = Math.max(enemyCx, playerCx) / TILE_SIZE;
            for (int x = x1; x <= x2; x++) {
                if (map[y][x] != 0) return false;
            }
            return true;
        }

        return false;
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
                if (map[y][x] != 0) return false;
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
        int speed = 6;
        int dx = 0, dy = 0;
        switch (t.direction) {
            case UP -> dy = -speed;
            case DOWN -> dy = speed;
            case LEFT -> dx = -speed;
            case RIGHT -> dx = speed;
        }
        bullets.add(new Bullet(bx, by, dx, dy, t.isPlayer));
        t.reload = t.isPlayer ? 16 : 35;
    }

    private void resetGame() {
        score = 0;
        lives = 3;
        gameOver = false;
        bullets.clear();
        enemies.clear();
        initMap();
        player.x = 2 * TILE_SIZE;
        player.y = (MAP_H - 2) * TILE_SIZE;
        player.direction = Direction.UP;
        spawnEnemy();
        spawnEnemy();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                int cell = map[y][x];
                if (cell == 1) {
                    g2.setColor(new Color(178, 87, 34));
                    g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (cell == 2) {
                    g2.setColor(Color.GRAY);
                    g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        drawTank(g2, player, new Color(55, 180, 80));
        for (Tank e : enemies) drawTank(g2, e, new Color(180, 60, 60));

        g2.setColor(Color.YELLOW);
        for (Bullet b : bullets) {
            g2.fillOval(b.x - 3, b.y - 3, 6, 6);
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score + "   Lives: " + lives + "   R - restart", 12, 18);

        if (gameOver) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 42f));
            String text = "GAME OVER";
            int w = g2.getFontMetrics().stringWidth(text);
            g2.setColor(new Color(255, 40, 40));
            g2.drawString(text, (WIDTH - w) / 2, HEIGHT / 2);
        }

        if (player.reload > 0) player.reload--;
        for (Tank e : enemies) if (e.reload > 0) e.reload--;
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

    private enum Direction {UP, DOWN, LEFT, RIGHT}

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
