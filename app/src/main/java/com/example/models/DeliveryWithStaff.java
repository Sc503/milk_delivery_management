package com.example.models;

public class DeliveryWithStaff {
    public long id;
    public long customerId;
    public String deliveryDate;
    public String deliveredTime;
    public String status;
    public long staffId;
    public String staffName;  //  From Staff table (joined via LEFT JOIN)

    // to check if staff is assigned
    public boolean hasStaff() {
        return staffId > 0 && staffName != null && !staffName.isEmpty();
    }
}