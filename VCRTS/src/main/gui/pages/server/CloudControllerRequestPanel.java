package gui.pages.server;

import controller.ServerController;
import models.Request;
import models.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel for viewing and approving/rejecting client requests in the Cloud Controller.
 */
public class CloudControllerRequestPanel extends JPanel {
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private JButton approveButton, rejectButton, refreshButton;
    private JTextArea responseArea;
    
    private ServerController serverController;
    
    public CloudControllerRequestPanel() {
        setLayout(new BorderLayout());
        serverController = ServerController.getInstance();
        
        // Create table for requests
        String[] columnNames = {"Request ID", "Client ID", "Client Name", "Type", "Data", "Status", "Timestamp"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override 
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        requestTable = new JTable(tableModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Center-align table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < requestTable.getColumnCount(); i++) {
            requestTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(requestTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // Top part of control panel for response area
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response Message"));
        
        responseArea = new JTextArea(3, 40);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        JScrollPane responseScroll = new JScrollPane(responseArea);
        responsePanel.add(responseScroll, BorderLayout.CENTER);
        
        controlPanel.add(responsePanel, BorderLayout.CENTER);
        
        // Bottom part of control panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        approveButton = new JButton("Approve Request");
        rejectButton = new JButton("Reject Request");
        refreshButton = new JButton("Refresh Requests");
        
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(refreshButton);
        
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.SOUTH);
        
        // Connect clients panel for viewing connected clients
        JPanel clientsPanel = new JPanel(new BorderLayout());
        clientsPanel.setBorder(BorderFactory.createTitledBorder("Connected Clients"));
        
        DefaultTableModel clientsModel = new DefaultTableModel(
            new String[]{"Client ID", "Name", "Role"}, 0);
        JTable clientsTable = new JTable(clientsModel);
        JScrollPane clientsScroll = new JScrollPane(clientsTable);
        clientsPanel.add(clientsScroll, BorderLayout.CENTER);
        
        JButton refreshClientsButton = new JButton("Refresh Clients");
        refreshClientsButton.addActionListener(e -> {
            updateConnectedClients(clientsModel);
        });
        clientsPanel.add(refreshClientsButton, BorderLayout.SOUTH);
        
        // Create a split pane with clients panel on top
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, clientsPanel, scrollPane);
        splitPane.setDividerLocation(150);
        add(splitPane, BorderLayout.CENTER);
        
        // Add action listeners
        approveButton.addActionListener(e -> approveSelectedRequest());
        rejectButton.addActionListener(e -> rejectSelectedRequest());
        refreshButton.addActionListener(e -> refreshRequests());
        
        // Initial data load
        refreshRequests();
        updateConnectedClients(clientsModel);
    }
    
    /**
     * Refreshes the requests table with the latest data.
     */
    public void refreshRequests() {
        tableModel.setRowCount(0);
        List<Request> requests = serverController.getPendingRequests();
        
        for (Request request : requests) {
            tableModel.addRow(new Object[] {
                request.getRequestId(),
                request.getClientId(),
                request.getClientName(),
                request.getRequestType(),
                request.getRequestData(),
                request.getStatus(),
                request.getTimestamp()
            });
        }
        
        // Enable/disable buttons based on selection
        boolean hasSelection = requestTable.getSelectedRow() != -1;
        approveButton.setEnabled(hasSelection);
        rejectButton.setEnabled(hasSelection);
    }
    
    /**
     * Updates the connected clients table.
     */
    private void updateConnectedClients(DefaultTableModel model) {
        model.setRowCount(0);
        List<User> clients = serverController.getConnectedClients();
        
        for (User client : clients) {
            model.addRow(new Object[] {
                client.getUserId(),
                client.getFullName(),
                client.getRole()
            });
        }
    }
    
    /**
     * Approves the selected request.
     */
    private void approveSelectedRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a request to approve.", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int requestId = (int) tableModel.getValueAt(selectedRow, 0);
        String responseMessage = responseArea.getText().trim();
        
        if (responseMessage.isEmpty()) {
            responseMessage = "Request approved by Cloud Controller.";
        }
        
        boolean success = serverController.approveRequest(requestId, responseMessage);
        
        if (success) {
            JOptionPane.showMessageDialog(this, 
                "Request approved successfully.", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            refreshRequests();
            responseArea.setText("");
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to approve request. Please try again.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Rejects the selected request.
     */
    private void rejectSelectedRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a request to reject.", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int requestId = (int) tableModel.getValueAt(selectedRow, 0);
        String responseMessage = responseArea.getText().trim();
        
        if (responseMessage.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please provide a reason for rejection.", 
                "Missing Information", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        boolean success = serverController.rejectRequest(requestId, responseMessage);
        
        if (success) {
            JOptionPane.showMessageDialog(this, 
                "Request rejected successfully.", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            refreshRequests();
            responseArea.setText("");
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to reject request. Please try again.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
