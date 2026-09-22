package com.qurban.livestock;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ScrollView;
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

    private ScrollView viewBuy, viewSell;
    private LinearLayout tabBuy, tabSell;
    private TextView tvTabBuyText, tvTabSellText, tvAuthStatusTop, tvCategoryLabel;
    private EditText etSearch;
    private LinearLayout layoutGridProducts;

    private Spinner sellSpinnerCategory;
    private EditText sellEtTitle, sellEtPrice, sellEtLocation, sellEtPhone;
    private Button sellBtnChooseImg, sellBtnSubmit;
    private ImageView sellIvPreview;

    private Uri selectedImageUri = null;
    private String currentCategoryFilter = "All";
    private final List<DocumentSnapshot> allListings = new ArrayList<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    sellIvPreview.setImageURI(selectedImageUri);
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

        viewBuy = findViewById(R.id.viewBuy);
        viewSell = findViewById(R.id.viewSell);
        tabBuy = findViewById(R.id.tabBuy);
        tabSell = findViewById(R.id.tabSell);
        tvTabBuyText = findViewById(R.id.tvTabBuyText);
        tvTabSellText = findViewById(R.id.tvTabSellText);
        tvAuthStatusTop = findViewById(R.id.tvAuthStatusTop);
        tvCategoryLabel = findViewById(R.id.tvCategoryLabel);
        etSearch = findViewById(R.id.etSearch);
        layoutGridProducts = findViewById(R.id.layoutGridProducts);

        sellSpinnerCategory = findViewById(R.id.sellSpinnerCategory);
        sellEtTitle = findViewById(R.id.sellEtTitle);
        sellEtPrice = findViewById(R.id.sellEtPrice);
        sellEtLocation = findViewById(R.id.sellEtLocation);
        sellEtPhone = findViewById(R.id.sellEtPhone);
        sellBtnChooseImg = findViewById(R.id.sellBtnChooseImg);
        sellBtnSubmit = findViewById(R.id.sellBtnSubmit);
        sellIvPreview = findViewById(R.id.sellIvPreview);

        String[] categories = {"Goat", "Buffalo", "Hen", "Fish"};
        sellSpinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));

        updateAuthStatus();

        tabBuy.setOnClickListener(v -> {
            viewBuy.setVisibility(View.VISIBLE);
            viewSell.setVisibility(View.GONE);
            tvTabBuyText.setTextColor(0xFF1B5E20);
            tvTabSellText.setTextColor(0xFF757575);
        });

        tabSell.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "വിൽക്കുന്നതിനായി ആദ്യം ലോഗിൻ ചെയ്യുക!", Toast.LENGTH_SHORT).show();
                showAuthDialog();
                return;
            }
            viewBuy.setVisibility(View.GONE);
            viewSell.setVisibility(View.VISIBLE);
            tvTabSellText.setTextColor(0xFF1B5E20);
            tvTabBuyText.setTextColor(0xFF757575);
        });

        tvAuthStatusTop.setOnClickListener(v -> showAuthDialog());

        findViewById(R.id.btnCatAll).setOnClickListener(v -> setCategoryFilter("All"));
        findViewById(R.id.btnCatGoat).setOnClickListener(v -> setCategoryFilter("Goat"));
        findViewById(R.id.btnCatBuffalo).setOnClickListener(v -> setCategoryFilter("Buffalo"));
        findViewById(R.id.btnCatHen).setOnClickListener(v -> setCategoryFilter("Hen"));
        findViewById(R.id.btnCatFish).setOnClickListener(v -> setCategoryFilter("Fish"));

        sellBtnChooseImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        sellBtnSubmit.setOnClickListener(v -> postNewListing());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderGrid(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        listenForListings();
    }

    private void updateAuthStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvAuthStatusTop.setText("Logout");
        } else {
            tvAuthStatusTop.setText("Login");
        }
    }

    private void setCategoryFilter(String category) {
        currentCategoryFilter = category;
        tvCategoryLabel.setText(category.equals("All") ? "All Livestock" : category + " Listings");
        renderGrid();
    }

    private void showAuthDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mAuth.signOut();
            updateAuthStatus();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email ID");
        final EditText etPass = new EditText(this);
        etPass.setHint("Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etEmail);
        layout.addView(etPass);

        builder.setTitle("Login / Register").setView(layout)
                .setPositiveButton("Login", (dialog, which) -> {
                    String em = etEmail.getText().toString().trim();
                    String pw = etPass.getText().toString().trim();
                    if (!em.isEmpty() && !pw.isEmpty()) {
                        mAuth.signInWithEmailAndPassword(em, pw)
                                .addOnSuccessListener(r -> updateAuthStatus())
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Register", (dialog, which) -> {
                    String em = etEmail.getText().toString().trim();
                    String pw = etPass.getText().toString().trim();
                    if (!em.isEmpty() && pw.length() >= 6) {
                        mAuth.createUserWithEmailAndPassword(em, pw)
                                .addOnSuccessListener(r -> updateAuthStatus())
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }).show();
    }

    private void postNewListing() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "ദയവായി ലോഗിൻ ചെയ്യുക!", Toast.LENGTH_SHORT).show();
            showAuthDialog();
            return;
        }

        String title = sellEtTitle.getText().toString().trim();
        String price = sellEtPrice.getText().toString().trim();
        String loc = sellEtLocation.getText().toString().trim();
        String phone = sellEtPhone.getText().toString().trim();
        String cat = sellSpinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || price.isEmpty() || loc.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "വിവരങ്ങൾ പൂർണ്ണമായി നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }

        sellBtnSubmit.setEnabled(false);
        sellBtnSubmit.setText("അപ്‌ലോഡ് ചെയ്യുന്നു...");

        if (selectedImageUri != null) {
            StorageReference ref = storage.getReference().child("livestock/" + UUID.randomUUID().toString());
            ref.putFile(selectedImageUri)
                    .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveData(cat, title, price, loc, phone, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        sellBtnSubmit.setEnabled(true);
                        sellBtnSubmit.setText("ലിസ്റ്റ് ചെയ്യുക (POST NOW)");
                        Toast.makeText(this, "ഫോട്ടോ അപ്‌ലോഡ് പരാജയം: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveData(cat, title, price, loc, phone, "");
        }
    }

    private void saveData(String cat, String title, String price, String loc, String phone, String imgUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("category", cat);
        map.put("title", title);
        map.put("price", price);
        map.put("location", loc);
        map.put("phone", phone);
        map.put("imageUrl", imgUrl);
        map.put("timestamp", System.currentTimeMillis());

        db.collection("qurban_listings").add(map).addOnSuccessListener(doc -> {
            sellBtnSubmit.setEnabled(true);
            sellBtnSubmit.setText("ലിസ്റ്റ് ചെയ്യുക (POST NOW)");
            sellEtTitle.setText("");
            sellEtPrice.setText("");
            sellEtLocation.setText("");
            sellEtPhone.setText("");
            sellIvPreview.setImageURI(null);
            selectedImageUri = null;

            Toast.makeText(this, "ലിസ്റ്റിംഗ് വിജയകരമായി ചേർത്തു!", Toast.LENGTH_SHORT).show();
            tabBuy.performClick();
        }).addOnFailureListener(e -> {
            sellBtnSubmit.setEnabled(true);
            sellBtnSubmit.setText("ലിസ്റ്റ് ചെയ്യുക (POST NOW)");
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void listenForListings() {
        db.collection("qurban_listings").addSnapshotListener((snaps, err) -> {
            if (err != null || snaps == null) return;
            allListings.clear();
            allListings.addAll(snaps.getDocuments());
            renderGrid();
        });
    }

    private void renderGrid() {
        layoutGridProducts.removeAllViews();
        String query = etSearch.getText().toString().trim().toLowerCase();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allListings) {
            String title = doc.getString("title");
            String cat = doc.getString("category");
            String loc = doc.getString("location");

            if (title == null) continue;

            if (!currentCategoryFilter.equals("All") && !currentCategoryFilter.equalsIgnoreCase(cat)) {
                continue;
            }

            if (!query.isEmpty() && !title.toLowerCase().contains(query) && (loc != null && !loc.toLowerCase().contains(query))) {
                continue;
            }
            filtered.add(doc);
        }

        LinearLayout currentRow = null;
        for (int i = 0; i < filtered.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                layoutGridProducts.addView(currentRow);
            }

            DocumentSnapshot doc = filtered.get(i);
            View card = inflater.inflate(R.layout.item_product_grid, currentRow, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            card.setLayoutParams(lp);

            TextView tvTitle = card.findViewById(R.id.tvCardTitle);
            TextView tvPrice = card.findViewById(R.id.tvCardPrice);
            TextView tvLoc = card.findViewById(R.id.tvCardLocation);
            ImageView iv = card.findViewById(R.id.ivProductImage);
            Button btnCall = card.findViewById(R.id.btnCall);

            tvTitle.setText(doc.getString("title"));
            tvPrice.setText("₹" + doc.getString("price"));
            tvLoc.setText("📍 " + doc.getString("location"));

            String img = doc.getString("imageUrl");
            if (img != null && !img.isEmpty()) {
                Glide.with(this).load(img).into(iv);
            }

            final String phone = doc.getString("phone");
            btnCall.setOnClickListener(v -> {
                if (phone != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(intent);
                }
            });

            if (currentRow != null) {
                currentRow.addView(card);
            }
        }
    }
}
