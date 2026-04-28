package com.chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.math.BigDecimal;

public class ReportTab extends JPanel {
    private JTable reportTable;
    private DefaultTableModel tableModel;

    public ReportTab() {
        setLayout(new BorderLayout());

        String[] columns = { "Genre", "Total Revenue ($)" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(reportTable);

        JLabel titleLabel = new JLabel("Total Revenue Per Genre", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        refreshReport();
    }

    public void refreshReport() {
        tableModel.setRowCount(0);

        String sql = "SELECT g.Name as Genre, SUM(il.UnitPrice * il.Quantity) as TotalRevenue " +
                "FROM InvoiceLine il " +
                "JOIN Track t ON il.TrackId = t.TrackId " +
                "JOIN Genre g ON t.GenreId = g.GenreId " +
                "JOIN Invoice i ON il.InvoiceId = i.InvoiceId " +
                "GROUP BY g.GenreId, g.Name " +
                "ORDER BY TotalRevenue DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Object[] row = {
                        rs.getString("Genre"),
                        rs.getBigDecimal("TotalRevenue")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage());
        }
    }
}