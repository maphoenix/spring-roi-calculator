package com.example.roi.service;

public class OccupancyProfile {

    /**
     * Returns daytime and night-time kWh split for the month
     * @param monthTotalUsage     total usage for the month (kWh)
     * @param daysAtHomePerWeek   0 to 5 (number of weekdays at home)
     * @param weekendDaytimeRatio e.g. 0.7
     * @param homeWeekdayRatio    e.g. 0.7
     * @param awayWeekdayRatio    e.g. 0.25
     * @return double[] {daytimeUsage, nightTimeUsage}
     */
    public static double[] computeMonthlyDaytimeUsageSplit(
            double monthTotalUsage,
            int daysAtHomePerWeek,
            double weekendDaytimeRatio,
            double homeWeekdayRatio,
            double awayWeekdayRatio
    ) {
        int weekends = 8;
        int weekdays = 30 - weekends;
        double weekdayDaytimeRatio =
                (daysAtHomePerWeek / 5.0) * homeWeekdayRatio +
                ((5 - daysAtHomePerWeek) / 5.0) * awayWeekdayRatio;

        double averageDaytimeRatio =
                ((weekends * weekendDaytimeRatio) + (weekdays * weekdayDaytimeRatio)) / 30.0;

        double daytimeUsage = monthTotalUsage * averageDaytimeRatio;
        double nightUsage = monthTotalUsage - daytimeUsage;
        return new double[]{daytimeUsage, nightUsage};
    }
}
