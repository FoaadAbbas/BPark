package common;

import java.io.Serializable;

/**
 * A data transfer object (DTO) that holds information about a single activity log entry.
 * This includes who performed the activity, what it was, and when it occurred.
 */
public class ActivityInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String subscriberCode;
    private String userName;
    private String activityType;
    private String details;
    private String timestamp;

    /**
     * Constructs a new ActivityInfo object.
     *
     * @param subscriberCode The code of the subscriber who performed the activity.
     * @param userName       The name of the subscriber.
     * @param activityType   The type of activity (e.g., LOGIN, PARK_CAR).
     * @param details        A description of the activity.
     * @param timestamp      The date and time the activity occurred.
     */
    public ActivityInfo(String subscriberCode, String userName, String activityType, String details, String timestamp) {
        this.subscriberCode = subscriberCode;
        this.userName = userName;
        this.activityType = activityType;
        this.details = details;
        this.timestamp = timestamp;
    }

    /**
     * A constructor for backward compatibility.
     *
     * @param activityType The type of activity.
     * @param details      A description of the activity.
     * @param timestamp    The date and time of the activity.
     */
    public ActivityInfo(String activityType, String details, String timestamp) {
        this(null, null, activityType, details, timestamp);
    }

    public String getSubscriberCode() { return subscriberCode; }
    public String getUserName() { return (userName != null) ? userName : "N/A"; }
    public String getActivityType() { return activityType; }
    public String getDetails() { return details; }
    public String getTimestamp() { return timestamp; }
}