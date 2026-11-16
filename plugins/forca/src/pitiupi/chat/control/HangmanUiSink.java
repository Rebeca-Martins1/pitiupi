package pitiupi.chat.control;


public interface HangmanUiSink {

    void onChannelReady(String peerIp);

    void onChannelClosed(String peerIp);

    /** Estado atual da partida (mascara tipo "_ A _ A"). word só vem preenchida quando a partida acaba. */
    void onGameState(String peerIp, String masked, int errors, String guessedLetters,
                     HangmanGame.Status status, String word);

    void onSystemMessage(String peerIp, String text);
}
