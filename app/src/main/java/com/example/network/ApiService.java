package com.example.network;

import com.example.models.LoginResponse;
import com.example.models.StaffListResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    //  Create Account (Registration)
    @FormUrlEncoded
    @POST("createaccount.php")
    Call<LoginResponse> createAccount(
            @Field("ownername") String ownerName,
            @Field("business_name") String businessName,
            @Field("mobile") String mobile,
            @Field("city") String city,
            @Field("address") String address,
            @Field("password") String password
    );

    //   Admin Login
    @FormUrlEncoded
    @POST("loginadmin.php")
    Call<LoginResponse> loginAdmin(
            @Field("mobile") String mobile,
            @Field("password") String password
    );

    //  Staff Login
    @FormUrlEncoded
    @POST("loginstaff.php")
    Call<LoginResponse> loginStaff(
            @Field("mobile") String mobile,
            @Field("password") String password,
            @Field("accountid") String accountId
    );

    //  Create Staff
    @FormUrlEncoded
    @POST("createstaff.php")
    Call<LoginResponse> createStaff(
            @Field("account_id") String accountId,
            @Field("name") String name,
            @Field("mobile") String mobile,
            @Field("password") String password
    );

    // update staff
    @FormUrlEncoded
    @POST("createstaff.php")
    Call<LoginResponse> updateStaff(
            @Field("staff_id") String staffId,
            @Field("account_id") String accountId,
            @Field("name") String name,
            @Field("mobile") String mobile,
            @Field("password") String password,
            @Field("status") String status
    );

    //  List All Staff
    @FormUrlEncoded
    @POST("listallstaff.php")
    Call<StaffListResponse> listStaff(
            @Field("account_id") String accountId
    );
}