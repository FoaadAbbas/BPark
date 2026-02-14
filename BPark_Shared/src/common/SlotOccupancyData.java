package common;

import java.io.Serializable;

public class SlotOccupancyData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String parkingSpace;
    private int totalOccupiedHours;

    public SlotOccupancyData(String parkingSpace, int totalOccupiedHours) {
        this.parkingSpace = parkingSpace;
        this.totalOccupiedHours = totalOccupiedHours;
    }

    public String getParkingSpace() {
        return parkingSpace;
    }

    public int getTotalOccupiedHours() {
        return totalOccupiedHours;
    }
}