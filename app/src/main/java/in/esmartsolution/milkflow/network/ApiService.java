package in.esmartsolution.milkflow.network;

import in.esmartsolution.milkflow.models.LoginResponse;
import in.esmartsolution.milkflow.models.StaffListResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {


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


    @FormUrlEncoded
    @POST("loginadmin.php")
    Call<LoginResponse> loginAdmin(
            @Field("mobile") String mobile,
            @Field("password") String password
    );


    @FormUrlEncoded
    @POST("loginstaff.php")
    Call<LoginResponse> loginStaff(
            @Field("mobile") String mobile,
            @Field("password") String password,
            @Field("accountid") String accountId
    );


    @FormUrlEncoded
    @POST("createstaff.php")
    Call<LoginResponse> createStaff(
            @Field("account_id") String accountId,
            @Field("name") String name,
            @Field("mobile") String mobile,
            @Field("password") String password
    );


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


    @FormUrlEncoded
    @POST("listallstaff.php")
    Call<StaffListResponse> listStaff(
            @Field("account_id") String accountId
    );


    @FormUrlEncoded
    @POST("getprofile.php")
    Call<LoginResponse> getProfile(
            @Field("account_id") String accountId
    );
}