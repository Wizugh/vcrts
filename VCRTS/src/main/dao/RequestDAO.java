package dao;

import db.FileManager;
import models.Request;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing client requests.
 */
public class RequestDAO {
    private static final Logger logger = Logger.getLogger(RequestDAO.class.getName());
    private static final String REQUESTS_FILE = "requests.txt";
    private static final String DELIMITER = "\\|";
    private static final String SEPARATOR = "|";
    
    /**
     * Converts a Request object to a line of text for storage.
     */
    private String requestToLine(Request request) {
        return request.getRequestId() + SEPARATOR +
               request.getClientId() + SEPARATOR +
               request.getClientName() + SEPARATOR +
               request.getRequestType() + SEPARATOR +
               request.getRequestData() + SEPARATOR +
               request.getStatus() + SEPARATOR +
               request.getTimestamp() + SEPARATOR +
               (request.getResponseMessage() != null ? request.getResponseMessage() : "");
    }
    
    /**
     * Converts a line of text to a Request object.
     */
    private Request lineToRequest(String line) {
        String[] parts = line.split(DELIMITER);
        if (parts.length < 7) {
            logger.warning("Invalid request data format: " + line);
            return null;
        }
        
        try {
            // Extract response message if available
            String responseMessage = parts.length > 7 ? parts[7] : "";
            
            return new Request(
                Integer.parseInt(parts[0]),  // requestId
                Integer.parseInt(parts[1]),  // clientId
                parts[2],                    // clientName
                parts[3],                    // requestType
                parts[4],                    // requestData
                parts[5],                    // status
                parts[6],                    // timestamp
                responseMessage              // responseMessage
            );
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Error parsing request data: " + line, e);
            return null;
        }
    }
    
    /**
     * Adds a new request to the file.
     * 
     * @param request The Request object to be added.
     * @return true if the request was successfully added, false otherwise.
     */
    public boolean addRequest(Request request) {
        // Generate a unique ID for the new request
        int requestId = FileManager.generateUniqueNumericId(REQUESTS_FILE);
        request.setRequestId(requestId);
        
        String requestLine = requestToLine(request);
        return FileManager.appendLine(REQUESTS_FILE, requestLine);
    }
    
    /**
     * Retrieves all requests from the file.
     * 
     * @return A list of all requests.
     */
    public List<Request> getAllRequests() {
        List<Request> requests = new ArrayList<>();
        List<String> lines = FileManager.readAllLines(REQUESTS_FILE);
        
        for (String line : lines) {
            Request request = lineToRequest(line);
            if (request != null) {
                requests.add(request);
            }
        }
        
        return requests;
    }
    
    /**
     * Retrieves all pending requests from the file.
     * 
     * @return A list of pending requests.
     */
    public List<Request> getPendingRequests() {
        List<Request> pendingRequests = new ArrayList<>();
        List<String> lines = FileManager.readAllLines(REQUESTS_FILE);
        
        for (String line : lines) {
            Request request = lineToRequest(line);
            if (request != null && Request.STATUS_PENDING.equals(request.getStatus())) {
                pendingRequests.add(request);
            }
        }
        
        return pendingRequests;
    }
    
    /**
     * Retrieves all requests for a specific client.
     * 
     * @param clientId The ID of the client.
     * @return A list of requests for the specified client.
     */
    public List<Request> getRequestsByClient(int clientId) {
        List<Request> clientRequests = new ArrayList<>();
        List<String> lines = FileManager.readAllLines(REQUESTS_FILE);
        
        for (String line : lines) {
            Request request = lineToRequest(line);
            if (request != null && request.getClientId() == clientId) {
                clientRequests.add(request);
            }
        }
        
        return clientRequests;
    }
    
    /**
     * Updates the status and response message of a request.
     * 
     * @param requestId The ID of the request to update.
     * @param status The new status.
     * @param responseMessage The response message.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateRequestStatus(int requestId, String status, String responseMessage) {
        List<String> lines = FileManager.readAllLines(REQUESTS_FILE);
        List<String> updatedLines = new ArrayList<>();
        boolean updated = false;
        
        for (String line : lines) {
            Request request = lineToRequest(line);
            if (request != null && request.getRequestId() == requestId) {
                request.setStatus(status);
                request.setResponseMessage(responseMessage);
                updatedLines.add(requestToLine(request));
                updated = true;
            } else {
                updatedLines.add(line);
            }
        }
        
        return updated && FileManager.writeAllLines(REQUESTS_FILE, updatedLines);
    }
}
