package com.chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;

public class NotificationsTab extends JPanel {
    private JTabbedPane innerTabPane;
    private CustomerCRUDPanel crudPanel;
    private InactiveCustomersPanel inactivePanel;

    public NotificationsTab() {
        setLayout(new BorderLayout());

        innerTabPane = new JTabbedPane();
        crudPanel = new CustomerCRUDPanel();
        inactivePanel = new InactiveCustomersPanel();

        innerTabPane.addTab("Customer CRUD", crudPanel);
        innerTabPane.addTab("Inactive Customers", inactivePanel);

        add(innerTabPane, BorderLayout.CENTER);
    }

    // Inner class for Customer CRUD
    class CustomerCRUDPanel extends JPanel {
        private JTable customerTable;
        private DefaultTableModel tableModel;
        private JTextField searchField;
        private JButton addButton, editButton, deleteButton, refreshButton;

        public CustomerCRUDPanel() {
            setLayout(new BorderLayout());

            // Toolbar
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            addButton = new JButton("Add Customer");
            editButton = new JButton("Edit Customer");
            deleteButton = new JButton("Delete Customer");
            refreshButton = new JButton("Refresh");
            searchField = new JTextField(15);

            toolbar.add(new JLabel("Search:"));
            toolbar.add(searchField);
            toolbar.add(addButton);
            toolbar.add(editButton);
            toolbar.add(deleteButton);
            toolbar.add(refreshButton);

            // Table
            String[] columns = { "Customer ID", "First Name", "Last Name", "Email", "Phone", "Country" };
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            customerTable = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(customerTable);

            add(toolbar, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            // Event handlers
            addButton.addActionListener(e -> showCustomerDialog(null));
            editButton.addActionListener(e -> {
                int selectedRow = customerTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int customerId = (int) tableModel.getValueAt(selectedRow, 0);
                    showCustomerDialog(customerId);
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a customer to edit");
                }
            });
            deleteButton.addActionListener(e -> deleteCustomer());
            refreshButton.addActionListener(e -> loadCustomers());
            searchField.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyReleased(java.awt.event.KeyEvent evt) {
                    filterCustomers();
                }
            });

            loadCustomers();
        }

        private void loadCustomers() {
            tableModel.setRowCount(0);
            String sql = "SELECT CustomerId, FirstName, LastName, Email, Phone, Country " +
                    "FROM Customer ORDER BY CustomerId";

            try (Connection conn = DatabaseConnection.getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Object[] row = {
                            rs.getInt("CustomerId"),
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getString("Email"),
                            rs.getString("Phone") != null ? rs.getString("Phone") : "",
                            rs.getString("Country")
                    };
                    tableModel.addRow(row);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading customers: " + e.getMessage());
            }
        }

        private void filterCustomers() {
            String searchText = searchField.getText().trim();
            if (searchText.isEmpty()) {
                loadCustomers();
                return;
            }

            // Clear and reload with filter
            tableModel.setRowCount(0);
            String sql = "SELECT CustomerId, FirstName, LastName, Email, Phone, Country " +
                    "FROM Customer " +
                    "WHERE FirstName LIKE ? OR LastName LIKE ? OR Country LIKE ? " +
                    "ORDER BY CustomerId";

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                String pattern = "%" + searchText + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                pstmt.setString(3, pattern);

                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Object[] row = {
                            rs.getInt("CustomerId"),
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getString("Email"),
                            rs.getString("Phone") != null ? rs.getString("Phone") : "",
                            rs.getString("Country")
                    };
                    tableModel.addRow(row);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void showCustomerDialog(Integer customerId) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    customerId == null ? "Add Customer" : "Edit Customer", true);
            dialog.setSize(450, 350);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Form fields
            JTextField firstNameField = new JTextField(20);
            JTextField lastNameField = new JTextField(20);
            JTextField emailField = new JTextField(20);
            JTextField phoneField = new JTextField(20);
            JTextField countryField = new JTextField(20);

            // Load data if editing
            if (customerId != null) {
                String sql = "SELECT FirstName, LastName, Email, Phone, Country FROM Customer WHERE CustomerId = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, customerId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        firstNameField.setText(rs.getString("FirstName"));
                        lastNameField.setText(rs.getString("LastName"));
                        emailField.setText(rs.getString("Email"));
                        phoneField.setText(rs.getString("Phone") != null ? rs.getString("Phone") : "");
                        countryField.setText(rs.getString("Country"));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Layout
            gbc.gridx = 0;
            gbc.gridy = 0;
            dialog.add(new JLabel("First Name:*"), gbc);
            gbc.gridx = 1;
            dialog.add(firstNameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            dialog.add(new JLabel("Last Name:*"), gbc);
            gbc.gridx = 1;
            dialog.add(lastNameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            dialog.add(new JLabel("Email:*"), gbc);
            gbc.gridx = 1;
            dialog.add(emailField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            dialog.add(new JLabel("Phone:"), gbc);
            gbc.gridx = 1;
            dialog.add(phoneField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            dialog.add(new JLabel("Country:*"), gbc);
            gbc.gridx = 1;
            dialog.add(countryField, gbc);

            // Buttons
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0;
            gbc.gridy = 5;
            gbc.gridwidth = 2;
            dialog.add(buttonPanel, gbc);

            saveButton.addActionListener(e -> {
                if (firstNameField.getText().trim().isEmpty() ||
                        lastNameField.getText().trim().isEmpty() ||
                        emailField.getText().trim().isEmpty() ||
                        countryField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please fill all required fields (*)");
                    return;
                }

                try {
                    if (customerId == null) {
                        // Insert
                        String sql = "INSERT INTO Customer (FirstName, LastName, Email, Phone, Country) " +
                                "VALUES (?, ?, ?, ?, ?)";
                        try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, firstNameField.getText().trim());
                            pstmt.setString(2, lastNameField.getText().trim());
                            pstmt.setString(3, emailField.getText().trim());
                            pstmt.setString(4, phoneField.getText().trim());
                            pstmt.setString(5, countryField.getText().trim());
                            pstmt.executeUpdate();
                        }
                    } else {
                        // Update
                        String sql = "UPDATE Customer SET FirstName=?, LastName=?, Email=?, Phone=?, Country=? " +
                                "WHERE CustomerId=?";
                        try (Connection conn = DatabaseConnection.getConnection();
                                PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, firstNameField.getText().trim());
                            pstmt.setString(2, lastNameField.getText().trim());
                            pstmt.setString(3, emailField.getText().trim());
                            pstmt.setString(4, phoneField.getText().trim());
                            pstmt.setString(5, countryField.getText().trim());
                            pstmt.setInt(6, customerId);
                            pstmt.executeUpdate();
                        }
                    }
                    dialog.dispose();
                    loadCustomers();
                    JOptionPane.showMessageDialog(this, "Customer saved successfully!");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Error saving customer: " + ex.getMessage());
                }
            });

            cancelButton.addActionListener(e -> dialog.dispose());
            dialog.setVisible(true);
        }

        private void deleteCustomer() {
            int selectedRow = customerTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a customer to delete");
                return;
            }

            int customerId = (int) tableModel.getValueAt(selectedRow, 0);
            String customerName = tableModel.getValueAt(selectedRow, 1) + " " + tableModel.getValueAt(selectedRow, 2);

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete " + customerName
                            + "?\nThis will also delete all associated invoices and invoice lines.",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseConnection.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Customer WHERE CustomerId = ?")) {
                    pstmt.setInt(1, customerId);
                    pstmt.executeUpdate();
                    loadCustomers();
                    JOptionPane.showMessageDialog(this, "Customer deleted successfully!");
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error deleting customer: " + e.getMessage());
                }
            }
        }
    }

    // Inner class for Inactive Customers
    class InactiveCustomersPanel extends JPanel {
        private JTable inactiveTable;
        private DefaultTableModel tableModel;
        private JTextField searchField;
        private TableRowSorter<DefaultTableModel> sorter;

        public InactiveCustomersPanel() {
            setLayout(new BorderLayout());

            // Search panel
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchField = new JTextField(20);
            searchField.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyReleased(java.awt.event.KeyEvent evt) {
                    filterTable();
                }
            });
            searchPanel.add(new JLabel("Search by name or country:"));
            searchPanel.add(searchField);

            JButton refreshButton = new JButton("Refresh");
            refreshButton.addActionListener(e -> loadInactiveCustomers());
            searchPanel.add(refreshButton);

            // Table
            String[] columns = { "Customer ID", "First Name", "Last Name", "Email", "Country", "Last Invoice Date",
                    "Status" };
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            inactiveTable = new JTable(tableModel);
            sorter = new TableRowSorter<>(tableModel);
            inactiveTable.setRowSorter(sorter);

            JScrollPane scrollPane = new JScrollPane(inactiveTable);

            add(searchPanel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);

            loadInactiveCustomers();
        }

        private void loadInactiveCustomers() {
            tableModel.setRowCount(0);

            String sql = "SELECT c.CustomerId, c.FirstName, c.LastName, c.Email, c.Country, " +
                    "MAX(i.InvoiceDate) as LastInvoiceDate, " +
                    "COUNT(i.InvoiceId) as InvoiceCount " +
                    "FROM Customer c " +
                    "LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                    "GROUP BY c.CustomerId, c.FirstName, c.LastName, c.Email, c.Country " +
                    "HAVING InvoiceCount = 0 OR LastInvoiceDate < DATE_SUB(NOW(), INTERVAL 2 YEAR) " +
                    "ORDER BY c.LastName, c.FirstName";

            try (Connection conn = DatabaseConnection.getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    String lastInvoice = rs.getString("LastInvoiceDate");
                    String status;
                    if (lastInvoice == null) {
                        status = "Never purchased";
                    } else {
                        status = "Inactive (>2 years)";
                    }

                    Object[] row = {
                            rs.getInt("CustomerId"),
                            rs.getString("FirstName"),
                            rs.getString("LastName"),
                            rs.getString("Email"),
                            rs.getString("Country"),
                            lastInvoice != null ? lastInvoice.substring(0, Math.min(10, lastInvoice.length()))
                                    : "No invoices",
                            status
                    };
                    tableModel.addRow(row);
                }

                if (tableModel.getRowCount() == 0) {
                    Object[] noResults = { 0, "No", "inactive", "customers", "found", "", "" };
                    tableModel.addRow(noResults);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading inactive customers: " + e.getMessage());
            }
        }

        private void filterTable() {
            String searchText = searchField.getText().trim();
            if (searchText.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 1, 2, 4));
            }
        }
    }
}