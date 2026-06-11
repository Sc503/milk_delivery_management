package com.example.models;

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

    // Unused constructor for Room
    public Customer() {
    }

    public Customer(String name, String mobile, String address, double latitude, double longitude, String createdDate) {
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdDate = createdDate;
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
}
