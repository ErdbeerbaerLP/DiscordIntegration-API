package de.erdbeerbaerlp.dcintegrationapi;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class Database implements AutoCloseable {
    public final StatusThread status;
    private Connection conn;

    public Database() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        connect();
        if (conn == null) {
            throw new SQLException();
        }
        status = new StatusThread();
        status.start();
        if (conn == null) {
            throw new SQLException();
        }
        runUpdate("""
                CREATE TABLE IF NOT EXISTS `links` (
                  `discordid` BIGINT NOT NULL,
                  `mcuuid` VARCHAR(32) NOT NULL,
                  PRIMARY KEY (`discordid`),
                  UNIQUE INDEX `discordid_UNIQUE` (`discordid` ASC) VISIBLE,
                  UNIQUE INDEX `mcuuid_UNIQUE` (`mcuuid` ASC) VISIBLE);""");
    }

    private void connect() throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://" + Config.instance().database.ip + ":" + Config.instance().database.port + "/" + Config.instance().database.dbName, Config.instance().database.username, Config.instance().database.password);
    }

    private boolean isConnected() {
        try {
            return conn.isValid(10);
        } catch (SQLException e) {
            return false;
        }
    }
    public void link(String dcid, String uuid) {
        runUpdate("INSERT INTO links (`discordid`,`mcuuid`) VALUES ('" + dcid + "', '" + uuid + "') ON DUPLICATE KEY UPDATE `mcuuid` =VALUES(`mcuuid`)");
    }

    public Link getLink(final Link input) {
        if((input.getDcID() != null) && (input.getUuid() != null) && (!input.getDcID().isEmpty() || !input.getUuid().isEmpty())) {
            try (final ResultSet res = query("SELECT `discordid`, `mcuuid` FROM links WHERE "+(input.getUuid().isEmpty()?"`discordid`":"`mcuuid`")+" = '" + (input.getUuid().isEmpty()?input.getDcID():input.getUuid())+"'")) {
                while (res != null && res.next()) {
                    if (res.wasNull())
                        return null;
                    return new Link(res.getString(2), res.getString(1));
                }

            } catch (SQLException e) {
                return null;
            }
        }
        return null;
    }
    public class StatusThread extends Thread {
        private boolean alive = true;

        public boolean isDBAlive() {
            return alive;
        }

        @Override
        public void run() {
            while (true) {
                alive = Database.this.isConnected();
                if (!alive) try {
                    System.err.println("Attempting Database reconnect...");
                    Database.this.connect();
                } catch (SQLException e) {
                    System.err.println("Failed to reconnect to database: " + e.getMessage());
                    try {
                        TimeUnit.SECONDS.sleep(15);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private void runUpdate(final String sql) {
        try (final Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ResultSet query(final String sql) {
        try {
            final Statement statement = conn.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }
}
