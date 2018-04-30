package com.disapp.listeners;

import com.disapp.containers.MessageHistory;
import com.disapp.utils.*;
import com.disapp.utils.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.*;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.disapp.utils.MessageUtils.Container;

public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String PREFIX;

    private static final String PREFIX_TTS;

    private static final int DEFAULT_USERS_COUNT = 42;

    static {
        String pref = Properties.getProperty("bot.prefix.no_tts");
        PREFIX = pref == null ? "/bot" : pref;

        pref = Properties.getProperty("bot.prefix.tts");
        PREFIX_TTS = pref == null ? "/BOT" : pref;
    }

    private final IUser ourUser;

    private final ConcurrentHashMap<Integer, MessageHistory> userMessageHistory;

    public MessageHandler(IUser ourUser) {
        this(ourUser, DEFAULT_USERS_COUNT);
    }

    public MessageHandler(IUser ourUser, int visibleUsers) {
        this.ourUser = ourUser;
        this.userMessageHistory = new ConcurrentHashMap<>(visibleUsers);
    }

    @EventSubscriber
    public void onReactionAdd(ReactionAddEvent event) {
        IUser author = event.getUser();
        IChannel channel = event.getChannel();
        if (author.isBot() || event.getReaction().getUserReacted(ourUser))
            return;
        IUser messageAuthor = event.getMessage().getAuthor();
        if (messageAuthor.equals(ourUser))
            channel.sendMessage(author.mention() + " " + Container.getPhrase("take_a_crap"));
        ReactionEmoji emoji = event.getReaction().getEmoji();
        event.getMessage().addReaction(emoji);
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event){
        IUser author = event.getAuthor();
        IChannel channel = event.getChannel();
        IMessage message = event.getMessage();
        boolean tts = message.getContent().trim().startsWith(PREFIX_TTS);
        boolean startMention =
                message.getContent().trim().startsWith(PREFIX) ||
                        message.getContent().trim().startsWith(PREFIX_TTS) ||
                        message.getContent().trim().startsWith(ourUser.mention(false));
        if (author.isBot() ||
                (!startMention && !(!message.getMentions().isEmpty() && message.getMentions().contains(ourUser))) ||
                message.mentionsEveryone() ||
                message.mentionsHere())
            return;
        if (!startMention) {
            message.addReaction(ReactionEmoji.of(Container.getEmoji("poop")));
            return;
        }

        int key = Objects.hash(author.getLongID(), channel.getLongID());
        MessageHistory history = this.userMessageHistory.get(key);
        if (history == null)
            this.userMessageHistory.put(key, history = new MessageHistory(author, channel));
        String content = MessageUtils.format(message.getContent());
        content =
                content.startsWith(PREFIX) ?
                        content.substring(PREFIX.length()).replaceAll("\\s+", " ").trim() :
                        content.startsWith(ourUser.mention()) ?
                                content.substring(ourUser.mention().length()).replaceAll("\\s+", " ").trim() :
                                "";
        if (content.isEmpty()) {
            int r = new Random().nextInt(Container.getEmojiList().keySet().size()), i = 0;
            for (String emojiName : Container.getEmojiList().keySet())
                if (r == ++i) {
                    channel.sendMessage(Container.getEmoji(emojiName));
                    return;
                }
        }
        String command = Container.getPhrase(content);
        if (command != null) {
            channel.sendMessage(command);
            return;
        }

        String replyName = history.repliedOnLastMessage() ?
                Container.findReplyName(content, history.peekMessage().getLineName()) :
                Container.findReplyName(content);
        if (replyName != null) {
            history.pushMessage(message, replyName);
            channel.sendMessage(Container.getPhrase(replyName).replace(MessageUtils.Patterns.USER_NAME, author.mention()), tts);
            return;
        }
        history.pushMessage(message);
        channel.sendMessage(Properties.getPhrase("default.phrase"));
    }
}
