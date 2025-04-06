package gui.pages.server;

import controller.ServerController;
import models.Request;
import models.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List; // Explicitly import java.util.List
import java.util.ArrayList;

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
        clientsPanel.setBorder(BorderFactory.createTitledBorder("Pending Requests Summary"));
        
        // Summary label
        JLabel summaryLabel = new JLabel("Pending requests: 0", SwingConstants.CENTER);
        summaryLabel.setFont(new Font("Arial", Font.BOLD, 14));
        clientsPanel.add(summaryLabel, BorderLayout.CENTER);
        
        // Create a split pane with clients panel on top
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, clientsPanel, scrollPane);
        splitPane.setDividerLocation(50);
        add(splitPane, BorderLayout.CENTER);
        
        // Add action listeners
        approveButton.addActionListener(e -> approveSelectedRequest());
        rejectButton.addActionListener(e -> rejectSelectedRequest());
        refreshButton.addActionListener(e -> refreshRequests());
        
        // Add selection listener to enable/disable buttons
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = requestTable.getSelectedRow() != -1;
            approveButton.setEnabled(hasSelection);
            rejectButton.setEnabled(hasSelection);
        });
        
        // Initial data load
        refreshRequests();
        
        // Start auto-refresh thread
        new Thread(() -> {
            try {
                while (true) {
                    // Refresh every 5 seconds
                    Thread.sleep(5000);
                    
                    // Update UI on event dispatch thread
                    SwingUtilities.invokeLater(() -> {
                        refreshRequests();
                        updateSummaryLabel(summaryLabel);
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Updates the pending requests summary label
     */
    private void updateSummaryLabel(JLabel label) {
        int pendingCount = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Request.STATUS_PENDING.equals(tableModel.getValueAt(i, 5))) {
                pendingCount++;
            }
        }
        
        label.setText("Pending requests: " + pendingCount);
    }
    
    /**
     * Refreshes the requests table with the latest data.
     */
    public void refreshRequests() {
        new Thread(() -> {
            final List<Request> requests = serverController.getPendingRequests();
            
            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                
                if (requests == null || requests.isEmpty()) {
                    // No requests found
                    updateSummaryLabel(new JLabel("Pending requests: 0"));
                    return;
                }
                
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
            });
        }).start();
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
        
        // Process approval in a separate thread
        final String finalResponseMessage = responseMessage;
        final int finalRequestId = requestId;
        
        new Thread(() -> {
            boolean success = serverController.approveRequest(finalRequestId, finalResponseMessage);
            
            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    JOptionPane.showMessageDialog(CloudControllerRequestPanel.this, 
                        "Request approved successfully.", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    refreshRequests();
                    responseArea.setText("");
                } else {
                    JOptionPane.showMessageDialog(CloudControllerRequestPanel.this, 
                        "Failed to approve request. Please try again.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
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
        
        // Process rejection in a separate thread
        final String finalResponseMessage = responseMessage;
        final int finalRequestId = requestId;
        
        new Thread(() -> {
            boolean success = serverController.rejectRequest(finalRequestId, finalResponseMessage);
            
            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    JOptionPane.showMessageDialog(CloudControllerRequestPanel.this, 
                        "Request rejected successfully.", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    refreshRequests();
                    responseArea.setText("");
                } else {
                    JOptionPane.showMessageDialog(CloudControllerRequestPanel.this, 
                        "Failed to reject request. Please try again.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }
}
