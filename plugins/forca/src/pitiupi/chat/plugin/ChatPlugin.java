package pitiupi.chat.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.application.presence.UserPeerView;
import pitiupi.chat.GUI.ForcaPanel;
import pitiupi.chat.control.HangmanController;
import pitiupi.chat.control.HangmanHost;
import pitiupi.chat.net.HangmanMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.models.User;
import pitiupi.net.tcp.TcpAttemptListener;
import pitiupi.net.tcp.TcpConnection;

import javax.swing.SwingUtilities;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class ChatPlugin extends Plugin implements HangmanHost {

    private ForcaPanel forcaPanel;
    private PluginWorkspace workspace;
    private ConnectionManager connectionManager;
    private HangmanController controller;

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connectionManager = connectionManager;
        this.workspace = workspace;
        this.controller = new HangmanController(this);
        this.forcaPanel = new ForcaPanel(controller);
        controller.setUiSink(forcaPanel);
        bindShellWorkspaceContent(forcaPanel);
        installPeerActions(workspace.peerActions());
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "forca.play",
                "Jogar forca com este peer",
                PeerActionTarget.SINGLE_PEER,
                this::openGameForPeer
        ));
    }

    private void openGameForPeer(PeerTarget target) {
        if (!(target instanceof PeerTarget.Single single)) {
            return;
        }
        User user = toUser(single.peer());
        if (user == null) {
            return;
        }
        forcaPanel.setPeer(user);
        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
        controller.openGame(user);
    }

    private static User toUser(UserPeerView peer) {
        if (peer.isBroadcastTarget() || peer.isSelf()) {
            return null;
        }
        return peer.getInetAddress()
                .map(addr -> new User(peer.getDisplayName(), addr, System.currentTimeMillis(), peer.getInstanceId()))
                .orElse(null);
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(HangmanMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        // A forca usa só TCP privado (receiveBytes); nada chega por aqui.
    }

    @Override
    public boolean receivePrivateConnection(TcpConnection connection) {
        SwingUtilities.invokeLater(() -> controller.connectionReceived(connection));
        return true;
    }

    @Override
    public boolean receiveBytes(TcpConnection connection, byte[] bytes) {
        HangmanMessage msg = parse(bytes);
        if (msg != null) {
            controller.receiveMessage(connection, msg);
        }
        return true;
    }

    @Override
    public boolean onSocketClosed(TcpConnection connection) {
        controller.closeGame(connection);
        return true;
    }

    @Override
    public void openConnection(String ip) {
        connectionManager.connectPluginTcpAsync(ip, getIdentifier(), getIdentifier(), new TcpAttemptListener() {
            @Override
            public void onConnecting(String endpoint) {
                forcaPanel.showStatus("Conectando em " + endpoint + "…");
            }

            @Override
            public void onSuccess(TcpConnection connection) {
                controller.registerOutgoingConnection(ip, connection);
            }

            @Override
            public void onFailure(String message) {
                controller.abortOutgoing(ip);
                forcaPanel.showStatus(message);
            }
        });
    }

    @Override
    public String getPluginIdentifier() {
        return getIdentifier();
    }

    private HangmanMessage parse(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            if (ois.readObject() instanceof HangmanMessage msg) {
                return msg;
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    @Override
    public String getName() {
        return "Forca";
    }

    @Override
    public String getDisplayName() {
        return "Forca";
    }

    @Override
    public String getAuthor() {
        return "Flávio Schiavoni";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getIdentifier() {
        return "Pitiupi.Forca";
    }
}