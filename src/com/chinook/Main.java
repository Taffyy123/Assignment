package com.chinook;

import javax.swing.*;
import java.awt.*;

public class Main {
    private JTabbedPane tabbedPane;
    private JFrame frame;
    
    // Tab components
    private EmployeeTab employeeTab;
    private TracksTab tracksTab;
    private ReportTab reportTab;
    private NotificationsTab notificationsTab;
    private RecommendationsTab recommendationsTab;
    
    public Main() {
        frame = new JFrame("Chinook Music Store Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 800);
        frame.setLocationRelativeTo(null);
        
        tabbedPane = new JTabbedPane();
        
        // Initialize tabs
        employeeTab = new EmployeeTab();
        tracksTab = new TracksTab();
        reportTab = new ReportTab();
        notificationsTab = new NotificationsTab();
        recommendationsTab = new RecommendationsTab();
        
        // Add tabs
        tabbedPane.addTab("Employees", employeeTab);
        tabbedPane.addTab("Tracks", tracksTab);
        tabbedPane.addTab("Genre Revenue Report", reportTab);
        tabbedPane.addTab("Customer Notifications", notificationsTab);
        tabbedPane.addTab("Customer Recommendations", recommendationsTab);
        
        // Add listener to refresh report tab when selected
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 2) {
                reportTab.refreshReport();
            }
        });
        
        frame.add(tabbedPane);
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Main();
        });
    }
}