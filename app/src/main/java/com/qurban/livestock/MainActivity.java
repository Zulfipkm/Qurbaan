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
    
    // Cart Items List
    private final List<Map<String, String>> cartList = new ArrayList<>();

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
                        Toast.makeText(this, "ഇമേജ് പ്രോസസ്സ് ചെയ്യുന്നതിൽ പിഴവ്: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "വിൽക്കുന്നതിനായി ആദ്യം ലോഗിൻ ചെയ്യുക!", Toast.LENGTH_SHORT).show();
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

        // "Flipkart-Qurban" മാറ്റി വെറും "Qurban Login" എന്ന് നൽകി
        builder.setTitle("Qurban Login").setView(layout)
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

        LinearLayout currentRow = null;
        for (int i = 0; i < filtered.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 10);
                currentRow.setLayoutParams(rowParams);
                layoutGridProducts.addView(currentRow);
            }

            DocumentSnapshot doc = filtered.get(i);
            View card = createProductCard(doc);
            if (currentRow != null) {
                currentRow.addView(card);
            }
        }
    }

    private View createProductCard(DocumentSnapshot doc) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        cardParams.setMargins(6, 6, 6, 6);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(6f);
        cardView.setCardElevation(3f);
        cardView.setCardBackgroundColor(Color.WHITE);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // പ്രൊഡക്റ്റ് ഇമേജ്
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

        // വിവരങ്ങൾ അടങ്ങിയ ഭാഗം
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(12, 10, 12, 12);

        // പേര്
        TextView tvTitle = new TextView(this);
        tvTitle.setText(doc.getString("title"));
        tvTitle.setTextColor(Color.parseColor("#212121"));
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        textContainer.addView(tvTitle);

        // റേറ്റിംഗ് & "✔ Qurban Verified"
        LinearLayout ratingRow = new LinearLayout(this);
        ratingRow.setOrientation(LinearLayout.HORIZONTAL);
        ratingRow.setGravity(Gravity.CENTER_VERTICAL);
        ratingRow.setPadding(0, 4, 0, 4);

        TextView tvRating = new TextView(this);
        tvRating.setText(" 4.5 ★ ");
        tvRating.setTextColor(Color.WHITE);
        tvRating.setTextSize(10f);
        tvRating.setTypeface(null, Typeface.BOLD);
        tvRating.setBackgroundColor(Color.parseColor("#388E3C"));
        ratingRow.addView(tvRating);

        TextView tvVerified = new TextView(this);
        tvVerified.setText("  ✔ Qurban Verified");
        tvVerified.setTextColor(Color.parseColor("#2874F0"));
        tvVerified.setTextSize(10f);
        tvVerified.setTypeface(null, Typeface.BOLD);
        ratingRow.addView(tvVerified);

        textContainer.addView(ratingRow);

        // വിലയും ഓഫറും
        String rawPrice = doc.getString("price");
        int priceVal = 0;
        try { priceVal = Integer.parseInt(rawPrice.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
        int originalPrice = priceVal > 0 ? (int)(priceVal * 1.20) : 0;

        LinearLayout priceRow = new LinearLayout(this);
        priceRow.setOrientation(LinearLayout.HORIZONTAL);
        priceRow.setGravity(Gravity.CENTER_VERTICAL);
        priceRow.setPadding(0, 2, 0, 2);

        TextView tvPrice = new TextView(this);
        tvPrice.setText("₹" + rawPrice);
        tvPrice.setTextColor(Color.parseColor("#212121"));
        tvPrice.setTextSize(15f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        priceRow.addView(tvPrice);

        if (originalPrice > 0) {
            TextView tvOldPrice = new TextView(this);
            tvOldPrice.setText(" ₹" + originalPrice);
            tvOldPrice.setTextColor(Color.parseColor("#878787"));
            tvOldPrice.setTextSize(11f);
            tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            priceRow.addView(tvOldPrice);
        }
        textContainer.addView(priceRow);

        TextView tvLoc = new TextView(this);
        tvLoc.setText("📍 " + doc.getString("location"));
        tvLoc.setTextColor(Color.parseColor("#757575"));
        tvLoc.setTextSize(11f);
        tvLoc.setPadding(0, 2, 0, 8);
        textContainer.addView(tvLoc);

        final String phone = doc.getString("phone");
        final String title = doc.getString("title");

        // ബട്ടൺ 1: WhatsApp Chat
        Button btnChat = new Button(this);
        btnChat.setText("💬 WhatsApp Chat");
        btnChat.setTextColor(Color.WHITE);
        btnChat.setTextSize(11f);
        btnChat.setTypeface(null, Typeface.BOLD);
        btnChat.setBackgroundColor(Color.parseColor("#25D366"));
        LinearLayout.LayoutParams chatP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 95);
        chatP.setMargins(0, 0, 0, 6);
        btnChat.setLayoutParams(chatP);
        btnChat.setOnClickListener(v -> {
            if (phone != null && !phone.isEmpty()) {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (cleanPhone.length() == 10) cleanPhone = "91" + cleanPhone;
                String msg = "Hello, I am interested to buy your livestock: " + title;
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(msg)));
                startActivity(i);
            }
        });
        textContainer.addView(btnChat);

        // ബട്ടൺ 2 & 3: Add to Cart & Buy Now
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnAddCart = new Button(this);
        btnAddCart.setText("🛒 Cart");
        btnAddCart.setTextColor(Color.parseColor("#212121"));
        btnAddCart.setTextSize(11f);
        btnAddCart.setTypeface(null, Typeface.BOLD);
        btnAddCart.setBackgroundColor(Color.parseColor("#FFE500")); // Flipkart Yellow
        LinearLayout.LayoutParams cartP = new LinearLayout.LayoutParams(0, 95, 1.0f);
        cartP.setMargins(0, 0, 3, 0);
        btnAddCart.setLayoutParams(cartP);
        btnAddCart.setOnClickListener(v -> {
            Map<String, String> item = new HashMap<>();
            item.put("title", title);
            item.put("price", rawPrice);
            cartList.add(item);
            Toast.makeText(this, title + " added to Cart! (Total: " + cartList.size() + ")", Toast.LENGTH_SHORT).show();
        });
        actionRow.addView(btnAddCart);

        Button btnBuy = new Button(this);
        btnBuy.setText("⚡ Buy Now");
        btnBuy.setTextColor(Color.WHITE);
        btnBuy.setTextSize(11f);
        btnBuy.setTypeface(null, Typeface.BOLD);
        btnBuy.setBackgroundColor(Color.parseColor("#FB641B")); // Flipkart Orange
        LinearLayout.LayoutParams buyP = new LinearLayout.LayoutParams(0, 95, 1.0f);
        buyP.setMargins(3, 0, 0, 0);
        btnBuy.setLayoutParams(buyP);
        btnBuy.setOnClickListener(v -> {
            if (phone != null && !phone.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Order Placement")
                        .setMessage("Direct booking for " + title + " at ₹" + rawPrice + ".\nCall the seller now to confirm delivery?")
                        .setPositiveButton("Call Now", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        actionRow.addView(btnBuy);

        textContainer.addView(actionRow);

        contentLayout.addView(textContainer);
        cardView.addView(contentLayout);
        return cardView;
    }
}
        
