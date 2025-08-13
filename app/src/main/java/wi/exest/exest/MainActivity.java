package wi.exest.exest;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.auth.FirebaseAuth;
import wi.exest.exest.activities.LoginActivity;
import wi.exest.exest.databinding.ActivityMainBinding;
import wi.exest.exest.fragment.FeedFragment;
import wi.exest.exest.fragment.ProfileFragment;
import wi.exest.exest.fragment.SearchFragment;
import wi.exest.exest.fragment.ChatFragment;
import wi.exest.exest.fragment.CoursesFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        // Проверяем авторизацию
        checkUserAuth();

        // Настраиваем нижнюю навигацию
        setupBottomNavigation();

        // Загружаем начальный фрагмент
        if (savedInstanceState == null) {
            loadFragment(new FeedFragment(), false);
        }
        
        // Анимация появления BottomNavigationMenu
        animateBottomNavigationAppearance();
    }
    
    private void animateBottomNavigationAppearance() {
        // Сначала скрываем BottomNavigationMenu
        binding.bottomNavigation.setTranslationY(100f);
        binding.bottomNavigation.setAlpha(0f);
        binding.bottomNavigation.setScaleX(0.8f);
        binding.bottomNavigation.setScaleY(0.8f);
        
        // Анимируем появление с современными эффектами
        binding.bottomNavigation.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .start();
    }

    private void setupBottomNavigation() {
        // Устанавливаем начальный выбранный элемент
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_feed);
        
        // Добавляем анимацию для иконок при нажатии
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Анимация нажатия
            animateIconPress(item);
            
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_feed) {
                if (!(currentFragment instanceof FeedFragment)) {
                    fragment = new FeedFragment();
                }
            } else if (itemId == R.id.navigation_search) {
                if (!(currentFragment instanceof SearchFragment)) {
                    fragment = new SearchFragment();
                }
            } else if (itemId == R.id.navigation_courses) {
                if (!(currentFragment instanceof CoursesFragment)) {
                    fragment = new CoursesFragment();
                }
            } else if (itemId == R.id.navigation_chat) {
                if (!(currentFragment instanceof ChatFragment)) {
                    fragment = new ChatFragment();
                }
            } else if (itemId == R.id.navigation_profile) {
                if (!(currentFragment instanceof ProfileFragment)) {
                    ProfileFragment profileFragment = new ProfileFragment();
                    Bundle args = new Bundle();
                    args.putString("username", sessionManager.getUsername());
                    profileFragment.setArguments(args);
                    fragment = profileFragment;
                }
            }
            
            if (fragment != null) {
                loadFragment(fragment, true);
                return true;
            }
            return false;
        });
    }
    
    private void animateIconPress(android.view.MenuItem item) {
        // Находим view элемента меню и анимируем его
        for (int i = 0; i < binding.bottomNavigation.getChildCount(); i++) {
            android.view.View child = binding.bottomNavigation.getChildAt(i);
            if (child instanceof android.view.ViewGroup) {
                android.view.ViewGroup group = (android.view.ViewGroup) child;
                for (int j = 0; j < group.getChildCount(); j++) {
                    android.view.View menuItem = group.getChildAt(j);
                    if (menuItem instanceof android.view.ViewGroup) {
                        android.view.ViewGroup menuItemGroup = (android.view.ViewGroup) menuItem;
                        for (int k = 0; k < menuItemGroup.getChildCount(); k++) {
                            android.view.View iconView = menuItemGroup.getChildAt(k);
                            if (iconView instanceof android.widget.ImageView) {
                                // Современная анимация нажатия
                                iconView.animate()
                                        .scaleX(0.7f)
                                        .scaleY(0.7f)
                                        .setDuration(100)
                                        .withEndAction(() -> {
                                            iconView.animate()
                                                    .scaleX(1f)
                                                    .scaleY(1f)
                                                    .setDuration(150)
                                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                                                    .start();
                                        })
                                        .start();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadFragment(Fragment fragment, boolean animate) {
        if (currentFragment == fragment) return;
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (animate && currentFragment != null) {
            // Современные анимации перехода
            transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            );
        }
        
        transaction.replace(R.id.fragment_container, fragment);
        
        // Очищаем back stack для предотвращения накопления фрагментов
        if (animate) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        
        transaction.commit();
        currentFragment = fragment;
    }

    private void checkUserAuth() {
        if (!sessionManager.isLoggedIn() || mAuth.getCurrentUser() == null) {
            redirectToLogin();
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        sessionManager.logout();
        redirectToLogin();
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}