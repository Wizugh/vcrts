// import gui.MainFrame;
// import javax.swing.SwingUtilities;
// import javax.swing.UIManager;
// import java.io.File;

// public class App {
//     public static void main(String[] args) {
//         // Create the data directory if it doesn't exist
//         File dataDir = new File("data");
//         if (!dataDir.exists()) {
//             dataDir.mkdirs();
//         }
        
//         // Tries to match the Appearance of the System
//         try {
//             UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//         // Starts the Main frame and displays it 
//         SwingUtilities.invokeLater(() -> {
//             new MainFrame().setVisible(true);
//         });
//     }
// }
