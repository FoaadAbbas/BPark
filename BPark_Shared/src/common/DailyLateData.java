package common;

import java.io.Serializable;

/**
 * A data transfer object (DTO) to hold aggregated data for the daily lateness report.
 * It encapsulates the number of late incidents on a specific date.
 */
public class DailyLateData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String date;
    private final int lateCount;

    /**
     * Constructs a DailyLateData object.
     *
     * @param date      The specific date ('YYYY-MM-DD').
     * @param lateCount The total count of late incidents on that date.
     */
    public DailyLateData(String date, int lateCount) {
        this.date = date;
        this.lateCount = lateCount;
    }

    public String getDate() { return date; }
    public int getLateCount() { return lateCount; }
}