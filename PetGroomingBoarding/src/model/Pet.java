package model;

/** Pet entity - plain Java object (POJO) mapped to the 'pet' table. */
public class Pet {
    private int petId;
    private int customerId;
    private String petName;
    private String species;
    private String breed;
    private int age;
    private String gender;
    private double weightKg;
    private String medicalNotes;
    private String status;

    public Pet() { }

    public Pet(int petId, int customerId, String petName, String species, String breed, int age, String gender, double weightKg, String medicalNotes, String status) {
        this.petId = petId;
        this.customerId = customerId;
        this.petName = petName;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.weightKg = weightKg;
        this.medicalNotes = medicalNotes;
        this.status = status;
    }

    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
