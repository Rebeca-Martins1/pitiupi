package pitiupi.chat.net;

import pitiupi.models.Messages.Message;

public class HangmanMessage extends Message {

    public enum Type { START, GUESS, STATE }

    private Type type;
    private char letter;
    private String masked;
    private int errors;
    private String guessedLetters;
    private String status;   // PLAYING, WON, LOST
    private String word;     // só no fim da partida

    public HangmanMessage() {
    }

    public static HangmanMessage guess(char letter) {
        HangmanMessage m = new HangmanMessage();
        m.type = Type.GUESS;
        m.letter = letter;
        return m;
    }

    public static HangmanMessage state(Type type, String masked, int errors,
                                       String guessedLetters, String status, String word) {
        HangmanMessage m = new HangmanMessage();
        m.type = type;
        m.masked = masked;
        m.errors = errors;
        m.guessedLetters = guessedLetters;
        m.status = status;
        m.word = word;
        return m;
    }

    public Type getType() { return type; }
    public char getLetter() { return letter; }
    public String getMasked() { return masked; }
    public int getErrors() { return errors; }
    public String getGuessedLetters() { return guessedLetters; }
    public String getStatus() { return status; }
    public String getWord() { return word; }
}