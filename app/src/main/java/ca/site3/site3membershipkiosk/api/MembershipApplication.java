package ca.site3.site3membershipkiosk.api;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MembershipApplication implements Serializable {
    @SerializedName("name")
    public String name;
    public String email;
    public String rfid;
    @SerializedName("stripe_payment_token")
    public String stripePaymentToken;
    @SerializedName("enable_vending_machine")
    public Boolean enableVendingMachine;
}
