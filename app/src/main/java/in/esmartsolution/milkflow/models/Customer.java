package in.esmartsolution.milkflow.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class Customer {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String mobile;
    private String address;
    private double latitude;
    private double longitude;
    private String createdDate; // YYYY-MM-DD format

    private double milkQuantity;
    private double milkRate;

    // Empty constructor required by Room
    public Customer() {
    }

    public Customer(
            String name,
            String mobile,
            String address,
            double latitude,
            double longitude,
            String createdDate,
            double milkQuantity,
            double milkRate
    ) {
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdDate = createdDate;
        this.milkQuantity = milkQuantity;
        this.milkRate = milkRate;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public double getMilkQuantity() {
        return milkQuantity;
    }

    public void setMilkQuantity(double milkQuantity) {
        this.milkQuantity = milkQuantity;
    }

    public double getMilkRate() {
        return milkRate;
    }

    public void setMilkRate(double milkRate) {
        this.milkRate = milkRate;
    }
}