package com.example.appointable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;

import android.text.InputType;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private Button btnSignIn;
    private TextView tvSignUp, tvForgotPassword;
    private EditText etEmail, etPassword;
    private ImageView ivTogglePassword;
    private AppCompatCheckBox rememberMeCheckBox;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SharedPreferences sharedPreferences;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        btnSignIn = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);

        //        Clear credentials after log out
        etEmail.setText("");
        etPassword.setText("");

        boolean isRemembered = sharedPreferences.getBoolean("remember", false);
        if (isRemembered) {
            String savedEmail = sharedPreferences.getString("email", "");
            String savedPassword = sharedPreferences.getString("password", "");
            etEmail.setText(savedEmail);
            etPassword.setText(savedPassword);
            rememberMeCheckBox.setChecked(true);
        }

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;

            if (isPasswordVisible) {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                ivTogglePassword.setImageResource(R.drawable.ic_eye);
            }

            etPassword.setSelection(etPassword.getText().length());

            ivTogglePassword.animate()
                    .rotationBy(180f)
                    .setDuration(150)
                    .start();
        });

        rememberMeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(100)
                    .withEndAction(() ->
                            buttonView.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(80)
                                    .start()
                    )
                    .start();
        });

        btnSignIn.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        String text = "Still don’t have an account? Sign Up";
        SpannableString spannableString = new SpannableString(text);

        int startIndex = text.indexOf("Sign Up");
        int endIndex = startIndex + "Sign Up".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        };

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvSignUp.setText(spannableString);
        tvSignUp.setMovementMethod(LinkMovementMethod.getInstance());
        tvSignUp.setHighlightColor(Color.TRANSPARENT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.getUid());
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {

                        if (rememberMeCheckBox.isChecked()) {
                            sharedPreferences.edit()
                                    .putBoolean("remember", true)
                                    .putString("email", email)
                                    .putString("password", password)
                                    .apply();
                        } else {
                            sharedPreferences.edit()
                                    .clear()
                                    .apply();
                        }

                        checkUserRoleAndRedirect(user.getUid());
                    } else {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                    }
                })
                .addOnFailureListener(e -> {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Sign In");
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkUserRoleAndRedirect(String uid) {

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        Toast.makeText(this, "User data not found.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String role = doc.getString("role");

                    if (role == null) {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        Toast.makeText(this, "Role not found for this account.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent;

                    if (role.equals("Student")) {
                        intent = new Intent(this, ParentsMainActivity.class);
                    } else if (role.equalsIgnoreCase("Sped Teacher") ||
                            role.equalsIgnoreCase("OT Associates") ||
                            role.equalsIgnoreCase("Sped Coordinator") ||
                            role.equalsIgnoreCase("Occupational Therapist")) {
                        intent = new Intent(this, TeachersMainActivity.class);
                    } else {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_LONG).show();
                        return;
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSignIn.setEnabled(true);
                    btnSignIn.setText("Sign In");
                    Toast.makeText(this, "Error loading user role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}