package pitiupi.chat.control;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Set;


public class HangmanGame {

    public static final int MAX_ERRORS = 6;

    public enum Status { PLAYING, WON, LOST }

    private final String word;
    private final Set<Character> guessed = new LinkedHashSet<>();
    private int errors;

    public HangmanGame(String rawWord) {
        this.word = normalize(rawWord);
        if (word.isEmpty()) {
            throw new IllegalArgumentException("Palavra inválida");
        }
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
        return s.replaceAll("[^A-Z]", "");
    }

    /** @return true se a letra existe na palavra; letras repetidas/inválidas não contam erro. */
    public synchronized boolean guess(char letter) {
        char c = Character.toUpperCase(letter);
        if (c < 'A' || c > 'Z' || status() != Status.PLAYING || guessed.contains(c)) {
            return false;
        }
        guessed.add(c);
        if (word.indexOf(c) >= 0) {
            return true;
        }
        errors++;
        return false;
    }

    public synchronized String masked() {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            sb.append(guessed.contains(c) ? c : '_').append(' ');
        }
        return sb.toString().trim();
    }

    public synchronized Status status() {
        if (errors >= MAX_ERRORS) {
            return Status.LOST;
        }
        for (char c : word.toCharArray()) {
            if (!guessed.contains(c)) {
                return Status.PLAYING;
            }
        }
        return Status.WON;
    }

    public synchronized int getErrors() {
        return errors;
    }

    public synchronized String getGuessedLetters() {
        StringBuilder sb = new StringBuilder();
        for (char c : guessed) {
            sb.append(c);
        }
        return sb.toString();
    }

    public String getWord() {
        return word;
    }
}