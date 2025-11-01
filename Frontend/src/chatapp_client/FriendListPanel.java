package chatapp_client;

import model.User;
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Panel hiển thị danh sách BẠN BÈ (đã accepted) và thanh tìm kiếm.
 */
public class FriendListPanel extends JPanel { 

    private JList<User> lstFriends;
    private MainForm mainForm;
    private DefaultListModel<User> listModel;
    private JTextField txtSearch;
    private List<User> allFriends; // Chỉ lưu bạn bè

    private Color backgroundSelectionColor = new Color(0, 120, 215);
    private Color backgroundNonSelectionColor = new Color(249, 249, 249);

    public FriendListPanel(MainForm main) {
        this.mainForm = main;
        this.allFriends = new ArrayList<>();
        setLayout(new BorderLayout());
        setBackground(backgroundNonSelectionColor);

        // --- Panel Tìm kiếm ---
        JPanel searchBarPanel = new JPanel(new BorderLayout(5, 0));
        searchBarPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        searchBarPanel.setBackground(new Color(230, 230, 230));
        txtSearch = new JTextField("Tìm bạn bè...");
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setForeground(Color.GRAY);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        txtSearch.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { if (txtSearch.getText().equals("Tìm bạn bè...")) { txtSearch.setText(""); txtSearch.setForeground(Color.BLACK); } }
            @Override public void focusLost(FocusEvent e) { if (txtSearch.getText().isEmpty()) { txtSearch.setText("Tìm bạn bè..."); txtSearch.setForeground(Color.GRAY); } }
        });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterList(); }
            @Override public void removeUpdate(DocumentEvent e) { filterList(); }
            @Override public void changedUpdate(DocumentEvent e) { filterList(); }
        });
        searchBarPanel.add(txtSearch, BorderLayout.CENTER);

        // --- Danh sách Bạn bè ---
        listModel = new DefaultListModel<>();
        lstFriends = new JList<>(listModel);
        lstFriends.setCellRenderer(new UserListRenderer()); // Dùng chung Renderer
        lstFriends.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstFriends.setBackground(backgroundNonSelectionColor);
        lstFriends.setFixedCellHeight(64);
        lstFriends.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    User selected = lstFriends.getSelectedValue();
                    if (selected != null) {
                        mainForm.showPrivateChatForm(selected.getId(), selected.getUsername());
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(lstFriends);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(searchBarPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadFriends();
    }

    void loadFriends() {
         if (mainForm.getLoggedInUserId() <= 0) return;
         
         // Gọi API /api/friendships/{userId}/list
         NetworkService.getFriends(mainForm.getLoggedInUserId()).thenAccept(friends -> {
            SwingUtilities.invokeLater(() -> {
                allFriends.clear();
                if (friends != null) {
                    allFriends.addAll(friends);
                }
                filterList();
            });
         }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi tải danh sách bạn bè: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            return null;
         });
    }

    private void filterList() {
        String query = txtSearch.getText(); if (query.equals("Tìm bạn bè...")) query = "";
        query = query.toLowerCase().trim();
        listModel.clear();
        for (User user : allFriends) {
            boolean matchesUsername = user.getUsername().toLowerCase().contains(query);
            boolean matchesSdt = (user.getSdt() != null && user.getSdt().contains(query));
            if (matchesUsername || matchesSdt) {
                listModel.addElement(user);
            }
        }
    }
}