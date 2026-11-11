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

public class SignUpActivity extends AppCompatActivity {

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
     *
     * @param bitmap
     * @return
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
     *
     * @param isLoading
     */
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     *
     */
    private void setAnimations() {
        // Color transition for Create Account text
        int color1 = Color.argb(255, 216, 61, 105);
        int color2 = Color.argb(255, 182, 87, 255);
        ObjectAnimator colorAnimator = ObjectAnimator.ofArgb(
                binding.textSignIn,
                "textColor",
                color1,
                color2);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setDuration(2000);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.start();
        // Squish animation for pressed Sign In button
        binding.buttonSignUp.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
            return false;
        });
    }

    /**
     *
     */
    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidDetails()) {
                signUp();
            }
        });
        binding.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    /**
     *
     * @param text
     * @return
     */
    private boolean checkForLowercase(String text) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLowerCase(text.charAt(i))) {
                count++;
            }
        }
        return count >= 4;
    }

    /**
     *
     * @param text
     * @return
     */
    private boolean checkForNum(String text) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     *
     * @param text
     * @return
     */
    private boolean checkForSpecialChar(String text) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < text.length() ; i++) {
            if (!Character.isLetterOrDigit(text.charAt(i))) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     *
     * @param text
     * @return
     */
    private boolean checkForUppercase(String text) {
        int count = 0;
        // Scans the inputted password for lowercase letters
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     *
     * @param inputField
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
     *
     * @param message
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     *
     */
    private void signUp() {
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

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }).addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    /**
     *
     * @return
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
        return isValidName(nameTrimmed)
                && isValidEmail(emailTrimmed)
                && isValidPassword(passwordTrimmed)
                && isValidPasswordMatch(passwordTrimmed, confirmPasswordTrimmed);
    }

    /**
     *
     * @param nameTrimmed
     * @return
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
     *
     * @param emailTrimmed
     * @return
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
     *
     * @param passwordTrimmed
     * @return
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
        } else if (!checkForNum(passwordTrimmed)) {
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
     *
     * @param passwordTrimmed
     * @param confirmPasswordTrimmed
     * @return
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
