package com.example.doubtbridge;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    TextInputEditText emailInput;
    MaterialButton sendBtn;
    MaterialToolbar toolbar;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailInput);
        sendBtn    = findViewById(R.id.sendBtn);
        toolbar    = findViewById(R.id.toolbar);
        auth       = FirebaseAuth.getInstance();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Pre-fill if email was passed
        String prefillEmail = getIntent().getStringExtra("email");
        if (prefillEmail != null) emailInput.setText(prefillEmail);

        sendBtn.setOnClickListener(v -> sendReset());
    }

    private void sendReset() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        sendBtn.setEnabled(false);
        sendBtn.setText("Sending...");
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(v -> {
                Toast.makeText(this,
                    "✅ Reset link sent to " + email + "\nCheck your inbox.",
                    Toast.LENGTH_LONG).show();
                finish();
            })
            .addOnFailureListener(e -> {
                sendBtn.setEnabled(true);
                sendBtn.setText("Send Reset Link");
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
