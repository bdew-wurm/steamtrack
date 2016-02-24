package net.bdew.wurm.common;

import com.wurmonline.server.DbConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModDb {
    private static final Logger logger = Logger.getLogger("bdew mod db");
    private static boolean initialized = false;
    private static Connection connection;

    private static final int SCHEMA_VER = 1;

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            connection = DbConnector.getLoginDbCon();

            try (ResultSet res = connection.getMetaData().getTables(null, null, "BDEW_SCHEMA", null)) {
                if (!res.next()) {
                    logger.info("Schema version table doesn't exist - creating");
                    try (PreparedStatement ps = connection.prepareStatement("CREATE TABLE BDEW_SCHEMA (" +
                            "Mod VARCHAR(100) NOT NULL," +
                            "Version INT NOT NULL," +
                            "PRIMARY KEY(Mod)" +
                            ")"
                    )) {
                        ps.execute();
                    }
                    setSchemaVer("*", SCHEMA_VER);
                }
            }
            int v = getSchemaVer("*");
            if (v != SCHEMA_VER)
                throw new RuntimeException(String.format("DB schema version unknown (%d), is this mod outdated?", v));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Error closing DB", e);
                    }
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getSchemaVer(String mod) throws SQLException {
        PreparedStatement st = connection.prepareStatement("SELECT Version FROM BDEW_SCHEMA WHERE Mod = ?");
        st.setString(1, mod);
        try (ResultSet res = st.executeQuery()) {
            if (res.next())
                return res.getInt(1);
            else
                return -1;
        }
    }

    public static void setSchemaVer(String mod, int version) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement("INSERT OR REPLACE INTO BDEW_SCHEMA (Mod, Version) VALUES (?,?)")) {
            st.setString(1, mod);
            st.setInt(2, version);
            st.execute();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}