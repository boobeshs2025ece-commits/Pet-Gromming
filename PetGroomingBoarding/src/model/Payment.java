package model;

import java.time.LocalDateTime;

/** Payment entity - plain Java object (POJO) mapped to the 'payment' table. */
public class Payment {
    private int paymentId;
    private Integer appointmentId;
    private Integer boardingId;
    private LocalDateTime paymentDate;
    private double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionReference;

    public Payment() { }

    public Payment(int paymentId, Integer appointmentId, Integer boardingId, LocalDateTime paymentDate, double amount, String paymentMethod, String paymentStatus, String transactionReference) {
        this.paymentId = paymentId;
        this.appointmentId = appointmentId;
        this.boardingId = boardingId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.transactionReference = transactionReference;
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }
    public Integer getBoardingId() { return boardingId; }
    public void setBoardingId(Integer boardingId) { this.boardingId = boardingId; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
}
