package com.example.appointable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private ImageView imgProfile;
    private EditText etFirstname, etLastname, etMiddlename, etSuffix;
    private EditText etBirthdate, etAge, etEmail, etUsername, etContactNumber;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spRole;
    private Button btnEditProfile;

    private boolean isEditing = false;

    private String currentProfileImageUrl = null;
    private String newProfileImageUrl = null;

    private static final String CLOUD_NAME = "djqcwj12e";
    private static final String UPLOAD_PRESET = "appointable_profile";

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private Button btnLogout;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupRoleSpinner();
        setupImagePicker();
        setEditable(false);

        loadUserData();

        imgProfile.setOnClickListener(v -> {
            if (!isEditing) return;
            openGallery();
        });

        btnEditProfile.setOnClickListener(v -> {
            if (!isEditing) {
                isEditing = true;
                setEditable(true);
                btnEditProfile.setText("Save");
            } else {
                if (validateFields()) {
                    saveProfileChanges();
                }
            }
        });


        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", getContext().MODE_PRIVATE);

        // Initialize buttons
        btnLogout = view.findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.imgProfile);

        etFirstname = view.findViewById(R.id.etFirstname);
        etLastname = view.findViewById(R.id.etLastname);
        etMiddlename = view.findViewById(R.id.etMiddlename);
        etSuffix = view.findViewById(R.id.etSuffix);

        etBirthdate = view.findViewById(R.id.etBirthdate);
        etAge = view.findViewById(R.id.etAge);
        etEmail = view.findViewById(R.id.etEmail);
        etUsername = view.findViewById(R.id.etUsername);
        etContactNumber = view.findViewById(R.id.etContactNumber);

        rgGender = view.findViewById(R.id.rgGender);
        rbMale = view.findViewById(R.id.rbMale);
        rbFemale = view.findViewById(R.id.rbFemale);

        spRole = view.findViewById(R.id.spRole);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupRoleSpinner() {
        if (getContext() == null) return;

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.role_list,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(adapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToCloudinary(imageUri);
                        }
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void setEditable(boolean editable) {
        etFirstname.setEnabled(editable);
        etLastname.setEnabled(editable);
        etMiddlename.setEnabled(editable);
        etSuffix.setEnabled(editable);
        etUsername.setEnabled(editable);
        etContactNumber.setEnabled(editable);

        rgGender.setEnabled(false);
        rbMale.setEnabled(false);
        rbFemale.setEnabled(false);
        etBirthdate.setEnabled(false);
        etAge.setEnabled(false);
        etEmail.setEnabled(false);
        spRole.setEnabled(false);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (!doc.exists()) return;

                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        String middleName = doc.getString("middleName");
                        String suffix = doc.getString("suffix");
                        String birthdate = doc.getString("birthdate");
                        String age = doc.getString("age");
                        String email = doc.getString("email");
                        String username = doc.getString("username");
                        String contact = doc.getString("contact");
                        String role = doc.getString("role");
                        String gender = doc.getString("gender");
                        String profileImageUrl = doc.getString("profileImageUrl");

                        etFirstname.setText(firstName != null ? firstName : "");
                        etLastname.setText(lastName != null ? lastName : "");
                        etMiddlename.setText(middleName != null ? middleName : "");
                        etSuffix.setText(suffix != null ? suffix : "");
                        etBirthdate.setText(birthdate != null ? birthdate : "");
                        etAge.setText(age != null ? age : "");
                        etEmail.setText(email != null ? email : "");
                        etUsername.setText(username != null ? username : "");
                        etContactNumber.setText(contact != null ? contact : "");

                        if (gender != null) {
                            if ("Male".equalsIgnoreCase(gender)) {
                                rbMale.setChecked(true);
                            } else if ("Female".equalsIgnoreCase(gender)) {
                                rbFemale.setChecked(true);
                            }
                        }

                        if (role != null) {
                            ArrayAdapter adapter = (ArrayAdapter) spRole.getAdapter();
                            if (adapter != null) {
                                int index = adapter.getPosition(role);
                                if (index >= 0) {
                                    spRole.setSelection(index);
                                }
                            }
                        }

                        currentProfileImageUrl = profileImageUrl;

                        if (getContext() == null) return;

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(ProfileFragment.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgProfile);
                        } else {
                            Glide.with(ProfileFragment.this)
                                    .load(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgProfile);
                        }
                    }
                });
    }

    private boolean validateFields() {
        String ageStr = etAge.getText().toString().trim();
        String contact = etContactNumber.getText().toString().trim();

        if (!TextUtils.isEmpty(ageStr)) {
            try {
                int age = Integer.parseInt(ageStr);
                if (age <= 0 || age > 120) {
                    Toast.makeText(getContext(), "Please enter a valid age.", Toast.LENGTH_SHORT).show();
                    etAge.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Age must be a number.", Toast.LENGTH_SHORT).show();
                etAge.requestFocus();
                return false;
            }
        }

        if (!TextUtils.isEmpty(contact) && contact.length() < 7) {
            Toast.makeText(getContext(), "Contact number seems too short.", Toast.LENGTH_SHORT).show();
            etContactNumber.requestFocus();
            return false;
        }

        return true;
    }

    private void saveProfileChanges() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        String firstName = etFirstname.getText().toString().trim();
        String lastName = etLastname.getText().toString().trim();
        String middleName = etMiddlename.getText().toString().trim();
        String suffix = etSuffix.getText().toString().trim();
        String contact = etContactNumber.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("middleName", middleName);
        updates.put("suffix", suffix);
        updates.put("contact", contact);
        updates.put("username", username);

        String finalImageUrl = (newProfileImageUrl != null && !newProfileImageUrl.isEmpty())
                ? newProfileImageUrl
                : currentProfileImageUrl;

        updates.put("profileImageUrl", finalImageUrl);

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    currentProfileImageUrl = finalImageUrl;
                    newProfileImageUrl = null;
                    Toast.makeText(getContext(), "Profile updated.", Toast.LENGTH_SHORT).show();
                    isEditing = false;
                    setEditable(false);
                    btnEditProfile.setText("Edit Profile");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Error updating profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

//    Logout function
    private void showLogoutDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {

                    mAuth.signOut();

                    // CLEAR REMEMBER ME
                    sharedPreferences.edit().clear().apply();

                    if (getActivity() != null) {
                        Intent i = new Intent(getActivity(), LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void uploadImageToCloudinary(Uri imageUri) {
        if (getContext() == null) return;

        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        byte[] imageBytes;
        try {
            imageBytes = readBytesFromUri(imageUri);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to read image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("image/*");
        RequestBody fileBody = RequestBody.create(mediaType, imageBytes);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "profile.jpg", fileBody)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build();

        String url = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Upload error: " + response.message(),
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                String body = response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    String secureUrl = json.getString("secure_url");

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        newProfileImageUrl = secureUrl;

                        Glide.with(ProfileFragment.this)
                                .load(secureUrl)
                                .circleCrop()
                                .into(imgProfile);

                        Toast.makeText(getContext(),
                                "Image selected. Tap Save to update profile.",
                                Toast.LENGTH_SHORT).show();
                    });
                } catch (JSONException e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Parse error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        if (getContext() == null) return new byte[0];

        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
        if (inputStream == null) return new byte[0];

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        inputStream.close();
        return byteBuffer.toByteArray();
    }
}

