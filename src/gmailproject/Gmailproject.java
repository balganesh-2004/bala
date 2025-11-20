package gmailproject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class Gmailproject extends JFrame {
    private JTabbedPane tabbedPane;
    private JPanel loginPanel, registerPanel, inboxPanel, sentPanel, sendEmailPanel, todayPanel;
    private JTextField loginUserField, regUserField, regEmailField;
    private JPasswordField loginPassField, regPassField;
    private JButton loginBtn, registerBtn;
    private Connection conn;
    private String username;
    private JTable inboxTable, sentTable, todayTable;

    public Gmailproject() {
        super("Mini Gmail");
        setSize(700, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(52, 73, 94)); // dark slate

        connectDatabase();
        createTablesIfNotExists();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(new Color(41, 128, 185));  // royal blue
        tabbedPane.setForeground(Color.WHITE);

        initLoginPanel();
        initRegisterPanel();
        tabbedPane.addTab("Login", loginPanel);
        tabbedPane.addTab("Register", registerPanel);

        add(tabbedPane);
    }

    private void connectDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/gmail_pro?useSSL=false&serverTimezone=UTC",
                "root", "1234"
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void createTablesIfNotExists() {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS gmail (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "username VARCHAR(50) UNIQUE NOT NULL," +
                         "email VARCHAR(100) UNIQUE NOT NULL," +
                         "password VARCHAR(100) NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS emails (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "sender VARCHAR(50) NOT NULL," +
                         "receiver VARCHAR(50) NOT NULL," +
                         "subject VARCHAR(200)," +
                         "body TEXT," +
                         "sent_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Table Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(39, 174, 96)); // emerald
        GridBagConstraints gbc = createGbc();

        JLabel uLbl = createLabel("Username:");
        JLabel pLbl = createLabel("Password:");
        loginUserField = createField(20);
        loginPassField = createPasswordField(20);
        loginBtn = createButton("Login", new Color(192, 57, 43)); // red
        loginBtn.addActionListener(e -> handleLogin());

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0; loginPanel.add(uLbl, gbc);
        gbc.gridx = 1; loginPanel.add(loginUserField, gbc);
        gbc.gridy = 1; gbc.gridx = 0; loginPanel.add(pLbl, gbc);
        gbc.gridx = 1; loginPanel.add(loginPassField, gbc);
        gbc.gridy = 2; gbc.gridx = 1; loginPanel.add(loginBtn, gbc);
    }

    private void initRegisterPanel() {
        registerPanel = new JPanel(new GridBagLayout());
        registerPanel.setBackground(new Color(142, 68, 173)); // purple
        GridBagConstraints gbc = createGbc();

        JLabel uLbl = createLabel("Username:");
        JLabel eLbl = createLabel("Email:");
        JLabel pLbl = createLabel("Password:");
        regUserField = createField(20);
        regEmailField = createField(20);
        regPassField = createPasswordField(20);
        registerBtn = createButton("Register", new Color(243, 156, 18)); // orange
        registerBtn.addActionListener(e -> handleRegister());

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0; registerPanel.add(uLbl, gbc);
        gbc.gridx = 1; registerPanel.add(regUserField, gbc);
        gbc.gridy = 1; gbc.gridx = 0; registerPanel.add(eLbl, gbc);
        gbc.gridx = 1; registerPanel.add(regEmailField, gbc);
        gbc.gridy = 2; gbc.gridx = 0; registerPanel.add(pLbl, gbc);
        gbc.gridx = 1; registerPanel.add(regPassField, gbc);
        gbc.gridy = 3; gbc.gridx = 1; registerPanel.add(registerBtn, gbc);
    }

    private void loadMainApp() {
        getContentPane().removeAll();
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(new Color(41, 128, 185));
        tabbedPane.setForeground(Color.WHITE);

        initInboxPanel();
        initSentPanel();
        initSendEmailPanel();
        initTodayPanel();
        tabbedPane.addTab("Inbox", inboxPanel);
        tabbedPane.addTab("Sent", sentPanel);
        tabbedPane.addTab("Compose", sendEmailPanel);
        tabbedPane.addTab("Today's", todayPanel);

        JButton logout = createButton("Logout", new Color(231, 76, 60)); // crimson
        logout.addActionListener(e -> {
            username = null;
            new Gmailproject().setVisible(true);
            dispose();
        });
        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(41, 128, 185));
        bottom.add(logout);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        revalidate(); repaint();

        loadInbox(); loadSentEmails(); loadTodayEmails();
    }

    private void initInboxPanel() {
        inboxPanel = new JPanel(new BorderLayout());
        inboxPanel.setBackground(new Color(230, 126, 34)); // pumpkin
        inboxTable = new JTable();
        styleTable(inboxTable);
        inboxPanel.add(new JScrollPane(inboxTable), BorderLayout.CENTER);

        // DELETE MAIL button in bright yellow with black text
        JButton del = createButton("Delete Mail", new Color(255, 223, 0)); // yellow
        del.setForeground(Color.BLACK);
        del.addActionListener(e -> deleteSelectedInboxEmail());
        inboxPanel.add(del, BorderLayout.SOUTH);

        inboxTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int id = (int) inboxTable.getModel().getValueAt(inboxTable.getSelectedRow(), 0);
                    showEmailBody(id);
                }
            }
        });
    }

    private void initSentPanel() {
        sentPanel = new JPanel(new BorderLayout());
        sentPanel.setBackground(new Color(46, 204, 113)); // bright green
        sentTable = new JTable();
        styleTable(sentTable);
        sentPanel.add(new JScrollPane(sentTable), BorderLayout.CENTER);
    }

    private void initTodayPanel() {
        todayPanel = new JPanel(new BorderLayout());
        todayPanel.setBackground(new Color(155, 89, 182)); // lavender
        todayTable = new JTable();
        styleTable(todayTable);
        todayPanel.add(new JScrollPane(todayTable), BorderLayout.CENTER);
    }

    private void initSendEmailPanel() {
        sendEmailPanel = new JPanel(new GridBagLayout());
        sendEmailPanel.setBackground(new Color(255, 182, 193)); // light pink
        GridBagConstraints gbc = createGbc();

        JLabel toLbl = createLabel("To:");
        JLabel subjLbl = createLabel("Subject:");
        JLabel bodyLbl = createLabel("Body:");
        JTextField toField = createField(30), subjField = createField(30);
        JTextArea bodyArea = new JTextArea(8, 30);
        JScrollPane sp = new JScrollPane(bodyArea);
        JButton send = createButton("Send", new Color(231, 76, 60)); // crimson
        send.addActionListener(e -> {
            String to = toField.getText().trim(), s = subjField.getText().trim(), b = bodyArea.getText().trim();
            if (to.isEmpty() || s.isEmpty() || b.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.");
                return;
            }
            sendEmail(to, s, b);
            toField.setText(""); subjField.setText(""); bodyArea.setText("");
        });

        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0; gbc.gridy = 0; sendEmailPanel.add(toLbl, gbc);
        gbc.gridx = 1; sendEmailPanel.add(toField, gbc);
        gbc.gridy = 1; gbc.gridx = 0; sendEmailPanel.add(subjLbl, gbc);
        gbc.gridx = 1; sendEmailPanel.add(subjField, gbc);
        gbc.gridy = 2; gbc.gridx = 0; sendEmailPanel.add(bodyLbl, gbc);
        gbc.gridx = 1; sendEmailPanel.add(sp, gbc);
        gbc.gridy = 3; gbc.gridx = 1; sendEmailPanel.add(send, gbc);
    }

    // --- Helper methods ---

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    private JTextField createField(int cols) {
        JTextField fld = new JTextField(cols);
        fld.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return fld;
    }

    private JPasswordField createPasswordField(int cols) {
        JPasswordField pwd = new JPasswordField(cols);
        pwd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return pwd;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    // --- Database operations ---

    private void handleLogin() {
        String u = loginUserField.getText().trim();
        String p = new String(loginPassField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter credentials.");
            return;
        }
        try {
            PreparedStatement st = conn.prepareStatement(
                "SELECT * FROM gmail WHERE username = ? AND password = ?"
            );
            st.setString(1, u);
            st.setString(2, p);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                username = u;
                JOptionPane.showMessageDialog(this, "Welcome, " + u + "!");
                loadMainApp();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid login.");
            }
            rs.close(); st.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Login error: " + e.getMessage());
        }
    }

    private void handleRegister() {
        String u = regUserField.getText().trim(), e = regEmailField.getText().trim();
        String p = new String(regPassField.getPassword());
        if (u.isEmpty() || e.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.");
            return;
        }
        try {
            PreparedStatement c = conn.prepareStatement(
                "SELECT * FROM gmail WHERE username = ? OR email = ?"
            );
            c.setString(1, u); c.setString(2, e);
            ResultSet rs = c.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Username or email exists.");
                rs.close(); c.close();
                return;
            }
            rs.close(); c.close();

            PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO gmail(username,email,password) VALUES(?,?,?)"
            );
            ins.setString(1, u); ins.setString(2, e); ins.setString(3, p);
            ins.executeUpdate(); ins.close();
            JOptionPane.showMessageDialog(this, "Registered! Please login.");
            tabbedPane.setSelectedIndex(0);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Reg error: " + ex.getMessage());
        }
    }

    private void loadInbox() {
        try {
            PreparedStatement st = conn.prepareStatement(
                "SELECT id, sender, subject, sent_time FROM emails WHERE receiver = ? ORDER BY sent_time DESC"
            );
            st.setString(1, username);
            ResultSet rs = st.executeQuery();
            Vector<String> cols = new Vector<>();
            cols.add("ID"); cols.add("From"); cols.add("Subject"); cols.add("Date");
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> r = new Vector<>();
                r.add(rs.getInt("id"));
                r.add(rs.getString("sender"));
                r.add(rs.getString("subject"));
                r.add(rs.getTimestamp("sent_time"));
                data.add(r);
            }
            inboxTable.setModel(new DefaultTableModel(data, cols) {
                public boolean isCellEditable(int r, int c) { return false; }
            });
            inboxTable.getColumnModel().getColumn(0).setMinWidth(0);
            inboxTable.getColumnModel().getColumn(0).setMaxWidth(0);
            rs.close(); st.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Inbox load error: " + e.getMessage());
        }
    }

    private void loadSentEmails() {
        try {
            PreparedStatement st = conn.prepareStatement(
                "SELECT id, receiver, subject, sent_time FROM emails WHERE sender = ? ORDER BY sent_time DESC"
            );
            st.setString(1, username);
            ResultSet rs = st.executeQuery();
            Vector<String> cols = new Vector<>();
            cols.add("ID"); cols.add("To"); cols.add("Subject"); cols.add("Date");
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> r = new Vector<>();
                r.add(rs.getInt("id"));
                r.add(rs.getString("receiver"));
                r.add(rs.getString("subject"));
                r.add(rs.getTimestamp("sent_time"));
                data.add(r);
            }
            sentTable.setModel(new DefaultTableModel(data, cols) {
                public boolean isCellEditable(int r, int c) { return false; }
            });
            sentTable.getColumnModel().getColumn(0).setMinWidth(0);
            sentTable.getColumnModel().getColumn(0).setMaxWidth(0);
            rs.close(); st.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Sent load error: " + e.getMessage());
        }
    }

    private void loadTodayEmails() {
        try {
            PreparedStatement st = conn.prepareStatement(
                "SELECT id, sender, subject, sent_time FROM emails " +
                "WHERE receiver = ? AND DATE(sent_time) = CURDATE() ORDER BY sent_time DESC"
            );
            st.setString(1, username);
            ResultSet rs = st.executeQuery();
            Vector<String> cols = new Vector<>();
            cols.add("ID"); cols.add("From"); cols.add("Subject"); cols.add("Date");
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> r = new Vector<>();
                r.add(rs.getInt("id"));
                r.add(rs.getString("sender"));
                r.add(rs.getString("subject"));
                r.add(rs.getTimestamp("sent_time"));
                data.add(r);
            }
            todayTable.setModel(new DefaultTableModel(data, cols) {
                public boolean isCellEditable(int r, int c) { return false; }
            });
            todayTable.getColumnModel().getColumn(0).setMinWidth(0);
            todayTable.getColumnModel().getColumn(0).setMaxWidth(0);
            rs.close(); st.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Today load error: " + e.getMessage());
        }
    }

    private void showEmailBody(int id) {
        try {
            PreparedStatement st = conn.prepareStatement("SELECT body FROM emails WHERE id = ?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, rs.getString("body"),
                                              "Email Content", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Email not found.");
            }
            rs.close(); st.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Show error: " + e.getMessage());
        }
    }

    private void deleteSelectedInboxEmail() {
        int row = inboxTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an email.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete this email?", "Confirm",
                                          JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        int id = (int) inboxTable.getModel().getValueAt(row, 0);
        try {
            PreparedStatement st = conn.prepareStatement("DELETE FROM emails WHERE id = ?");
            st.setInt(1, id);
            st.executeUpdate(); st.close();
            loadInbox(); loadTodayEmails();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Delete error: " + e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            PreparedStatement st = conn.prepareStatement(
                "INSERT INTO emails(sender,receiver,subject,body) VALUES(?,?,?,?)"
            );
            st.setString(1, username);
            st.setString(2, to);
            st.setString(3, subject);
            st.setString(4, body);
            st.executeUpdate(); st.close();
            JOptionPane.showMessageDialog(this, "Email sent successfully!");
            loadSentEmails();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Send error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gmailproject().setVisible(true));
    }
}
