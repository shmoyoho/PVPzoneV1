package com.galyakyxnya.pvpzone.database;

import com.galyakyxnya.pvpzone.Main;

import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {
    private final Main plugin;
    private Connection connection;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/pvpzone.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            createTables();
            plugin.getLogger().info("База данных SQLite подключена!");

        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка подключения к базе данных", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Таблица игроков
            String playersTable = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "rating INTEGER DEFAULT 0, " +
                    "points INTEGER DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            // Таблица бонусов игроков
            String bonusesTable = "CREATE TABLE IF NOT EXISTS player_bonuses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_uuid TEXT, " +
                    "bonus_id TEXT, " +
                    "level INTEGER DEFAULT 1, " +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE, " +
                    "UNIQUE(player_uuid, bonus_id)" +
                    ")";

            // Таблица сохраненных инвентарей
            String inventoryTable = "CREATE TABLE IF NOT EXISTS player_inventory (" +
                    "player_uuid TEXT PRIMARY KEY, " +
                    "inventory_data TEXT, " +
                    "armor_data TEXT, " +
                    "saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE" +
                    ")";

            stmt.execute(playersTable);
            stmt.execute(bonusesTable);
            stmt.execute(inventoryTable);

            // Создаем индексы для ускорения поиска
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_name ON players(name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bonuses_player ON player_bonuses(player_uuid)");

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

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка закрытия соединения с базой данных", e);
        }
    }

    // Методы для работы с игроками
    public void savePlayer(String uuid, String name, int rating, int points) throws SQLException {
        String sql = "INSERT OR REPLACE INTO players (uuid, name, rating, points, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, name);
            stmt.setInt(3, rating);
            stmt.setInt(4, points);
            stmt.executeUpdate();
        }
    }

    public ResultSet getPlayer(String uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, uuid);
        return stmt.executeQuery();
    }

    public ResultSet getTopPlayers(int limit) throws SQLException {
        String sql = "SELECT * FROM players ORDER BY rating DESC LIMIT ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, limit);
        return stmt.executeQuery();
    }

    // Методы для работы с бонусами
    public void savePlayerBonus(String playerUuid, String bonusId, int level) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_bonuses (player_uuid, bonus_id, level) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, bonusId);
            stmt.setInt(3, level);
            stmt.executeUpdate();
        }
    }

    public ResultSet getPlayerBonuses(String playerUuid) throws SQLException {
        String sql = "SELECT bonus_id, level FROM player_bonuses WHERE player_uuid = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, playerUuid);
        return stmt.executeQuery();
    }

    public void removePlayerBonus(String playerUuid, String bonusId) throws SQLException {
        String sql = "DELETE FROM player_bonuses WHERE player_uuid = ? AND bonus_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, bonusId);
            stmt.executeUpdate();
        }
    }

    // Методы для работы с инвентарями
    public void savePlayerInventory(String playerUuid, String inventoryData, String armorData) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_inventory (player_uuid, inventory_data, armor_data, saved_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, inventoryData);
            stmt.setString(3, armorData);
            stmt.executeUpdate();
        }
    }

    public ResultSet getPlayerInventory(String playerUuid) throws SQLException {
        String sql = "SELECT inventory_data, armor_data FROM player_inventory WHERE player_uuid = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, playerUuid);
        return stmt.executeQuery();
    }

    public void removePlayerInventory(String playerUuid) throws SQLException {
        String sql = "DELETE FROM player_inventory WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.executeUpdate();
        }
    }

    // Метод для миграции данных из YAML в SQLite
    public void migrateFromYaml() {
        plugin.getLogger().info("Миграция данных из YAML в SQLite...");
        // Здесь можно добавить логику миграции старых данных
        plugin.getLogger().info("Миграция завершена!");
    }
}