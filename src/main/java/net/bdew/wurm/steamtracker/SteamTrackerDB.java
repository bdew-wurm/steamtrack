package net.bdew.wurm.steamtracker;

import com.wurmonline.server.DbConnector;
import com.wurmonline.server.LoginHandler;
import net.bdew.wurm.common.ModDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SteamTrackerDB {
    private static Connection connection;

    public static class SteamBan {
        public final String GM;
        public final String reason;

        public SteamBan(String GM, String reason) {
            this.GM = GM;
            this.reason = reason;
        }
    }

    public static void init() throws SQLException {
        ModDb.init();
        connection = ModDb.getConnection();
        int version = ModDb.getSchemaVer("BDEW_STEAMTRACKER");
        if (version < 1) {
            SteamTracker.logger.info("Creating database tables");

            try (PreparedStatement st = connection.prepareStatement("CREATE TABLE BDEW_STEAMTRACKER_PLAYERS (" +
                    "WurmID LONG NOT NULL," +
                    "SteamID LONG NOT NULL," +
                    "PRIMARY KEY(WurmID)" +
                    ")")) {
                st.execute();
            }

            try (PreparedStatement st = connection.prepareStatement("CREATE TABLE BDEW_STEAMTRACKER_BANS (" +
                    "SteamID LONG NOT NULL," +
                    "GM VARCHAR(100) NOT NULL," +
                    "Reason VARCHAR(200) NOT NULL," +
                    "PRIMARY KEY(SteamID)" +
                    ")")) {
                st.execute();
            }

            ModDb.setSchemaVer("BDEW_STEAMTRACKER", 1);
        }
    }

    public static Optional<Long> getSteamIdForPlayer(long wurmId) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("SELECT SteamID from BDEW_STEAMTRACKER_PLAYERS WHERE WurmID = ?")) {
            st.setLong(1, wurmId);
            try (ResultSet res = st.executeQuery()) {
                if (res.next())
                    return Optional.of(res.getLong(1));
                else
                    return Optional.empty();
            }
        }
    }

    public static List<Long> getPlayersForSteamId(long steamId) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("SELECT WurmID FROM BDEW_STEAMTRACKER_PLAYERS WHERE SteamID = ?")) {
            st.setLong(1, steamId);
            List<Long> result = new ArrayList<>();
            try (ResultSet res = st.executeQuery()) {
                while (res.next())
                    result.add(res.getLong(1));
            }
            return result;
        }
    }

    public static void updateSteamIdForPlayer(long wurmId, long steamId) throws SQLException {
        Optional<Long> current = getSteamIdForPlayer(wurmId);
        if (current.isPresent() && current.get() != steamId)
            SteamTracker.logger.warning(String.format("Player %d changed steam ID from %d to %d", wurmId, current.get(), steamId));
        try (PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO BDEW_STEAMTRACKER_PLAYERS (WurmId, SteamId) VALUES (?,?)")) {
            st.setLong(1, wurmId);
            st.setLong(2, steamId);
            st.execute();
        }
    }

    public static Optional<String> getNameForPlayer(long wurmId) throws SQLException {
        Connection dbcon = DbConnector.getPlayerDbCon();
        try {
            try (PreparedStatement ps = dbcon.prepareStatement("SELECT NAME FROM PLAYERS WHERE WURMID=?")) {
                ps.setLong(1, wurmId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getString(1));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } finally {
            DbConnector.returnConnection(dbcon);
        }
    }

    public static Optional<Long> getPlayerId(String name) throws SQLException {
        Connection dbcon = DbConnector.getPlayerDbCon();
        try {
            try (PreparedStatement ps = dbcon.prepareStatement("SELECT WURMID FROM PLAYERS WHERE NAME=?")) {
                ps.setString(1, LoginHandler.raiseFirstLetter(name));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getLong(1));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } finally {
            DbConnector.returnConnection(dbcon);
        }
    }

    public static Optional<SteamBan> getSteamBan(long steamId) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("SELECT GM, Reason from BDEW_STEAMTRACKER_BANS WHERE SteamID = ?")) {
            st.setLong(1, steamId);
            try (ResultSet res = st.executeQuery()) {
                if (res.next())
                    return Optional.of(new SteamBan(res.getString("GM"), res.getString("Reason")));
                else
                    return Optional.empty();
            }
        }
    }

    public static void banSteamId(long steamId, String GM, String reason) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO BDEW_STEAMTRACKER_BANS (SteamID, GM, Reason) VALUES (?,?,?)")) {
            st.setLong(1, steamId);
            st.setString(2, GM);
            st.setString(3, reason);
            st.execute();
        }
    }

    public static void unbanSteamId(long steamId) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("DELETE FROM BDEW_STEAMTRACKER_BANS WHERE SteamID = ?")) {
            st.setLong(1, steamId);
            st.execute();
        }
    }

}
