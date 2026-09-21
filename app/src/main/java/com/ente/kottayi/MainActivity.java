package com.ente.kottayi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private View splashOverlay, layoutLanguageModal, viewHomeFeed, viewSellForm;
    private LinearLayout containerProducts;
    private TextView tvSplashGoat, tvSplashBrand, tvSellHeading;
    private Button btnChangeLang, btnSubmitListing, navHome, navSell;
    private Button btnLangMalayalam, btnLangEnglish, btnLangHindi, btnLangTamil;

    private LinearLayout btnCatAll, btnCatGoat, btnCatBuffalo, btnCatHen, btnCatFish;
    private EditText etSearchQuery;
    private Spinner spinnerSellCategory;
    private EditText etTitle, etBreed, etWeight, etAge, etPrice, etQuality, etSellerName, etPhone, etLocation, etUpi;

    private String currentLang = "ml";
    private String selectedCategory = "All";

    public static class AnimalListing {
        String id, userId, category, title, breed, age, quality, sellerName, phone, location, upiId;
        long weight, price;

        public AnimalListing() {}

        public AnimalListing(String id, String userId, String category, String title, String breed, long weight, String age, long price, String quality, String sellerName, String phone, String location, String upiId) {
            this.id = id;
            this.userId = userId;
            this.category = category;
            this.title = title;
            this.breed = breed;
            this.weight = weight;
            this.age = age;
            this.price = price;
            this.quality = quality;
            this.sellerName = sellerName;
            this.phone = phone;
            this.location = location;
            this.upiId = upiId;
        }
    }

    private List<AnimalListing> fullList = new ArrayList<>();

    private final String[] categoryItems = {
            "🐐 ആട് (Goat)",
            "🐃 പോത്ത് / എരുമ (Buffalo)",
            "🐔 നാടൻ കോഴി (Hen)",
            "🐟 മത്സ്യം (Live Fish)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("QurbanSettings", MODE_PRIVATE);

        initViews();
        setupLanguage();
        setupCategories();
        setupBottomNav();

        // 1. ആടിന്റെ കരച്ചിൽ ശബ്ദം പ്ലേ ചെയ്യുക
        playGoatSound();

        // 2. സ്പ്ലാഷ് ആനിമേഷൻ പ്രവർത്തിപ്പിക്കുക
        startSplashAnimation();

        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously();
        }

        listenToDatabase();
    }

    private void initViews() {
        splashOverlay = findViewById(R.id.splashOverlay);
        layoutLanguageModal = findViewById(R.id.layoutLanguageModal);
        viewHomeFeed = findViewById(R.id.viewHomeFeed);
        viewSellForm = findViewById(R.id.viewSellForm);
        containerProducts = findViewById(R.id.containerProducts);

        tvSplashGoat = findViewById(R.id.tvSplashGoat);
        tvSplashBrand = findViewById(R.id.tvSplashBrand);
        tvSellHeading = findViewById(R.id.tvSellHeading);

        btnChangeLang = findViewById(R.id.btnChangeLang);
        btnSubmitListing = findViewById(R.id.btnSubmitListing);
        navHome = findViewById(R.id.navHome);
        navSell = findViewById(R.id.navSell);

        btnLangMalayalam = findViewById(R.id.btnLangMalayalam);
        btnLangEnglish = findViewById(R.id.btnLangEnglish);
        btnLangHindi = findViewById(R.id.btnLangHindi);
        btnLangTamil = findViewById(R.id.btnLangTamil);

        btnCatAll = findViewById(R.id.btnCatAll);
        btnCatGoat = findViewById(R.id.btnCatGoat);
        btnCatBuffalo = findViewById(R.id.btnCatBuffalo);
        btnCatHen = findViewById(R.id.btnCatHen);
        btnCatFish = findViewById(R.id.btnCatFish);

        etSearchQuery = findViewById(R.id.etSearchQuery);
        spinnerSellCategory = findViewById(R.id.spinnerSellCategory);

        etTitle = findViewById(R.id.etTitle);
        etBreed = findViewById(R.id.etBreed);
        etWeight = findViewById(R.id.etWeight);
        etAge = findViewById(R.id.etAge);
        etPrice = findViewById(R.id.etPrice);
        etQuality = findViewById(R.id.etQuality);
        etSellerName = findViewById(R.id.etSellerName);
        etPhone = findViewById(R.id.etPhone);
        etLocation = findViewById(R.id.etLocation);
        etUpi = findViewById(R.id.etUpi);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryItems);
        spinnerSellCategory.setAdapter(spinnerAdapter);

        btnSubmitListing.setOnClickListener(v -> saveListing());

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderFlipkartCards(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * ആടിന്റെ ശബ്ദം (Goat Bleating "Meeehhh") കൃത്യമായ ഫ്രീക്വൻസി മോഡുലേഷനിലൂടെ ഉണ്ടാക്കുന്നു
     */
    private void playGoatSound() {
        new Thread(() -> {
            try {
                int sampleRate = 44100;
                int durationMs = 1200; // 1.2 സെക്കൻഡ്
                int totalSamples = (sampleRate * durationMs) / 1000;
                short[] samples = new short[totalSamples];

                for (int i = 0; i < totalSamples; i++) {
                    double t = (double) i / sampleRate;
                    double progress = (double) i / totalSamples;

                    // ആടിന്റെ വിറയ്ക്കുന്ന ശബ്ദം (Tremolo & Vibrato - ~22Hz)
                    double vibrato = Math.sin(2.0 * Math.PI * 22.0 * t) * 20.0;
                    double baseFreq = 260.0 + vibrato; // 260Hz പിച്ചിന്റെ അടിത്തറ

                    // വേവ് ഫോം നിർമ്മാണം
                    double wave = Math.sin(2.0 * Math.PI * baseFreq * t)
                            + 0.5 * Math.sin(2.0 * Math.PI * (baseFreq * 2) * t)
                            + 0.3 * Math.sin(2.0 * Math.PI * (baseFreq * 3) * t);

                    // ശബ്ദം ഉയർന്നു താഴ്ന്നു പോകുന്ന ഘടന (Envelope)
                    double envelope;
                    if (progress < 0.15) {
                        envelope = progress / 0.15; // തുടക്കം
                    } else {
                        envelope = Math.pow(1.0 - progress, 1.2); // പതുക്കെ കുറയുന്നു
                    }

                    samples[i] = (short) (wave * envelope * 22000);
                }

                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(samples.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                track.write(samples, 0, samples.length);
                track.play();
            } catch (Exception ignored) {}
        }).start();
    }

    private void startSplashAnimation() {
        try {
            Animation bounce = AnimationUtils.loadAnimation(this, R.anim.splash_bounce);
            tvSplashGoat.startAnimation(bounce);
            tvSplashBrand.startAnimation(bounce);
        } catch (Exception ignored) {}

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (splashOverlay != null) {
                splashOverlay.animate().alpha(0.0f).setDuration(400).withEndAction(() -> {
                    splashOverlay.setVisibility(View.GONE);
                    if (!prefs.getBoolean("lang_chosen", false)) {
                        layoutLanguageModal.setVisibility(View.VISIBLE);
                    }
                }).start();
            }
        }, 2000);
    }

    private void setupLanguage() {
        currentLang = prefs.getString("selected_lang", "ml");

        btnChangeLang.setOnClickListener(v -> layoutLanguageModal.setVisibility(View.VISIBLE));

        btnLangMalayalam.setOnClickListener(v -> changeLang("ml"));
        btnLangEnglish.setOnClickListener(v -> changeLang("en"));
        btnLangHindi.setOnClickListener(v -> changeLang("hi"));
        btnLangTamil.setOnClickListener(v -> changeLang("ta"));

        updateLanguageUI();
    }

    private void changeLang(String langCode) {
        currentLang = langCode;
        prefs.edit().putString("selected_lang", langCode).putBoolean("lang_chosen", true).apply();
        layoutLanguageModal.setVisibility(View.GONE);
        updateLanguageUI();
        renderFlipkartCards();
    }

    private void updateLanguageUI() {
        switch (currentLang) {
            case "en":
                navHome.setText("🏠 Home");
                navSell.setText("➕ Sell");
                tvSellHeading.setText("Sell Livestock (Listing Details)");
                btnSubmitListing.setText("List Now 🚀");
                etSearchQuery.setHint("Search Goat, Buffalo, Hen...");
                break;
            case "hi":
                navHome.setText("🏠 होम");
                navSell.setText("➕ बेचें");
                tvSellHeading.setText("जानवर बेचें (विवरण दर्ज करें)");
                btnSubmitListing.setText("लिस्ट करें 🚀");
                etSearchQuery.setHint("बकरा, भैंस, मुर्गा खोजें...");
                break;
            case "ta":
                navHome.setText("🏠 முகப்பு");
                navSell.setText("➕ விற்க");
                tvSellHeading.setText("விலங்குகளை விற்க (விவரங்கள்)");
                btnSubmitListing.setText("பதிவிட 🚀");
                etSearchQuery.setHint("ஆடு, எருமை, கோழி தேடுக...");
                break;
            case "ml":
            default:
                navHome.setText("🏠 ഹോം");
                navSell.setText("➕ വിൽക്കാൻ");
                tvSellHeading.setText("വിൽപ്പന വിവരങ്ങൾ നൽകുക");
                btnSubmitListing.setText("പോസ്റ്റ് ചെയ്യുക 🚀");
                etSearchQuery.setHint("ആട്, പോത്ത്, കോഴി തിരയുക...");
                break;
        }
    }

    private void setupCategories() {
        btnCatAll.setOnClickListener(v -> setCategoryFilter("All"));
        btnCatGoat.setOnClickListener(v -> setCategoryFilter("Goat"));
        btnCatBuffalo.setOnClickListener(v -> setCategoryFilter("Buffalo"));
        btnCatHen.setOnClickListener(v -> setCategoryFilter("Hen"));
        btnCatFish.setOnClickListener(v -> setCategoryFilter("Fish"));
    }

    private void setCategoryFilter(String cat) {
        selectedCategory = cat;
        renderFlipkartCards();
    }

    private void setupBottomNav() {
        navHome.setOnClickListener(v -> {
            viewHomeFeed.setVisibility(View.VISIBLE);
            viewSellForm.setVisibility(View.GONE);
            navHome.setTextColor(Color.parseColor("#2874F0"));
            navSell.setTextColor(Color.parseColor("#878787"));
            renderFlipkartCards();
        });

        navSell.setOnClickListener(v -> {
            viewHomeFeed.setVisibility(View.GONE);
            viewSellForm.setVisibility(View.VISIBLE);
            navSell.setTextColor(Color.parseColor("#2874F0"));
            navHome.setTextColor(Color.parseColor("#878787"));
        });
    }

    private void listenToDatabase() {
        db.collection("qurban_listings").addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            fullList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                AnimalListing item = new AnimalListing(
                        doc.getId(),
                        doc.getString("userId"),
                        doc.getString("category"),
                        doc.getString("title"),
                        doc.getString("breed"),
                        doc.getLong("weight") != null ? doc.getLong("weight") : 0,
                        doc.getString("age"),
                        doc.getLong("price") != null ? doc.getLong("price") : 0,
                        doc.getString("quality"),
                        doc.getString("sellerName"),
                        doc.getString("phone"),
                        doc.getString("location"),
                        doc.getString("upiId")
                );
                fullList.add(item);
            }
            renderFlipkartCards();
        });
    }

    private void saveListing() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String title = etTitle.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String quality = etQuality.getText().toString().trim();
        String sName = etSellerName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String loc = etLocation.getText().toString().trim();
        String upi = etUpi.getText().toString().trim();
        String cat = spinnerSellCategory.getSelectedItem().toString();

        if (title.isEmpty() || priceStr.isEmpty() || phone.isEmpty() || loc.isEmpty()) {
            Toast.makeText(this, "പ്രധാന വിവരങ്ങൾ നൽകുക", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getUid());
        map.put("category", cat);
        map.put("title", title);
        map.put("breed", breed.isEmpty() ? "നാടൻ" : breed);
        map.put("weight", weightStr.isEmpty() ? 0 : Long.parseLong(weightStr));
        map.put("age", age.isEmpty() ? "-" : age);
        map.put("price", Long.parseLong(priceStr));
        map.put("quality", quality.isEmpty() ? "ഓർഗാനിക് തീറ്റ" : quality);
        map.put("sellerName", sName.isEmpty() ? "കർഷകൻ" : sName);
        map.put("phone", phone);
        map.put("location", loc);
        map.put("upiId", upi);

        db.collection("qurban_listings").add(map).addOnSuccessListener(doc -> {
            Toast.makeText(this, "ലിസ്റ്റിംഗ് വിജയകരമായി ചേർത്തു!", Toast.LENGTH_LONG).show();
            etTitle.setText("");
            etBreed.setText("");
            etWeight.setText("");
            etAge.setText("");
            etPrice.setText("");
            etQuality.setText("");
            etSellerName.setText("");
            etPhone.setText("");
            etLocation.setText("");
            etUpi.setText("");
            navHome.performClick();
        });
    }

    private void renderFlipkartCards() {
        containerProducts.removeAllViews();
        String query = etSearchQuery.getText().toString().toLowerCase().trim();

        List<AnimalListing> filtered = new ArrayList<>();
        for (AnimalListing a : fullList) {
            boolean matchesCat = selectedCategory.equals("All") || (a.category != null && a.category.toLowerCase().contains(selectedCategory.toLowerCase()));
            boolean matchesQuery = query.isEmpty() || (a.title != null && a.title.toLowerCase().contains(query)) || (a.breed != null && a.breed.toLowerCase().contains(query));
            if (matchesCat && matchesQuery) filtered.add(a);
        }

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No animals found matching criteria.");
            empty.setTextColor(Color.parseColor("#878787"));
            empty.setPadding(20, 60, 20, 20);
            containerProducts.addView(empty);
            return;
        }

        for (AnimalListing item : filtered) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(14, 14, 14, 14);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(4, 4, 4, 10);
            card.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText(item.title);
            title.setTextColor(Color.parseColor("#212121"));
            title.setTextSize(16f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView meta = new TextView(this);
            meta.setText("📍 " + item.location + " • " + item.sellerName);
            meta.setTextColor(Color.parseColor("#878787"));
            meta.setTextSize(12f);

            TextView specs = new TextView(this);
            specs.setText("🧬 " + item.breed + "   |   ⚖️ " + item.weight + " kg (Live)   |   🦷 " + item.age);
            specs.setTextColor(Color.parseColor("#388E3C"));
            specs.setTextSize(12.5f);
            specs.setTypeface(null, android.graphics.Typeface.BOLD);
            specs.setPadding(0, 6, 0, 4);

            TextView feed = new TextView(this);
            feed.setText("🌿 " + item.quality);
            feed.setTextColor(Color.parseColor("#616161"));
            feed.setTextSize(11.5f);

            LinearLayout priceRow = new LinearLayout(this);
            priceRow.setOrientation(LinearLayout.HORIZONTAL);
            priceRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            priceRow.setPadding(0, 6, 0, 10);

            TextView price = new TextView(this);
            price.setText("₹" + item.price);
            price.setTextColor(Color.parseColor("#212121"));
            price.setTextSize(18f);
            price.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView assuredBadge = new TextView(this);
            assuredBadge.setText(" ✔ Qurban Assured ");
            assuredBadge.setTextColor(Color.parseColor("#2874F0"));
            assuredBadge.setTextSize(11f);
            assuredBadge.setTypeface(null, android.graphics.Typeface.ITALIC | android.graphics.Typeface.BOLD);

            priceRow.addView(price);
            priceRow.addView(assuredBadge);

            LinearLayout actRow = new LinearLayout(this);
            actRow.setOrientation(LinearLayout.HORIZONTAL);
            actRow.setWeightSum(3);

            Button btnCall = new Button(this);
            btnCall.setText("Call 📞");
            btnCall.setBackgroundColor(Color.parseColor("#2E7D32"));
            btnCall.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p1.rightMargin = 4;
            btnCall.setLayoutParams(p1);
            btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.phone))));

            Button btnChat = new Button(this);
            btnChat.setText("Chat 💬");
            btnChat.setBackgroundColor(Color.parseColor("#0288D1"));
            btnChat.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p2.rightMargin = 4;
            btnChat.setLayoutParams(p2);
            btnChat.setOnClickListener(v -> {
                String clean = item.phone.replaceAll("[^0-9]", "");
                if (clean.length() == 10) clean = "91" + clean;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean + "&text=" + Uri.encode("Hello " + item.sellerName + ", I want to buy '" + item.title + "' on Qurban App.")));
                startActivity(intent);
            });

            Button btnBuy = new Button(this);
            btnBuy.setText("Buy ⚡");
            btnBuy.setBackgroundColor(Color.parseColor("#FB641B"));
            btnBuy.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            btnBuy.setLayoutParams(p3);
            btnBuy.setOnClickListener(v -> {
                String targetUpi = (item.upiId == null || item.upiId.isEmpty()) ? "qurban@upi" : item.upiId;
                long adv = Math.min(1000, item.price);
                Uri upiUri = Uri.parse("upi://pay").buildUpon()
                        .appendQueryParameter("pa", targetUpi)
                        .appendQueryParameter("pn", item.sellerName)
                        .appendQueryParameter("tn", "Booking: " + item.title)
                        .appendQueryParameter("am", String.valueOf(adv))
                        .appendQueryParameter("cu", "INR")
                        .build();
                try {
                    startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, upiUri), "Advance Pay"));
                } catch (Exception e) {
                    Toast.makeText(this, "UPI App not found", Toast.LENGTH_SHORT).show();
                }
            });

            actRow.addView(btnCall);
            actRow.addView(btnChat);
            actRow.addView(btnBuy);

            card.addView(title);
            card.addView(meta);
            card.addView(specs);
            card.addView(feed);
            card.addView(priceRow);
            card.addView(actRow);

            containerProducts.addView(card);
        }
    }
}
