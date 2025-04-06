package controller;

import dao.JobDAO;
import dao.RequestDAO;
import dao.VehicleDAO;
import models.Job;
import models.Request;
import models.Vehicle;

import java.util.ArrayList;
import java.util.List; // Explicitly import java.util.List
import java.util.concurrent.CopyOnWriteArrayList;
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
    
    // In-memory storage for pending requests
    private CopyOnWriteArrayList<Request> pendingRequests;
    
    // Thread for processing requests
    private RequestProcessorThread processorThread;
    
    // Private constructor (singleton pattern)
    private ServerController() {
        requestDAO = new RequestDAO();
        jobDAO = new JobDAO();
        vehicleDAO = new VehicleDAO();
        pendingRequests = new CopyOnWriteArrayList<>();
        
        // Start the request processor thread
        processorThread = new RequestProcessorThread();
        processorThread.start();
        
        logger.info("ServerController initialized with request processor thread");
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
     * Submits a request from a client to the server.
     * 
     * @param request The request to submit.
     * @return true if submitted successfully, false otherwise.
     */
    public boolean submitRequest(Request request) {
        // Generate a unique ID for the request
        int requestId = pendingRequests.size() + 1;
        request.setRequestId(requestId);
        
        // Add to pending requests (in memory)
        pendingRequests.add(request);
        logger.info("Request #" + requestId + " submitted and added to pending queue");
        return true;
    }
    
    /**
     * Gets all pending requests from memory.
     * 
     * @return A list of pending requests.
     */
    public List<Request> getPendingRequests() {
        return new ArrayList<>(pendingRequests);
    }
    
    /**
     * Gets all requests for a specific client.
     * This combines in-memory pending requests with stored requests.
     * 
     * @param clientId The ID of the client.
     * @return A list of requests for the specified client.
     */
    public List<Request> getClientRequests(int clientId) {
        // Get requests from memory
        List<Request> clientRequests = new ArrayList<>();
        for (Request request : pendingRequests) {
            if (request.getClientId() == clientId) {
                clientRequests.add(request);
            }
        }
        
        // Add requests from storage
        List<Request> storedRequests = requestDAO.getRequestsByClient(clientId);
        if (storedRequests != null && !storedRequests.isEmpty()) {
            clientRequests.addAll(storedRequests);
        }
        
        return clientRequests;
    }
    
    /**
     * Approves a request and processes it.
     * 
     * @param requestId The ID of the request to approve.
     * @param responseMessage The response message.
     * @return true if approved successfully, false otherwise.
     */
    public boolean approveRequest(int requestId, String responseMessage) {
        // Find the request in the pending list
        Request targetRequest = null;
        for (Request request : pendingRequests) {
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
            targetRequest.setStatus(Request.STATUS_APPROVED);
            targetRequest.setResponseMessage(responseMessage);
            
            // Save the approved request to database
            requestDAO.addRequest(targetRequest);
            
            // Remove from pending requests
            pendingRequests.remove(targetRequest);
            
            logger.info("Request #" + requestId + " approved and processed");
            return true;
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
        // Find the request in the pending list
        Request targetRequest = null;
        for (Request request : pendingRequests) {
            if (request.getRequestId() == requestId) {
                targetRequest = request;
                break;
            }
        }
        
        if (targetRequest == null) {
            return false; // Request not found
        }
        
        // Update request status
        targetRequest.setStatus(Request.STATUS_REJECTED);
        targetRequest.setResponseMessage(responseMessage);
        
        // Save the rejected request to database
        requestDAO.addRequest(targetRequest);
        
        // Remove from pending requests
        pendingRequests.remove(targetRequest);
        
        logger.info("Request #" + requestId + " rejected");
        return true;
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
    
    /**
     * Thread class for processing requests in background
     */
    private class RequestProcessorThread extends Thread {
        private boolean running = true;
        
        public RequestProcessorThread() {
            setDaemon(true); // Set as daemon thread so it doesn't prevent JVM from exiting
        }
        
        @Override
        public void run() {
            logger.info("Request processor thread started");
            
            while (running) {
                try {
                    // This thread could be used for automatic processing of requests
                    // or for other background tasks related to the server
                    
                    // Sleep to avoid excessive CPU usage
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.warning("Request processor thread interrupted: " + e.getMessage());
                    running = false;
                }
            }
            
            logger.info("Request processor thread stopped");
        }
        
        public void stopProcessing() {
            running = false;
            interrupt();
        }
    }
}
