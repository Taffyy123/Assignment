package com.chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

public class RecommendationsTab extends JPanel {
    private JComboBox<String> customerCombo;
    private JLabel totalSpentLabel, totalPurchasesLabel, lastPurchaseLabel, favoriteGenreLabel;
    private JTable recommendationsTable;
    private DefaultTableModel tableModel;
    private Map<String, Integer> customerIdMap;
    private JPanel summaryPanel;
    private JPanel contentPanel;

    public RecommendationsTab() {
        setLayout(new BorderLayout(10, 10));

        // Customer selection panel
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectionPanel.add(new JLabel("Select Customer:"));
        customerCombo = new JComboBox<>();
        customerCombo.setPreferredSize(new Dimension(300, 25));
        customerCombo.addActionListener(e -> {
            if (customerCombo.getSelectedItem() != null && customerCombo.getSelectedItem() != "") {
                loadCustomerInsights();
            }
        });
        selectionPanel.add(customerCombo);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            loadCustomers();
            if (customerCombo.getItemCount() > 0) {
                loadCustomerInsights();
            }
        });
        selectionPanel.add(refreshButton);

        add(selectionPanel, BorderLayout.NORTH);

        // Main content panel
        contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Summary panel
        summaryPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Spending Summary"));
        summaryPanel.setPreferredSize(new Dimension(400, 120));

        summaryPanel.add(new JLabel("Total Amount Spent:"));
        totalSpentLabel = new JLabel("$0.00");
        summaryPanel.add(totalSpentLabel);

        summaryPanel.add(new JLabel("Total Number of Purchases:"));
        totalPurchasesLabel = new JLabel("0");
        summaryPanel.add(totalPurchasesLabel);

        summaryPanel.add(new JLabel("Most Recent Purchase:"));
        lastPurchaseLabel = new JLabel("N/A");
        summaryPanel.add(lastPurchaseLabel);

        summaryPanel.add(new JLabel("Favorite Genre:"));
        favoriteGenreLabel = new JLabel("N/A");
        summaryPanel.add(favoriteGenreLabel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.4;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        contentPanel.add(summaryPanel, gbc);

        // Recommendations panel
        JPanel recommendationsPanel = new JPanel(new BorderLayout());
        recommendationsPanel.setBorder(BorderFactory.createTitledBorder("Track Recommendations"));
        recommendationsPanel.setPreferredSize(new Dimension(600, 400));

        String[] columns = { "Track Name", "Genre", "Album", "Composer", "Price ($)" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recommendationsTable = new JTable(tableModel);
        recommendationsTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(recommendationsTable);
        recommendationsPanel.add(scrollPane, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.6;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        contentPanel.add(recommendationsPanel, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Initialize
        loadCustomers();
    }

    private void loadCustomers() {
        customerCombo.removeAllItems();
        customerIdMap = new HashMap<>();

        String sql = "SELECT CustomerId, FirstName, LastName FROM Customer ORDER BY LastName, FirstName";

        // Add a placeholder
        customerCombo.addItem("-- Select a Customer --");

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int customerId = rs.getInt("CustomerId");
                String firstName = rs.getString("FirstName");
                String lastName = rs.getString("LastName");
                String displayName = lastName + ", " + firstName + " (ID: " + customerId + ")";

                customerCombo.addItem(displayName);
                customerIdMap.put(displayName, customerId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading customers: " + e.getMessage());
        }
    }

    private void loadCustomerInsights() {
        String selected = (String) customerCombo.getSelectedItem();
        if (selected == null || selected.equals("-- Select a Customer --")) {
            // Reset labels
            totalSpentLabel.setText("$0.00");
            totalPurchasesLabel.setText("0");
            lastPurchaseLabel.setText("N/A");
            favoriteGenreLabel.setText("N/A");
            tableModel.setRowCount(0);
            return;
        }

        int customerId = customerIdMap.get(selected);

        // Load all insights
        loadSpendingSummary(customerId);
        loadFavoriteGenre(customerId);
        loadRecommendations(customerId);
    }

    private void loadSpendingSummary(int customerId) {
        String sql = "SELECT " +
                "SUM(il.UnitPrice * il.Quantity) as TotalSpent, " +
                "COUNT(DISTINCT i.InvoiceId) as TotalPurchases, " +
                "MAX(i.InvoiceDate) as LastPurchaseDate " +
                "FROM Customer c " +
                "LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                "LEFT JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId " +
                "WHERE c.CustomerId = ? " +
                "GROUP BY c.CustomerId";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                BigDecimal totalSpent = rs.getBigDecimal("TotalSpent");
                int totalPurchases = rs.getInt("TotalPurchases");
                String lastPurchaseDate = rs.getString("LastPurchaseDate");

                totalSpentLabel.setText(totalSpent != null ? "$" + totalSpent.toString() : "$0.00");
                totalPurchasesLabel.setText(String.valueOf(totalPurchases));
                lastPurchaseLabel
                        .setText(lastPurchaseDate != null ? lastPurchaseDate.substring(0, 10) : "No purchases");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFavoriteGenre(int customerId) {
        String sql = "SELECT g.Name as GenreName, COUNT(*) as PurchaseCount " +
                "FROM InvoiceLine il " +
                "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                "JOIN Track t ON il.TrackId = t.TrackId " +
                "JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE i.CustomerId = ? " +
                "GROUP BY g.GenreId, g.Name " +
                "ORDER BY PurchaseCount DESC " +
                "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String favoriteGenre = rs.getString("GenreName");
                favoriteGenreLabel.setText(favoriteGenre != null ? favoriteGenre : "None");
            } else {
                favoriteGenreLabel.setText("None");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            favoriteGenreLabel.setText("Error");
        }
    }

    private void loadRecommendations(int customerId) {
        tableModel.setRowCount(0);

        // First, get the customer's favorite genres (top 2)
        String genreSql = "SELECT g.GenreId, g.Name, COUNT(*) as PurchaseCount " +
                "FROM InvoiceLine il " +
                "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                "JOIN Track t ON il.TrackId = t.TrackId " +
                "JOIN Genre g ON t.GenreId = g.GenreId " +
                "WHERE i.CustomerId = ? " +
                "GROUP BY g.GenreId, g.Name " +
                "ORDER BY PurchaseCount DESC " +
                "LIMIT 2";

        java.util.List<Integer> favoriteGenreIds = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(genreSql)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                favoriteGenreIds.add(rs.getInt("GenreId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (favoriteGenreIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "This customer has no purchase history to base recommendations on.");
            return;
        }

        // Get tracks the customer already purchased
        String purchasedSql = "SELECT DISTINCT t.TrackId " +
                "FROM InvoiceLine il " +
                "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                "JOIN Track t ON il.TrackId = t.TrackId " +
                "WHERE i.CustomerId = ?";

        Set<Integer> purchasedTrackIds = new HashSet<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(purchasedSql)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                purchasedTrackIds.add(rs.getInt("TrackId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Build IN clause for favorite genres
        String genrePlaceholders = String.join(",", Collections.nCopies(favoriteGenreIds.size(), "?"));

        // Get recommendations based on favorite genres, excluding already purchased
        // tracks
        String recSql = "SELECT t.Name, g.Name as GenreName, a.Title as AlbumTitle, " +
                "t.Composer, t.UnitPrice " +
                "FROM Track t " +
                "JOIN Genre g ON t.GenreId = g.GenreId " +
                "JOIN Album a ON t.AlbumId = a.AlbumId " +
                "WHERE t.GenreId IN (" + genrePlaceholders + ") " +
                "AND t.TrackId NOT IN (SELECT DISTINCT il.TrackId " +
                "                     FROM InvoiceLine il " +
                "                     JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                "                     WHERE i.CustomerId = ?) " +
                "ORDER BY RAND() " +
                "LIMIT 15";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(recSql)) {

            // Set genre IDs
            int index = 1;
            for (int genreId : favoriteGenreIds) {
                pstmt.setInt(index++, genreId);
            }
            // Set customer ID for NOT IN subquery
            pstmt.setInt(index, customerId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Object[] row = {
                        rs.getString("Name"),
                        rs.getString("GenreName"),
                        rs.getString("AlbumTitle"),
                        rs.getString("Composer") != null ? rs.getString("Composer") : "Unknown",
                        rs.getBigDecimal("UnitPrice")
                };
                tableModel.addRow(row);
            }

            if (tableModel.getRowCount() == 0) {
                Object[] noResults = { "No recommendations available", "-", "-", "-", "-" };
                tableModel.addRow(noResults);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading recommendations: " + e.getMessage());
        }
    }
}