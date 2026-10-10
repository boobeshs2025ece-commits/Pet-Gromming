package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Boarding entity - plain Java object (POJO) mapped to the 'boarding' table. */
public class Boarding {
    private int boardingId;
    private int petId;
    private int kennelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int totalDays;
    private double dailyRate;
    private double totalAmount;
    private String status;
    private String specialInstructions;
    private LocalDateTime bookedOn;

    public Boarding() { }

    public Boarding(int boardingId, int petId, int kennelId, LocalDate checkInDate, LocalDate checkOutDate, int totalDays, double dailyRate, double totalAmount, String status, String specialInstructions, LocalDateTime bookedOn) {
        this.boardingId = boardingId;
        this.petId = petId;
        this.kennelId = kennelId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalDays = totalDays;
        this.dailyRate = dailyRate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.specialInstructions = specialInstructions;
        this.bookedOn = bookedOn;
    }

    public int getBoardingId() { return boardingId; }
    public void setBoardingId(int boardingId) { this.boardingId = boardingId; }
    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }
    public int getKennelId() { return kennelId; }
    public void setKennelId(int kennelId) { this.kennelId = kennelId; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    public LocalDateTime getBookedOn() { return bookedOn; }
    public void setBookedOn(LocalDateTime bookedOn) { this.bookedOn = bookedOn; }
}
