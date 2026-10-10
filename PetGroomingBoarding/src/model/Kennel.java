package model;

/** Kennel entity - plain Java object (POJO) mapped to the 'kennel' table. */
public class Kennel {
    private int kennelId;
    private String kennelName;
    private String kennelType;
    private String petType;
    private double dailyRate;
    private String status;

    public Kennel() { }

    public Kennel(int kennelId, String kennelName, String kennelType, String petType, double dailyRate, String status) {
        this.kennelId = kennelId;
        this.kennelName = kennelName;
        this.kennelType = kennelType;
        this.petType = petType;
        this.dailyRate = dailyRate;
        this.status = status;
    }

    public int getKennelId() { return kennelId; }
    public void setKennelId(int kennelId) { this.kennelId = kennelId; }
    public String getKennelName() { return kennelName; }
    public void setKennelName(String kennelName) { this.kennelName = kennelName; }
    public String getKennelType() { return kennelType; }
    public void setKennelType(String kennelType) { this.kennelType = kennelType; }
    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }
    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
