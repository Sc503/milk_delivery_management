package com.example.network;

import com.example.models.LoginResponse;
import com.example.models.StaffListResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    // ✅ 1. Create Account (Registration)
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

    // ✅ 2. Admin Login
    @FormUrlEncoded
    @POST("loginadmin.php")
    Call<LoginResponse> loginAdmin(
            @Field("mobile") String mobile,
            @Field("password") String password
    );

    // ✅ 3. Staff Login
    @FormUrlEncoded
    @POST("loginstaff.php")
    Call<LoginResponse> loginStaff(
            @Field("mobile") String mobile,
            @Field("password") String password,
            @Field("accountid") String accountId
    );

    // ✅ 4. Create Staff
    @FormUrlEncoded
    @POST("createstaff.php")
    Call<LoginResponse> createStaff(
            @Field("account_id") String accountId,
            @Field("name") String name,
            @Field("mobile") String mobile,
            @Field("password") String password
    );

    // ✅ 5. List All Staff
    @FormUrlEncoded
    @POST("listallstaff.php")
    Call<StaffListResponse> listStaff(
            @Field("account_id") String accountId
    );
}