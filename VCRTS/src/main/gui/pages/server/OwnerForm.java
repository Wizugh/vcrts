package gui.pages.server;

import javax.swing.*;
import java.awt.*;
import controller.ServerController;
import models.Request;
import models.Vehicle;

public class OwnerForm extends JPanel {
    private int ownerId;
    private String ownerName;
    private JTextField modelField, makeField, yearField, vinField;
    private JSpinner hoursSpinner, minutesSpinner, secondsSpinner;
    private JLabel statusLabel;
    
    private ServerController serverController;

    public OwnerForm(int ownerId, String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        serverController = ServerController.getInstance();
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Owner Vehicle Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(Box.createVerticalStrut(50));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Use GridBagLayout for more control over form layout
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Create and add owner ID field (read-only)
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel ownerIdLabel = new JLabel("Owner ID:", SwingConstants.CENTER);
        formPanel.add(ownerIdLabel, gbc);

        gbc.gridx = 1;
        JTextField ownerIdField = new JTextField(String.valueOf(ownerId));
        ownerIdField.setEditable(false);
        ownerIdField.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(ownerIdField, gbc);

        // Create and add model field
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel modelLabel = new JLabel("Model:", SwingConstants.CENTER);
        formPanel.add(modelLabel, gbc);

        gbc.gridx = 1;
        modelField = new JTextField(15);
        modelField.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(modelField, gbc);

        // Create and add make field
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel makeLabel = new JLabel("Make:", SwingConstants.CENTER);
        formPanel.add(makeLabel, gbc);

        gbc.gridx = 1;
        makeField = new JTextField(15);
        makeField.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(makeField, gbc);

        // Create and add year field
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel yearLabel = new JLabel("Year:", SwingConstants.CENTER);
        formPanel.add(yearLabel, gbc);

        gbc.gridx = 1;
        yearField = new JTextField(15);
        yearField.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(yearField, gbc);

        // Create and add VIN field
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel vinLabel = new JLabel("VIN:", SwingConstants.CENTER);
        formPanel.add(vinLabel, gbc);

        gbc.gridx = 1;
        vinField = new JTextField(15);
        vinField.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(vinField, gbc);

        // Create and add residency time with spinners
        gbc.gridx = 0;
        gbc.gridy = 5;
        JLabel residencyTimeLabel = new JLabel("Residency Time:", SwingConstants.CENTER);
        formPanel.add(residencyTimeLabel, gbc);

        gbc.gridx = 1;
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Hours spinner (0-23)
        SpinnerNumberModel hoursModel = new SpinnerNumberModel(1, 0, 23, 1);
        hoursSpinner = new JSpinner(hoursModel);
        JSpinner.NumberEditor hoursEditor = new JSpinner.NumberEditor(hoursSpinner, "00");
        hoursSpinner.setEditor(hoursEditor);
        hoursSpinner.setPreferredSize(new Dimension(60, 25));

        // Minutes spinner (0-59)
        SpinnerNumberModel minutesModel = new SpinnerNumberModel(0, 0, 59, 1);
        minutesSpinner = new JSpinner(minutesModel);
        JSpinner.NumberEditor minutesEditor = new JSpinner.NumberEditor(minutesSpinner, "00");
        minutesSpinner.setEditor(minutesEditor);
        minutesSpinner.setPreferredSize(new Dimension(60, 25));

        // Seconds spinner (0-59)
        SpinnerNumberModel secondsModel = new SpinnerNumberModel(0, 0, 59, 1);
        secondsSpinner = new JSpinner(secondsModel);
        JSpinner.NumberEditor secondsEditor = new JSpinner.NumberEditor(secondsSpinner, "00");
        secondsSpinner.setEditor(secondsEditor);
        secondsSpinner.setPreferredSize(new Dimension(60, 25));

        timePanel.add(hoursSpinner);
        timePanel.add(new JLabel("h"));
        timePanel.add(minutesSpinner);
        timePanel.add(new JLabel("m"));
        timePanel.add(secondsSpinner);
        timePanel.add(new JLabel("s"));

        formPanel.add(timePanel, gbc);

        // Add status label
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        formPanel.add(statusLabel, gbc);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Add the submit button inside the formPanel
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;  // Span across two columns
        gbc.anchor = GridBagConstraints.CENTER;
        JButton submitButton = new JButton("Submit Vehicle Registration Request");
        formPanel.add(submitButton, gbc);

        submitButton.addActionListener(e -> {
            submitVehicleRegistrationRequest();
        });

        add(mainPanel, BorderLayout.CENTER);
        
        // Add a panel to view request status
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("Request Status"));
        
        String[] columns = {"Request ID", "Type", "Status", "Timestamp", "Response"};
        DefaultListModel<String> requestListModel = new DefaultListModel<>();
        JList<String> requestList = new JList<>(requestListModel);
        JScrollPane requestScroll = new JScrollPane(requestList);
        requestPanel.add(requestScroll, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("Refresh Request Status");
        refreshButton.addActionListener(e -> {
            refreshRequestStatus(requestListModel);
        });
        requestPanel.add(refreshButton, BorderLayout.SOUTH);
        
        add(requestPanel, BorderLayout.SOUTH);
        
        // Initial refresh
        refreshRequestStatus(requestListModel);
    }
    
    // Constructor that takes just ownerId for backward compatibility
    public OwnerForm(int ownerId) {
        this(ownerId, "Unknown");
    }

    /**
     * Submits a vehicle registration request to the server.
     */
    private void submitVehicleRegistrationRequest() {
        String model = modelField.getText().trim();
        String make = makeField.getText().trim();
        String year = yearField.getText().trim();
        String vin = vinField.getText().trim();

        // Format residency time from spinners
        int hours = (Integer) hoursSpinner.getValue();
        int minutes = (Integer) minutesSpinner.getValue();
        int seconds = (Integer) secondsSpinner.getValue();
        String residencyTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        if (model.isEmpty() || make.isEmpty() || year.isEmpty() || vin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (hours == 0 && minutes == 0 && seconds == 0) {
            JOptionPane.showMessageDialog(this, "Residency time must be greater than zero!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prepare the request data
        // Format: ownerId|model|make|year|vin|residencyTime
        String requestData = ownerId + "|" + model + "|" + make + "|" + year + "|" + vin + "|" + residencyTime;
        
        // Create and submit the request
        Request request = new Request(ownerId, ownerName, Request.TYPE_REGISTER_VEHICLE, requestData);
        
        // Submit the request in a separate thread
        new Thread(() -> {
            boolean success = serverController.submitRequest(request);
            
            // Update UI on the event dispatch thread
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    statusLabel.setText("Request submitted successfully! Awaiting approval.");
                    statusLabel.setForeground(new Color(0, 128, 0)); // Dark green
                    
                    // Clear fields after successful submission
                    modelField.setText("");
                    makeField.setText("");
                    yearField.setText("");
                    vinField.setText("");
                    hoursSpinner.setValue(1);
                    minutesSpinner.setValue(0);
                    secondsSpinner.setValue(0);
                    
                    // Refresh the request status
                    refreshRequestStatus(((JList<String>)((JScrollPane)((JPanel)getComponent(1)).getComponent(0)).getViewport().getView()).getModel());
                } else {
                    statusLabel.setText("Error submitting request. Please try again.");
                    statusLabel.setForeground(Color.RED);
                }
            });
        }).start();
    }
    
    /**
     * Refreshes the request status list.
     */
    private void refreshRequestStatus(DefaultListModel<String> model) {
        // Use a separate thread for fetching request data
        new Thread(() -> {
            List<Request> requests = serverController.getClientRequests(ownerId);
            
            // Update UI on the event dispatch thread
            SwingUtilities.invokeLater(() -> {
                model.clear();
                
                if (requests.isEmpty()) {
                    model.addElement("No requests found.");
                    return;
                }
                
                for (Request request : requests) {
                    String status = request.getStatus();
                    String statusText = "";
                    
                    if (Request.STATUS_PENDING.equals(status)) {
                        statusText = "PENDING - Awaiting approval";
                    } else if (Request.STATUS_APPROVED.equals(status)) {
                        statusText = "APPROVED - " + request.getResponseMessage();
                    } else if (Request.STATUS_REJECTED.equals(status)) {
                        statusText = "REJECTED - " + request.getResponseMessage();
                    }
                    
                    String requestType = request.getRequestType().equals(Request.TYPE_REGISTER_VEHICLE) ?
                        "Vehicle Registration" : "Other Request";
                        
                    model.addElement("#" + request.getRequestId() + " - " + requestType + " - " + statusText);
                }
            });
        }).start();
    }
}
