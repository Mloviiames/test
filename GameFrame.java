package 课设;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GameFrame extends JFrame {
    // 定义游戏中的各种图块类型，使用枚举表示
    private enum TileType {// 每个枚举代表一种图块，包含 ID、描述、颜色和是否是障碍物



        WALL(1, "墙", Color.GRAY, true), // 墙壁，不可穿越
        FLOOR(0, "地板", new Color(240, 240, 240), false), // 地板，可行走
        PLAYER(2, "玩家", Color.BLUE, false), // 玩家角色
        BOX(3, "箱子", Color.ORANGE, false), // 可推动的箱子
        TARGET(4, "目标点", Color.RED, false),// 箱子需要放置的目标位置
        BOX_ON_TARGET(5, "已放置的箱子", Color.GREEN, false),// 箱子已放在目标点
        PLAYER_ON_TARGET(6, "在目标点的玩家", Color.CYAN, false); // 玩家站在目标点

        // 图块的唯一标识符（ID），用于在地图数据中表示不同类型的图块
        final int id;
        // 图块的中文描述，用于调试或展示给用户（例如“墙”、“玩家”等）
        final String description;
        // 图块的显示颜色，用于绘制图形界面中的图块
        final Color color;
        // 指示该图块是否为障碍物，true 表示不可穿越（如墙壁），false 表示可行走或可推动经过
        final boolean isObstacle;


        TileType(int id, String description, Color color, boolean isObstacle) {
            this.id = id;//图块的唯一标识符（用于地图数据表示）
            this.description = description;//图块的中文描述（便于调试和展示）
            this.color = color;//图块在界面上显示的颜色
            this.isObstacle = isObstacle;//是否为障碍物（true 表示不可穿越，如墙）
        }

        static TileType fromId(int id) {//根据图块 ID 获取对应的枚举实例
            for (TileType type : values()) {//主要用于从地图数组中的整数转换为对应的图块类型
                if (type.id == id) return type;
            }
            return FLOOR;//@return 对应的 TileType 实例，如果找不到则返回 FLOOR（地板）
        }
    }

    // 游戏配置
    // 定义每个地图图块的绘制尺寸（像素），用于控制游戏地图中每个单元格的大小
    private static final int TILE_SIZE = 60;
    // 信息面板的高度（像素），用于设置下方显示关卡、步数等信息的面板区域高度
    private static final int INFO_PANEL_HEIGHT = 80;
    // 信息文本的字体样式，用于显示关卡、步数、剩余箱子等 UI 文字
    private static final Font INFO_FONT = new Font("微软雅黑", Font.BOLD, 16);

    // 游戏状态
    // 当前关卡编号（从 0 开始）
    private int currentLevel = 0;
    // 玩家已移动的步数
    private int moveCount = 0;
    // 当前地图数据，二维数组表示地图布局（每个单元格存储图块 ID）
    private int[][] currentMap;
    // 玩家在地图上的位置坐标（x, y）
    private Point playerPos = new Point();
    // 剩余未放置到目标点的箱子数量
    private int boxesLeft = 0;


    // 资源缓存
    // 用于缓存各种图块类型的图像资源，提高绘图效率（基于 TileType 枚举作为键）
    private Map<TileType, Image> tileImages = new HashMap<>();
    // 分别存储玩家、箱子和目标点的特殊图像资源
    private Image playerImg, boxImg, targetImg;


    // UI组件
    // 游戏主面板，用于承载和绘制游戏地图内容
    private JPanel gamePanel;
    // 关卡信息标签：显示当前关卡编号（例如“关卡: 1/3”）
    private JLabel levelLabel;
    // 步数统计标签：显示玩家已移动的步数
    private JLabel movesLabel;
    // 箱子数量标签：显示当前剩余未放置到目标点的箱子数量
    private JLabel boxesLabel;
    // 重玩按钮：点击后重新加载当前关卡
    private JButton restartButton;
    // 上一关按钮：点击后切换至上一关卡（如果存在）
    private JButton prevLevelButton;
    // 下一关按钮：点击后切换至下一关卡（如果存在）
    private JButton nextLevelButton;

    //构造函数，用于初始化游戏窗口和相关资源
    public SokobanGame() {
        initUI();// 初始化用户界面组件和布局
        loadResources();// 加载游戏所需的图像资源（如图块、图标等）
        loadLevel(currentLevel); // 加载初始关卡数据并设置玩家位置和箱子数
    }

    //初始化游戏主窗口的基本界面设置
    private void initUI() {
        setTitle("增强版推箱子游戏");// 设置窗口标题为“增强版推箱子游戏”
        // 设置关闭窗口时退出应用程序
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 设置主窗口使用 BorderLayout 布局管理器，便于组织面板和组件分布
        setLayout(new BorderLayout());

        // 游戏主面板
        // 创建游戏主面板，用于绘制和响应游戏地图的绘制请求
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // 调用父类方法以确保背景正确清除

                // 自定义绘制逻辑：调用 renderGame 方法绘制当前游戏状态
                renderGame(g);
            }
        };

        // 设置游戏主面板的背景颜色为白色，提升视觉清晰度
        gamePanel.setBackground(Color.WHITE);

        // 使面板可获得焦点，以便能够接收键盘事件
        gamePanel.setFocusable(true);

        // 添加键盘监听器，用于响应玩家按键操作（如方向键控制角色移动）
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 当有按键按下时，调用 handleKeyPress 方法处理逻辑
                handleKeyPress(e);
            }
        });

        // 将自定义的游戏主面板添加到窗口的中央区域
        add(gamePanel, BorderLayout.CENTER);

        // 信息面板
        // 创建信息面板，使用 GridLayout 布局排列关卡信息、步数、按钮等组件
        JPanel infoPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        // 设置面板边距为上下左右各 10 像素，提升界面美观性
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // 设置背景颜色为浅灰色，增强与主游戏区域的区分度
        infoPanel.setBackground(new Color(230, 230, 250));
        // 创建并初始化关卡显示标签，初始显示“关卡: 1”
        levelLabel = createInfoLabel("关卡: 1");
        // 创建并初始化关卡显示标签，初始显示“关卡: 1”
        movesLabel = createInfoLabel("步数: 0");
        // 创建并初始化剩余箱子数量标签，初始显示“剩余箱子: 0”
        boxesLabel = createInfoLabel("剩余箱子: 0");

        // 创建“重玩”按钮，并为其绑定点击事件：重新加载当前关卡
        restartButton = new JButton("重玩");
        restartButton.addActionListener(e -> loadLevel(currentLevel));
        // 创建“上一关”按钮，并为其绑定点击事件：
        // 如果当前不是第一关，则加载上一关
        prevLevelButton = new JButton("上一关");
        prevLevelButton.addActionListener(e -> {
            if (currentLevel > 0) loadLevel(currentLevel - 1);
        });
        // 创建“下一关”按钮，并为其绑定点击事件：
        // 如果当前不是最后一关，则加载下一关卡
        nextLevelButton = new JButton("下一关");
        nextLevelButton.addActionListener(e -> {
            if (currentLevel < getLevelCount() - 1) loadLevel(currentLevel + 1);
        });

        // 将各个 UI 组件依次添加到信息面板中，按 GridLayout 的顺序排列
        infoPanel.add(levelLabel);      // 添加关卡信息标签
        infoPanel.add(movesLabel);     // 添加步数信息标签
        infoPanel.add(boxesLabel);     // 添加剩余箱子数量标签
        infoPanel.add(restartButton);  // 添加“重玩”按钮
        infoPanel.add(prevLevelButton); // 添加“上一关”按钮
        infoPanel.add(nextLevelButton); // 添加“下一关”按钮
        // 将组装好的信息面板添加到窗口的南侧（下方）
        add(infoPanel, BorderLayout.SOUTH);

        // 设置窗口尺寸为 800x600 像素
        setSize(800, 600);

        // 将窗口居中显示在屏幕中央
        setLocationRelativeTo(null);
    }
    /**
     * 创建一个具有统一样式的信息标签（JLabel），用于显示关卡、步数、剩余箱子等信息
     *
     * @param text 标签上要显示的文本内容
     * @return 返回配置好的 JLabel 实例
     */
    private JLabel createInfoLabel(String text) {
        // 创建 JLabel 对象，并设置初始文本内容
        JLabel label = new JLabel(text);
        // 设置字体样式为预定义的 INFO_FONT（"微软雅黑", 粗体, 16号字）
        label.setFont(INFO_FONT);
        // 设置文本水平居中对齐，提升 UI 视觉美观性
        label.setHorizontalAlignment(SwingConstants.CENTER);
        // 返回配置完成的 JLabel 实例
        return label;
    }

    /**
     * 加载游戏中所需的所有图块资源（图像），包括基本图块和带图标图块。
     * 这些图像资源将被缓存到 tileImages 映射中，供游戏运行时绘制地图使用。
     */
    private void loadResources() {
        // 遍历所有 TileType 枚举值，创建基础图块图像
        for (TileType type : TileType.values()) {
            // 使用统一方法生成图块图像，并以类型为键存入缓存映射
            tileImages.put(type, createTileImage(type.color, type.description));
        }

        // 创建带有特殊图标的图块图像（玩家、箱子、目标点等）
        playerImg = createTileImageWithIcon(TileType.PLAYER.color, "P", TileType.PLAYER.description);
        boxImg = createTileImageWithIcon(TileType.BOX.color, "B", TileType.BOX.description);
        targetImg = createTileImageWithIcon(TileType.TARGET.color, "○", TileType.TARGET.description);

        // 替换默认图块图像为带图标的新图像
        tileImages.put(TileType.PLAYER, playerImg);         // 玩家图块替换为带“P”的图标
        tileImages.put(TileType.BOX, boxImg);               // 箱子图块替换为带“B”的图标
        tileImages.put(TileType.TARGET, targetImg);         // 目标点图块替换为带“○”的图标

        // 特殊状态图块：箱子在目标点上
        tileImages.put(TileType.BOX_ON_TARGET,
                createTileImageWithIcon(TileType.BOX_ON_TARGET.color, "B", TileType.BOX_ON_TARGET.description));

        // 特殊状态图块：玩家站在目标点上
        tileImages.put(TileType.PLAYER_ON_TARGET,
                createTileImageWithIcon(TileType.PLAYER_ON_TARGET.color, "P", TileType.PLAYER_ON_TARGET.description));
    }

    /**
     * 创建一个基本图块图像，仅包含纯色背景和黑色边框。
     *
     * @param bgColor 图块的背景颜色（用于表示不同类型的图块）
     * @param text    图块的描述文本（当前未使用，但可用于调试或未来扩展）
     * @return 返回生成的 BufferedImage 对象，表示一个基础图块图像
     */
    private Image createTileImage(Color bgColor, String text) {
        // 创建一个透明的图像对象，尺寸为 TILE_SIZE x TILE_SIZE 像素
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);

        // 获取图形上下文，用于绘制图像内容
        Graphics2D g = img.createGraphics();

        // 设置背景颜色并填充整个图块区域
        g.setColor(bgColor);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // 设置边框颜色为黑色，并绘制边框（留出1像素的边界）
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);

        // 释放图形资源，完成图像绘制
        g.dispose();

        // 返回创建好的图块图像
        return img;
    }

    /**
     * 创建一个带有图标的图块图像，包含背景颜色、边框和中心文字图标。
     *
     * @param bgColor 图块的背景颜色（用于区分不同类型）
     * @param icon    要绘制在图块中央的文字图标（如 "P" 表示玩家）
     * @param text    图块的描述文本（当前未使用，保留用于调试或未来扩展）
     * @return 返回生成的 BufferedImage 对象，表示一个带图标的图块图像
     */
    private Image createTileImageWithIcon(Color bgColor, String icon, String text) {
        // 创建透明图像，尺寸为 TILE_SIZE x TILE_SIZE 像素
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);

        // 获取图形上下文进行绘图操作
        Graphics2D g = img.createGraphics();

        // 绘制背景色
        g.setColor(bgColor);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);

        // 绘制黑色边框（略小于图块区域，留出1像素边距）
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);

        // 设置文字颜色为黑色，并定义字体样式
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));

        // 获取字体度量信息，用于计算文字居中位置
        FontMetrics fm = g.getFontMetrics();
        int x = (TILE_SIZE - fm.stringWidth(icon)) / 2; // 水平居中
        int y = (TILE_SIZE - fm.getHeight()) / 2 + fm.getAscent(); // 垂直居中

        // 在图块中心绘制图标文字
        g.drawString(icon, x, y);

        // 释放图形资源
        g.dispose();

        // 返回生成好的带图标图块图像
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
    /**
     * 获取游戏中的关卡总数。
     *
     * @return 返回关卡数组的长度，即总关卡数量
     */
    private int getLevelCount() {
        return getLevels().length;
    }

    /**
     * 加载指定关卡的地图数据，并初始化游戏状态。
     *
     * @param level 要加载的关卡索引（从 0 开始）
     */
    private void loadLevel(int level) {
        // 设置当前关卡编号
        currentLevel = level;

        // 重置步数和箱子计数
        moveCount = 0;
        boxesLeft = 0;

        // 获取指定关卡的地图数据（二维数组）
        int[][] levelData = getLevels()[level];

        // 创建新的地图数组，用于保存当前关卡的地图状态
        currentMap = new int[levelData.length][];

        // 遍历每一行地图数据并复制到 currentMap 中
        for (int i = 0; i < levelData.length; i++) {
            currentMap[i] = new int[levelData[i].length];
            System.arraycopy(levelData[i], 0, currentMap[i], 0, levelData[i].length);

            // 遍历当前行的所有图块，查找玩家和箱子的位置
            for (int j = 0; j < levelData[i].length; j++) {
                int tile = levelData[i][j];

                // 查找玩家位置（包括普通玩家和站在目标点上的玩家）
                if (tile == TileType.PLAYER.id || tile == TileType.PLAYER_ON_TARGET.id) {
                    playerPos.setLocation(j, i); // 设置玩家坐标
                }

                // 统计箱子数量（未放置在目标点的箱子）
                if (tile == TileType.BOX.id) {
                    boxesLeft++;
                }
            }
        }

        // 更新 UI 界面元素（如关卡、步数、按钮状态等）
        updateUI();

        // 让游戏面板重新获得键盘焦点，确保按键事件生效
        gamePanel.requestFocusInWindow();
    }

    /**
     * 更新用户界面中的信息标签和按钮状态。
     * 该方法在每次玩家移动后被调用，确保 UI 显示与当前游戏状态同步。
     */
    private void updateUI() {
        // 更新关卡信息标签，显示当前关卡编号及总关卡数
        levelLabel.setText("关卡: " + (currentLevel + 1) + "/" + getLevelCount());

        // 更新步数标签，显示玩家已移动的步数
        movesLabel.setText("步数: " + moveCount);

        // 更新剩余箱子标签，显示当前尚未放置到目标点的箱子数量
        boxesLabel.setText("剩余箱子: " + boxesLeft);

        // 如果当前不是第一关，则启用“上一关”按钮，否则禁用
        prevLevelButton.setEnabled(currentLevel > 0);

        // 如果当前不是最后一关，则启用“下一关”按钮，否则禁用
        nextLevelButton.setEnabled(currentLevel < getLevelCount() - 1);

        // 重绘游戏面板，使地图更新为最新状态
        gamePanel.repaint();
    }
    /**
     * 绘制游戏地图和辅助信息。
     * 该方法由 gamePanel 的 paintComponent 调用，用于渲染当前游戏状态。
     *
     * @param g 传入的 Graphics 对象，用于执行绘图操作
     */
    private void renderGame(Graphics g) {
        // 如果地图数据为空（未加载关卡），则直接返回，避免空指针异常
        if (currentMap == null) return;

        // 计算地图总宽度和高度（基于 TILE_SIZE）
        int mapWidth = currentMap[0].length * TILE_SIZE;
        int mapHeight = currentMap.length * TILE_SIZE;

        // 计算地图绘制的起始坐标，使地图居中显示在面板上
        int startX = (gamePanel.getWidth() - mapWidth) / 2;
        int startY = (gamePanel.getHeight() - mapHeight) / 2;

        // 双重循环遍历地图二维数组，逐个绘制图块
        for (int y = 0; y < currentMap.length; y++) {
            for (int x = 0; x < currentMap[y].length; x++) {
                // 根据当前单元格的 ID 获取对应的 TileType 枚举
                TileType tile = TileType.fromId(currentMap[y][x]);

                // 从缓存中获取对应的图块图像
                Image img = tileImages.get(tile);

                // 如果图像存在，则绘制到指定位置
                if (img != null) {
                    g.drawImage(img, startX + x * TILE_SIZE, startY + y * TILE_SIZE, null);
                }

                // 如果是目标点或其上的特殊状态（箱子/玩家），绘制白色半透明圆圈作为标记
                if (tile == TileType.TARGET || tile == TileType.BOX_ON_TARGET || tile == TileType.PLAYER_ON_TARGET) {
                    g.setColor(new Color(255, 255, 255, 100)); // 半透明白色
                    // 在目标点中心绘制一个圆形标记，增强视觉识别
                    g.fillOval(startX + x * TILE_SIZE + 10,
                            startY + y * TILE_SIZE + 10,
                            TILE_SIZE - 20,
                            TILE_SIZE - 20);
                }
            }
        }

        // 绘制帮助文本提示用户操作方式
        g.setColor(Color.BLACK); // 设置文字颜色为黑色
        g.setFont(new Font("微软雅黑", Font.PLAIN, 14)); // 设置字体样式和大小

        // 提示信息内容
        String helpText = "使用方向键或WASD移动，R重玩当前关卡";

        // 获取文本宽度，以便实现水平居中
        int textWidth = g.getFontMetrics().stringWidth(helpText);

        // 在窗口顶部居中绘制帮助信息
        g.drawString(helpText, (gamePanel.getWidth() - textWidth) / 2, 30);
    }

    /**
     * 处理键盘按键事件，控制玩家移动或执行其他操作（如重玩当前关卡）。
     *
     * @param e KeyEvent 传入的按键事件对象，包含按键信息
     */
    private void handleKeyPress(KeyEvent e) {
        boolean moved = false; // 标记是否发生移动
        int dx = 0, dy = 0;    // 移动方向增量，默认为不移动

        // 判断按下的是哪个键，并设置相应的移动方向
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:   // 上方向键
            case KeyEvent.VK_W:    // W 键
                dy = -1; // 向上移动
                break;
            case KeyEvent.VK_LEFT: // 左方向键
            case KeyEvent.VK_A:    // A 键
                dx = -1; // 向左移动
                break;
            case KeyEvent.VK_DOWN: // 下方向键
            case KeyEvent.VK_S:    // S 键
                dy = 1; // 向下移动
                break;
            case KeyEvent.VK_RIGHT:// 右方向键
            case KeyEvent.VK_D:    // D 键
                dx = 1; // 向右移动
                break;
            case KeyEvent.VK_R:    // R 键：重玩当前关卡
                loadLevel(currentLevel);
                return; // 直接返回，不再继续处理移动逻辑
        }

        // 如果是方向键或 WASD 导致的移动，则尝试移动玩家
        if (dx != 0 || dy != 0) {
            moved = tryMovePlayer(dx, dy); // 调用方法尝试移动玩家
        }

        // 如果移动成功
        if (moved) {
            moveCount++;       // 步数加一
            updateUI();        // 更新用户界面显示步数和剩余箱子数等信息

            // 检查是否胜利（所有箱子都已放置在目标点上）
            if (checkWin()) {
                showWinMessage(); // 显示胜利提示并处理是否进入下一关
            }
        }
    }

    /**
     * 尝试移动玩家，并处理与箱子的交互。
     * 该方法在 handleKeyPress 中被调用，用于实现游戏核心移动逻辑。
     *
     * @param dx 横向移动方向（-1 表示左移，1 表示右移）
     * @param dy 纵向移动方向（-1 表示上移，1 表示下移）
     * @return 如果成功移动返回 true，否则返回 false
     */
    private boolean tryMovePlayer(int dx, int dy) {
        // 计算新的玩家坐标
        int newX = playerPos.x + dx;
        int newY = playerPos.y + dy;

        // 检查是否越界（超出地图范围）
        if (newX < 0 || newY < 0 || newY >= currentMap.length || newX >= currentMap[newY].length) {
            return false;
        }

        // 获取目标位置的图块类型
        TileType targetTile = TileType.fromId(currentMap[newY][newX]);

        // 判断是否撞墙（不可穿越的障碍物）
        if (targetTile.isObstacle) {
            return false;
        }

        // 如果目标位置是箱子或“箱子在目标点”的状态，则尝试推动箱子
        if (targetTile == TileType.BOX || targetTile == TileType.BOX_ON_TARGET) {
            int boxNewX = newX + dx; // 箱子的新 X 坐标
            int boxNewY = newY + dy; // 箱子的新 Y 坐标

            // 检查箱子是否能被推动（新位置是否合法）
            if (boxNewX < 0 || boxNewY < 0 || boxNewY >= currentMap.length || boxNewX >= currentMap[boxNewY].length) {
                return false;
            }

            // 获取箱子要移动到的位置的图块类型
            TileType newBoxTile = TileType.fromId(currentMap[boxNewY][boxNewX]);

            // 如果目标位置是障碍物或者已经有另一个箱子，则不能推动
            if (newBoxTile.isObstacle || newBoxTile == TileType.BOX || newBoxTile == TileType.BOX_ON_TARGET) {
                return false;
            }

            // 移动箱子
            if (newBoxTile == TileType.TARGET) {
                // 如果箱子到达目标点，使用 BOX_ON_TARGET 图块类型表示
                currentMap[boxNewY][boxNewX] = TileType.BOX_ON_TARGET.id;
                boxesLeft--; // 剩余箱子数减一
            } else {
                // 否则放置普通箱子
                currentMap[boxNewY][boxNewX] = TileType.BOX.id;
            }

            // 更新原箱子所在位置的图块状态
            if (targetTile == TileType.BOX_ON_TARGET) {
                // 如果原位置是 BOX_ON_TARGET，恢复为 TARGET
                currentMap[newY][newX] = TileType.TARGET.id;
                boxesLeft++; // 因为箱子被移开，剩余箱子数加一
            } else {
                // 否则恢复为地板
                currentMap[newY][newX] = TileType.FLOOR.id;
            }
        }

        // 更新玩家当前位置的状态为地板或目标点
        TileType currentPlayerTile = TileType.fromId(currentMap[playerPos.y][playerPos.x]);
        if (currentPlayerTile == TileType.PLAYER_ON_TARGET) {
            // 如果玩家原本站在目标点上，则还原为目标点
            currentMap[playerPos.y][playerPos.x] = TileType.TARGET.id;
        } else {
            // 否则还原为地板
            currentMap[playerPos.y][playerPos.x] = TileType.FLOOR.id;
        }

        // 更新玩家坐标
        playerPos.translate(dx, dy);

        // 设置新位置的图块为玩家（或玩家在目标点的状态）
        TileType newPlayerTile = TileType.fromId(currentMap[playerPos.y][playerPos.x]);
        if (newPlayerTile == TileType.TARGET) {
            // 如果新位置是目标点，则使用 PLAYER_ON_TARGET 类型
            currentMap[playerPos.y][playerPos.x] = TileType.PLAYER_ON_TARGET.id;
        } else {
            // 否则使用普通玩家图块
            currentMap[playerPos.y][playerPos.x] = TileType.PLAYER.id;
        }

        // 返回 true 表示移动成功
        return true;
    }
    /**
     * 检查是否满足胜利条件。
     * 当所有箱子都已放置在目标点上时，即判定为胜利。
     *
     * @return 如果剩余未放置到目标点的箱子数量为 0，返回 true（胜利），否则返回 false
     */
    private boolean checkWin() {
        return boxesLeft == 0;// 只要没有剩余未放置的箱子，就视为胜利
    }

    /**
     * 显示玩家胜利的消息
     * 此方法根据当前关卡是否为最后一关来决定显示的消息内容以及是否提供进入下一关的选项
     */
    private void showWinMessage() {// 构造胜利消息，包含步数信息
        String message = "恭喜过关！\n步数: " + moveCount;// 如果当前关卡不是最后一关，则询问玩家是否进入下一关
        if (currentLevel < getLevelCount() - 1) {
            message += "\n是否进入下一关？";// 使用确认对话框，玩家可以选择“是”或“否”
            int option = JOptionPane.showConfirmDialog(this, message, "胜利",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);// 如果玩家选择“是”，则加载下一关
            if (option == JOptionPane.YES_OPTION) {
                loadLevel(currentLevel + 1);
            }
        } else {// 如果玩家已经通关所有关卡，则显示通关消息
            JOptionPane.showMessageDialog(this, message + "\n你已通关所有关卡！",
                    "胜利", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    /**
     * 程序的入口点 main 方法。
     * 用于启动 Swing 应用程序。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 设置UI风格（可选）
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 初始化必要目录
        initDataDirectories();

        // 启动登录界面
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }

    private static void initDataDirectories() {
        // 确保用户数据和记录目录存在
        new File("user_data").mkdirs();
        new File("game_records").mkdirs();

        // 打印调试信息（可选）
        System.out.println("数据目录初始化完成：" +
                new File("user_data").getAbsolutePath());
    }
}