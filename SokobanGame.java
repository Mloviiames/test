package 课设;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class SokobanGame extends JFrame {
    // 游戏元素枚举
    private enum TileType {
        WALL(1, "墙", Color.GRAY, true),
        FLOOR(0, "地板", new Color(240, 240, 240), false),
        PLAYER(2, "玩家", Color.BLUE, false),
        BOX(3, "箱子", Color.ORANGE, false),
        TARGET(4, "目标点", Color.RED, false),
        BOX_ON_TARGET(5, "已放置的箱子", Color.GREEN, false),
        PLAYER_ON_TARGET(6, "在目标点的玩家", Color.CYAN, false);

        final int id;
        final String description;
        final Color color;
        final boolean isObstacle;

        TileType(int id, String description, Color color, boolean isObstacle) {
            this.id = id;
            this.description = description;
            this.color = color;
            this.isObstacle = isObstacle;
        }

        static TileType fromId(int id) {
            for (TileType type : values()) {
                if (type.id == id) return type;
            }
            return FLOOR;
        }
    }

    // 游戏配置
    private static final int TILE_SIZE = 60;
    private static final int INFO_PANEL_HEIGHT = 80;
    private static final Font INFO_FONT = new Font("微软雅黑", Font.BOLD, 16);

    // 游戏状态
    private int currentLevel = 0;
    private int moveCount = 0;
    private int[][] currentMap;
    private Point playerPos = new Point();
    private int boxesLeft = 0;

    // 资源缓存
    private Map<TileType, Image> tileImages = new HashMap<>();
    private Image playerImg, boxImg, targetImg;

    // UI组件
    private JPanel gamePanel;
    private JLabel levelLabel;
    private JLabel movesLabel;
    private JLabel boxesLabel;
    private JButton restartButton;
    private JButton prevLevelButton;
    private JButton nextLevelButton;

    public SokobanGame() {
        initUI();
        loadResources();
        loadLevel(currentLevel);
    }

    private void initUI() {
        setTitle("增强版推箱子游戏");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 游戏主面板
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderGame(g);
            }
        };
        gamePanel.setBackground(Color.WHITE);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        add(gamePanel, BorderLayout.CENTER);

        // 信息面板
        JPanel infoPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoPanel.setBackground(new Color(230, 230, 250));

        levelLabel = createInfoLabel("关卡: 1");
        movesLabel = createInfoLabel("步数: 0");
        boxesLabel = createInfoLabel("剩余箱子: 0");

        restartButton = new JButton("重玩");
        restartButton.addActionListener(e -> loadLevel(currentLevel));

        prevLevelButton = new JButton("上一关");
        prevLevelButton.addActionListener(e -> {
            if (currentLevel > 0) loadLevel(currentLevel - 1);
        });

        nextLevelButton = new JButton("下一关");
        nextLevelButton.addActionListener(e -> {
            if (currentLevel < getLevelCount() - 1) loadLevel(currentLevel + 1);
        });

        infoPanel.add(levelLabel);
        infoPanel.add(movesLabel);
        infoPanel.add(boxesLabel);
        infoPanel.add(restartButton);
        infoPanel.add(prevLevelButton);
        infoPanel.add(nextLevelButton);

        add(infoPanel, BorderLayout.SOUTH);

        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(INFO_FONT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private void loadResources() {
        // 创建基本图块
        for (TileType type : TileType.values()) {
            tileImages.put(type, createTileImage(type.color, type.description));
        }

        // 特殊图块（带图标）
        playerImg = createTileImageWithIcon(TileType.PLAYER.color, "P", TileType.PLAYER.description);
        boxImg = createTileImageWithIcon(TileType.BOX.color, "B", TileType.BOX.description);
        targetImg = createTileImageWithIcon(TileType.TARGET.color, "○", TileType.TARGET.description);

        // 替换默认图块
        tileImages.put(TileType.PLAYER, playerImg);
        tileImages.put(TileType.BOX, boxImg);
        tileImages.put(TileType.TARGET, targetImg);
        tileImages.put(TileType.BOX_ON_TARGET,
                createTileImageWithIcon(TileType.BOX_ON_TARGET.color, "B", TileType.BOX_ON_TARGET.description));
        tileImages.put(TileType.PLAYER_ON_TARGET,
                createTileImageWithIcon(TileType.PLAYER_ON_TARGET.color, "P", TileType.PLAYER_ON_TARGET.description));
    }

    private Image createTileImage(Color bgColor, String text) {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(bgColor);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // 边框
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, TILE_SIZE-1, TILE_SIZE-1);

        g.dispose();
        return img;
    }

    private Image createTileImageWithIcon(Color bgColor, String icon, String text) {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(bgColor);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // 边框
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, TILE_SIZE-1, TILE_SIZE-1);

        // 图标
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int x = (TILE_SIZE - fm.stringWidth(icon)) / 2;
        int y = (TILE_SIZE - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(icon, x, y);

        g.dispose();
        return img;
    }

    private int[][][] getLevels() {
        return new int[][][] {
                // 第一关
                {
                        {1, 1, 1, 1, 1, 1, 1},
                        {1, 0, 0, 0, 0, 0, 1},
                        {1, 0, 4, 3, 0, 0, 1},
                        {1, 0, 0, 0, 0, 2, 1},
                        {1, 1, 1, 1, 1, 1, 1}
                },
                // 第二关
                {
                        {1, 1, 1, 1, 1, 1, 1, 1},
                        {1, 0, 0, 0, 0, 0, 0, 1},
                        {1, 0, 4, 3, 0, 3, 4, 1},
                        {1, 0, 0, 0, 2, 0, 0, 1},
                        {1, 0, 4, 3, 0, 3, 4, 1},
                        {1, 0, 0, 0, 0, 0, 0, 1},
                        {1, 1, 1, 1, 1, 1, 1, 1}
                },
                // 第三关（新增）
                {
                        {1, 1, 1, 1, 1, 1, 1, 1, 1},
                        {1, 0, 0, 0, 0, 0, 0, 0, 1},
                        {1, 0, 4, 0, 3, 0, 4, 0, 1},
                        {1, 0, 0, 1, 1, 1, 0, 0, 1},
                        {1, 0, 3, 0, 2, 0, 3, 0, 1},
                        {1, 0, 4, 0, 3, 0, 4, 0, 1},
                        {1, 0, 0, 0, 0, 0, 0, 0, 1},
                        {1, 1, 1, 1, 1, 1, 1, 1, 1}
                }
        };
    }

    private int getLevelCount() {
        return getLevels().length;
    }

    private void loadLevel(int level) {
        currentLevel = level;
        moveCount = 0;
        boxesLeft = 0;

        int[][] levelData = getLevels()[level];
        currentMap = new int[levelData.length][];

        for (int i = 0; i < levelData.length; i++) {
            currentMap[i] = new int[levelData[i].length];
            System.arraycopy(levelData[i], 0, currentMap[i], 0, levelData[i].length);

            for (int j = 0; j < levelData[i].length; j++) {
                int tile = levelData[i][j];
                if (tile == TileType.PLAYER.id || tile == TileType.PLAYER_ON_TARGET.id) {
                    playerPos.setLocation(j, i);
                }
                if (tile == TileType.BOX.id) {
                    boxesLeft++;
                }
            }
        }

        updateUI();
        gamePanel.requestFocusInWindow();
    }

    private void updateUI() {
        levelLabel.setText("关卡: " + (currentLevel + 1) + "/" + getLevelCount());
        movesLabel.setText("步数: " + moveCount);
        boxesLabel.setText("剩余箱子: " + boxesLeft);

        prevLevelButton.setEnabled(currentLevel > 0);
        nextLevelButton.setEnabled(currentLevel < getLevelCount() - 1);

        gamePanel.repaint();
    }

    private void renderGame(Graphics g) {
        if (currentMap == null) return;

        int mapWidth = currentMap[0].length * TILE_SIZE;
        int mapHeight = currentMap.length * TILE_SIZE;
        int startX = (gamePanel.getWidth() - mapWidth) / 2;
        int startY = (gamePanel.getHeight() - mapHeight) / 2;

        // 绘制地图
        for (int y = 0; y < currentMap.length; y++) {
            for (int x = 0; x < currentMap[y].length; x++) {
                TileType tile = TileType.fromId(currentMap[y][x]);
                Image img = tileImages.get(tile);
                if (img != null) {
                    g.drawImage(img, startX + x * TILE_SIZE, startY + y * TILE_SIZE, null);
                }

                // 在目标点上添加特殊标记
                if (tile == TileType.TARGET || tile == TileType.BOX_ON_TARGET || tile == TileType.PLAYER_ON_TARGET) {
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillOval(startX + x * TILE_SIZE + 10, startY + y * TILE_SIZE + 10,
                            TILE_SIZE - 20, TILE_SIZE - 20);
                }
            }
        }

        // 绘制帮助文本
        g.setColor(Color.BLACK);
        g.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        String helpText = "使用方向键或WASD移动，R重玩当前关卡";
        int textWidth = g.getFontMetrics().stringWidth(helpText);
        g.drawString(helpText, (gamePanel.getWidth() - textWidth) / 2, 30);
    }

    private void handleKeyPress(KeyEvent e) {
        boolean moved = false;
        int dx = 0, dy = 0;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                dy = -1;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                dx = -1;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                dy = 1;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                dx = 1;
                break;
            case KeyEvent.VK_R:
                loadLevel(currentLevel);
                return;
        }

        if (dx != 0 || dy != 0) {
            moved = tryMovePlayer(dx, dy);
        }

        if (moved) {
            moveCount++;
            updateUI();

            if (checkWin()) {
                showWinMessage();
            }
        }
    }

    private boolean tryMovePlayer(int dx, int dy) {
        int newX = playerPos.x + dx;
        int newY = playerPos.y + dy;

        // 检查边界
        if (newX < 0 || newY < 0 || newY >= currentMap.length || newX >= currentMap[newY].length) {
            return false;
        }

        TileType targetTile = TileType.fromId(currentMap[newY][newX]);

        // 检查是否撞墙
        if (targetTile.isObstacle) {
            return false;
        }

        // 检查是否推动箱子
        if (targetTile == TileType.BOX || targetTile == TileType.BOX_ON_TARGET) {
            int boxNewX = newX + dx;
            int boxNewY = newY + dy;

            // 检查箱子是否可以推动
            if (boxNewX < 0 || boxNewY < 0 || boxNewY >= currentMap.length || boxNewX >= currentMap[boxNewY].length) {
                return false;
            }

            TileType newBoxTile = TileType.fromId(currentMap[boxNewY][boxNewX]);
            if (newBoxTile.isObstacle || newBoxTile == TileType.BOX || newBoxTile == TileType.BOX_ON_TARGET) {
                return false;
            }

            // 移动箱子
            if (newBoxTile == TileType.TARGET) {
                currentMap[boxNewY][boxNewX] = TileType.BOX_ON_TARGET.id;
                boxesLeft--;
            } else {
                currentMap[boxNewY][boxNewX] = TileType.BOX.id;
            }

            // 更新原箱子位置
            if (targetTile == TileType.BOX_ON_TARGET) {
                currentMap[newY][newX] = TileType.TARGET.id;
                boxesLeft++;
            } else {
                currentMap[newY][newX] = TileType.FLOOR.id;
            }
        }

        // 更新玩家位置
        TileType currentPlayerTile = TileType.fromId(currentMap[playerPos.y][playerPos.x]);
        if (currentPlayerTile == TileType.PLAYER_ON_TARGET) {
            currentMap[playerPos.y][playerPos.x] = TileType.TARGET.id;
        } else {
            currentMap[playerPos.y][playerPos.x] = TileType.FLOOR.id;
        }

        playerPos.translate(dx, dy);

        TileType newPlayerTile = TileType.fromId(currentMap[playerPos.y][playerPos.x]);
        if (newPlayerTile == TileType.TARGET) {
            currentMap[playerPos.y][playerPos.x] = TileType.PLAYER_ON_TARGET.id;
        } else {
            currentMap[playerPos.y][playerPos.x] = TileType.PLAYER.id;
        }

        return true;
    }

    private boolean checkWin() {
        return boxesLeft == 0;
    }

    private void showWinMessage() {
        String message = "恭喜过关！\n步数: " + moveCount;
        if (currentLevel < getLevelCount() - 1) {
            message += "\n是否进入下一关？";
            int option = JOptionPane.showConfirmDialog(this, message, "胜利",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (option == JOptionPane.YES_OPTION) {
                loadLevel(currentLevel + 1);
            }
        } else {
            JOptionPane.showMessageDialog(this, message + "\n你已通关所有关卡！",
                    "胜利", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SokobanGame game = new SokobanGame();
            game.setVisible(true);
        });
    }
}