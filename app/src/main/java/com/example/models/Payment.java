package com.example.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payments")
public class Payment {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long customerId;

    private String month;   // yyyy-MM

    private double totalAmount;

    private String status;   // Paid / Pending

    //  REQUIRED for Room
    public Payment() {
    }

    public Payment(long customerId,
                   String month,
                   double totalAmount,
                   String status) {

        this.customerId = customerId;
        this.month = month;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // GETTERS
    public long getId() {
        return id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getMonth() {
        return month;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    // SETTERS
    public void setId(long id) {
        this.id = id;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAmount() {
        return totalAmount;
    }
    }
