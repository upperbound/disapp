package com.disapp.utils;

import com.disapp.annotations.InitClass;
import com.disapp.containers.LineContainer;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@InitClass
public class MessageUtils {
    private static final Logger logger = LoggerFactory.getLogger(MessageUtils.class);

    private enum Ranges {HUNDREDS, THOUSANDS, MILLIONS, BILLIONS, TRILLIONS}

    private static class ThreeChar {
        char h, d, u;
        Ranges range;
    }

    private MessageUtils() {}

    private static final char[] ESCAPING_CHARACTERS;

    static {
        int escCount = 0;
        for (Object key : Properties.Files.regexp.keySet()) {
            String sk = (String) key;
            if (sk.startsWith("esc."))
                escCount ++;
        }
        ESCAPING_CHARACTERS = new char[escCount];
        escCount = 0;
        for (Object key : Properties.Files.regexp.keySet()) {
            String sk = (String) key;
            if (sk.startsWith("esc."))
                ESCAPING_CHARACTERS[escCount++] = Properties.getRegexp(sk).charAt(0);
        }
    }

    @InitClass
    public static class Container {
        private static final ConcurrentSkipListMap<String, Pair<String, List<LineContainer>>> phrases;

        private static final ConcurrentSkipListMap<String, String> emoji;

        static {
            Properties.init();
            phrases = new ConcurrentSkipListMap<>();
            emoji = new ConcurrentSkipListMap<>();
            Properties.Files.emoji.keySet().forEach(key -> {
                String sk = (String) key;
                emoji.put(
                        sk.substring(sk.indexOf(".") + 1).toUpperCase(),
                        Properties.getEmoji(sk)
                );
            });
            Properties.Files.phrases.keySet().forEach(key -> {
                String sk = (String) key;
                if (sk.startsWith("file.")) {
                    File file = new File(Properties.FileSystem.SETTINGS_DIRECTORY + Properties.FileSystem.DEFAULT_SEPARATOR + Properties.getPhrase(sk));
                    try {
                        InputStream stream;
                        if(file.exists() && file.isFile())
                            stream = new FileInputStream(file);
                        else
                            stream = MessageUtils.class.getResourceAsStream(Properties.getPhrase(sk));
                        byte[] buffer = new byte[stream.available()];
                        int read = stream.read(buffer);
                        if (read > 0) {
                            String[] buf = new String(buffer).split(System.getProperty("line.separator"));
                            String name = sk.substring(sk.indexOf(".") + 1).toUpperCase();
                            for (int i = 0; i < buf.length - 1; i++) {
                                List<LineContainer> put = new CopyOnWriteArrayList<>();
                                String putValue = buf[i + 1].trim(), putLine = buf[i].trim();
                                if (putValue.isEmpty() && putLine.isEmpty())
                                    continue;
                                put.add(new LineContainer(putLine));
                                phrases.put(
                                        name +
                                                IntStream.range(0, String.valueOf(buf.length).length() - String.valueOf(i).length())
                                                        .mapToObj(z -> "0").collect(Collectors.joining()) +
                                                i,
                                        new Pair<>(putValue, put)
                                );
                            }
                        }
                    } catch (IOException e) {
                        MessageUtils.logger.error(e.getMessage(), e);
                    }
                }
                else if (sk.startsWith("phrase.") && !sk.matches(".*\\.map_\\d+$"))
                    phrases.put(
                            sk.substring(sk.indexOf(".") + 1).toUpperCase(),
                            new Pair<>(Properties.getPhrase(sk), new CopyOnWriteArrayList<>())
                    );
            });
            Properties.Files.phrases.keySet().forEach(key -> {
                String sk = (String) key;
                if (sk.matches("^phrase\\.\\w+\\.map_\\d+$")) {
                    String name = sk.substring(sk.indexOf(".") + 1);
                    phrases.get(name.substring(0, name.indexOf(".")).toUpperCase()).getValue().add(new LineContainer(Properties.getPhrase(sk)));
                }
            });
        }

        private Container() {}

        public static String getEmoji(String name) {
            return emoji.get(name.toUpperCase());
        }

        public static Map<String, String> getEmojiList() {
            return emoji;
        }

        public static String getPhrase(String name) {
            Pair<String, List<LineContainer>> value = phrases.get(name.toUpperCase());
            return value != null ? value.getKey() : null;
        }

        public static String findReplyName(String pattern) {
            return findReplyName(pattern, null);
        }

        public static String findReplyName(String pattern, String lineName) {
            Iterator<String> iterator = phrases.keySet().iterator();
            if (lineName != null) {
                while (iterator.hasNext())
                    if (iterator.next().equals(lineName))
                        break;
                if (!iterator.hasNext()) return null;
            }
            else
                lineName = phrases.keySet().first();
            String resultCaseSensitive = null;
            String resultNotSensitive = null;
            String resultRoughly = null;
            final String patternEscaped = MessageUtils.escapeCharacters(pattern);
            final String patternLower = patternEscaped.toLowerCase();
            final Map<String, Integer> patternWords = MessageUtils.wordsByCount(MessageUtils.lineSplit(pattern, true));
            boolean sr = patternWords.keySet().size() >= 3;
            for (String word : patternWords.keySet())
                sr |= word.length() >= 3;
            final boolean searchRoughly = sr;
            String curName = "";
            Pair<String, List<LineContainer>> current;
            iterator.next();
            while (!curName.equals(lineName)) {
                if (!iterator.hasNext()) {
                    if (resultNotSensitive != null || resultRoughly != null)
                        break;
                    iterator = phrases.keySet().iterator();
                }
                curName = iterator.next();
                current = phrases.get(curName);
                for (LineContainer line : current.getValue()) {
                    if (line.getLine().matches(patternEscaped)) {
                        resultCaseSensitive = curName;
                        break;
                    }
                    if (resultNotSensitive == null && line.getLineLowerCase().matches(patternLower)) {
                        resultNotSensitive = curName;
                    }
                    if (resultRoughly == null && searchRoughly && matchRoughly(line, patternWords)) {
                        resultRoughly = curName;
                    }
                }
                if (resultCaseSensitive != null)
                    break;
            }
            return resultCaseSensitive == null ?
                    resultNotSensitive == null ?
                            resultRoughly :
                            resultNotSensitive :
                    resultCaseSensitive;
        }

        private static boolean matchRoughly(LineContainer container, Map<String, Integer> patternWords) {
            return patternWords.keySet().stream().allMatch(word -> {
                Integer words = container.getWordsByCount().get(word);
                Integer wordsFormatted = container.getWordsFormattedByCount().get(word);
                Integer wordsPattern = patternWords.get(word);
                return (words != null && words >= wordsPattern) || (wordsFormatted != null && wordsFormatted >= wordsPattern);
            });
        }
    }

    @InitClass
    public static class Patterns {
        public static final String ONLY_NUMBERS = Properties.getRegexp("pattern.only_numbers");
        public static final String RUSSIAN_WORDS = Properties.getRegexp("pattern.russian_words");
        public static final String WHITESPACE_AND_PUNCTUATION = Properties.getRegexp("pattern.whitespace_and_punctuation");
        public static final String USER_NAME = "@USER_NAME";

        public static final String[] ENDINGS;

        public enum SEQUENCE_POLICY {EXACTLY, AT_LEAST, NOT_MORE}
        private static final String SEQUENCE_OF_SYMBOLS = Properties.getRegexp("pattern.sequence_of_symbols");
        private static final String SEQUENCE_OF_LETTERS = Properties.getRegexp("pattern.sequence_of_letters");

        static {
            int endingsCount = 0;
            for (Object key : Properties.Files.regexp.keySet()) {
                String sk = (String) key;
                if (sk.startsWith("pattern.endings."))
                    endingsCount ++;
            }
            ENDINGS = new String[endingsCount];
            endingsCount = 0;
            for (Object key : Properties.Files.regexp.keySet()) {
                String sk = (String) key;
                if (sk.startsWith("pattern.endings."))
                    ENDINGS[endingsCount++] = Properties.getRegexp(sk);
            }
        }

        private Patterns() {}

        public static String SEQUENCE_OF_SYMBOLS(int count) {
            return SEQUENCE_OF_LETTERS(count, SEQUENCE_POLICY.EXACTLY);
        }

        public static String SEQUENCE_OF_SYMBOLS(int count, SEQUENCE_POLICY policy) {
            assert count > 0;
            switch (policy){
                case AT_LEAST: return SEQUENCE_OF_SYMBOLS + "{" + (count - 1) + ",}";
                case NOT_MORE: return SEQUENCE_OF_SYMBOLS + "{0," + (count - 1) + "}";
            }
            return SEQUENCE_OF_SYMBOLS + "{" + (count - 1) + "}";
        }

        public static String SEQUENCE_OF_SYMBOLS(int start, int end) {
            assert start > 0;
            assert start <= end;
            return SEQUENCE_OF_SYMBOLS + "{" + (start - 1) + "," + (end - 1) + "}";
        }

        public static String SEQUENCE_OF_LETTERS(int count) {
            return SEQUENCE_OF_LETTERS(count, SEQUENCE_POLICY.EXACTLY);
        }

        public static String SEQUENCE_OF_LETTERS(int count, SEQUENCE_POLICY policy) {
            assert count > 0;
            switch (policy) {
                case AT_LEAST: return SEQUENCE_OF_LETTERS + "{" + (count - 1) + ",}";
                case NOT_MORE: return SEQUENCE_OF_LETTERS + "{0," + (count - 1) + "}";
            }
            return SEQUENCE_OF_LETTERS + "{" + (count - 1) + "}";
        }

        public static String SEQUENCE_OF_LETTERS(int start, int end) {
            assert start > 0;
            assert start <= end;
            return SEQUENCE_OF_LETTERS + "{" + (start - 1) + "," + (end - 1) + "}";
        }
    }

    public static boolean containsEscapingCharacter(char symbol) {
        for (char c : ESCAPING_CHARACTERS)
            if (c == symbol)
                return true;
        return false;
    }

    public static String removeEscapingCharacters(String pattern) {
        StringBuilder result = new StringBuilder();
        for (char symbol : pattern.toCharArray())
            if (!containsEscapingCharacter(symbol))
                result.append(symbol);
            else
                result.append(' ');
        return result.toString();
    }

    public static String escapeCharacters(String pattern) {
        StringBuilder result = new StringBuilder();
        for (char symbol : pattern.toCharArray()) {
            if (containsEscapingCharacter(symbol))
                result.append('\\').append(symbol);
            else
                result.append(symbol);
        }
        return result.toString();
    }

    public static String format(String message) {
        return message.toLowerCase()
                .replace('Ё', 'Е') //.replace((char) 1025, (char) 1045)
                .replace('ё', 'е') //.replace((char) 1105, (char) 1077)
                ;
    }

    public static Map<String, Integer> wordsByCount(List<String> words) {
        final TreeMap<String, Integer> result = new TreeMap<>();
        words.forEach(word -> {
            Integer i;
            if ((i = result.get(word)) != null)
                result.put(word, i + 1);
            else
                result.put(word, 1);
        });
        return result;
    }

    public static List<String> lineSplit(String line, boolean format) {
        String[] words = line.trim().split(Patterns.WHITESPACE_AND_PUNCTUATION);
        if (!format)
            return new ArrayList<>(Arrays.asList(words));

        final ArrayList<String> result = new ArrayList<>();
        for (String word : words) {
            if (word.matches(Patterns.ONLY_NUMBERS)) {
                try {
                    String number2Text = MessageUtils.number2Text(word.replace(',', '.'));
                    if (number2Text != null && !number2Text.isEmpty())
                        Collections.addAll(result, number2Text.split(""));
                }
                catch (NumberFormatException ignored) {
                    result.add(word);
                }
                continue;
            }
            word = word.replaceAll(Patterns.SEQUENCE_OF_LETTERS(3, Patterns.SEQUENCE_POLICY.AT_LEAST), "$1$1$1");
            for (String ending : Patterns.ENDINGS)
                if (word.matches(ending))
                    word = word.replace(ending, "");
            result.add(word);
        }
        return result;
    }

    public static String number2Text(Double number) {
        return number2Text(BigDecimal.valueOf(number));
    }

    public static String number2Text(Long number) {
        return number2Text(BigDecimal.valueOf(number));
    }

    public static String number2Text(Integer number) { return number2Text(BigDecimal.valueOf(number.longValue()));
    }

    public static String number2Text(String number) throws NumberFormatException {
        return number2Text(BigDecimal.valueOf(Double.valueOf(number.endsWith(".") ? number + "0" : number)));
    }

    private static String number2Text(BigDecimal number){
        Stack<ThreeChar> threeChars = new Stack<>();
        threeChars.push(new ThreeChar());
        threeChars.peek().range = Ranges.HUNDREDS;

        if(number == null || number.compareTo(BigDecimal.ZERO) < 0) return null;
        String s = number.toString();
        int floats = s.length() - (s.lastIndexOf('.') == - 1 ? s.length() : s.lastIndexOf('.'));
        if(floats == 2) s += '0';
        String[] sa = s.split("\\.");
        if(floats > 3 || sa[0].length() > 15) return null;
        StringBuilder sb = new StringBuilder(sa[0]).reverse();
        for(int i = 0; i < sb.length(); i++){
            if(i > 0 && i % 3 == 0){
                threeChars.push(new ThreeChar());
            }
            ThreeChar threeChar = threeChars.peek();
            switch(i){
                case 0:
                    threeChar.u = sb.charAt(i);
                    break;
                case 3:
                    threeChar.range = Ranges.THOUSANDS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 6:
                    threeChar.range = Ranges.MILLIONS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 9:
                    threeChar.range = Ranges.BILLIONS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 12:
                    threeChar.range = Ranges.TRILLIONS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 2:
                case 5:
                case 8:
                case 11:
                case 14:
                    threeChar.h = sb.charAt(i);
                    break;
                default:
                    threeChar.d = sb.charAt(i);
            }
        }
        StringBuilder result = new StringBuilder();
        while(!threeChars.isEmpty()){
            ThreeChar thch = threeChars.pop();
            if(thch.h > '0'){
                result.append(getHundreds(thch.h));
                result.append(' ');
            }
            if(thch.d > '0'){
                if(thch.d > '1' || thch.u == '0') result.append(getDecades(thch.d));
                else result.append(getTeens(thch.u));
                result.append(' ');
            }
            if(thch.u > '0' && thch.d != '1'){
                result.append(getUnits(thch.u, thch.range == Ranges.THOUSANDS));
                result.append(' ');
            }
            switch(thch.range){
                case TRILLIONS:
                    if(thch.d == '1' || thch.u == '0' || thch.u > '4') result.append("триллионов");
                    else if(thch.u > '1')result.append("триллиона");
                    else result.append("триллион");
                    result.append(' ');
                    break;
                case BILLIONS:
                    if(thch.d == '1' || thch.u == '0' || thch.u > '4') result.append("миллиардов");
                    else if(thch.u > '1')result.append("миллиарда");
                    else result.append("миллиард");
                    result.append(' ');
                    break;
                case MILLIONS:
                    if(thch.d == '1' || thch.u == '0' || thch.u > '4') result.append("миллионов");
                    else if(thch.u > '1')result.append("миллиона");
                    else result.append("миллион");
                    result.append(' ');
                    break;
                case THOUSANDS:
                    if(thch.d == '1' || thch.u == '0' || thch.u > '4') result.append("тысяч");
                    else if(thch.u > '1')result.append("тысячи");
                    else result.append("тысяча");
                    result.append(' ');
                    break;
                default:
                    result.append(' ');
            }
        }
        return result.toString().trim();
    }

    private static String getHundreds(char num){
        switch(num){
            case '1':
                return "сто";
            case '2':
                return "двести";
            case '3':
                return "триста";
            case '4':
                return "четыреста";
            case '5':
                return "пятьсот";
            case '6':
                return "шестьсот";
            case '7':
                return "семьсот";
            case '8':
                return "восемьсот";
            case '9':
                return "девятьсот";
            default: return null;
        }
    }

    private static String getDecades(char num){
        switch(num){
            case '1':
                return "десять";
            case '2':
                return "двадцать";
            case '3':
                return "тридцать";
            case '4':
                return "сорок";
            case '5':
                return "пятьдесят";
            case '6':
                return "шестьдесят";
            case '7':
                return "семьдесят";
            case '8':
                return "восемьдесят";
            case '9':
                return "девяносто";
            default: return null;
        }
    }

    private static String getUnits(char num, boolean female){
        switch(num){
            case '1':
                return female ? "одна" : "один";
            case '2':
                return female ? "две"  : "два";
            case '3':
                return "три";
            case '4':
                return "четыре";
            case '5':
                return "пять";
            case '6':
                return "шесть";
            case '7':
                return "семь";
            case '8':
                return "восемь";
            case '9':
                return "девять";
            default: return null;
        }
    }

    private static String getTeens(char num){
        String s = "";
        switch(num){
            case '1':
                s = "один"; break;
            case '2':
                s = "две"; break;
            case '3':
                s = "три"; break;
            case '4':
                s = "четыр"; break;
            case '5':
                s = "пят"; break;
            case '6':
                s = "шест"; break;
            case '7':
                s = "сем"; break;
            case '8':
                s = "восем"; break;
            case '9':
                s = "девят"; break;
        }
        return s + "надцать";
    }
}
