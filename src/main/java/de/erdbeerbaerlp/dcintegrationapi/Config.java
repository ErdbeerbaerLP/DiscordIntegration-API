package de.erdbeerbaerlp.dcintegrationapi;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;

public class Config {
    @TomlIgnore
    public static File configFile = new File("./api.toml");
    public static Config instance() {
        return INSTANCE;
    }

    @TomlIgnore
    private static Config INSTANCE;

    static {
        INSTANCE = new Config();
    }
    public void loadConfig() throws IOException, IllegalStateException {
        if (!configFile.exists()) {
            INSTANCE = new Config();
            INSTANCE.saveConfig();
            return;
        }
        INSTANCE = new Toml().read(configFile).to(Config.class);
        INSTANCE.saveConfig(); //Re-write the config so new values get added after updates
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode"})
    public void saveConfig() throws IOException {
        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        }
        final TomlWriter w = new TomlWriter.Builder()
                .indentValuesBy(2)
                .indentTablesBy(4)
                .padArrayDelimitersBy(2)
                .build();
        w.write(this, configFile);
    }


    @TomlComment("MySQL Database settings")
    public Database database = new Database();
    @TomlComment("Generic discord settings")
    public General general = new General();

    public static class Database {
        @TomlComment("Database IP")
        public String ip = "0.0.0.0";
        @TomlComment("Database Port")
        public int port = 3306;
        @TomlComment("Username")
        public String username = "dciapi";
        @TomlComment("Password")
        public String password = "password";
        @TomlComment("Database Name")
        public String dbName = "DCIGlobalLinking";
    }

    public static class General {
        @TomlComment("Discord client ID")
        public String clientID = "0";
        @TomlComment("Discord OAUTH2 Secret")
        public String clientSecret = "0";
        @TomlComment("Redirection URL for OAUTH2")
        public String redirURL = "https://api.erdbeerbaerlp.de/dcintegration/login";
    }
}
