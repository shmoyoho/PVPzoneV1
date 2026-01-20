package com.galyakyxnya.pvpzone.database;

import com.galyakyxnya.pvpzone.Main;

import java.sql.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class DatabaseManager {
    private final Main plugin;
    private Connection connection;
    private final ConcurrentLinkedQueue<Runnable> queryQueue = new ConcurrentLinkedQueue<>();
    private boolean isQueueRunning = false;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        initializeDatabase();
        startQueueProcessor();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/pvpzone.db";

            // Улучшенные параметры для уменьшения блокировок
            String connectionString = "jdbc:sqlite:" + dbPath +
                    "?journal_mode=WAL" +
                    "&synchronous=NORMAL" +
                    "&temp_store=MEMORY" +
                    "&mmap_size=268435456" + // 256MB memory mapping
                    "&cache_size=-2000" + // 2MB кэш
                    "&page_size=4096" +
                    "&locking_mode=NORMAL" +
                    "&foreign_keys=ON";

            connection = DriverManager.getConnection(connectionString);

            // Настройки через PRAGMA
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA temp_store = MEMORY");
                stmt.execute("PRAGMA mmap_size = 268435456");
                stmt.execute("PRAGMA cache_size = -2000");
                stmt.execute("PRAGMA page_size = 4096");
                stmt.execute("PRAGMA locking_mode = NORMAL");
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA busy_timeout = 3000"); // 3 секунды таймаут
            }

            createTables();
            plugin.getLogger().info("База данных SQLite подключена!");

        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка подключения к базе данных", e);
        }
    }

    private void startQueueProcessor() {
        // Запускаем обработчик очереди в отдельном потоке
        new Thread(() -> {
            while (plugin.isEnabled()) {
                try {
                    processQueue();
                    Thread.sleep(50); // Небольшая задержка между обработкой
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PvpZone-DB-Queue").start();
    }

    private void processQueue() {
        while (!queryQueue.isEmpty()) {
            Runnable task = queryQueue.poll();
            if (task != null) {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Ошибка выполнения задачи в очереди БД", e);
                }
            }
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Таблица игроков (упрощенная версия)
            String playersTable = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "rating INTEGER DEFAULT 0, " +
                    "points INTEGER DEFAULT 0" +
                    ")";

            // Таблица бонусов игроков
            String bonusesTable = "CREATE TABLE IF NOT EXISTS player_bonuses (" +
                    "player_uuid TEXT, " +
                    "bonus_id TEXT, " +
                    "level INTEGER DEFAULT 1, " +
                    "PRIMARY KEY (player_uuid, bonus_id)" +
                    ")";

            // Упрощенная таблица инвентарей (без внешних ключей для скорости)
            String inventoryTable = "CREATE TABLE IF NOT EXISTS player_inventory (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "inventory_data TEXT, " +
                    "armor_data TEXT" +
                    ")";

            stmt.execute(playersTable);
            stmt.execute(bonusesTable);
            stmt.execute(inventoryTable);

            // Создаем индексы
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_name ON players(name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_rating ON players(rating DESC)");

        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка переподключения к базе данных", e);
        }
        return connection;
    }

    // Асинхронные методы для записи
    public void savePlayerAsync(String uuid, String name, int rating, int points) {
        // Если плагин отключается, пропускаем асинхронные сохранения
        if (!plugin.isEnabled()) {
            return;
        }

        queryQueue.offer(() -> {
            try {
                savePlayerSync(uuid, name, rating, points);
            } catch (SQLException e) {
                // Если это не ошибка закрытия соединения, логируем
                if (!e.getMessage().contains("closed") && !e.getMessage().contains("interrupted")) {
                    plugin.getLogger().log(Level.WARNING, "Ошибка сохранения игрока: " + uuid, e);
                }
            }
        });
    }

    // Синхронный метод (для внутреннего использования)
    private void savePlayerSync(String uuid, String name, int rating, int points) throws SQLException {
        String sql = "INSERT OR REPLACE INTO players (uuid, name, rating, points) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, name != null ? name : "Unknown");
            stmt.setInt(3, rating);
            stmt.setInt(4, points);
            stmt.executeUpdate();
        }
    }

    // Синхронные методы для чтения (они быстрые и безопасные)
    public ResultSet getPlayerSync(String uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, uuid);
        return stmt.executeQuery();
    }

    public ResultSet getTopPlayersSync(int limit) throws SQLException {
        String sql = "SELECT * FROM players ORDER BY rating DESC LIMIT ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, limit);
        return stmt.executeQuery();
    }

    // Асинхронные методы для бонусов
    public void savePlayerBonusAsync(String playerUuid, String bonusId, int level) {
        queryQueue.offer(() -> {
            try {
                String sql = "INSERT OR REPLACE INTO player_bonuses (player_uuid, bonus_id, level) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid);
                    stmt.setString(2, bonusId);
                    stmt.setInt(3, level);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка сохранения бонуса: " + playerUuid + "/" + bonusId, e);
            }
        });
    }

    public ResultSet getPlayerBonusesSync(String playerUuid) throws SQLException {
        String sql = "SELECT bonus_id, level FROM player_bonuses WHERE player_uuid = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, playerUuid);
        return stmt.executeQuery();
    }

    // Асинхронные методы для инвентарей
    public void savePlayerInventoryAsync(String playerUuid, String inventoryData, String armorData) {
        if (inventoryData == null || armorData == null) return;

        queryQueue.offer(() -> {
            try {
                String sql = "INSERT OR REPLACE INTO player_inventory (player_uuid, inventory_data, armor_data) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid);
                    stmt.setString(2, inventoryData);
                    stmt.setString(3, armorData);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка сохранения инвентаря: " + playerUuid, e);
            }
        });
    }

    public ResultSet getPlayerInventorySync(String playerUuid) throws SQLException {
        String sql = "SELECT inventory_data, armor_data FROM player_inventory WHERE player_uuid = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, playerUuid);
        return stmt.executeQuery();
    }

    public void closeConnection() {
        try {
            // 1. Останавливаем обработку очереди
            isQueueRunning = false;

            // 2. Даем время на завершение текущих задач (но не слишком много)
            int attempts = 0;
            while (!queryQueue.isEmpty() && attempts < 50) { // Макс 2.5 секунды
                Thread.sleep(50);
                attempts++;
                processQueue(); // Пробуем обработать оставшиеся задачи
            }

            // 3. Если остались задачи - логируем
            if (!queryQueue.isEmpty()) {
                plugin.getLogger().warning("Осталось " + queryQueue.size() + " задач в очереди БД при отключении");
            }

            // 4. Очищаем очередь
            queryQueue.clear();

            // 5. Закрываем соединение если оно открыто
            if (connection != null && !connection.isClosed()) {
                try {
                    // Сначала пытаемся закрыть все открытые Statement'ы
                    try (Statement stmt = connection.createStatement()) {
                        // Пытаемся сделать checkpoint, но не критично если не получится
                        try {
                            stmt.execute("PRAGMA wal_checkpoint(PASSIVE)");
                        } catch (SQLException e) {
                            // Игнорируем ошибку checkpoint при отключении
                            plugin.getLogger().info("Checkpoint не выполнен (нормально при отключении)");
                        }
                    }
                } catch (SQLException e) {
                    // Игнорируем ошибки при закрытии Statement'ов
                } finally {
                    // Всегда пытаемся закрыть соединение
                    try {
                        connection.close();
                        plugin.getLogger().info("Соединение с базой данных закрыто");
                    } catch (SQLException e) {
                        // Если не удалось закрыть - это не критично при отключении
                        plugin.getLogger().warning("Не удалось корректно закрыть соединение с БД: " + e.getMessage());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("Поток обработки БД прерван при отключении");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка закрытия соединения с базой данных", e);
        }
    }
}