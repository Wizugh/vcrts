package controller;

import dao.JobDAO;
import dao.RequestDAO;
import dao.VehicleDAO;
import models.Job;
import models.Request;
import models.User;
import models.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller for managing client-server communication.
 * This is a singleton class to ensure there's only one instance.
 */
public class ServerController {
    private static final Logger logger = Logger.getLogger(ServerController.class.getName());
    private static ServerController instance;
    
    private RequestDAO requestDAO;
    private JobDAO jobDAO;
    private VehicleDAO vehicleDAO;
    
    // Connected clients (userId -> User)
    private Map<Integer, User> connectedClients;
    
    // Private constructor (singleton pattern)
    private ServerController() {
        requestDAO = new RequestDAO();
        jobDAO = new JobDAO();
        vehicleDAO = new VehicleDAO();
        connectedClients = new HashMap<>();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized ServerController getInstance() {
        if (instance == null) {
            instance = new ServerController();
        }
        return instance;
    }
    
    /**
     * Connects a client to the server.
     * 
     * @param user The user to connect.
     * @return true if connected successfully, false if already connected or failed.
     */
    public boolean connectClient(User user) {
        if (connectedClients.containsKey(user.getUserId())) {
            return false; // Already connected
        }
        
        connectedClients.put(user.getUserId(), user);
        logger.info("Client connected: " + user.getUserId() + " - " + user.getFullName());
        return true;
    }
    
    /**
     * Disconnects a client from the server.
     * 
     * @param userId The ID of the user to disconnect.
     * @return true if disconnected successfully, false if not connected or failed.
     */
    public boolean disconnectClient(int userId) {
        if (!connectedClients.containsKey(userId)) {
            return false; // Not connected
        }
        
        User user = connectedClients.remove(userId);
        logger.info("Client disconnected: " + userId + " - " + user.getFullName());
        return true;
    }
    
    /**
     * Checks if a client is connected.
     * 
     * @param userId The ID of the user to check.
     * @return true if connected, false otherwise.
     */
    public boolean isClientConnected(int userId) {
        return connectedClients.containsKey(userId);
    }
    
    /**
     * Gets a list of connected clients.
     * 
     * @return A list of connected users.
     */
    public List<User> getConnectedClients() {
        return new ArrayList<>(connectedClients.values());
    }
    
    /**
     * Submits a request from a client to the server.
     * 
     * @param request The request to submit.
     * @return true if submitted successfully, false otherwise.
     */
    public boolean submitRequest(Request request) {
        // Check if client is connected
        if (!isClientConnected(request.getClientId())) {
            return false; // Client not connected
        }
        
        return requestDAO.addRequest(request);
    }
    
    /**
     * Gets all pending requests.
     * 
     * @return A list of pending requests.
     */
    public List<Request> getPendingRequests() {
        return requestDAO.getPendingRequests();
    }
    
    /**
     * Gets all requests for a specific client.
     * 
     * @param clientId The ID of the client.
     * @return A list of requests for the specified client.
     */
    public List<Request> getClientRequests(int clientId) {
        return requestDAO.getRequestsByClient(clientId);
    }
    
    /**
     * Approves a request.
     * 
     * @param requestId The ID of the request to approve.
     * @param responseMessage The response message.
     * @return true if approved successfully, false otherwise.
     */
    public boolean approveRequest(int requestId, String responseMessage) {
        // Get the request first
        List<Request> allRequests = requestDAO.getAllRequests();
        Request targetRequest = null;
        
        for (Request request : allRequests) {
            if (request.getRequestId() == requestId) {
                targetRequest = request;
                break;
            }
        }
        
        if (targetRequest == null) {
            return false; // Request not found
        }
        
        // Process the request based on its type
        boolean success = false;
        
        if (Request.TYPE_REGISTER_VEHICLE.equals(targetRequest.getRequestType())) {
            // Parse vehicle data and register it
            success = processVehicleRegistration(targetRequest);
        } else if (Request.TYPE_ADD_JOB.equals(targetRequest.getRequestType())) {
            // Parse job data and add it
            success = processJobAddition(targetRequest);
        }
        
        if (success) {
            // Update request status
            return requestDAO.updateRequestStatus(requestId, Request.STATUS_APPROVED, responseMessage);
        }
        
        return false;
    }
    
    /**
     * Rejects a request.
     * 
     * @param requestId The ID of the request to reject.
     * @param responseMessage The reason for rejection.
     * @return true if rejected successfully, false otherwise.
     */
    public boolean rejectRequest(int requestId, String responseMessage) {
        return requestDAO.updateRequestStatus(requestId, Request.STATUS_REJECTED, responseMessage);
    }
    
    /**
     * Processes a vehicle registration request.
     * 
     * @param request The request to process.
     * @return true if processed successfully, false otherwise.
     */
    private boolean processVehicleRegistration(Request request) {
        try {
            // Parse vehicle data from the request
            // Format: ownerId|model|make|year|vin|residencyTime
            String[] parts = request.getRequestData().split("\\|");
            if (parts.length < 6) {
                return false; // Invalid data format
            }
            
            int ownerId = Integer.parseInt(parts[0]);
            String model = parts[1];
            String make = parts[2];
            String year = parts[3];
            String vin = parts[4];
            String residencyTime = parts[5];
            
            // Create and add the vehicle
            Vehicle vehicle = new Vehicle(ownerId, model, make, year, vin, residencyTime);
            return vehicleDAO.addVehicle(vehicle);
            
        } catch (Exception e) {
            logger.warning("Error processing vehicle registration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Processes a job addition request.
     * 
     * @param request The request to process.
     * @return true if processed successfully, false otherwise.
     */
    private boolean processJobAddition(Request request) {
        try {
            // Parse job data from the request
            // Format: jobId|jobName|jobOwnerId|duration|deadline|status
            String[] parts = request.getRequestData().split("\\|");
            if (parts.length < 6) {
                return false; // Invalid data format
            }
            
            String jobId = parts[0];
            String jobName = parts[1];
            int jobOwnerId = Integer.parseInt(parts[2]);
            String duration = parts[3];
            String deadline = parts[4];
            String status = parts[5];
            
            // Create and add the job
            Job job = new Job(jobId, jobName, jobOwnerId, duration, deadline, status);
            return jobDAO.addJob(job);
            
        } catch (Exception e) {
            logger.warning("Error processing job addition: " + e.getMessage());
            return false;
        }
    }
}
