package info.adricarda.fingerprint;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class FingerprintActivity extends AppCompatActivity {

    private TextView textView;
    private static final int LOCK_REQUEST_CODE = 221;
    private static final int SECURITY_SETTING_REQUEST_CODE = 233;
    private KeyguardManager keyguardManager;
    FingerprintManager fingerprintManager;

    void authenticateWithSystemPin(){
        Intent i = keyguardManager.createConfirmDeviceCredentialIntent("Secure Password Manager", "");
        try {
            //Start activity for result
            startActivityForResult(i, LOCK_REQUEST_CODE);
        } catch (Exception e) {
            textView.setText("Lock screen security not enabled in Settings");
        }
    }

    public void authenticate(){
        if(!fingerprintManager.isHardwareDetected()){
            authenticateWithSystemPin();
            //textView.setText("Your Device does not have a Fingerprint Sensor");
        }else {
            // Checks whether fingerprint permission is set on manifest
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                textView.setText("Fingerprint authentication permission not enabled");
            }else{
                // Check whether at least one fingerprint is registered
                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    //textView.setText("Register at least one fingerprint in Settings");
                    authenticateWithSystemPin();
                }
                else{
                    // Checks whether lock screen security is enabled or not
                    if (!keyguardManager.isKeyguardSecure()) {
                        textView.setText("Lock screen security not enabled in Settings");

                    }else{
                        FingerprintHandler helper = new FingerprintHandler(this);
                        helper.startAuth(fingerprintManager, null);
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        // Initializing both Android Keyguard Manager and Fingerprint Manager
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        textView = (TextView) findViewById(R.id.desc);
    }

    @Override
    protected void onResume(){
        super.onResume();
        authenticate();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOCK_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    //If screen lock authentication is success update text
                    startHomeActivity();
                } else {
                    //If screen lock authentication is failed update text
                    textView.setText("Athentication error");
                }
                break;
            case SECURITY_SETTING_REQUEST_CODE:
                //When user is enabled Security settings then we don't get any kind of RESULT_OK
                //So we need to check whether device has enabled screen lock or not
                if (keyguardManager.isKeyguardSecure()) {
                    //If screen lock enabled show toast and start intent to authenticate user
                    authenticateWithSystemPin();
                } else {
                    //If screen lock is not enabled just update text
                    textView.setText("Please, enable system lock");
                }

                break;
        }
    }

    void startHomeActivity() {
        finish();
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
    }

}