package com.qurban.livestock;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etAuthEmail, etAuthPassword;
    private Button btnLogin, btnRegister, btnLogout;
    private TextView tvUserStatus;

    private Spinner spinnerCategory;
    private EditText etTitle, etPrice, etWeight, etPhone, etLocation;
    private Button btnListNow;
    private TextView tvFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Auth Views
        etAuthEmail = findViewById(R.id.etAuthEmail);
        etAuthPassword = findViewById(R.id.etAuthPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogout = findViewById(R.id.btnLogout);
        tvUserStatus = findViewById(R.id.tvUserStatus);

        // Listing Views
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etTitle = findViewById(R.id.etTitle);
        etPrice = findViewById(R.id.etPrice);
        etWeight = findViewById(R.id.etWeight);
        etPhone = findViewById(R.id.etPhone);
        etLocation = findViewById(R.id.etLocation);
        btnListNow = findViewById(R.id.btnListNow);
        tvFeed = findViewById(R.id.tvFeed);

        // Categories
        String[] categories = {"ആട് (Goat)", "പോത്ത് (Buffalo)", "കോഴി (Hen)", "മത്സ്യം (Fish)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        // Auth Status Check
        updateAuthUI(mAuth.getCurrentUser());

        // Buttons
        btnRegister.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            updateAuthUI(null);
            Toast.makeText(MainActivity.this, "ലോഗ് ഔട്ട് ചെയ്തു", Toast.LENGTH_SHORT).show();
        });

        btnListNow.setOnClickListener(v -> saveListing());

        // Listen for listings
        listenForListings();
    }

    private void updateAuthUI(FirebaseUser user) {
        if (user != null) {
            tvUserStatus.setText("ലോഗിൻ ചെയ്തിരിക്കുന്നു: " + user.getEmail());
            etAuthEmail.setVisibility(View.GONE);
            etAuthPassword.setVisibility(View.GONE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            tvUserStatus.setText("Login / Register");
            etAuthEmail.setVisibility(View.VISIBLE);
            etAuthPassword.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }
    }

    private void registerUser() {
        String email = etAuthEmail.getText().toString().trim();
        String pass = etAuthPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "ഇമെയിലും പാസ്‌വേഡും നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "പാസ്‌വേഡ് കുറഞ്ഞത് 6 അക്ഷരം വേണം", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(MainActivity.this, "രജിസ്ട്രേഷൻ വിജയകരം!", Toast.LENGTH_SHORT).show();
                    updateAuthUI(authResult.getUser());
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "രജിസ്ട്രേഷൻ പരാജയം: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loginUser() {
        String email = etAuthEmail.getText().toString().trim();
        String pass = etAuthPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "ഇമെയിലും പാസ്‌വേഡും നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(MainActivity.this, "ലോഗിൻ വിജയകരം!", Toast.LENGTH_SHORT).show();
                    updateAuthUI(authResult.getUser());
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "ലോഗിൻ പരാജയം: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void saveListing() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "ദയവായി ആദ്യം മുകളിൽ ലോഗിൻ ചെയ്യുക!", Toast.LENGTH_LONG).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || priceStr.isEmpty() || phone.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "ദയവായി പ്രധാന വിവരങ്ങൾ നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }

        btnListNow.setEnabled(false);

        Map<String, Object> item = new HashMap<>();
        item.put("userId", user.getUid());
        item.put("userEmail", user.getEmail());
        item.put("category", category);
        item.put("title", title);
        item.put("price", priceStr);
        item.put("weight", weightStr.isEmpty() ? "-" : weightStr);
        item.put("phone", phone);
        item.put("location", location);
        item.put("timestamp", System.currentTimeMillis());

        db.collection("qurban_listings")
                .add(item)
                .addOnSuccessListener(doc -> {
                    btnListNow.setEnabled(true);
                    Toast.makeText(MainActivity.this, "ലിസ്റ്റിംഗ് വിജയകരമായി ചേർത്തു!", Toast.LENGTH_SHORT).show();
                    etTitle.setText("");
                    etPrice.setText("");
                    etWeight.setText("");
                    etPhone.setText("");
                    etLocation.setText("");
                })
                .addOnFailureListener(e -> {
                    btnListNow.setEnabled(true);
                    Toast.makeText(MainActivity.this, "എറർ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForListings() {
        db.collection("qurban_listings")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        tvFeed.setText("ഡാറ്റ ലോഡ് ചെയ്യുന്നതിൽ തടസ്സം: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        tvFeed.setText("ഇതുവരെ ലിസ്റ്റിംഗുകൾ ഒന്നും ചേർത്തിട്ടില്ല.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String title = doc.getString("title");
                        String cat = doc.getString("category");
                        String price = doc.getString("price");
                        String phone = doc.getString("phone");
                        String loc = doc.getString("location");

                        sb.append("🏷 ").append(title).append(" (").append(cat).append(")\n")
                                .append("💰 വില: ₹").append(price).append("\n")
                                .append("📍 സ്ഥലം: ").append(loc).append("\n")
                                .append("📞 ഫോൺ: ").append(phone).append("\n")
                                .append("-------------------------------------------\n\n");
                    }
                    tvFeed.setText(sb.toString());
                });
    }
}
