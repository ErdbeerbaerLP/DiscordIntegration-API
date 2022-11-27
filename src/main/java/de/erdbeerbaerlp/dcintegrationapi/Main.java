package de.erdbeerbaerlp.dcintegrationapi;

import com.google.gson.Gson;
import io.mokulu.discord.oauth.DiscordAPI;
import io.mokulu.discord.oauth.DiscordOAuth;
import io.mokulu.discord.oauth.model.TokensResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

@SpringBootApplication

@RestController
public class Main {
    private static DiscordOAuth oauthHandler;
    private static Database db;

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
        Config.instance().loadConfig();
        if (Config.instance().general.clientID.equals("0")) return;
        db = new Database();
        SpringApplication.run(Main.class, args);
        oauthHandler = new DiscordOAuth(Config.instance().general.clientID, Config.instance().general.clientSecret, Config.instance().general.redirURL, new String[]{"identify"});


    }


    @GetMapping(value = "/link", produces = "application/json")
    public Link getLink(@RequestBody final Link link) {
        return Objects.requireNonNullElse( db.getLink(link), link);
    }

    @GetMapping(value = "/login", produces = "text/html")
    public String login(@RequestParam(required = false) String code) {
        if (code == null) {
            return "<html><head><meta http-equiv=\"refresh\" content=\"0; url=" + oauthHandler.getAuthorizationURL("") + "\" /> </head></html>";
        } else
            try {
                final TokensResponse tokens = oauthHandler.getTokens(code);
                final DiscordAPI api = new DiscordAPI(tokens.getAccessToken());
                return "<html>\n"
                        + "<header><title>Discord Integration global linking</title></header>\n"
                        + "<body>\n"
                        + "Hello, " + api.fetchUser().getUsername() + "!\n"
                        + "<form action=\"save\" method=\"post\">\n" +
                        "  <input type=\"hidden\" id=\"token\" name=\"token\" value=\"" + tokens.getAccessToken() + "\"><br>\n" +
                        "  <label for=\"lname\">Enter your Minecraft Name or UUID here</label><br>\n" +
                        "  <input type=\"text\" id=\"mc\" name=\"mc\" value=\"\"><br><br>\n" +
                        "  <input type=\"submit\" value=\"Save\">\n" +
                        "</form> \n"
                        + "</body>\n"
                        + "</html>";

            } catch (IOException e) {
                return "Login failed";
            }
    }

    static final Gson gson = new Gson();

    @PostMapping(value = "/save", consumes = "application/x-www-form-urlencoded", produces = "text/html")
    public String getLink(@RequestParam String token, @RequestParam String mc) {

        try {
            if (token != null && !token.isEmpty() && mc != null && !mc.isEmpty()) {
                final DiscordAPI api = new DiscordAPI(token);
                final String id = api.fetchUser().getId();
                try {
                    final UUID uuid = UUID.fromString(mc);
                    db.link(id, uuid.toString().replace("-", ""));
                    return "Link successful! You can close this tab now.";
                } catch (IllegalArgumentException e) {
                    try {
                        final URL u = new URL("https://api.mojang.com/users/profiles/minecraft/" + mc);
                        final HttpsURLConnection urlConnection = (HttpsURLConnection) u.openConnection();
                        urlConnection.connect();
                        if (urlConnection.getResponseCode() == 204) return "Minecraft name not found!";
                        MCProfile p = gson.fromJson(new InputStreamReader(urlConnection.getInputStream()), MCProfile.class);
                        if (p.id != null) {
                            db.link(id, p.id);
                            return "Link successful! You can close this tab now.";
                        } else {
                            return "Link failed!";
                        }

                    } catch (IOException ex) {
                        return ex.getMessage();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return "Invalid Request";
    }

    @ExceptionHandler({Exception.class})
    @ResponseBody
    public String handleException(Exception e) {
        return "{\"message\": \"" + e.getMessage() + "\"}";
    }
}
