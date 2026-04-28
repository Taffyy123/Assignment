package com.chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;

public class TracksTab extends JPanel {
    private JTable tracksTable;
    private DefaultTableModel tableModel;
    private JButton addButton;

    public TracksTab() {
        setLayout(new BorderLayout());

        // Create table
        String[] columns = { "Track ID", "Name", "Album", "Genre", "Media Type", "Composer", "Price" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tracksTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(tracksTable);

        // Add button panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add New Track");
        addButton.addActionListener(e -> showAddTrackDialog());
        buttonPanel.add(addButton);

        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadTracksData();
    }

    private void loadTracksData() {
        tableModel.setRowCount(0);

        String sql = "SELECT t.TrackId, t.Name, a.Title as Album, g.Name as Genre, " +
                "mt.Name as MediaType, t.Composer, t.UnitPrice " +
                "FROM Track t " +
                "JOIN Album a ON t.AlbumId = a.AlbumId " +
                "JOIN Genre g ON t.GenreId = g.GenreId " +
                "JOIN MediaType mt ON t.MediaTypeId = mt.MediaTypeId " +
                "ORDER BY t.TrackId DESC LIMIT 100";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("TrackId"),
                        rs.getString("Name"),
                        rs.getString("Album"),
                        rs.getString("Genre"),
                        rs.getString("MediaType"),
                        rs.getString("Composer") != null ? rs.getString("Composer") : "",
                        rs.getBigDecimal("UnitPrice")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading tracks: " + e.getMessage());
        }
    }

    private void showAddTrackDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Track", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Form fields
        JTextField nameField = new JTextField(20);
        JTextField composerField = new JTextField(20);
        JTextField priceField = new JTextField(20);

        // Dropdowns
        JComboBox<String> albumCombo = new JComboBox<>();
        JComboBox<String> genreCombo = new JComboBox<>();
        JComboBox<String> mediaTypeCombo = new JComboBox<>();

        // Store IDs for selected values
        Map<String, Integer> albumIdMap = new HashMap<>();
        Map<String, Integer> genreIdMap = new HashMap<>();
        Map<String, Integer> mediaTypeIdMap = new HashMap<>();

        // Load data for dropdowns
        loadAlbums(albumCombo, albumIdMap);
        loadGenres(genreCombo, genreIdMap);
        loadMediaTypes(mediaTypeCombo, mediaTypeIdMap);

        // Layout form
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Track Name:"), gbc);
        gbc.gridx = 1;
        dialog.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        dialog.add(new JLabel("Album:"), gbc);
        gbc.gridx = 1;
        dialog.add(albumCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        dialog.add(new JLabel("Genre:"), gbc);
        gbc.gridx = 1;
        dialog.add(genreCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        dialog.add(new JLabel("Media Type:"), gbc);
        gbc.gridx = 1;
        dialog.add(mediaTypeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        dialog.add(new JLabel("Composer:"), gbc);
        gbc.gridx = 1;
        dialog.add(composerField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        dialog.add(new JLabel("Unit Price:"), gbc);
        gbc.gridx = 1;
        dialog.add(priceField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        saveButton.addActionListener(e -> {
            try {
                String trackName = nameField.getText().trim();
                if (trackName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Track name is required");
                    return;
                }

                String selectedAlbum = (String) albumCombo.getSelectedItem();
                String selectedGenre = (String) genreCombo.getSelectedItem();
                String selectedMediaType = (String) mediaTypeCombo.getSelectedItem();
                double price = Double.parseDouble(priceField.getText().trim());

                int albumId = albumIdMap.get(selectedAlbum);
                int genreId = genreIdMap.get(selectedGenre);
                int mediaTypeId = mediaTypeIdMap.get(selectedMediaType);

                String insertSql = "INSERT INTO Track (Name, AlbumId, MediaTypeId, GenreId, Composer, UnitPrice) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                try (Connection conn = DatabaseConnection.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setString(1, trackName);
                    pstmt.setInt(2, albumId);
                    pstmt.setInt(3, mediaTypeId);
                    pstmt.setInt(4, genreId);
                    pstmt.setString(5, composerField.getText().trim());
                    pstmt.setDouble(6, price);
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(dialog, "Track added successfully!");
                    dialog.dispose();
                    loadTracksData();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid price format");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error adding track: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void loadAlbums(JComboBox<String> combo, Map<String, Integer> idMap) {
        String sql = "SELECT AlbumId, Title FROM Album ORDER BY Title";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String title = rs.getString("Title");
                int id = rs.getInt("AlbumId");
                combo.addItem(title);
                idMap.put(title, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadGenres(JComboBox<String> combo, Map<String, Integer> idMap) {
        String sql = "SELECT GenreId, Name FROM Genre ORDER BY Name";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("Name");
                int id = rs.getInt("GenreId");
                combo.addItem(name);
                idMap.put(name, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMediaTypes(JComboBox<String> combo, Map<String, Integer> idMap) {
        String sql = "SELECT MediaTypeId, Name FROM MediaType ORDER BY Name";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("Name");
                int id = rs.getInt("MediaTypeId");
                combo.addItem(name);
                idMap.put(name, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}