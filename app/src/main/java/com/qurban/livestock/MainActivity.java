package com.qurban.livestock;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ScrollView viewBuy, viewSell;
    private LinearLayout tabBuy, tabSell;
    private TextView tvTabBuyText, tvTabSellText, tvAuthStatusTop, tvCategoryLabel;
    private EditText etSearch;
    private LinearLayout layoutGridProducts;

    private Spinner sellSpinnerCategory;
    private EditText sellEtTitle, sellEtPrice, sellEtLocation, sellEtPhone;
    private Button sellBtnChooseImg, sellBtnSubmit;
    private ImageView sellIvPreview;

    private String base64ImageString = "";
    private String currentCategoryFilter = "All";
    private final List<DocumentSnapshot> allListings = new ArrayList<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        Bitmap selectedBitmap = BitmapFactory.decodeStream(imageStream);

                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(selectedBitmap, 500, (int) (selectedBitmap.getHeight() * (500.0 / selectedBitmap.getWidth())), true);
                        sellIvPreview.setImageBitmap(scaledBitmap);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        byte[] imageBytes = baos.toByteArray();
                        base64ImageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                    } catch (Exception e) {
                        Toast.makeText(this, "Image processing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
            tvTabBuyText.setTextColor(Color.parseColor("#2874F0"));
            tvTabSellText.setTextColor(Color.parseColor("#717478"));
        });

        tabSell.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please Login to Sell!", Toast.LENGTH_SHORT).show();
                showAuthDialog();
                return;
            }
            viewBuy.setVisibility(View.GONE);
            viewSell.setVisibility(View.VISIBLE);
            tvTabSellText.setTextColor(Color.parseColor("#2874F0"));
            tvTabBuyText.setTextColor(Color.parseColor("#717478"));
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
        tvCategoryLabel.setText(category.equals("All") ? "Suggested For You" : category + " Deals");
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

        builder.setTitle("Flipkart-Qurban Login").setView(layout)
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
            Toast.makeText(this, "Please Login First!", Toast.LENGTH_SHORT).show();
            showAuthDialog();
            return;
        }

        String title = sellEtTitle.getText().toString().trim();
        String price = sellEtPrice.getText().toString().trim();
        String loc = sellEtLocation.getText().toString().trim();
        String phone = sellEtPhone.getText().toString().trim();
        String cat = sellSpinnerCategory.getSelectedItem().toString();

        if (title.isEmpty() || price.isEmpty() || loc.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        sellBtnSubmit.setEnabled(false);
        sellBtnSubmit.setText("Uploading...");

        Map<String, Object> map = new HashMap<>();
        map.put("category", cat);
        map.put("title", title);
        map.put("price", price);
        map.put("location", loc);
        map.put("phone", phone);
        map.put("imageBase64", base64ImageString);
        map.put("timestamp", System.currentTimeMillis());

        db.collection("qurban_listings").add(map).addOnSuccessListener(doc -> {
            sellBtnSubmit.setEnabled(true);
            sellBtnSubmit.setText("POST NOW");
            sellEtTitle.setText("");
            sellEtPrice.setText("");
            sellEtLocation.setText("");
            sellEtPhone.setText("");
            sellIvPreview.setImageURI(null);
            base64ImageString = "";

            Toast.makeText(this, "Ad Published Successfully!", Toast.LENGTH_SHORT).show();
            tabBuy.performClick();
        }).addOnFailureListener(e -> {
            sellBtnSubmit.setEnabled(true);
            sellBtnSubmit.setText("POST NOW");
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

        // Exact Flipkart 2-Column Grid
        LinearLayout currentRow = null;
        for (int i = 0; i < filtered.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 8);
                currentRow.setLayoutParams(rowParams);
                layoutGridProducts.addView(currentRow);
            }

            DocumentSnapshot doc = filtered.get(i);
            View card = createFlipkartCard(doc);
            if (currentRow != null) {
                currentRow.addView(card);
            }
        }
    }

    private View createFlipkartCard(DocumentSnapshot doc) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        cardParams.setMargins(4, 4, 4, 4);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(4f);
        cardView.setCardElevation(2f);
        cardView.setCardBackgroundColor(Color.WHITE);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Product Image
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 260);
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(Color.parseColor("#F9F9F9"));

        String base64 = doc.getString("imageBase64");
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            } catch (Exception ignored) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        contentLayout.addView(imageView);

        // Details Container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(10, 8, 10, 10);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(doc.getString("title"));
        tvTitle.setTextColor(Color.parseColor("#212121"));
        tvTitle.setTextSize(13f);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        textContainer.addView(tvTitle);

        // Rating Badge (Flipkart Style Green 4.3 ★)
        LinearLayout ratingRow = new LinearLayout(this);
        ratingRow.setOrientation(LinearLayout.HORIZONTAL);
        ratingRow.setGravity(Gravity.CENTER_VERTICAL);
        ratingRow.setPadding(0, 3, 0, 3);

        TextView tvRating = new TextView(this);
        tvRating.setText(" 4.3 ★ ");
        tvRating.setTextColor(Color.WHITE);
        tvRating.setTextSize(10f);
        tvRating.setTypeface(null, Typeface.BOLD);
        tvRating.setBackgroundColor(Color.parseColor("#388E3C")); // Flipkart Rating Green
        ratingRow.addView(tvRating);

        TextView tvAssured = new TextView(this);
        tvAssured.setText("  ✔ Assured");
        tvAssured.setTextColor(Color.parseColor("#2874F0"));
        tvAssured.setTextSize(10f);
        tvAssured.setTypeface(null, Typeface.BOLD);
        ratingRow.addView(tvAssured);

        textContainer.addView(ratingRow);

        // Flipkart Price Row (₹Price + Strikethrough + Discount)
        String rawPrice = doc.getString("price");
        int priceVal = 0;
        try { priceVal = Integer.parseInt(rawPrice.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
        int originalPrice = priceVal > 0 ? (int)(priceVal * 1.25) : 0;

        LinearLayout priceRow = new LinearLayout(this);
        priceRow.setOrientation(LinearLayout.HORIZONTAL);
        priceRow.setGravity(Gravity.CENTER_VERTICAL);
        priceRow.setPadding(0, 2, 0, 2);

        TextView tvPrice = new TextView(this);
        tvPrice.setText("₹" + rawPrice);
        tvPrice.setTextColor(Color.parseColor("#212121"));
        tvPrice.setTextSize(14f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        priceRow.addView(tvPrice);

        if (originalPrice > 0) {
            TextView tvOldPrice = new TextView(this);
            tvOldPrice.setText(" ₹" + originalPrice);
            tvOldPrice.setTextColor(Color.parseColor("#878787"));
            tvOldPrice.setTextSize(11f);
            tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            priceRow.addView(tvOldPrice);

            TextView tvDiscount = new TextView(this);
            tvDiscount.setText(" 20% off");
            tvDiscount.setTextColor(Color.parseColor("#388E3C"));
            tvDiscount.setTextSize(11f);
            tvDiscount.setTypeface(null, Typeface.BOLD);
            priceRow.addView(tvDiscount);
        }
        textContainer.addView(priceRow);

        // Free Delivery tag
        TextView tvFreeDelivery = new TextView(this);
        tvFreeDelivery.setText("Free Delivery • " + doc.getString("location"));
        tvFreeDelivery.setTextColor(Color.parseColor("#388E3C"));
        tvFreeDelivery.setTextSize(10f);
        tvFreeDelivery.setPadding(0, 1, 0, 6);
        textContainer.addView(tvFreeDelivery);

        final String phone = doc.getString("phone");
        final String title = doc.getString("title");

        // Action Buttons Row (WhatsApp + Buy Now)
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnChat = new Button(this);
        btnChat.setText("WhatsApp");
        btnChat.setTextColor(Color.WHITE);
        btnChat.setTextSize(10f);
        btnChat.setBackgroundColor(Color.parseColor("#25D366"));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, 75, 1.0f);
        p1.setMargins(0, 0, 2, 0);
        btnChat.setLayoutParams(p1);
        btnChat.setOnClickListener(v -> {
            if (phone != null && !phone.isEmpty()) {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (cleanPhone.length() == 10) cleanPhone = "91" + cleanPhone;
                String msg = "Hi, I am interested in your listing: " + title + " (Qurban App)";
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(msg)));
                startActivity(i);
            }
        });
        btnRow.addView(btnChat);

        Button btnBuy = new Button(this);
        btnBuy.setText("Buy Now");btnBuy.setTextColor(Color.WHITE);
        btnBuy.setTextSize(10f);
        btnBuy.setBackgroundColor(Color.parseColor("#FB641B")); // Flipkart Buy Now Orange
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, 75, 1.0f);
        p2.setMargins(2, 0, 0, 0);
        btnBuy.setLayoutParams(p2);
        btnBuy.setOnClickListener(v -> {
            if (phone != null && !phone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(intent);
            }
        });
        btnRow.addView(btnBuy);

        textContainer.addView(btnRow);

        contentLayout.addView(textContainer);
        cardView.addView(contentLayout);
        return cardView;
    }
}
        
  
