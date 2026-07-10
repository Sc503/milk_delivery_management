package com.example.models;

import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Generated("jsonschema2pojo")
public class MyData implements Serializable {

    @SerializedName("id")
    @Expose
    private Integer id;

    @SerializedName("ownername")
    @Expose
    private String ownername;

    @SerializedName("business_name")
    @Expose
    private String businessName;

    @SerializedName("mobile")
    @Expose
    private String mobile;

    @SerializedName("city")
    @Expose
    private String city;

    @SerializedName("address")
    @Expose
    private String address;

    @SerializedName("reg_date_time")
    @Expose
    private String regDateTime;

    @SerializedName("status")
    @Expose
    private Integer status;

    @SerializedName("remarks")
    @Expose
    private String remarks;

    //  New fields for staff/owner details
    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("usertype")
    @Expose
    private String usertype;

    @SerializedName("account_id")
    @Expose
    private Integer accountId;

    @SerializedName("isactive")
    @Expose
    private Integer isactive;

    // ============ Existing Getters and Setters ============

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOwnername() {
        return ownername;
    }

    public void setOwnername(String ownername) {
        this.ownername = ownername;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegDateTime() {
        return regDateTime;
    }

    public void setRegDateTime(String regDateTime) {
        this.regDateTime = regDateTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    // ============ New Getters and Setters ============

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsertype() {
        return usertype;
    }

    public void setUsertype(String usertype) {
        this.usertype = usertype;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getIsactive() {
        return isactive;
    }

    public void setIsactive(Integer isactive) {
        this.isactive = isactive;
    }

    // ============ Optional: Helper Methods ============

    @Override
    public String toString() {
        return "MyData{" +
                "id=" + id +
                ", ownername='" + ownername + '\'' +
                ", businessName='" + businessName + '\'' +
                ", mobile='" + mobile + '\'' +
                ", name='" + name + '\'' +
                ", usertype='" + usertype + '\'' +
                ", accountId=" + accountId +
                ", isactive=" + isactive +
                '}';
    }
}