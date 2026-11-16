package pitiupi.chat.control;

public interface HangmanHost {
    String getPluginIdentifier();

    void openConnection(String ip);
}
