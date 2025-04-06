package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a request from a client to the server.
 * Used for simulating client-server communication.
 */
public class Request {
    // Request types
    public static final String TYPE_REGISTER_VEHICLE = "REGISTER_VEHICLE";
    public static final String TYPE_ADD_JOB = "ADD_JOB";
    
    // Request status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    
    private int requestId;
    private int clientId;
    private String clientName;
    private String requestType;
    private String requestData;
    private String status;
    private String timestamp;
    private String responseMessage;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public Request(int clientId, String clientName, String requestType, String requestData) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.requestType = requestType;
        this.requestData = requestData;
        this.status = STATUS_PENDING;
        this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
    
    // Constructor with ID for loading from storage
    public Request(int requestId, int clientId, String clientName, String requestType, 
                  String requestData, String status, String timestamp, String responseMessage) {
        this.requestId = requestId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.requestType = requestType;
        this.requestData = requestData;
        this.status = status;
        this.timestamp = timestamp;
        this.responseMessage = responseMessage;
    }
    
    // Getters and setters
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    
    public String getRequestData() { return requestData; }
    public void setRequestData(String requestData) { this.requestData = requestData; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
}
