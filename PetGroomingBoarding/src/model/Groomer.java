package model;

/** Groomer entity - plain Java object (POJO) mapped to the 'groomer' table. */
public class Groomer {
    private int groomerId;
    private String fullName;
    private String specialization;
    private String phone;
    private String status;

    public Groomer() { }

    public Groomer(int groomerId, String fullName, String specialization, String phone, String status) {
        this.groomerId = groomerId;
        this.fullName = fullName;
        this.specialization = specialization;
        this.phone = phone;
        this.status = status;
    }

    public int getGroomerId() { return groomerId; }
    public void setGroomerId(int groomerId) { this.groomerId = groomerId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
