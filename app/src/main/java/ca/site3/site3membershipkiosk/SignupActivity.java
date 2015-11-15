package ca.site3.site3membershipkiosk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

import ca.site3.site3membershipkiosk.api.MembershipApplication;
import ca.site3.site3membershipkiosk.api.MembershipApplicationResponse;
import ca.site3.site3membershipkiosk.api.service.ApiService;

public class SignupActivity extends AppCompatActivity implements OnClickListener {

    private static final String LOG_TAG = SignupActivity.class.getSimpleName();

    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;
    String[][] techListsArray;
    private NfcAdapter nfcAdapter;

    private MembershipApplication membershipApplication = new MembershipApplication();

    EditText fullName;
    EditText email;
    EditText rfid;
    EditText paymentCardNumber;
    EditText paymentExpiry;
    EditText paymentCCV;
    CheckBox enableVendingMachine;

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            String message = intent.getStringExtra(ApiService.API_RESPONSE_ERROR);
            MembershipApplicationResponse statusResponse = (MembershipApplicationResponse) intent.getSerializableExtra(ApiService.API_RESPONSE_BODY);

            if (ApiService.API_RESPONSE_CREATE_MEMBERSHIP_APPLICATION_INTENT.equals(action)) {
                boolean wasSuccessful = intent.getBooleanExtra(ApiService.API_RESPONSE_SUCCESS_STATUS, false);

                if (wasSuccessful) {
                    handleSuccess(statusResponse);
                } else {
                    handleFailure(message);
                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        fullName = (EditText) findViewById(R.id.full_name);
        email = (EditText) findViewById(R.id.email);
        rfid = (EditText) findViewById(R.id.rfid);
        enableVendingMachine = (CheckBox) findViewById(R.id.enable_vending_machine);

        paymentCardNumber = (EditText) findViewById(R.id.payment_card_number);
        paymentExpiry = (EditText) findViewById(R.id.payment_expiry);
        paymentCCV = (EditText) findViewById(R.id.payment_ccv);

        findViewById(R.id.submit).setOnClickListener(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        intentFiltersArray = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };
        techListsArray = new String[][] { new String[] { NfcF.class.getName() } };
    }

    public void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
        unregisterReceiver(receiver);
    }

    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
        registerReceiver(receiver, new IntentFilter(ApiService.API_RESPONSE_CREATE_MEMBERSHIP_APPLICATION_INTENT));
    }

    private void submit() {

        // if successful will post details to our server
        getTokenForPaymentInformation();
    }

    private void submitDetailsToSite3() {
        membershipApplication.name = fullName.getText().toString();
        membershipApplication.email = email.getText().toString();
        membershipApplication.rfid = rfid.getText().toString();
        membershipApplication.enableVendingMachine = enableVendingMachine.isChecked();

        Intent serviceIntent = new Intent(this, ApiService.class);
        serviceIntent.putExtra(ApiService.API_REQUEST_TYPE, ApiService.API_CREATE_MEMBERSHIP_APPLICATION_INTENT);
        serviceIntent.putExtra(ApiService.API_REQUEST_DATA, membershipApplication);
        startService(serviceIntent);
    }

    private static int[] parsePaymentExpiry(String expiryString) {
        String[] splitExpiry = expiryString.split("[^0-9]");

        Integer expiryMonth = 0;
        Integer expiryYear = 0;

        try {
            expiryMonth = Integer.parseInt(splitExpiry[0]);
            expiryYear = Integer.parseInt(splitExpiry[1]);

            // Add 2000 if the expiry year is less than four digits
            // #Y3K
            if (splitExpiry[1].length() < 4) {
                expiryYear += 2000;
            }
        } catch (Exception e) {
            // don't care
        }

        return new int[] {expiryMonth, expiryYear};
    }

    private void getTokenForPaymentInformation() {
        int[] expiryDetails = parsePaymentExpiry(paymentExpiry.getText().toString());
        Card card = new Card(paymentCardNumber.getText().toString(), expiryDetails[0], expiryDetails[1], paymentCCV.getText().toString());

        try {
            Stripe stripe = new Stripe(getString(R.string.stripe_token));
            stripe.createToken(card, stripeCallback);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Stripe error:", e);
        }
    }

    public void onNewIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Toast.makeText(SignupActivity.this, "Tag scanned", Toast.LENGTH_SHORT).show();

        String hexTagId = tagIdToHex(tagFromIntent.getId());
        rfid.setText(hexTagId);
        membershipApplication.rfid = rfid.getText().toString();
    }

    private String tagIdToHex(byte[] tagId) {
        String hexTagId = "";
        for (byte b : tagId) {
            hexTagId = hexTagId + String.format("%02x", b);
        }
        return hexTagId;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.submit:
                submit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_signup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.edit_rfid:
                rfid.setVisibility(View.VISIBLE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void handleSuccess(MembershipApplicationResponse response) {
        Toast.makeText(SignupActivity.this, "Success", Toast.LENGTH_SHORT).show();
    }

    public void handleFailure(String error) {
        Toast.makeText(SignupActivity.this, "Failure", Toast.LENGTH_SHORT).show();
    }

    TokenCallback stripeCallback = new TokenCallback() {
        @Override
        public void onError(Exception error) {
            // Show localized error message
            Toast.makeText(SignupActivity.this,
                    error.getLocalizedMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }

        @Override
        public void onSuccess(Token token) {
            // Send token to your server
            membershipApplication.stripePaymentToken = token.getId();
            submitDetailsToSite3();
        }
    };
}
