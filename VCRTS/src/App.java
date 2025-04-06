package main;

import gui.pages.server.ServerFrame;
import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Main application class to demonstrate the VCRTS system with client-server functionality.
 * This class launches multiple frames to simulate different users interacting with the system.
 */
public class VCRTSApp {
    private static final Logger logger = Logger.getLogger(VCRTSApp.class.getName());
    
    public static void main(String[] args) {
        // Set up look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warning("Could not set native look and feel: " + e.getMessage());
        }
        
        // Start the server frame
        SwingUtilities.invokeLater(() -> {
            // Create and position the server frame (Cloud Controller)
            ServerFrame serverFrame = new ServerFrame();
            serverFrame.setTitle("VCRTS Cloud Controller");
            serverFrame.setVisible(true);
            
            // Create a vehicle owner client frame
            ServerFrame vehicleOwnerFrame = new ServerFrame();
            vehicleOwnerFrame.setTitle("VCRTS Vehicle Owner Client");
            
            // Position the vehicle owner frame
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            vehicleOwnerFrame.setLocation(0, 0);
            vehicleOwnerFrame.setSize(screenSize.width / 2, screenSize.height);
            vehicleOwnerFrame.setVisible(true);
            
            // Create a job owner client frame
            ServerFrame jobOwnerFrame = new ServerFrame();
            jobOwnerFrame.setTitle("VCRTS Job Owner Client");
            
            // Position the job owner frame
            jobOwnerFrame.setLocation(screenSize.width / 2, 0);
            jobOwnerFrame.setSize(screenSize.width / 2, screenSize.height);
            jobOwnerFrame.setVisible(true);
            
            // Show instructions dialog
            showInstructions();
        });
    }
    
    /**
     * Shows instructions for using the application.
     */
    private static void showInstructions() {
        String instructions = 
            "VCRTS Client-Server System Demonstration\n\n" +
            "Three applications have been launched:\n" +
            "1. VCRTS Cloud Controller (Server)\n" +
            "2. VCRTS Vehicle Owner Client\n" +
            "3. VCRTS Job Owner Client\n\n" +
            "To demonstrate the system:\n" +
            "1. Create accounts in each application\n" +
            "2. Log in to the Cloud Controller as a cloud controller user\n" +
            "3. Log in to the Vehicle Owner client as a vehicle owner\n" +
            "4. Log in to the Job Owner client as a job owner\n" +
            "5. In the Vehicle Owner client, register a vehicle (this sends a request to the server)\n" +
            "6. In the Job Owner client, add a job (this sends a request to the server)\n" +
            "7. In the Cloud Controller, go to the Client Requests tab to approve or reject the requests\n" +
            "8. Check the status of the requests in the respective clients\n\n" +
            "Note: Only one user can be connected to the server at a time. Use the Connect/Disconnect buttons to manage connections.";
        
        JOptionPane.showMessageDialog(null, instructions, "VCRTS Instructions", JOptionPane.INFORMATION_MESSAGE);
    }
}
