package com.example.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String userType;
    private String mobile;
    private String password;

    public User(String userType, String mobile, String password) {
        this.userType = userType;
        this.mobile = mobile;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserType() {
        return userType;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPassword() {
        return password;
    }
}