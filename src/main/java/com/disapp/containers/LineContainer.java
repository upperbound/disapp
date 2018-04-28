package com.disapp.containers;

import com.disapp.utils.MessageUtils;

import java.util.List;
import java.util.Map;

public class LineContainer {
    private String line;

    private String lineLowerCase;

    private List<String> words;

    private List<String> wordsFormatted;

    private Map<String, Integer> wordsByCount;

    private Map<String, Integer> wordsFormattedByCount;

    public LineContainer(String line) {
        this.line = line;
        this.lineLowerCase = line.toLowerCase();
        this.words = MessageUtils.lineSplit(this.lineLowerCase, false);
        this.wordsFormatted = MessageUtils.lineSplit(this.lineLowerCase, true);
        this.wordsByCount = MessageUtils.wordsByCount(this.words);
        this.wordsFormattedByCount = MessageUtils.wordsByCount(this.wordsFormatted);
    }

    public LineContainer setLine(String line) {
        this.line = line;
        this.lineLowerCase = line.toLowerCase();
        this.words = MessageUtils.lineSplit(this.lineLowerCase, false);
        this.wordsFormatted = MessageUtils.lineSplit(this.lineLowerCase, true);
        this.wordsByCount = MessageUtils.wordsByCount(this.words);
        this.wordsFormattedByCount = MessageUtils.wordsByCount(this.wordsFormatted);

        return this;
    }

    public String getLine() {
        return line;
    }

    public String getLineLowerCase() {
        return lineLowerCase;
    }

    public List<String> getWords() {
        return words;
    }

    public List<String> getWordsFormatted() {
        return wordsFormatted;
    }

    public Map<String, Integer> getWordsByCount() {
        return wordsByCount;
    }

    public Map<String, Integer> getWordsFormattedByCount() {
        return wordsFormattedByCount;
    }
}
