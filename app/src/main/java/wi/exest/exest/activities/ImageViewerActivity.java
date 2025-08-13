package wi.exest.exest.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.MaterialToolbar;
import wi.exest.exest.R;
import wi.exest.exest.databinding.ActivityImageViewerBinding;
import wi.exest.exest.utils.ErrorReporter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ImageViewerActivity extends AppCompatActivity {
    
    private ActivityImageViewerBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        loadImage();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
    
    private void loadImage() {
        try {
            String imageBase64 = getIntent().getStringExtra("image_base64");
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                // Декодируем Base64 в Bitmap
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                
                if (bitmap != null) {
                    binding.fullscreenImage.setImageBitmap(bitmap);
                } else {
                    showError("Не удалось загрузить изображение");
                }
            } else {
                showError("Изображение не найдено");
            }
        } catch (Exception e) {
            ErrorReporter.logError(this, "Ошибка загрузки изображения", "ImageViewerActivity", e);
            showError("Ошибка загрузки изображения");
        }
    }
    
    private void showError(String message) {
        binding.errorText.setVisibility(View.VISIBLE);
        binding.errorText.setText(message);
        binding.fullscreenImage.setVisibility(View.GONE);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
} 