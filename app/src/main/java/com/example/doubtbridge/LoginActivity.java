package com.example.doubtbridge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText emailInput, passwordInput;
    MaterialButton loginBtn;
    TextView signupText, forgotText;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput  = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn    = findViewById(R.id.loginBtn);
        signupText  = findViewById(R.id.signupText);
        forgotText  = findViewById(R.id.forgotPasswordText);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        loginBtn.setOnClickListener(v -> loginUser());
        signupText.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));

        if (forgotText != null) {
            forgotText.setOnClickListener(v -> {
                Intent i = new Intent(this, ForgotPasswordActivity.class);
                String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
                if (!email.isEmpty()) i.putExtra("email", email);
                startActivity(i);
            });
        }

        // Hide Google login button - not configured
        MaterialButton googleBtn = findViewById(R.id.googleLoginBtn);
        if (googleBtn != null) googleBtn.setVisibility(android.view.View.GONE);
    }

    private void loginUser() {
        String email    = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                String uid = auth.getCurrentUser().getUid();
                db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            if ("teacher".equals(role)) {
                                startActivity(new Intent(this, TeacherDashboardActivity.class));
                            } else {
                                startActivity(new Intent(this, DashboardActivity.class));
                            }
                            finish();
                        } else {
                            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .addOnFailureListener(e -> {
                loginBtn.setEnabled(true);
                loginBtn.setText("Login");
                Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
