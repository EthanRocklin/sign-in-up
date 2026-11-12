package com.example.chat_app.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat_app.databinding.ActivitySignInBinding;
import com.example.chat_app.utilities.Constants;
import com.example.chat_app.utilities.PreferenceManager;
import com.example.chat_app.utilities.TriState;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        setAnimations();
    }

    /**
     * This method sets any animations needed for the Sign In (Login) page.
     */
    private void setAnimations() {
        // Color transition for Create Account text
        int color1 = Color.argb(255, 216, 61, 105);
        int color2 = Color.argb(255, 182, 87, 255);
        int color3 = Color.argb(255, 232, 136, 88);
        ObjectAnimator colorAnimator = ObjectAnimator.ofArgb(
                binding.textCreateAccount,
                "textColor",
                color1,
                color2,
                color3);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setDuration(3000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.start();
        // Squish animation for pressed Sign In button
        binding.btnLogin.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
            return false;
        });
    }

    /**
     * This method sets any event listeners needed for the Sign In (Login) page.
     */
    private void setListeners() {
        binding.textCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), CreateAccountActivity.class)));
        binding.btnLogin.setOnClickListener(v -> {
            if (validateCredentials()) {
                login();
            }
        });
    }

    /**
     * This method highlights the passed EditText field red, green, or its default
     * color depending on the validity of the input.
     * @param inputField the EditText field to be highlighted
     * @param isValid the status of the input's validity (TRUE, FALSE, UNKNOWN)
     */
    private void highlightInputField(EditText inputField, TriState isValid) {
        // Gets the GradientDrawable of the EditText element to change the
        Drawable background = inputField.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable gradient = (GradientDrawable) background.mutate();
            int strokePx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 2,
                    inputField.getResources().getDisplayMetrics());
            // Colors based on the credential being valid
            int color = Color.argb(255, 0, 0, 0);
            if (isValid == TriState.TRUE) {
                color = Color.argb(255, 125, 255, 89);
            } else if (isValid == TriState.FALSE) {
                color = Color.argb(255, 255, 89, 100);
            }
            gradient.setStroke(strokePx, color);
        }
    }

    /**
     * This method toggles the visibility Login button and loading icon
     * based on the login status.
     * @param isLoading a boolean that indicates if the application is
     *                  in the process of logging the user in.
     */
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.btnLogin.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnLogin.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * This method attempts to login the user with their inputted credentials
     */
    private void login() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()
                            && task.getResult() != null
                            && !task.getResult().getDocuments().isEmpty()) {
                        // Highlights input fields with green indicating valid credentials
                        highlightInputField(binding.inputEmail, TriState.TRUE);
                        highlightInputField(binding.inputPassword, TriState.TRUE);
                        // Creates and configures snapshot with user's info
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));;
                        // Creates a new Intent for starting the application's main activity upon successful login
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        highlightInputField(binding.inputEmail, TriState.FALSE);
                        highlightInputField(binding.inputPassword, TriState.FALSE);
                        showToast("Unable to Login.\nCheck email and password");
                    }
                });
    }

    /**
     * This method displays a Toast notification message on the user's screen.
     * @param message the text to include in the notification
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * This method validates that all the user's inputted credentials are valid
     *      before confirming them with the database's stored credentials.
     * @return a boolean representing if the user's entered credentials have
     *         valid formats
     */
    private boolean validateCredentials() {
        String emailTrimmed = binding.inputEmail.getText().toString().trim();
        String passwordTrimmed = binding.inputPassword.getText().toString().trim();
        return isValidEmail(emailTrimmed) && isValidPassword(passwordTrimmed);
    }

    /**
     * This method validates that the user entered a validly formatted email address.
     * @param emailTrimmed a String to check for a valid format
     * @return a boolean representing if the inputted email is formatted correctly
     */
    private Boolean isValidEmail(String emailTrimmed) {
        if (emailTrimmed.isEmpty()) {
            highlightInputField(binding.inputEmail, TriState.FALSE);
            showToast("Please Enter Your Email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            highlightInputField(binding.inputEmail, TriState.FALSE);
            showToast("Please Enter A Valid Email Address");
            return false;
        }
        return true;
    }

    /**
     * This method validates that the user entered a validly formatted email address.
     * @param passwordTrimmed a String to check for a valid format
     * @return a boolean representing if the inputted email is formatted correctly
     */
    private Boolean isValidPassword(String passwordTrimmed) {
        if (passwordTrimmed.isEmpty()) {
            highlightInputField(binding.inputEmail, TriState.UNKNOWN);
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Please Enter Your Password");
            return false;
        }
        return true;
    }

}
