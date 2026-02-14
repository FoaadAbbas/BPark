package common;

import java.io.Serializable;

public class SubscriberParkingData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String subscriberId;
    private String subscriberName;
    private int totalParkedHours;

    public SubscriberParkingData(String subscriberId, String subscriberName, int totalParkedHours) {
        this.subscriberId = subscriberId;
        this.subscriberName = subscriberName;
        this.totalParkedHours = totalParkedHours;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public int getTotalParkedHours() {
        return totalParkedHours;
    }
}