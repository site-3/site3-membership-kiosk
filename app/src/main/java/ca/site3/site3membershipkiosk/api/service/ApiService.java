package ca.site3.site3membershipkiosk.api.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ca.site3.site3membershipkiosk.R;
import ca.site3.site3membershipkiosk.api.MembershipApplication;
import ca.site3.site3membershipkiosk.api.MembershipApplicationResponse;
import ca.site3.site3membershipkiosk.api.Site3Service;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ApiService extends IntentService {

    public static final String API_REQUEST_TYPE = "API_REQUEST_TYPE";
    public static final String API_REQUEST_DATA = "API_REQUEST_DATA";

    public static final String API_CREATE_MEMBERSHIP_APPLICATION_INTENT = "API_CREATE_MEMBERSHIP_APPLICATION_INTENT";

    public static final String API_RESPONSE_CREATE_MEMBERSHIP_APPLICATION_INTENT = "API_RESPONSE_CREATE_MEMBERSHIP_APPLICATION_INTENT";

    public static final String API_RESPONSE_SUCCESS_STATUS = "success";
    public static final String API_RESPONSE_BODY = "body";
    public static final String API_RESPONSE_ERROR = "error";
    public static final String API_RESPONSE_ERRORS = "errors";

    public ApiService() {
        super("API Service");
    }

    public static Site3Service createService(final Context context) {
        RestAdapter.Builder restAdapterBuilder = new RestAdapter.Builder()
                .setEndpoint(context.getString(R.string.api_endpoint))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Accept", "application/json");
                    }
                });

        return restAdapterBuilder.build().create(Site3Service.class);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String requestType = intent.getStringExtra(API_REQUEST_TYPE);

        if (API_CREATE_MEMBERSHIP_APPLICATION_INTENT.equals(requestType)) {
            createMembershipApplication(intent);
        }
    }

    private void createMembershipApplication(final Intent requestIntent) {
        MembershipApplication membershipApplication = (MembershipApplication) requestIntent.getSerializableExtra(API_REQUEST_DATA);

        JsonObject json = new Gson().toJsonTree(membershipApplication).getAsJsonObject();

        createService(getBaseContext()).createMemberApplication(json, new Callback<MembershipApplicationResponse>() {
            @Override
            public void success(MembershipApplicationResponse membershipApplicationResponse, Response response) {
                Intent responseIntent = new Intent(API_RESPONSE_CREATE_MEMBERSHIP_APPLICATION_INTENT);
                responseIntent.putExtra(API_RESPONSE_SUCCESS_STATUS, true);
                responseIntent.putExtra(API_RESPONSE_BODY, membershipApplicationResponse);
                ApiService.this.sendBroadcast(responseIntent);
            }

            @Override
            public void failure(RetrofitError error) {
                Intent responseIntent = new Intent(API_RESPONSE_CREATE_MEMBERSHIP_APPLICATION_INTENT);
                responseIntent.putExtra(API_RESPONSE_SUCCESS_STATUS, false);
                responseIntent.putExtra(API_RESPONSE_ERROR, error.toString());
                ApiService.this.sendBroadcast(responseIntent);
            }
        });
    }
}
