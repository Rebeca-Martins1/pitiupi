package pitiupi.chat.control;

import pitiupi.chat.net.HangmanMessage;
import pitiupi.models.User;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.presentation.shell.PeerIpv4;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//Era o PrivateChatController

public class HangmanController {

    private final HangmanHost host;
    private final Map<String, TcpConnection> connections = new HashMap<>();
    private final Set<String> pendingConnections = new HashSet<>();
    private final Map<String, HangmanGame> gamesAsMaster = new HashMap<>();
    private HangmanUiSink uiSink;

    public HangmanController(HangmanHost host) {
        this.host = host;
    }

    public void setUiSink(HangmanUiSink uiSink) {
        this.uiSink = uiSink;
    }

    private static String addressKey(TcpConnection connection) {
        return PeerIpv4.fromInetAddress(connection.getAddress());
    }

    //conexão

    public void openGame(User user) {
        if (user == null || user.getAddress() == null) {
            return;
        }
        String address = PeerIpv4.fromInetAddress(user.getAddress());
        TcpConnection existing = connections.get(address);
        if ((existing != null && !existing.isClosed()) || pendingConnections.contains(address)) {
            return;
        }
        pendingConnections.add(address);
        host.openConnection(address);
    }

    public void registerOutgoingConnection(String address, TcpConnection connection) {
        if (address == null || connection == null) {
            return;
        }
        String canonical = PeerIpv4.normalize(address);
        pendingConnections.remove(canonical);
        connections.put(canonical, connection);
        if (uiSink != null) {
            uiSink.onChannelReady(canonical);
        }
    }

    public void connectionReceived(TcpConnection connection) {
        String address = addressKey(connection);
        pendingConnections.remove(address);
        connections.put(address, connection);
        if (uiSink != null) {
            uiSink.onChannelReady(address);
        }
    }

    public void abortOutgoing(String address) {
        if (address != null) {
            pendingConnections.remove(PeerIpv4.normalize(address));
        }
    }

    public void closeGame(TcpConnection connection) {
        String address = addressKey(connection);
        connections.remove(address);
        pendingConnections.remove(address);
        gamesAsMaster.remove(address);
        if (uiSink != null) {
            uiSink.onChannelClosed(address);
        }
    }

    public boolean hasOpenChannel(User user) {
        if (user == null || user.getAddress() == null) {
            return false;
        }
        TcpConnection c = connections.get(PeerIpv4.fromInetAddress(user.getAddress()));
        return c != null && !c.isClosed();
    }

    // ---------- jogo ----------

    /** Eu sou o mestre: escolho a palavra e aviso o outro lado. */
    public boolean startGame(User user, String word) {
        String address = PeerIpv4.fromInetAddress(user.getAddress());
        TcpConnection c = connections.get(address);
        if (c == null || c.isClosed()) {
            return false;
        }
        try {
            HangmanGame game = new HangmanGame(word);
            gamesAsMaster.put(address, game);
            send(c, snapshot(HangmanMessage.Type.START, game));
            notifyState(address, game);
            return true;
        } catch (IllegalArgumentException ex) {
            if (uiSink != null) {
                uiSink.onSystemMessage(address, "Palavra inválida: use apenas letras.");
            }
            return false;
        }
    }

    public boolean guess(User user, char letter) {
        String address = PeerIpv4.fromInetAddress(user.getAddress());
        TcpConnection c = connections.get(address);
        if (c == null || c.isClosed()) {
            return false;
        }
        return send(c, HangmanMessage.guess(letter));
    }

    public void receiveMessage(TcpConnection connection, HangmanMessage msg) {
        String address = addressKey(connection);
        switch (msg.getType()) {
            case GUESS -> {
                HangmanGame game = gamesAsMaster.get(address);
                if (game == null) {
                    return;
                }
                game.guess(msg.getLetter());
                send(connection, snapshot(HangmanMessage.Type.STATE, game));
                notifyState(address, game);
            }
            case START, STATE -> {
                if (uiSink != null) {
                    uiSink.onGameState(address, msg.getMasked(), msg.getErrors(),
                            msg.getGuessedLetters(),
                            HangmanGame.Status.valueOf(msg.getStatus()), msg.getWord());
                }
            }
        }
    }


    private HangmanMessage snapshot(HangmanMessage.Type type, HangmanGame game) {
        boolean over = game.status() != HangmanGame.Status.PLAYING;
        return HangmanMessage.state(type, game.masked(), game.getErrors(),
                game.getGuessedLetters(), game.status().name(), over ? game.getWord() : null);
    }

    private void notifyState(String address, HangmanGame game) {
        if (uiSink != null) {
            boolean over = game.status() != HangmanGame.Status.PLAYING;
            uiSink.onGameState(address, game.masked(), game.getErrors(),
                    game.getGuessedLetters(), game.status(), over ? game.getWord() : game.getWord());
        }
    }

    private boolean send(TcpConnection connection, HangmanMessage msg) {
        try {
            connection.send(msg.toByteArray());
            return true;
        } catch (Exception ex) {
            System.out.println("Error sending hangman TCP: " + ex.getMessage());
            return false;
        }
    }
}