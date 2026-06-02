import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Random;

/** Карта боя: клетки, стены, спавны, разрушение кирпичей и коллизии с блоками. */
public class GameMap {
    // Сетка карты: 0 — пусто, -1 — сталь, 1..3 — прочность кирпича.
    private final int[][] cells = new int[GameConfig.MAP_H][GameConfig.MAP_W];
    // Физические и визуальные границы оставшейся части кирпича внутри клетки.
    private final Rectangle[][] brickBounds = new Rectangle[GameConfig.MAP_H][GameConfig.MAP_W];
    private final Random random;

    GameMap(Random random) {
        this.random = random;
    }

    void init() {
        for (int y = 0; y < GameConfig.MAP_H; y++) {
            for (int x = 0; x < GameConfig.MAP_W; x++) {
                setEmptyCell(x, y);
            }
        }

        for (int x = 0; x < GameConfig.MAP_W; x++) {
            setSteelCell(x, 0);
            setSteelCell(x, GameConfig.MAP_H - 1);
        }
        for (int y = 0; y < GameConfig.MAP_H; y++) {
            setSteelCell(0, y);
            setSteelCell(GameConfig.MAP_W - 1, y);
        }

        for (int y = 2; y < GameConfig.MAP_H - 2; y++) {
            for (int x = 2; x < GameConfig.MAP_W - 2; x++) {
                if (isReservedSpawnAreaOrExit(x, y)) {
                    continue;
                }

                float roll = random.nextFloat();
                if (roll < GameConfig.STEEL_SPAWN_CHANCE) {
                    setSteelCell(x, y);
                } else if (roll < GameConfig.STEEL_SPAWN_CHANCE + GameConfig.BRICK_SPAWN_CHANCE) {
                    setBrickCell(x, y);
                }
            }
        }

        clearEnemySpawnAreasAndExits();
        buildEnemySpawnWalls();
        buildPlayerSpawnFort(GameConfig.PLAYER_START_TILE_X, GameConfig.PLAYER_START_TILE_Y);
        buildPlayerSpawnFort(GameConfig.PLAYER_TWO_START_TILE_X, GameConfig.PLAYER_TWO_START_TILE_Y);
    }

    void draw(Graphics2D g2) {
        for (int y = 0; y < GameConfig.MAP_H; y++) {
            for (int x = 0; x < GameConfig.MAP_W; x++) {
                int cell = cells[y][x];
                if (cell > GameConfig.CELL_EMPTY) {
                    drawBrick(g2, x, y);
                } else if (cell == GameConfig.CELL_STEEL) {
                    g2.setColor(GameConfig.STEEL_COLOR);
                    g2.fillRect(x * GameConfig.TILE_SIZE, y * GameConfig.TILE_SIZE,
                            GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
                }
            }
        }
    }

    boolean blocks(Rectangle target) {
        int left = target.x / GameConfig.TILE_SIZE;
        int right = (target.x + target.width - 1) / GameConfig.TILE_SIZE;
        int top = target.y / GameConfig.TILE_SIZE;
        int bottom = (target.y + target.height - 1) / GameConfig.TILE_SIZE;

        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if (cells[y][x] == GameConfig.CELL_STEEL) return true;
                if (cells[y][x] > GameConfig.CELL_EMPTY && brickIntersects(x, y, target)) return true;
            }
        }
        return false;
    }

    boolean isInsideTankArea(Rectangle target) {
        return target.x >= GameConfig.TILE_SIZE
                && target.y >= GameConfig.TILE_SIZE
                && target.x + target.width < GameConfig.WIDTH - GameConfig.TILE_SIZE
                && target.y + target.height < GameConfig.HEIGHT - GameConfig.TILE_SIZE;
    }

    boolean hitWall(Bullet bullet) {
        if (bullet.x < 0 || bullet.y < 0 || bullet.x >= GameConfig.WIDTH || bullet.y >= GameConfig.HEIGHT) {
            return true;
        }

        int tx = bullet.x / GameConfig.TILE_SIZE;
        int ty = bullet.y / GameConfig.TILE_SIZE;
        if (cells[ty][tx] > GameConfig.CELL_EMPTY && brickContainsPoint(tx, ty, bullet.x, bullet.y)) {
            damageBrick(tx, ty, bullet);
            return true;
        }
        return cells[ty][tx] == GameConfig.CELL_STEEL;
    }

    boolean wallContainsPoint(int worldX, int worldY) {
        if (worldX < 0 || worldY < 0 || worldX >= GameConfig.WIDTH || worldY >= GameConfig.HEIGHT) return true;

        int tileX = worldX / GameConfig.TILE_SIZE;
        int tileY = worldY / GameConfig.TILE_SIZE;
        if (cells[tileY][tileX] == GameConfig.CELL_STEEL) return true;
        return cells[tileY][tileX] > GameConfig.CELL_EMPTY && brickContainsPoint(tileX, tileY, worldX, worldY);
    }

    static int tileToCenteredPixel(int tile, int size) {
        return tile * GameConfig.TILE_SIZE + (GameConfig.TILE_SIZE - size) / 2;
    }

    private void setEmptyCell(int x, int y) {
        if (!isInsideMap(x, y)) return;
        cells[y][x] = GameConfig.CELL_EMPTY;
        brickBounds[y][x] = null;
    }

    private void setSteelCell(int x, int y) {
        if (!isInsideMap(x, y)) return;
        cells[y][x] = GameConfig.CELL_STEEL;
        brickBounds[y][x] = null;
    }

    private void setBrickCell(int x, int y) {
        if (!isInsideMap(x, y)) return;
        cells[y][x] = GameConfig.BRICK_MAX_HEALTH;
        brickBounds[y][x] = new Rectangle(0, 0, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
    }

    private boolean isInsideMap(int x, int y) {
        return x >= 0 && y >= 0 && x < GameConfig.MAP_W && y < GameConfig.MAP_H;
    }

    private boolean isReservedSpawnAreaOrExit(int x, int y) {
        if (isPlayerSpawnOrExit(x, y, GameConfig.PLAYER_START_TILE_X, GameConfig.PLAYER_START_TILE_Y)) return true;
        if (isPlayerSpawnOrExit(x, y, GameConfig.PLAYER_TWO_START_TILE_X, GameConfig.PLAYER_TWO_START_TILE_Y)) return true;

        for (int lane : GameConfig.ENEMY_SPAWN_LANES) {
            if (Math.abs(x - lane) <= GameConfig.ENEMY_SPAWN_WALL_RADIUS
                    && y >= GameConfig.ENEMY_SPAWN_TILE_Y
                    && y <= GameConfig.ENEMY_SPAWN_TILE_Y + GameConfig.ENEMY_SPAWN_WALL_RADIUS + 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlayerSpawnOrExit(int x, int y, int spawnX, int spawnY) {
        boolean inFort = Math.abs(x - spawnX) <= GameConfig.PLAYER_SPAWN_WALL_RADIUS
                && Math.abs(y - spawnY) <= GameConfig.PLAYER_SPAWN_WALL_RADIUS;
        boolean inExit = x == spawnX && y >= spawnY - GameConfig.PLAYER_SPAWN_WALL_RADIUS - 1 && y <= spawnY;
        return inFort || inExit;
    }

    private void clearEnemySpawnAreasAndExits() {
        for (int lane : GameConfig.ENEMY_SPAWN_LANES) {
            for (int x = lane - GameConfig.ENEMY_SPAWN_WALL_RADIUS; x <= lane + GameConfig.ENEMY_SPAWN_WALL_RADIUS; x++) {
                setEmptyCell(x, GameConfig.ENEMY_SPAWN_TILE_Y);
            }
            setEmptyCell(lane, GameConfig.ENEMY_SPAWN_TILE_Y + 1);
            setEmptyCell(lane, GameConfig.ENEMY_SPAWN_TILE_Y + 2);
        }
    }

    private void buildEnemySpawnWalls() {
        int wallY = GameConfig.ENEMY_SPAWN_TILE_Y + 1;
        for (int lane : GameConfig.ENEMY_SPAWN_LANES) {
            for (int x = lane - GameConfig.ENEMY_SPAWN_WALL_RADIUS; x <= lane + GameConfig.ENEMY_SPAWN_WALL_RADIUS; x++) {
                if (x == lane) continue;
                setSteelCell(x, wallY);
            }
        }
    }

    private void buildPlayerSpawnFort(int spawnX, int spawnY) {
        for (int y = spawnY - GameConfig.PLAYER_SPAWN_WALL_RADIUS; y <= spawnY + GameConfig.PLAYER_SPAWN_WALL_RADIUS; y++) {
            for (int x = spawnX - GameConfig.PLAYER_SPAWN_WALL_RADIUS; x <= spawnX + GameConfig.PLAYER_SPAWN_WALL_RADIUS; x++) {
                if (!isInsideMap(x, y)) continue;

                boolean perimeter = Math.abs(x - spawnX) == GameConfig.PLAYER_SPAWN_WALL_RADIUS
                        || Math.abs(y - spawnY) == GameConfig.PLAYER_SPAWN_WALL_RADIUS;
                boolean exitGate = x == spawnX && y == spawnY - GameConfig.PLAYER_SPAWN_WALL_RADIUS;
                if (perimeter && !exitGate) {
                    setSteelCell(x, y);
                } else {
                    setEmptyCell(x, y);
                }
            }
        }
        setEmptyCell(spawnX, spawnY - GameConfig.PLAYER_SPAWN_WALL_RADIUS - 1);
    }

    private void drawBrick(Graphics2D g2, int tileX, int tileY) {
        Rectangle bounds = brickBounds[tileY][tileX];
        if (bounds == null) return;

        g2.setColor(GameConfig.BRICK_COLOR);
        g2.fillRect(tileX * GameConfig.TILE_SIZE + bounds.x, tileY * GameConfig.TILE_SIZE + bounds.y,
                bounds.width, bounds.height);
    }

    private void damageBrick(int tileX, int tileY, Bullet bullet) {
        cells[tileY][tileX]--;
        if (cells[tileY][tileX] <= GameConfig.CELL_EMPTY) {
            setEmptyCell(tileX, tileY);
            return;
        }

        Rectangle bounds = brickBounds[tileY][tileX];
        if (bounds == null) {
            bounds = new Rectangle(0, 0, GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
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
        int removed = Math.min(GameConfig.BRICK_DAMAGE_STEP, bounds.width);
        bounds.x += removed;
        bounds.width -= removed;
    }

    private void removeBrickFromRight(Rectangle bounds) {
        bounds.width = Math.max(0, bounds.width - GameConfig.BRICK_DAMAGE_STEP);
    }

    private void removeBrickFromTop(Rectangle bounds) {
        int removed = Math.min(GameConfig.BRICK_DAMAGE_STEP, bounds.height);
        bounds.y += removed;
        bounds.height -= removed;
    }

    private void removeBrickFromBottom(Rectangle bounds) {
        bounds.height = Math.max(0, bounds.height - GameConfig.BRICK_DAMAGE_STEP);
    }

    private boolean brickIntersects(int tileX, int tileY, Rectangle target) {
        Rectangle bounds = getBrickWorldBounds(tileX, tileY);
        return bounds != null && bounds.intersects(target);
    }

    private boolean brickContainsPoint(int tileX, int tileY, int worldX, int worldY) {
        Rectangle bounds = getBrickWorldBounds(tileX, tileY);
        return bounds != null && bounds.contains(worldX, worldY);
    }

    private Rectangle getBrickWorldBounds(int tileX, int tileY) {
        Rectangle bounds = brickBounds[tileY][tileX];
        if (bounds == null) return null;
        return new Rectangle(tileX * GameConfig.TILE_SIZE + bounds.x, tileY * GameConfig.TILE_SIZE + bounds.y,
                bounds.width, bounds.height);
    }
}
