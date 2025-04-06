package gui.pages.server;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import controller.ServerController;
import dao.CloudControllerDAO;
import dao.JobDAO;
import models.Job;
import models.Request;
import models.User;

public class ClientDashboard extends JPanel {
    private static final Logger logger = Logger.getLogger(ClientDashboard.class.getName());

    private User client; // Authenticated client (job owner)
    private JobDAO jobDAO = new JobDAO();
    private CloudControllerDAO cloudControllerDAO = new CloudControllerDAO();
    private ServerController serverController;

    private JTable jobTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;
    private JButton refreshButton, addJobButton, viewTimesButton, refreshRequestsButton;
    private DefaultListModel<String> requestListModel;
    private JList<String> requestList;

    public ClientDashboard(User client) {
        this.client = client;
        serverController = ServerController.getInstance();
        setLayout(new BorderLayout());
        setBackground(new Color(43, 43, 43));

        // Top panel with title
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(43, 43, 43));
        JLabel titleLabel = new JLabel("Job Owner Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Create a tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Jobs tab
        JPanel jobsPanel = createJobsPanel();
        tabbedPane.addTab("My Jobs", jobsPanel);
        
        // Requests tab
        JPanel requestsPanel = createRequestsPanel();
        tabbedPane.addTab("My Requests", requestsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);

        // Filter, Refresh & Add Job Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(43, 43, 43));

        // Center-align the components
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        String[] statuses = {"All", "Queued", "In Progress", "Completed"};
        statusFilter = new JComboBox<>(statuses);
        statusFilter.setBackground(Color.WHITE);
        statusFilter.setFont(new Font("Arial", Font.PLAIN, 14));
        statusFilter.addActionListener(e -> updateTable());
        controlPanel.add(statusFilter);

        refreshButton = new JButton("Refresh Jobs");
        refreshButton.setBackground(Color.WHITE);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshButton.addActionListener(e -> updateTable());
        controlPanel.add(refreshButton);

        addJobButton = new JButton("Add Job");
        addJobButton.setBackground(Color.WHITE);
        addJobButton.setFont(new Font("Arial", Font.BOLD, 14));
        addJobButton.addActionListener(e -> openAddJobDialog());
        controlPanel.add(addJobButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Auto-refresh every 10 seconds
        new Timer(10000, e -> {
            updateTable();
            refreshRequestStatus();
        }).start();
        
        // Connect client to server if not already connected
        if (!serverController.isClientConnected(client.getUserId())) {
            serverController.connectClient(client);
        }
        
        updateTable();
        refreshRequestStatus();
    }
    
    /**
     * Creates the Jobs panel
     */
    private JPanel createJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table setup
        String[] columnNames = {"Job ID", "Status", "Duration", "Time to Complete", "Created At", "Estimated Completion"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        jobTable = new JTable(tableModel);
        jobTable.setBackground(new Color(230, 230, 230));
        jobTable.setForeground(Color.BLACK);
        jobTable.setRowHeight(30);
        jobTable.setFont(new Font("Arial", Font.PLAIN, 14));

        // Center-align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < jobTable.getColumnCount(); i++) {
            jobTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JTableHeader header = jobTable.getTableHeader();
        header.setBackground(new Color(200, 200, 200));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Arial", Font.BOLD, 15));

        // Center table header text
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        JScrollPane scrollPane = new JScrollPane(jobTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the Requests panel
     */
    private JPanel createRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add title
        JLabel titleLabel = new JLabel("Job Requests Status", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Create list for requests
        requestListModel = new DefaultListModel<>();
        requestList = new JList<>(requestListModel);
        requestList.setFont(new Font("Arial", Font.PLAIN, 14));
        requestList.setCellRenderer(new RequestListCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(requestList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add refresh button
        refreshRequestsButton = new JButton("Refresh Requests");
        refreshRequestsButton.addActionListener(e -> refreshRequestStatus());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(refreshRequestsButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * Opens a dialog with input fields for adding a new job.
     */
    private void openAddJobDialog() {
        // Check if client is connected to server
        if (!serverController.isClientConnected(client.getUserId())) {
            int choice = JOptionPane.showConfirmDialog(this,
                "You are not connected to the server. Connect now?",
                "Connection Required",
                JOptionPane.YES_NO_OPTION);
                
            if (choice == JOptionPane.YES_OPTION) {
                serverController.connectClient(client);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Request not submitted. Connection required.", 
                    "Connection Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    
        // Create a panel with more sophisticated layout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Job ID field
        JPanel jobIdPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        jobIdPanel.add(new JLabel("Job ID:"));
        JTextField jobIdField = new JTextField(15);
        jobIdField.setHorizontalAlignment(SwingConstants.CENTER);
        jobIdPanel.add(jobIdField);
        panel.add(jobIdPanel);
        
        // Job Name field
        JPanel jobNamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        jobNamePanel.add(new JLabel("Job Name:"));
        JTextField jobNameField = new JTextField(15);
        jobNameField.setHorizontalAlignment(SwingConstants.CENTER);
        jobNamePanel.add(jobNameField);
        panel.add(jobNamePanel);

        // Duration panel with spinner for hours, minutes, and seconds
        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        durationPanel.add(new JLabel("Duration:"));

        // Hours spinner (0-23)
        SpinnerNumberModel hoursModel = new SpinnerNumberModel(0, 0, 23, 1);
        JSpinner hoursSpinner = new JSpinner(hoursModel);
        JSpinner.NumberEditor hoursEditor = new JSpinner.NumberEditor(hoursSpinner, "00");
        hoursSpinner.setEditor(hoursEditor);
        hoursSpinner.setPreferredSize(new Dimension(60, 25));

        // Minutes spinner (0-59)
        SpinnerNumberModel minutesModel = new SpinnerNumberModel(0, 0, 59, 1);
        JSpinner minutesSpinner = new JSpinner(minutesModel);
        JSpinner.NumberEditor minutesEditor = new JSpinner.NumberEditor(minutesSpinner, "00");
        minutesSpinner.setEditor(minutesEditor);
        minutesSpinner.setPreferredSize(new Dimension(60, 25));

        // Seconds spinner (0-59)
        SpinnerNumberModel secondsModel = new SpinnerNumberModel(0, 0, 59, 1);
        JSpinner secondsSpinner = new JSpinner(secondsModel);
        JSpinner.NumberEditor secondsEditor = new JSpinner.NumberEditor(secondsSpinner, "00");
        secondsSpinner.setEditor(secondsEditor);
        secondsSpinner.setPreferredSize(new Dimension(60, 25));

        durationPanel.add(hoursSpinner);
        durationPanel.add(new JLabel("h"));
        durationPanel.add(minutesSpinner);
        durationPanel.add(new JLabel("m"));
        durationPanel.add(secondsSpinner);
        durationPanel.add(new JLabel("s"));
        panel.add(durationPanel);

        // Deadline date picker
        JPanel deadlinePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        deadlinePanel.add(new JLabel("Deadline:"));

        // Date picker using JSpinner with date editor
        Calendar calendar = Calendar.getInstance();
        Date initialDate = calendar.getTime();
        calendar.add(Calendar.YEAR, 10); // Allow dates up to 10 years in the future
        Date lastDate = calendar.getTime();
        calendar.add(Calendar.YEAR, -20); // Allow dates up to 10 years in the past
        Date firstDate = calendar.getTime();
        SpinnerDateModel dateModel = new SpinnerDateModel(initialDate, firstDate, lastDate, Calendar.DAY_OF_MONTH);

        JSpinner dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setPreferredSize(new Dimension(150, 25));

        deadlinePanel.add(dateSpinner);
        panel.add(deadlinePanel);

        // Note about status
        JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel noteLabel = new JLabel("Note: Job request will be sent to the Cloud Controller for approval.");
        noteLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        notePanel.add(noteLabel);
        panel.add(notePanel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Job", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String jobId = jobIdField.getText().trim();
            String jobName = jobNameField.getText().trim();

            // Format the duration from spinner values
            int hours = (Integer) hoursSpinner.getValue();
            int minutes = (Integer) minutesSpinner.getValue();
            int seconds = (Integer) secondsSpinner.getValue();
            String duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            // Format the deadline from date spinner
            Date selectedDate = (Date) dateSpinner.getValue();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String deadline = dateFormat.format(selectedDate);

            if (jobId.isEmpty() || jobName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Job ID and Job Name are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (hours == 0 && minutes == 0 && seconds == 0) {
                JOptionPane.showMessageDialog(this, "Duration must be greater than zero!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Prepare the request data
            // Format: jobId|jobName|jobOwnerId|duration|deadline|status
            String requestData = jobId + "|" + jobName + "|" + client.getUserId() + "|" + 
                                 duration + "|" + deadline + "|" + CloudControllerDAO.STATE_QUEUED;
            
            // Create and submit the request
            Request request = new Request(client.getUserId(), client.getFullName(), 
                                         Request.TYPE_ADD_JOB, requestData);
            
            boolean success = serverController.submitRequest(request);
            
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "Job request submitted successfully! Awaiting approval.", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                refreshRequestStatus();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to submit job request!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void updateTable() {
        try {
            tableModel.setRowCount(0);
            String selectedStatus = (String) statusFilter.getSelectedItem();
            // If "All" is selected, getJobsByClient returns all jobs for the client.
            List<Job> jobs = jobDAO.getJobsByClient(client.getUserId(), selectedStatus);

            // Keep track of cumulative time for FIFO
            long cumulativeMinutes = 0;

            for (Job job : jobs) {
                // Calculate job duration in minutes
                long durationMinutes = 0;
                try {
                    String[] timeParts = job.getDuration().split(":");
                    int hours = Integer.parseInt(timeParts[0]);
                    int minutes = Integer.parseInt(timeParts[1]);
                    int seconds = Integer.parseInt(timeParts[2]);

                    durationMinutes = hours * 60 + minutes + (seconds > 0 ? 1 : 0); // Round up seconds
                } catch (Exception e) {
                    durationMinutes = 60; // Default to 1 hour if parsing fails
                }

                // Add to cumulative time if not completed
                if (!job.getStatus().equals(CloudControllerDAO.STATE_COMPLETED)) {
                    cumulativeMinutes += durationMinutes;
                }

                // Format the cumulative time as hours and minutes
                long totalHours = cumulativeMinutes / 60;
                long totalMinutes = cumulativeMinutes % 60;
                String timeToComplete = totalHours > 0 ?
                        String.format("%dh %dm", totalHours, totalMinutes) :
                        String.format("%dm", totalMinutes);

                // For completed jobs, don't show time to complete
                if (job.getStatus().equals(CloudControllerDAO.STATE_COMPLETED)) {
                    timeToComplete = "Completed";
                }

                tableModel.addRow(new Object[]{
                        job.getJobId(),
                        job.getStatus(),
                        job.getDuration(),
                        timeToComplete,
                        job.getCreatedTimestamp(),
                        "Not calculated"  // Placeholder for completion time
                });
            }
        } catch(Exception ex) {
            Logger.getLogger(ClientDashboard.class.getName()).log(Level.SEVERE, "Error updating job table: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, "Error loading job data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Refreshes the request status list.
     */
    private void refreshRequestStatus() {
        requestListModel.clear();
        
        if (!serverController.isClientConnected(client.getUserId())) {
            requestListModel.addElement("Not connected to server. Connect to view requests.");
            return;
        }
        
        List<Request> requests = serverController.getClientRequests(client.getUserId());
        
        if (requests.isEmpty()) {
            requestListModel.addElement("No requests found.");
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
            
            String requestType = request.getRequestType().equals(Request.TYPE_ADD_JOB) ?
                "Add Job" : "Other Request";
                
            requestListModel.addElement("#" + request.getRequestId() + " - " + requestType + " - " + statusText);
        }
    }
    
    /**
     * Custom cell renderer for request list items
     */
    private class RequestListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value != null) {
                String text = value.toString();
                
                if (text.contains("PENDING")) {
                    setForeground(Color.BLUE);
                } else if (text.contains("APPROVED")) {
                    setForeground(new Color(0, 128, 0)); // Dark green
                } else if (text.contains("REJECTED")) {
                    setForeground(Color.RED);
                } else {
                    setForeground(Color.BLACK);
                }
            }
            
            return c;
        }
    }
}
