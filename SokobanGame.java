package tom.jiafei;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.util.Date;
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
    private String currentUser;

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

    public SokobanGame(String username) {
        this.currentUser = username;
        initUI();
        loadResources();
        loadLevel(currentLevel);
    }

    private void initUI() {
        setTitle("增强版推箱子游戏 - 用户: " + currentUser);
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

        // 菜单栏
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("游戏");
        menuBar.add(gameMenu);

        JMenu userMenu = new JMenu("用户");
        JMenuItem historyItem = new JMenuItem("历史记录");
        historyItem.addActionListener(e -> showHistory());
        userMenu.add(historyItem);

        JMenuItem logoutItem = new JMenuItem("注销");
        logoutItem.addActionListener(e -> logout());
        userMenu.add(logoutItem);
        menuBar.add(userMenu);

        setJMenuBar(menuBar);

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
                // 第三关
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
        saveGameRecord();
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

    private void saveGameRecord() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("records.dat", true))) {
            pw.printf("%s|%d|%d|%d%n",
                    currentUser, currentLevel, moveCount, System.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showHistory() {
        StringBuilder records = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("records.dat"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts[0].equals(currentUser)) {
                    records.append(String.format("关卡 %d: %d 步 (%s)%n",
                            Integer.parseInt(parts[1]) + 1,
                            Integer.parseInt(parts[2]),
                            new Date(Long.parseLong(parts[3])).toString()));
                }
            }
        } catch (IOException e) {
            records.append("暂无历史记录");
        }

        JTextArea textArea = new JTextArea(records.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        JOptionPane.showMessageDialog(this, scrollPane,
                currentUser + "的游戏记录", JOptionPane.PLAIN_MESSAGE);
    }

    private void logout() {
        dispose();
        new LoginDialog().setVisible(true);
    }

    private static class LoginDialog extends JDialog {
        public LoginDialog() {
            super((JFrame)null, "用户登录", true);
            setSize(350, 250);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            initUI();
        }

        private void initUI() {
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("登录", createLoginPanel());
            tabbedPane.addTab("注册", createRegisterPanel());
            add(tabbedPane);
        }

        private JPanel createLoginPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();

            inputPanel.add(new JLabel("用户名:"));
            inputPanel.add(usernameField);
            inputPanel.add(new JLabel("密码:"));
            inputPanel.add(passwordField);
            panel.add(inputPanel, BorderLayout.CENTER);

            JButton loginBtn = new JButton("登录");
            loginBtn.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
                    return;
                }

                if (checkLogin(username, password)) {
                    dispose();
                    new SokobanGame(username).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "用户名或密码错误");
                }
            });

            JPanel btnPanel = new JPanel();
            btnPanel.add(loginBtn);
            panel.add(btnPanel, BorderLayout.SOUTH);

            return panel;
        }

        private JPanel createRegisterPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JPasswordField confirmField = new JPasswordField();

            inputPanel.add(new JLabel("用户名:"));
            inputPanel.add(usernameField);
            inputPanel.add(new JLabel("密码:"));
            inputPanel.add(passwordField);
            inputPanel.add(new JLabel("确认密码:"));
            inputPanel.add(confirmField);
            panel.add(inputPanel, BorderLayout.CENTER);

            JButton registerBtn = new JButton("注册");
            registerBtn.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String confirm = new String(confirmField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
                    return;
                }

                if (!password.equals(confirm)) {
                    JOptionPane.showMessageDialog(this, "两次输入的密码不一致");
                    return;
                }

                if (registerUser(username, password)) {
                    JOptionPane.showMessageDialog(this, "注册成功，请登录");
                    usernameField.setText("");
                    passwordField.setText("");
                    confirmField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "用户名已存在");
                }
            });

            JPanel btnPanel = new JPanel();
            btnPanel.add(registerBtn);
            panel.add(btnPanel, BorderLayout.SOUTH);

            return panel;
        }

        private boolean checkLogin(String username, String password) {
            try (BufferedReader br = new BufferedReader(new FileReader("users.dat"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts[0].equals(username) && parts[1].equals(hashPassword(password))) {
                        return true;
                    }
                }
            } catch (IOException e) {
                return false;
            }
            return false;
        }

        private boolean registerUser(String username, String password) {
            // 检查用户是否存在
            try (BufferedReader br = new BufferedReader(new FileReader("users.dat"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(username + ":")) {
                        return false;
                    }
                }
            } catch (IOException e) {
                // 文件不存在，继续注册
            }

            // 注册新用户
            try (PrintWriter pw = new PrintWriter(new FileWriter("users.dat", true))) {
                pw.println(username + ":" + hashPassword(password));
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                throw new RuntimeException("密码加密失败", e);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginDialog().setVisible(true);
        });
    }
}
