package pitiupi.chat.GUI;

import pitiupi.chat.control.HangmanController;
import pitiupi.chat.control.HangmanGame;
import pitiupi.chat.control.HangmanUiSink;
import pitiupi.models.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.net.InetAddress;

/** Tela da forca. Todos os callbacks de rede passam por invokeLater. */
public class ForcaPanel extends JPanel implements HangmanUiSink {

    private final HangmanController controller;
    private User peer;

    private final JLabel statusLabel = new JLabel("Escolha um peer na lista lateral → Jogar forca.");
    private final JLabel maskLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel lettersLabel = new JLabel("Letras: ", SwingConstants.CENTER);
    private final JPasswordField wordField = new JPasswordField(14);
    private final JButton startButton = new JButton("Iniciar (sou o mestre)");
    private final JTextField letterField = new JTextField(2);
    private final JButton guessButton = new JButton("Chutar");
    private final Gallows gallows = new Gallows();

    public ForcaPanel(HangmanController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        maskLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(gallows, BorderLayout.CENTER);
        JPanel texts = new JPanel(new GridLayout(2, 1));
        texts.add(maskLabel);
        texts.add(lettersLabel);
        center.add(texts, BorderLayout.SOUTH);

        JPanel master = new JPanel();
        master.setBorder(BorderFactory.createTitledBorder("Mestre: escolha a palavra"));
        master.add(wordField);
        master.add(startButton);

        JPanel guesser = new JPanel();
        guesser.setBorder(BorderFactory.createTitledBorder("Adivinhador: uma letra"));
        guesser.add(letterField);
        guesser.add(guessButton);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 8, 0));
        bottom.add(master);
        bottom.add(guesser);

        add(statusLabel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        startButton.setEnabled(false);
        guessButton.setEnabled(false);
        startButton.addActionListener(e -> startGame());
        wordField.addActionListener(e -> startGame());
        guessButton.addActionListener(e -> guess());
        letterField.addActionListener(e -> guess());
    }

    public void setPeer(User peer) {
        this.peer = peer;
        showStatus("Jogando com " + peer.getUserName());
        startButton.setEnabled(controller.hasOpenChannel(peer));
    }

    public void showStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void startGame() {
        if (peer == null) {
            return;
        }
        String word = new String(wordField.getPassword());
        if (controller.startGame(peer, word)) {
            wordField.setText("");
            startButton.setEnabled(false);
            guessButton.setEnabled(false); // o mestre não chuta
        }
    }

    private void guess() {
        String t = letterField.getText().trim();
        if (peer == null || t.isEmpty()) {
            return;
        }
        controller.guess(peer, t.charAt(0));
        letterField.setText("");
    }

    private void adoptPeer(String ip) {
        if (peer != null) {
            return;
        }
        try {
            peer = new User(ip, InetAddress.getByName(ip), System.currentTimeMillis(), null);
        } catch (Exception ignored) {
        }
    }

    // ---------- HangmanUiSink ----------

    @Override
    public void onChannelReady(String peerIp) {
        SwingUtilities.invokeLater(() -> {
            adoptPeer(peerIp);
            statusLabel.setText("Conectado com " + peerIp + ". Um de vocês inicia como mestre.");
            startButton.setEnabled(true);
        });
    }

    @Override
    public void onChannelClosed(String peerIp) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Conexão encerrada (" + peerIp + ").");
            startButton.setEnabled(false);
            guessButton.setEnabled(false);
        });
    }

    @Override
    public void onGameState(String peerIp, String masked, int errors, String guessedLetters,
                            HangmanGame.Status status, String word) {
        SwingUtilities.invokeLater(() -> {
            adoptPeer(peerIp);
            maskLabel.setText(masked);
            lettersLabel.setText("Letras: " + guessedLetters + "   Erros: " + errors + "/" + HangmanGame.MAX_ERRORS);
            gallows.setErrors(errors);
            boolean iAmMaster = word != null && status == HangmanGame.Status.PLAYING;
            boolean playing = status == HangmanGame.Status.PLAYING;
            guessButton.setEnabled(playing && !iAmMaster);
            startButton.setEnabled(!playing);
            switch (status) {
                case WON -> statusLabel.setText("Acertaram! A palavra era " + word + ".");
                case LOST -> statusLabel.setText("Enforcou! A palavra era " + word + ".");
                default -> statusLabel.setText(iAmMaster
                        ? "Palavra: " + word + " — aguardando as letras."
                        : "Sua vez: chute uma letra.");
            }
        });
    }

    @Override
    public void onSystemMessage(String peerIp, String text) {
        showStatus(text);
    }

    /** Desenho simples: uma parte do boneco por erro. */
    private static class Gallows extends JPanel {
        private int errors;

        Gallows() {
            setPreferredSize(new Dimension(300, 240));
        }

        void setErrors(int errors) {
            this.errors = errors;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(3));
            g2.setColor(Color.DARK_GRAY);
            int w = getWidth();
            int h = getHeight();
            int x = w / 2 - 40;
            g2.drawLine(x - 60, h - 20, x + 60, h - 20);   
            g2.drawLine(x - 30, h - 20, x - 30, 20);       
            g2.drawLine(x - 30, 20, x + 30, 20);           
            g2.drawLine(x + 30, 20, x + 30, 45);           
            if (errors >= 1) g2.drawOval(x + 15, 45, 30, 30);                 
            if (errors >= 2) g2.drawLine(x + 30, 75, x + 30, 130);            
            if (errors >= 3) g2.drawLine(x + 30, 85, x + 5, 110);             
            if (errors >= 4) g2.drawLine(x + 30, 85, x + 55, 110);            
            if (errors >= 5) g2.drawLine(x + 30, 130, x + 8, 165);            
            if (errors >= 6) g2.drawLine(x + 30, 130, x + 52, 165);           
            g2.dispose();
        }
    }
}