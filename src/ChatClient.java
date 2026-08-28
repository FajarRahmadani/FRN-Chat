import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ChatClient extends JFrame {

    private static final Color BG_CHAT     = new Color(0x24, 0x1A, 0x14);
    private static final Color PANEL       = new Color(0x33, 0x26, 0x1E);
    private static final Color FIELD       = new Color(0x40, 0x30, 0x26);
    private static final Color BORDER_SOFT = new Color(0x55, 0x42, 0x35);
    private static final Color ACCENT      = new Color(0xC8, 0x80, 0x3C);
    private static final Color ACCENT_INK  = new Color(0x2A, 0x1C, 0x12);
    private static final Color BUBBLE_ME   = new Color(0xA6, 0x6A, 0x33);
    private static final Color BUBBLE_THEM = new Color(0x3E, 0x30, 0x27);
    private static final Color TEXT_ME     = new Color(0xFF, 0xF4, 0xE8);
    private static final Color TEXT_THEM   = new Color(0xEC, 0xE0, 0xD4);
    private static final Color TEXT_CREAM  = new Color(0xE8, 0xD9, 0xC9);
    private static final Color SENDER_C    = new Color(0xE0, 0xA8, 0x67);
    private static final Color TIME_C      = new Color(0x9C, 0x86, 0x75);
    private static final Color SYSTEM_C    = new Color(0xB8, 0x9B, 0x86);
    private static final Color SYSTEM_BG   = new Color(0x2E, 0x23, 0x1B);

    private static final SecretKey APP_KEY = buildKey();
    private static SecretKey buildKey() {
        try { return Crypto.deriveKey(resolveSharedSecret()); }
        catch (Exception e) { throw new RuntimeException("Gagal menyiapkan kunci", e); }
    }

    /**
     * The AES key is derived from a shared secret that every participant
     * of a given FRN Chat "room" must agree on out-of-band (it must never
     * be committed to source control or shipped as a fixed default,
     * otherwise the encryption provides no real confidentiality).
     *
     * Resolution order:
     *   1. FRN_CHAT_SECRET environment variable.
     *   2. Interactive prompt at startup (not persisted anywhere).
     */
    private static String resolveSharedSecret() {
        String fromEnv = System.getenv("FRN_CHAT_SECRET");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        String entered = JOptionPane.showInputDialog(
                null,
                "Masukkan kunci enkripsi bersama (harus sama di semua client):",
                "FRN Chat - Kunci Enkripsi",
                JOptionPane.PLAIN_MESSAGE);

        if (entered == null || entered.isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "Kunci enkripsi wajib diisi. Aplikasi akan ditutup.",
                    "FRN Chat", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return entered;
    }

    private JPanel messagesPanel;
    private JScrollPane scrollPane;
    private JTextField txtInputField;
    private JTextField txtUsernameField;
    private JTextField txtServerAddressField;
    private JButton btnConnect;
    private JButton btnSend;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final int port = 2106;
    private String myUsername = "";
    private final SecretKey key = APP_KEY;   

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern TIME_P = Pattern.compile("^\\[(\\d{1,2}:\\d{2}(?::\\d{2})?)\\]\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern MSG_P  = Pattern.compile("^\\[(.*?)\\]:\\s?(.*)$", Pattern.DOTALL);

    public ChatClient() {
        setTitle("FRN Chat");
        setSize(460, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_CHAT);
        initComponents();
    }

    private void initComponents() {
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        inputPanel.setOpaque(false);

        txtServerAddressField = styleField(new JTextField("localhost"));
        txtUsernameField = styleField(new JTextField("User_" + (int)(Math.random() * 1000)));

        inputPanel.add(makeLabel("IP Server:"));
        inputPanel.add(txtServerAddressField);
        inputPanel.add(makeLabel("Username:"));
        inputPanel.add(txtUsernameField);

        btnConnect = styleButton(new JButton("Hubungkan"));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(PANEL);
        topPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(btnConnect, BorderLayout.EAST);

        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_CHAT);
        messagesPanel.setBorder(new EmptyBorder(10, 6, 10, 6));

        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setBackground(BG_CHAT);
        northWrap.add(messagesPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(northWrap);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_CHAT);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(PANEL);
        bottomPanel.setBorder(new EmptyBorder(10, 12, 12, 12));

        txtInputField = styleField(new JTextField());
        txtInputField.setEditable(false);
        btnSend = styleButton(new JButton("Kirim"));
        btnSend.setEnabled(false);

        bottomPanel.add(txtInputField, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        btnConnect.addActionListener(e -> connectToServer());
        btnSend.addActionListener(e -> sendMessage());
        txtInputField.addActionListener(e -> sendMessage());
    }

    private JLabel makeLabel(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(TEXT_CREAM);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return l;
    }

    private JTextField styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(TEXT_CREAM);
        f.setCaretColor(ACCENT);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SOFT, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        return f;
    }

    private JButton styleButton(JButton b) {
        b.setBackground(ACCENT);
        b.setForeground(ACCENT_INK);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void connectToServer() {
        String serverAddress = txtServerAddressField.getText().trim();
        String username = txtUsernameField.getText().trim();

        if (serverAddress.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "IP Server dan Username tidak boleh kosong!",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            socket = new Socket(serverAddress, port); 
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            myUsername = username;
            writer.println(username);

            txtServerAddressField.setEditable(false);
            txtUsernameField.setEditable(false);
            btnConnect.setEnabled(false);

            txtInputField.setEditable(true);
            btnSend.setEnabled(true);
            txtInputField.requestFocus();

            new Thread(new IncomingMessagesReader()).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Gagal terhubung ke " + serverAddress + "\nPastikan IP benar dan Server (TLS) sudah aktif.",
                    "Koneksi Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = txtInputField.getText().trim();
        if (!message.isEmpty() && writer != null) {
            try {
                String body = "enc:" + Crypto.encrypt(key, message); 
                writer.println(body);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal mengenkripsi pesan.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            txtInputField.setText("");
            txtInputField.requestFocus();
        }
    }

    private class IncomingMessagesReader implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> handleIncoming(msg));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> addSystemMessage("Koneksi ke server terputus."));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    txtInputField.setEditable(false);
                    btnSend.setEnabled(false);
                });
            }
        }
    }

    private void handleIncoming(String raw) {
        String time = null;
        Matcher tm = TIME_P.matcher(raw);
        if (tm.matches()) {
            time = tm.group(1);
            raw = tm.group(2);
        }
        if (time == null) time = LocalTime.now().format(TF);

        Matcher mm = MSG_P.matcher(raw);
        if (mm.matches()) {
            String sender = mm.group(1);
            String body = mm.group(2);
            if (sender.equalsIgnoreCase("SYSTEM")) {
                addSystemMessage(body);
            } else {
                String text = decryptBody(body);
                if (sender.equals(myUsername)) addBubble(null, text, time, true);
                else addBubble(sender, text, time, false);
            }
        } else {
            addSystemMessage(raw);
        }
    }

    private String decryptBody(String body) {
        if (body.startsWith("enc:")) {
            try {
                return Crypto.decrypt(key, body.substring(4));
            } catch (Exception e) {
                return "\uD83D\uDD12 [pesan terenkripsi - aplikasi/kunci berbeda]";
            }
        }
        return body;
    }

    private void addBubble(String sender, String text, String time, boolean me) {
        RoundedPanel bubble = new RoundedPanel(18, me ? BUBBLE_ME : BUBBLE_THEM);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(7, 12, 6, 12));

        if (sender != null) {
            JLabel s = new JLabel(sender);
            s.setForeground(SENDER_C);
            s.setFont(new Font("SansSerif", Font.BOLD, 12));
            s.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(s);
            bubble.add(Box.createVerticalStrut(2));
        }

        JLabel msg = makeWrappedLabel(text, me ? TEXT_ME : TEXT_THEM);
        msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(msg);

        JLabel t = new JLabel(time);
        t.setForeground(TIME_C);
        t.setFont(new Font("SansSerif", Font.PLAIN, 10));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(Box.createVerticalStrut(3));
        bubble.add(t);

        addRow(bubble, me ? BorderLayout.EAST : BorderLayout.WEST);
    }

    private JLabel makeWrappedLabel(String text, Color color) {
        String esc = text.replace("&", "&amp;").replace("<", "&lt;")
                         .replace(">", "&gt;").replace("\n", "<br>");
        Font font = new Font("SansSerif", Font.PLAIN, 14);
        JLabel lbl = new JLabel("<html>" + esc + "</html>");
        lbl.setFont(font);
        lbl.setForeground(color);
        int maxW = 250;
        if (lbl.getPreferredSize().width > maxW) {
            lbl = new JLabel("<html><body style='width:" + maxW + "px'>" + esc + "</body></html>");
            lbl.setFont(font);
            lbl.setForeground(color);
        }
        return lbl;
    }

    private void addSystemMessage(String text) {
        RoundedPanel pill = new RoundedPanel(14, SYSTEM_BG);
        pill.setLayout(new BoxLayout(pill, BoxLayout.X_AXIS));
        pill.setBorder(new EmptyBorder(5, 12, 5, 12));
        JLabel l = new JLabel(text);
        l.setForeground(SYSTEM_C);
        l.setFont(new Font("SansSerif", Font.ITALIC, 12));
        pill.add(l);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 10, 4, 10));
        row.add(pill);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        scrollToBottom();
    }

    private void addRow(JComponent bubble, String side) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 10, 4, 10));
        row.add(bubble, side);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = scrollPane.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        RoundedPanel(int radius, Color bg) {
            this.radius = radius; this.bg = bg;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Crypto {
        private static final byte[] SALT = "ChatAppLocal-salt-v1".getBytes();

        static SecretKey deriveKey(String passphrase) throws Exception {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), SALT, 65536, 256);
            byte[] k = f.generateSecret(spec).getEncoded();
            return new SecretKeySpec(k, "AES");
        }

        static String encrypt(SecretKey key, String plaintext) throws Exception {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plaintext.getBytes("UTF-8"));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        }

        static String decrypt(SecretKey key, String b64) throws Exception {
            byte[] in = Base64.getDecoder().decode(b64);
            byte[] iv = Arrays.copyOfRange(in, 0, 12);
            byte[] ct = Arrays.copyOfRange(in, 12, in.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ct), "UTF-8");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient().setVisible(true));
    }
}
