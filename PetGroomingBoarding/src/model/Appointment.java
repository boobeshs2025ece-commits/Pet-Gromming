package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Appointment entity - plain Java object (POJO) mapped to the 'appointment' table. */
public class Appointment {
    private int appointmentId;
    private int petId;
    private int serviceId;
    private int groomerId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private double price;
    private String status;
    private LocalDateTime bookedOn;

    public Appointment() { }

    public Appointment(int appointmentId, int petId, int serviceId, int groomerId, LocalDate appointmentDate, LocalTime startTime, LocalTime endTime, double price, String status, LocalDateTime bookedOn) {
        this.appointmentId = appointmentId;
        this.petId = petId;
        this.serviceId = serviceId;
        this.groomerId = groomerId;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = status;
        this.bookedOn = bookedOn;
    }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }
    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }
    public int getGroomerId() { return groomerId; }
    public void setGroomerId(int groomerId) { this.groomerId = groomerId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getBookedOn() { return bookedOn; }
    public void setBookedOn(LocalDateTime bookedOn) { this.bookedOn = bookedOn; }
}
