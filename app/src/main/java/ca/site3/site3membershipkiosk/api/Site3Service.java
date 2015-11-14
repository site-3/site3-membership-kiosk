package ca.site3.site3membershipkiosk.api;

import com.google.gson.JsonObject;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

public interface Site3Service {
    @POST("/apply")
    void createMemberApplication(@Body JsonObject membershipDetails, Callback<MembershipApplicationResponse> callback);
}
