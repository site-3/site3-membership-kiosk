package ca.site3.site3membershipkiosk;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

public class SignupActivity extends AppCompatActivity implements OnClickListener {

    private static final String LOG_TAG = SignupActivity.class.getSimpleName();

    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;
    String[][] techListsArray;
    private NfcAdapter nfcAdapter;

    private MemberDetails memberDetails = new MemberDetails();

    EditText fullName;
    EditText email;
    EditText rfid;
    EditText paymentCardNumber;
    EditText paymentExpiry;
    EditText paymentCCV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        fullName = (EditText) findViewById(R.id.full_name);
        email = (EditText) findViewById(R.id.email);
        rfid = (EditText) findViewById(R.id.rfid);

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
    }

    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    private void submit() {

        // if successful will post details to our server
        getTokenForPaymentInformation();
    }

    private void submitDetailsToSite3() {
        memberDetails.fullName = fullName.getText().toString();
        memberDetails.email = email.getText().toString();
        memberDetails.rfid = rfid.getText().toString();

        Toast.makeText(SignupActivity.this, "TODO: submit to Site 3", Toast.LENGTH_LONG).show();
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
        memberDetails.rfid = rfid.getText().toString();
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
            memberDetails.stripeCustomerToken = token.getId();
            submitDetailsToSite3();
        }
    };
}
