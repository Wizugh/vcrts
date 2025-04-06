

import gui.pages.server.ServerFrame;
import javax.swing.*;
import java.awt.*;

public class ClientServerDemo {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set up look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set native look and feel: " + e.getMessage());
            }
            
            // Get screen dimensions
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = screenSize.width;
            int screenHeight = screenSize.height;
            
            // Calculate window sizes (half the screen width each)
            int windowWidth = screenWidth / 2;
            int windowHeight = screenHeight - 100; // Allow for taskbar
            
            // Create and position the server window (left side)
            ServerFrame serverFrame = new ServerFrame();
            serverFrame.setTitle("VCRTS Cloud Controller (Server)");
            serverFrame.setSize(windowWidth, windowHeight);
            serverFrame.setLocation(0, 0);
            serverFrame.setVisible(true);
            
            // Create and position the client window (right side)
            ServerFrame clientFrame = new ServerFrame();
            clientFrame.setTitle("VCRTS Client (Vehicle/Job Owner)");
            clientFrame.setSize(windowWidth, windowHeight);
            clientFrame.setLocation(windowWidth, 0);
            clientFrame.setVisible(true);
            
            // Show instructions
            showInstructions();
        });
    }
    
    private static void showInstructions() {
        String instructions = 
            "VCRTS Client-Server Demo\n\n" +
            "This demo runs both the client and server in separate windows:\n" +
            "• Left window: Cloud Controller (Server)\n" +
            "• Right window: Client (Vehicle Owner or Job Owner)\n\n" +
            "To demonstrate the request/approval workflow:\n\n" +
            "1. Create accounts in both windows or use existing ones\n" +
            "2. In the left window: Log in as a cloud controller user\n" +
            "3. In the right window: Log in as a vehicle owner or job owner\n" +
            "4. In the right window: Submit a vehicle registration or job request\n" +
            "5. In the left window: Go to the 'Client Requests' tab to view and approve/reject the request\n" +
            "6. In the right window: Check the 'My Requests' tab to see the updated status\n\n" +
            "The two applications share the same database files, allowing for real-time interaction.";
        
        JOptionPane.showMessageDialog(null, instructions, "VCRTS Demo Instructions", JOptionPane.INFORMATION_MESSAGE);
    }
}