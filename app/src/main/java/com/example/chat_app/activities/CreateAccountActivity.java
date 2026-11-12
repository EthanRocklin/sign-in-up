package com.example.chat_app.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chat_app.databinding.ActivitySignUpBinding;
import com.example.chat_app.utilities.Constants;
import com.example.chat_app.utilities.PreferenceManager;
import com.example.chat_app.utilities.TriState;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class CreateAccountActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        setAnimations();
    }

    /**
     *
     */
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.profileImage.setImageBitmap(bitmap);
                        binding.textAddImage.setVisibility(View.GONE);
                        encodedImage = encodeImage(bitmap);
                    } catch (FileNotFoundException e) {
                       Log.d("WARNING", "Image file not found");
                    }
                }
            });

    /**
     * This method encodes the user's uploaded profile image into a common format
     * and represents the encoded image as a String.
     * @param bitmap the image's grid of colored pixels
     * @return a String representing the encoded image
     */
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * This method toggles the visibility Create button and loading icon
     * based on the login status.
     * @param isLoading a boolean that indicates if the application is
     *                  in the process of logging the user in.
     */
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.btnCreate.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnCreate.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
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
                binding.textSignIn,
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
        binding.btnCreate.setOnTouchListener((v, event) -> {
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
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.btnCreate.setOnClickListener(v -> {
            if (isValidDetails()) {
                createAccount();
            }
        });
        binding.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    /**
     * This method checks for the minimum required amount of lowercase letters
     *      in the user's inputted password.
     * @param password the user's inputted password
     * @return a boolean indicating if the user entered the minimum number of
     *         lowercase letters
     */
    private boolean checkForLowercase(String password) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < password.length(); i++) {
            if (Character.isLowerCase(password.charAt(i))) {
                count++;
            }
        }
        return count >= 4;
    }

    /**
     * This method checks for the minimum required amount of digits in the user's
     *      inputted password.
     * @param password the user's inputted password
     * @return a boolean indicating if the user entered the minimum number of digits
     */
    private boolean checkForDigit(String password) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < password.length(); i++) {
            if (Character.isDigit(password.charAt(i))) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     * This method checks for the minimum required amount of special characters in
     *      the user's inputted password.
     * @param password the user's inputted password
     * @return a boolean indicating if the user entered the minimum number of
     *         special characters
     */
    private boolean checkForSpecialChar(String password) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < password.length() ; i++) {
            if (!Character.isLetterOrDigit(password.charAt(i))) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     * This method checks for the minimum required amount of uppercase letters
     *      in the user's inputted password.
     * @param password the user's inputted password
     * @return a boolean indicating if the user entered the minimum number of
     *         uppercase letters
     */
    private boolean checkForUppercase(String password) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < password.length(); i++) {
            if (Character.isUpperCase(password.charAt(i))) {
                count++;
            }
        }
        return count >= 2;
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
     * This method displays a Toast notification message on the user's screen.
     * @param message the text to include in the notification
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * This method attempts to create an account with the user's inputted details.
     */
    private void createAccount() {
        // Checking loading
        loading(true);
        // Post data to Firebase
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, String> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        //
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    // Highlights input fields with green indicating valid credentials
                    highlightInputField(binding.inputName, TriState.TRUE);
                    highlightInputField(binding.inputEmail, TriState.TRUE);
                    highlightInputField(binding.inputPassword, TriState.TRUE);
                    highlightInputField(binding.inputConfirmPassword, TriState.TRUE);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    // Creates a new Intent for starting the application's main activity upon successful account creation
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }).addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    /**
     * This method validates that all the user's inputted account details are valid
     *      before the application attempts to create a new account with them.
     * @return a boolean representing if the user's entered account details have
     *         valid formats
     */
    private Boolean isValidDetails() {
        String nameTrimmed = binding.inputName.getText().toString().trim();
        String emailTrimmed = binding.inputEmail.getText().toString().trim();
        String passwordTrimmed = binding.inputPassword.getText().toString().trim();
        String confirmPasswordTrimmed = binding.inputConfirmPassword.getText().toString().trim();
        if (encodedImage == null) {
            showToast("Please Select An Image");
            return false;
        }
        // Checks all requirements for account details and returns the result
        return isValidImage(encodedImage)
                && isValidName(nameTrimmed)
                && isValidEmail(emailTrimmed)
                && isValidPassword(passwordTrimmed)
                && isValidPasswordMatch(passwordTrimmed, confirmPasswordTrimmed);
    }

    /**
     * This method validates that the user uploaded an image for their new account.
     * @param encodedImage a String representing the uploaded image after encoding
     * @return a boolean representing if the user uploaded an image
     */
    private Boolean isValidImage(String encodedImage) {
        if (encodedImage == null) {
            showToast("Please Select An Image");
            return false;
        }
        return true;
    }

    /**
     * This method validates that the user's inputted name is validly formatted.
     * @param nameTrimmed a String to check for a valid format
     * @return a boolean representing if the inputted name is formatted correctly
     */
    private Boolean isValidName(String nameTrimmed) {
        if (nameTrimmed.isEmpty()) {
            highlightInputField(binding.inputName, TriState.FALSE);
            showToast("Please Enter Your Name");
            return false;
        } else if (nameTrimmed.length() > 45) {
            highlightInputField(binding.inputName, TriState.FALSE);
            showToast("Name must not exceed 45 characters");
            return false;
        }
        return true;
    }

    /**
     * This method validates that the user entered a validly formatted email address.
     * @param emailTrimmed the email to check for a valid format
     * @return a boolean representing if the inputted email is formatted correctly
     */
    private Boolean isValidEmail(String emailTrimmed) {
        highlightInputField(binding.inputName, TriState.UNKNOWN);
        if (emailTrimmed.isEmpty()) {
            highlightInputField(binding.inputEmail, TriState.FALSE);
            showToast("Please Enter Your Email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            highlightInputField(binding.inputEmail, TriState.FALSE);
            showToast("Please Enter A Valid Email");
            return false;
        } else if (emailTrimmed.length() > 254) {
            highlightInputField(binding.inputEmail, TriState.FALSE);
            showToast("Email must not exceed 254 characters");
            return false;
        }
        return true;
    }

    /**
     * This method validates that the user entered a validly formatted email address.
     * @param passwordTrimmed the email to check for a valid format
     * @return a boolean representing if the inputted email is formatted correctly
     */
    private Boolean isValidPassword(String passwordTrimmed) {
        highlightInputField(binding.inputEmail, TriState.UNKNOWN);
        if (passwordTrimmed.isEmpty()) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Please Enter Your Password");
            return false;
        } else if (!checkForUppercase(passwordTrimmed)) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Password needs at least 2 uppercase letters");
            return false;
        } else if (!checkForLowercase(passwordTrimmed)) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Password needs at least 4 lowercase letters");
            return false;
        } else if (!checkForDigit(passwordTrimmed)) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Password needs at least 2 digits");
            return false;
        } else if (!checkForSpecialChar(passwordTrimmed)) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Password needs at least 2 special characters");
            return false;
        } else if (passwordTrimmed.length() > 45) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            showToast("Password must not exceed 45 characters");
            return false;
        }
        return true;
    }

    /**
     * This method validates that the two passwords the user entered match as part of
     *      confirming the password to be stored in the database.
     * @param passwordTrimmed a String representing the first password inputted
     * @param confirmPasswordTrimmed a String representing the second password inputted
     * @return a boolean representing the status of the passwords matching
     */
    private Boolean isValidPasswordMatch(String passwordTrimmed, String confirmPasswordTrimmed) {
        if (!passwordTrimmed.equals(confirmPasswordTrimmed)) {
            highlightInputField(binding.inputPassword, TriState.FALSE);
            highlightInputField(binding.inputConfirmPassword, TriState.FALSE);
            showToast("Passwords Do Not Match");
            return false;
        }
        return true;
    }

}
