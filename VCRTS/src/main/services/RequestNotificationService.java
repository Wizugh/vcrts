package services;

import controller.ServerController;
import models.Request;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RequestNotificationService {
    private static RequestNotificationService instance;
    private ServerController serverController;
    private Map<Integer, Set<Integer>> monitoredRequestIds;
    private Map<Integer, String> lastKnownStatus;
    private JFrame parentFrame;
    private int userId;
    private javax.swing.Timer refreshTimer;
    private static final int REFRESH_INTERVAL = 2000; // 2 seconds

    private RequestNotificationService() {
        serverController = ServerController.getInstance();
        monitoredRequestIds = new ConcurrentHashMap<>();
        lastKnownStatus = new ConcurrentHashMap<>();
    }

    public static synchronized RequestNotificationService getInstance() {
        if (instance == null) {
            instance = new RequestNotificationService();
        }
        return instance;
    }

    public void startMonitoring(int userId, JFrame parentFrame) {
        this.userId = userId;
        this.parentFrame = parentFrame;
        
        // Initialize the set of monitored request IDs for this user if needed
        monitoredRequestIds.putIfAbsent(userId, new HashSet<>());
        
        // Update the list of monitored requests
        updateMonitoredRequests();
        
        // Start the refresh timer if not already running
        if (refreshTimer == null || !refreshTimer.isRunning()) {
            refreshTimer = new javax.swing.Timer(REFRESH_INTERVAL, e -> checkForStatusChanges());
            refreshTimer.start();
        }
    }

    public void stopMonitoring() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    private void updateMonitoredRequests() {
        List<Request> requests = serverController.getClientRequests(userId);
        Set<Integer> requestIds = monitoredRequestIds.get(userId);
        
        if (requests != null) {
            for (Request request : requests) {
                int requestId = request.getRequestId();
                
                // Add this request to the monitored set
                requestIds.add(requestId);
                
                // Update last known status if not already tracked
                if (!lastKnownStatus.containsKey(requestId)) {
                    lastKnownStatus.put(requestId, request.getStatus());
                }
            }
        }
    }

    private void checkForStatusChanges() {
        updateMonitoredRequests(); // Update our list first
        
        List<Request> currentRequests = serverController.getClientRequests(userId);
        if (currentRequests == null || currentRequests.isEmpty()) {
            return;
        }
        
        for (Request request : currentRequests) {
            int requestId = request.getRequestId();
            String currentStatus = request.getStatus();
            String previousStatus = lastKnownStatus.get(requestId);
            
            // Check if status changed from PENDING to something else
            if (previousStatus != null && 
                Request.STATUS_PENDING.equals(previousStatus) && 
                !Request.STATUS_PENDING.equals(currentStatus)) {
                
                // Status changed - notify the user with an invasive popup
                showNotificationPopup(request);
                
                // Update the last known status
                lastKnownStatus.put(requestId, currentStatus);
            }
        }
    }

    private void showNotificationPopup(Request request) {
        SwingUtilities.invokeLater(() -> {
            // Create a custom dialog for more invasive appearance
            JDialog dialog = new JDialog(parentFrame);
            dialog.setUndecorated(true); // Remove window decorations
            dialog.setModal(true);
            
            // Set size and position (centered on screen)
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setSize(450, 250);
            dialog.setLocation(
                (screenSize.width - dialog.getWidth()) / 2,
                (screenSize.height - dialog.getHeight()) / 2
            );
            
            // Make dialog background semi-transparent
            dialog.setOpacity(0.95f);
            
            // Create panel with border
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createLineBorder(Color.RED, 5));
            
            // Create a blinking effect for the title
            JLabel titleLabel = new JLabel("REQUEST STATUS UPDATED", JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
            
            if (Request.STATUS_APPROVED.equals(request.getStatus())) {
                titleLabel.setForeground(new Color(0, 150, 0)); // Green
                panel.setBackground(new Color(220, 255, 220)); // Light green
            } else {
                titleLabel.setForeground(new Color(150, 0, 0)); // Red
                panel.setBackground(new Color(255, 220, 220)); // Light red
            }
            
            // Start a timer to make the title blink
            javax.swing.Timer blinkTimer = new javax.swing.Timer(500, e -> {
                if (titleLabel.getForeground().equals(Color.WHITE)) {
                    if (Request.STATUS_APPROVED.equals(request.getStatus())) {
                        titleLabel.setForeground(new Color(0, 150, 0));
                    } else {
                        titleLabel.setForeground(new Color(150, 0, 0));
                    }
                } else {
                    titleLabel.setForeground(Color.WHITE);
                }
            });
            blinkTimer.start();
            
            panel.add(titleLabel, BorderLayout.NORTH);
            
            // Request type and status
            String requestType = request.getRequestType().equals(Request.TYPE_REGISTER_VEHICLE) ?
                "Vehicle Registration" : "Job Addition";
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            JLabel typeLabel = new JLabel("Request Type: " + requestType);
            typeLabel.setFont(new Font("Arial", Font.BOLD, 16));
            typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel statusLabel = new JLabel("Status: " + request.getStatus());
            statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel messageLabel = new JLabel("Message: " + request.getResponseMessage());
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            contentPanel.add(typeLabel);
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(statusLabel);
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(messageLabel);
            
            panel.add(contentPanel, BorderLayout.CENTER);
            
            // Close button
            JButton closeButton = new JButton("CLOSE");
            closeButton.setFont(new Font("Arial", Font.BOLD, 16));
            closeButton.setPreferredSize(new Dimension(100, 40));
            closeButton.addActionListener(e -> {
                blinkTimer.stop();
                dialog.dispose();
            });
            
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            buttonPanel.add(closeButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.add(panel);
            
            // Play a sound (optional)
            Toolkit.getDefaultToolkit().beep();
            
            // Display the dialog
            dialog.setVisible(true);
        });
    }
}
