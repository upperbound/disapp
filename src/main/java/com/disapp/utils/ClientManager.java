package com.disapp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

public class ClientManager {
    private static Logger logger = LoggerFactory.getLogger(ClientManager.class);

    private static final ClientBuilder builder = new ClientBuilder();

    public static IDiscordClient getClient(String token, boolean login) {
        builder.withToken(token);
        try {
            return login ? builder.login() : builder.build();
        }
        catch (DiscordException e) {
            logger.error(e.getErrorMessage(), e);
        }
        return null;
    }
}
