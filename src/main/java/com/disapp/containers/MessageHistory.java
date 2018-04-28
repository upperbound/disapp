package com.disapp.containers;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.Stack;

public class MessageHistory {
    private IUser author;

    private IChannel channel;

    private Stack<MessageContainer> lastMessages;

    public MessageHistory(IUser author, IChannel channel) {
        this.author = author;
        this.channel = channel;
        lastMessages = new Stack<>();
    }

    public boolean repliedOnLastMessage() {
        if (!lastMessages.isEmpty() && lastMessages.peek().replied())
            return true;
        else
            lastMessages.clear();
        return false;
    }

    public MessageContainer peekMessage() {
        return !lastMessages.isEmpty() ? lastMessages.peek() : null;
    }

    public MessageHistory pushMessage(IMessage message) {
        return pushMessage(message, false);
    }

    public MessageHistory pushMessage(IMessage message, String lineName) {
        lastMessages.push(new MessageContainer(message, lineName));
        return this;
    }

    public MessageHistory pushMessage(IMessage message, boolean replied) {
        lastMessages.push(new MessageContainer(message, replied));
        return this;
    }

    public class MessageContainer {
        private IMessage message;

        private String lineName;

        private boolean replied;

        public MessageContainer(IMessage message, boolean replied) {
            this(message, null, replied);
        }

        public MessageContainer(IMessage message, String lineName) {
            this(message, lineName, lineName != null);
        }

        public MessageContainer(IMessage message, String lineName, boolean replied) {
            this.message = message;
            this.lineName = lineName;
            this.replied = replied;
        }

        public String getLineName() {
            return this.lineName;
        }

        public boolean replied() {
            return this.replied;
        }
    }
}
