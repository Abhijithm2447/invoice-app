package com.example.invoiceprinting;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private Button SendVerificationButton, VerifyButton;
    private EditText InputPhoneNumber, InputVerificationCode;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference UsersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth = FirebaseAuth.getInstance();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        InputPhoneNumber = (EditText) findViewById(R.id.phone_number_input);
        InputVerificationCode = (EditText) findViewById(R.id.verification_code_input);
        SendVerificationButton = (Button) findViewById(R.id.send_ver_code_button);
        VerifyButton = (Button) findViewById(R.id.verify_button);
        loadingBar = new ProgressDialog(this);

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {

                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Invalid phone number please enter your valid phone number with your country code.", Toast.LENGTH_SHORT).show();

                SendVerificationButton.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);

                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }

            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {

                mVerificationId = verificationId;
                mResendToken = token;

                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Code has been send please check", Toast.LENGTH_SHORT).show();

                SendVerificationButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);

                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String currentUserId = mAuth.getCurrentUser().getUid();
                            String deveiceToken = FirebaseInstanceId.getInstance().getToken();
                            UsersRef.child(currentUserId).child("device_token")
                                    .setValue(deveiceToken)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                loadingBar.dismiss();
                                                Toast.makeText(PhoneLoginActivity.this, "Congratulations we logged in successfully", Toast.LENGTH_SHORT).show();
                                                SendUserToMainActivity();
                                            }
                                        }
                                    });

                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void setSendVerificationButton(View view){



        String phoneNumber = InputPhoneNumber.getText().toString();
        if(TextUtils.isEmpty(phoneNumber)){
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
        }else {
            loadingBar.setTitle("Phone Verification");
            loadingBar.setMessage("Please wait, while we authenticating your phone");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    PhoneLoginActivity.this,               // Activity (for callback binding)
                    callbacks);        // OnVerificationStateChangedCallbacks
        }
    }

    public void setVerifyButton(View view){
        SendVerificationButton.setVisibility(View.INVISIBLE);
        InputPhoneNumber.setVisibility(View.INVISIBLE);

        String verificationCode = InputVerificationCode.getText().toString();
        if(TextUtils.isEmpty(verificationCode)){
            Toast.makeText(this, "Please write verification code first", Toast.LENGTH_SHORT).show();
        }else {

            loadingBar.setTitle("Verification Code");
            loadingBar.setMessage("Please wait, while we verifying your Verification Code");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
            signInWithPhoneAuthCredential(credential);
        }
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(PhoneLoginActivity.this, BarCodeActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
