package common;

import java.io.Serializable;

/**
 * A data transfer object (DTO) for holding daily parking counts for a monthly report.
 */
public class MonthlyReportData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String date;
    private final int parkingCount;

    /**
     * Constructs a MonthlyReportData object.
     *
     * @param date         The specific date ('YYYY-MM-DD').
     * @param parkingCount The total number of cars parked on that date.
     */
    public MonthlyReportData(String date, int parkingCount) {
        this.date = date;
        this.parkingCount = parkingCount;
    }

    public String getDate() { return date; }
    public int getParkingCount() { return parkingCount; }
}