public class ReportEntity {
    private int year;
    private int month;
    private int week;
    private int unitsSold;
    private int unitPrice;
    private int revenue;

    public ReportEntity(int year, int month, int week, int unitsSold, int unitPrice, int revenue) {
        this.year = year;
        this.month = month;
        this.week = week;
        this.unitsSold = unitsSold;
        this.unitPrice = unitPrice;
        this.revenue = revenue;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public int getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(int unitsSold) {
        this.unitsSold = unitsSold;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getRevenue() {
        return revenue;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }
}
