package com.disapp;

import com.disapp.annotations.InitClass;
import com.disapp.listeners.MessageHandler;
import com.disapp.utils.ClientManager;
import com.disapp.utils.Properties;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
import java.io.*;
import java.util.*;
import java.util.List;

public class BotRunner extends Application {
    private static final Logger logger;

    private static final IDiscordClient client;

    static {
        logger = LoggerFactory.getLogger(BotRunner.class);
        java.util.Properties properties = new java.util.Properties();
        String file = "token.properties";
        try {
            logger.info(file + " is loading from classpath");
            properties.load(new InputStreamReader(Properties.class.getClassLoader().getResourceAsStream(file)));
            logger.info(file + " loaded");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        client = ClientManager.getClient(properties.getProperty("TOKEN"), false);
    }

    public static void main(String args[]) {
        initialize();
        if (client == null) {
            Platform.exit();
            return;
        }
        else if (!client.isLoggedIn())
            try {
                client.login();
            }
            catch (DiscordException e) {
                logger.error(e.getErrorMessage(), e);
                Platform.exit();
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

        if (args.length == 1 && args[0].equals("-console")) {
            Platform.exit();
            consoleMode();
        }
        else
            launch(args);
    }

    private static void consoleMode() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.next().equals("logout")) {
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
        primaryStage.setTitle(com.disapp.utils.Properties.getProperty("gui.title"));
        Image icon = getIcon(Properties.getProperty("gui.title.icon"));
        if (icon != null)
            primaryStage.getIcons().add(icon);
        primaryStage.setScene(scene);
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(20);

        final List<Pair<String, Long>> ch = new ArrayList<>();
        client.getChannels().stream().filter(
                channel -> channel.getModifiedPermissions(client.getOurUser()).contains(Permissions.SEND_MESSAGES)
        ).forEach(channel -> ch.add(new Pair<>(channel.getGuild().getName() + "@" + channel.getName(), channel.getLongID())));
        ChoiceBox<Pair<String, Long>> channels = new ChoiceBox<>(FXCollections.observableList(ch));
        channels.getSelectionModel().select(0);
        grid.add(channels, 1, 0);

        TextArea area = new TextArea();
        grid.add(area, 0, 1, 2, 4);
        final KeyCombination enter = new KeyCodeCombination(KeyCode.ENTER);
        final KeyCodeCombination shiftEnter = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);
        area.getScene().getAccelerators().put(
                shiftEnter,
                () -> {
                    area.setText(area.getText() + "\n");
                    area.end();
                });
        area.setOnKeyReleased(event -> {
            if (enter.match(event)) {
                client.getChannels().stream().filter(
                        c -> c.getLongID() == channels.getSelectionModel().getSelectedItem().getValue()
                ).forEach(c -> {
                    String text = area.getText();
                    if (!text.isEmpty())
                        c.sendMessage(text);
                    area.clear();
                });
            }
        });
        primaryStage.show();
    }

    private static Image getIcon(String name) {
        File file = new File(Properties.FileSystem.ICONS_DIRECTORY + Properties.FileSystem.DEFAULT_SEPARATOR + name);
        if (file.exists() && file.isFile()) {
            try {
                return new Image(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
        else {
            InputStream stream = BotRunner.class.getResourceAsStream(name);
            if (stream != null)
                return new Image(stream);
        }
        return null;
    }

    public void stop() throws Exception {
        logout();
        super.stop();
    }
}
