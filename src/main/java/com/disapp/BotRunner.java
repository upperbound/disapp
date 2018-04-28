package com.disapp;

import com.disapp.listeners.MessageHandler;
import com.disapp.utils.ClientManager;
import com.disapp.utils.Properties;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.DiscordException;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.disapp.utils.MessageUtils.Container;

public class BotRunner extends Application {
    private static final String TOKEN;

    private static final Logger logger = LoggerFactory.getLogger(BotRunner.class);

    private static final IDiscordClient client;

    static {
        java.util.Properties properties = new java.util.Properties();
        String file = "token.properties";
        try {
            logger.info(file + " is loading from classpath");
            properties.load(com.disapp.utils.Properties.class.getClassLoader().getResourceAsStream(file));
            logger.info(file + " loaded");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        TOKEN = properties.getProperty("TOKEN");
        client = ClientManager.getClient(TOKEN, com.disapp.utils.Properties.getBoolean("bot.login"));
    }

    public static void main(String args[]) {
        initialize();
        if (client == null) return;
        else if (!client.isLoggedIn())
            try {
                client.login();
            }
            catch (DiscordException e) {
                logger.error(e.getErrorMessage(), e);
                return;
            }
        int sleepSeconds = 0;
        while (!client.isReady())
            try {
                Thread.sleep(1000);
                sleepSeconds++;
                if (sleepSeconds > 10)
                    throw new InterruptedException("trying to login is timed out");
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                client.logout();
                return;
            }
        client.getDispatcher().registerListener(
                new MessageHandler(
                        client.getOurUser(),
                        (int) client.getChannels().stream().mapToLong(channel -> channel.getUsersHere().stream().filter(user -> !user.isBot()).count()).sum())
        );

        if (args.length == 1 && args[0].equals("-console"))
            consoleMode();
        else
            launch(args);
    }

    private static void consoleMode() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.next().equals("lg")) {
                logout();
                return;
            }
        }
    }

    private static void logout() {
        if (client != null)
            client.logout();
    }

    private static void initialize() {
        Reflections ref = new Reflections("com.disapp");
        for (Class<?> cl : ref.getTypesAnnotatedWith(InitClass.class)) {
            try {
                logger.info("initialize " + Class.forName(cl.getName()));
            } catch (ClassNotFoundException e) {
                logger.error("could not initialize class " + cl.getName(), e);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        assert client != null;

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GridPane grid = new GridPane();
        Scene scene = new Scene(grid, gd.getDisplayMode().getWidth()/3, gd.getDisplayMode().getHeight()/3);
        primaryStage.setTitle(com.disapp.utils.Properties.getString("gui.title"));
        InputStream icon = BotRunner.class.getResourceAsStream(Properties.getString("gui.title.icon"));
        if (icon != null)
            primaryStage.getIcons().add(new Image(icon));
        primaryStage.setScene(scene);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        final List<Pair<String, Long>> ch = new ArrayList<>();
        client.getChannels().stream().filter(
                channel -> channel.getModifiedPermissions(client.getOurUser()).contains(Permissions.SEND_MESSAGES)
        ).forEach(channel -> ch.add(new Pair<>(channel.getGuild().getName() + "@" + channel.getName(), channel.getLongID())));
        ChoiceBox<Pair<String, Long>> channels = new ChoiceBox<>(FXCollections.observableList(ch));
        channels.getSelectionModel().select(0);
        grid.add(channels, 1, 0);
        Button crap = new Button(Properties.getString("gui.button.crap_into_channel"));
        crap.setOnAction(event -> {
            client.getChannels().stream().filter(
                    c -> c.getLongID() == channels.getSelectionModel().getSelectedItem().getValue()
            ).forEach(c -> c.sendMessage(Container.getPhrase("crap")));
        });
        grid.add(crap, 0, 0);
        primaryStage.show();
    }

    public void stop() throws Exception {
        logout();
        super.stop();
    }
}
