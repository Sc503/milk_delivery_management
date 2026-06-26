package com.example.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "staff")
public class Staff {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String mobile1;
    private String mobile2;
    private String address;

    // Aadhar or PAN image path
    private String documentPath;

    // Aadhaar / PAN
    private String documentType;

    // NEW FIELDS
    private String joiningDate;
    private double monthlySalary;


    @Ignore
    public Staff(String name,
                 String mobile1,
                 String mobile2,
                 String address,
                 String documentPath,
                 String documentType) {

        this(name,
                mobile1,
                mobile2,
                address,
                documentPath,
                documentType,
                "",
                0.0);
    }

    public Staff(String name,
                 String mobile1,
                 String mobile2,
                 String address,
                 String documentPath,
                 String documentType,
                 String joiningDate,
                 double monthlySalary
                 ) {

        this.name = name;
        this.mobile1 = mobile1;
        this.mobile2 = mobile2;
        this.address = address;
        this.documentPath = documentPath;
        this.documentType = documentType;
        this.joiningDate = joiningDate;
        this.monthlySalary = monthlySalary;

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getMobile1() {
        return mobile1;
    }

    public String getPhone1() {
        return mobile1;
    }

    public String getMobile2() {
        return mobile2;
    }

    public String getPhone2() {
        return mobile2;
    }

    public String getAddress() {
        return address;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setMobile1(String mobile1) {
        this.mobile1 = mobile1;
    }

    public void setMobile2(String mobile2) {
        this.mobile2 = mobile2;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setMonthlySalary(double monthlySalary) {
        this.monthlySalary = monthlySalary;
    }


}