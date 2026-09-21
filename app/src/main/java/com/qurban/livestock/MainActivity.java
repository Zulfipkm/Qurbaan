package com.qurban.livestock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private EditText etAuthEmail, etAuthPassword, etSearch;
    private Button btnLogin, btnRegister, btnLogout;
    private TextView tvUserStatus, tvSelectedCategoryTitle;

    private Spinner spinnerCategory;
    private EditText etTitle, etPrice, etWeight, etPhone, etLocation;
    private Button btnSelectImage, btnListNow;
    private ImageView ivPreview;
    private LinearLayout containerListings;

    private Uri selectedImageUri = null;
    private String currentCategoryFilter = "All";
    private final List<DocumentSnapshot> allListings = new ArrayList<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivPreview.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // UI Views
        etAuthEmail = findViewById(R.id.etAuthEmail);
        etAuthPassword = findViewById(R.id.etAuthPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogout = findViewById(R.id.btnLogout);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        etSearch = findViewById(R.id.etSearch);
        tvSelectedCategoryTitle = findViewById(R.id.tvSelectedCategoryTitle);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        etTitle = findViewById(R.id.etTitle);
        etPrice = findViewById(R.id.etPrice);
        etWeight = findViewById(R.id.etWeight);
        etPhone = findViewById(R.id.etPhone);
        etLocation = findViewById(R.id.etLocation);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnListNow = findViewById(R.id.btnListNow);
        ivPreview = findViewById(R.id.ivPreview);
        containerListings = findViewById(R.id.containerListings);

        // Spinner Setup
        String[] categories = {"ആട്", "പോത്ത്", "കോഴി", "മത്സ്യം"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        updateAuthUI(mAuth.getCurrentUser());

        // Button Listeners
        btnRegister.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            updateAuthUI(null);
        });

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnListNow.setOnClickListener(v -> uploadImageAndSaveListing());

        // Round Icons Filters
        findViewById(R.id.btnCatAll).setOnClickListener(v -> filterCategory("All"));
        findViewById(R.id.btnCatGoat).setOnClickListener(v -> filterCategory("ആട്"));
        findViewById(R.id.btnCatBuffalo).setOnClickListener(v -> filterCategory("പോത്ത്"));
        findViewById(R.id.btnCatHen).setOnClickListener(v -> filterCategory("കോഴി"));
        findViewById(R.id.btnCatFish).setOnClickListener(v -> filterCategory("മത്സ്യം"));

        // Search Text Watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderFilteredListings();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listenForListings();
    }

    private void filterCategory(String category) {
        currentCategoryFilter = category;
        tvSelectedCategoryTitle.setText("ലഭ്യമായ മൃഗങ്ങൾ (" + category + ")");
        renderFilteredListings();
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
            tvUserStatus.setText("Login / Register to Sell");
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
        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "സാധുവായ ഇമെയിലും കുറഞ്ഞത് 6 അക്ഷരമുള്ള പാസ്‌വേഡും നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> updateAuthUI(res.getUser()))
                .addOnFailureListener(e -> Toast.makeText(this, "പരാജയം: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loginUser() {
        String email = etAuthEmail.getText().toString().trim();
        String pass = etAuthPassword.getText().toString().trim();
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "ഇമെയിലും പാസ്‌വേഡും നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> updateAuthUI(res.getUser()))
                .addOnFailureListener(e -> Toast.makeText(this, "ലോഗിൻ പരാജയം: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void uploadImageAndSaveListing() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "വിൽക്കുന്നതിനായി ആദ്യം ലോഗിൻ ചെയ്യുക!", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || price.isEmpty() || phone.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "എല്ലാ പ്രധാന വിവരങ്ങളും നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }

        btnListNow.setEnabled(false);

        if (selectedImageUri != null) {
            StorageReference ref = storage.getReference().child("livestock_images/" + UUID.randomUUID().toString());
            ref.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveToFirestore(category, title, price, weight, phone, location, uri.toString(), user);
                    }))
                    .addOnFailureListener(e -> {
                        btnListNow.setEnabled(true);
                        Toast.makeText(this, "ഫോട്ടോ അപ്‌ലോഡ് പരാജയം: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            saveToFirestore(category, title, price, weight, phone, location, "", user);
        }
    }

    private void saveToFirestore(String category, String title, String price, String weight, String phone, String location, String imageUrl, FirebaseUser user) {
        Map<String, Object> item = new HashMap<>();
        item.put("userId", user.getUid());
        item.put("userEmail", user.getEmail());
        item.put("category", category);
        item.put("title", title);
        item.put("price", price);
        item.put("weight", weight);
        item.put("phone", phone);
        item.put("location", location);
        item.put("imageUrl", imageUrl);
        item.put("timestamp", System.currentTimeMillis());

        db.collection("qurban_listings")
                .add(item)
                .addOnSuccessListener(doc -> {
                    btnListNow.setEnabled(true);
                    selectedImageUri = null;
                    ivPreview.setImageURI(null);
                    etTitle.setText("");
                    etPrice.setText("");
                    etWeight.setText("");
                    etPhone.setText("");
                    etLocation.setText("");
                    Toast.makeText(this, "വിജയകരമായി ചേർത്തു!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnListNow.setEnabled(true);
                    Toast.makeText(this, "എറർ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void listenForListings() {
        db.collection("qurban_listings")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    allListings.clear();
                    allListings.addAll(snapshots.getDocuments());
                    renderFilteredListings();
                });
    }

    private void renderFilteredListings() {
        containerListings.removeAllViews();
        String query = etSearch.getText().toString().trim().toLowerCase();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : allListings) {
            String title = doc.getString("title");
            String cat = doc.getString("category");
            String price = doc.getString("price");
            String loc = doc.getString("location");
            String phone = doc.getString("phone");
            String imgUrl = doc.getString("imageUrl");

            if (title == null) continue;

            // ഫിൽട്ടർ പരിശോധന
            if (!currentCategoryFilter.equals("All") && !cat.equals(currentCategoryFilter)) {
                continue;
            }

            // സെർച്ച് പരിശോധന
            if (!query.isEmpty() && !title.toLowerCase().contains(query) && (loc != null && !loc.toLowerCase().contains(query))) {
                continue;
            }

            View cardView = inflater.inflate(R.layout.item_product_card, containerListings, false);

            TextView tvTitle = cardView.findViewById(R.id.tvCardTitle);
            TextView tvCategory = cardView.findViewById(R.id.tvCardCategory);
            TextView tvPrice = cardView.findViewById(R.id.tvCardPrice);
            TextView tvLocation = cardView.findViewById(R.id.tvCardLocation);
            TextView tvPhone = cardView.findViewById(R.id.tvCardPhone);
            ImageView ivImg = cardView.findViewById(R.id.ivProductImage);

            tvTitle.setText(title);
            tvCategory.setText("വിഭാഗം: " + cat);
            tvPrice.setText("₹" + price);
            tvLocation.setText("സ്ഥലം: " + loc);
            tvPhone.setText("📞 വിളിക്കുക: " + phone);

            if (imgUrl != null && !imgUrl.isEmpty()) {
                Glide.with(this).load(imgUrl).into(ivImg);
            }

            containerListings.addView(cardView);
        }
    }
}
