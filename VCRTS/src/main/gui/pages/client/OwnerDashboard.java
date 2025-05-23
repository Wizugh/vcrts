package gui.pages.server;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import dao.VehicleDAO;
import models.Vehicle;
import services.RequestNotificationService;

public class OwnerDashboard extends JPanel {
    private static final Logger logger = Logger.getLogger(OwnerDashboard.class.getName());

    private int ownerId;
    private String ownerName;
    private VehicleDAO vehicleDAO = new VehicleDAO();
    private RequestNotificationService notificationService;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    // Components for the vehicle list view
    private JTable vehicleTable;
    private DefaultTableModel tableModel;

    public OwnerDashboard(int ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.notificationService = RequestNotificationService.getInstance();
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Start monitoring for request updates
        SwingUtilities.invokeLater(() -> {
            notificationService.startMonitoring(ownerId, SwingUtilities.getWindowAncestor(this) instanceof JFrame ? 
                (JFrame)SwingUtilities.getWindowAncestor(this) : null);
        });

        // Top navigation with title
        JPanel topNav = new JPanel(new BorderLayout());
        topNav.setBackground(new Color(43, 43, 43));
        JLabel titleLabel = new JLabel("Owner Dashboard - Vehicle Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        topNav.add(titleLabel, BorderLayout.CENTER);
        add(topNav, BorderLayout.NORTH);

        // Navigation panel to switch between registration form and list view
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        navPanel.setBackground(new Color(43, 43, 43));
        JButton viewVehiclesButton = new JButton("View Registered Vehicles");
        JButton addMoreVehiclesButton = new JButton("Register New Vehicle");
        navPanel.add(addMoreVehiclesButton);
        navPanel.add(viewVehiclesButton);
        add(navPanel, BorderLayout.SOUTH);

        // Content panel with CardLayout: one card for OwnerForm and one for vehicle list view.
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Create OwnerForm to register vehicles
        contentPanel.add(new OwnerForm(ownerId, ownerName), "form");
        contentPanel.add(createVehicleListPanel(), "list");
        add(contentPanel, BorderLayout.CENTER);

        addMoreVehiclesButton.addActionListener(e -> cardLayout.show(contentPanel, "form"));
        viewVehiclesButton.addActionListener(e -> {
            refreshVehicleTable();
            cardLayout.show(contentPanel, "list");
        });
    }
    
    // Constructor for backward compatibility
    public OwnerDashboard(int ownerId) {
        this(ownerId, "Unknown");
    }

    private JPanel createVehicleListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(43, 43, 43));

        JLabel listTitle = new JLabel("Registered Vehicles", SwingConstants.CENTER);
        listTitle.setFont(new Font("Arial", Font.BOLD, 20));
        listTitle.setForeground(Color.WHITE);
        panel.add(listTitle, BorderLayout.NORTH);

        String[] columnNames = {"Owner ID", "Model", "Make", "Year", "VIN", "Residency Time", "Registered At"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        vehicleTable = new JTable(tableModel);
        vehicleTable.setBackground(new Color(230, 230, 230));
        vehicleTable.setForeground(Color.BLACK);
        vehicleTable.setRowHeight(30);
        vehicleTable.setFont(new Font("Arial", Font.PLAIN, 14));

        // Center-align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < vehicleTable.getColumnCount(); i++) {
            vehicleTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JTableHeader header = vehicleTable.getTableHeader();
        header.setBackground(new Color(200, 200, 200));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Arial", Font.BOLD, 15));

        // Center table header text
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        JScrollPane scrollPane = new JScrollPane(vehicleTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBackground(new Color(43, 43, 43));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshVehicleTable());
        controlPanel.add(refreshButton);
        panel.add(controlPanel, BorderLayout.SOUTH);

        // Use javax.swing.Timer explicitly to avoid ambiguity
        new javax.swing.Timer(10000, e -> refreshVehicleTable()).start();
        refreshVehicleTable();
        return panel;
    }

    public void refreshVehicleTable() {
        try {
            tableModel.setRowCount(0);
            List<Vehicle> vehicles = vehicleDAO.getVehiclesByOwner(ownerId);
            for (Vehicle v : vehicles) {
                tableModel.addRow(new Object[]{
                        v.getOwnerId(),
                        v.getModel(),
                        v.getMake(),
                        v.getYear(),
                        v.getVin(),
                        v.getResidencyTime(),
                        v.getRegisteredTimestamp()
                });
            }
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Error refreshing vehicle table: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, "Error loading vehicle data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
