package common;

import java.io.Serializable;

public class SubscriberInfo implements Serializable {
    private static final long serialVersionUID = 2L;

    private String subscriptionCode;
    private String userName;
    private String phoneNumber;
    private String email;
    private String id;
    private int late_count;
    private boolean isFrozen;

    public SubscriberInfo(String subscriptionCode, String userName, String phoneNumber, String email, String id, int late_count, boolean isFrozen) {
        this.subscriptionCode = subscriptionCode;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.id = id;
        this.late_count = late_count;
        this.isFrozen = isFrozen;
    }

    // Getters
    public String getSubscriptionCode() { return subscriptionCode; }
    public String getUserName() { return userName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getId() { return id; }
    //NEW: Getters for the new fields.
    public int getLateCount() { return late_count; }
    public boolean isFrozen() { return isFrozen; }
    public void setFrozen(boolean frozen) { isFrozen = frozen; }


    @Override
    public String toString() {
        return "SubscriberInfo{" +
                "subscriptionCode='" + subscriptionCode + '\'' +
                ", userName='" + userName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", id='" + id + '\'' +
                ", late_count=" + late_count +
                ", isFrozen=" + isFrozen +
                '}';
    }
}