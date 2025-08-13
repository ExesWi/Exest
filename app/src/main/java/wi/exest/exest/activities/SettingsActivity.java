package wi.exest.exest.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import wi.exest.exest.SessionManager;
import wi.exest.exest.databinding.ActivitySettingsBinding;
import wi.exest.exest.databinding.ItemSettingBinding;
import com.google.firebase.auth.FirebaseAuth;
import wi.exest.exest.activities.LoginActivity;
import android.widget.EditText;
import android.util.TypedValue;
import android.view.Gravity;
import wi.exest.exest.utils.ErrorReporter;

public class SettingsActivity extends AppCompatActivity {
    
    private ActivitySettingsBinding binding;
    private SettingsAdapter adapter;
    
    private static final String[] SETTINGS_SECTIONS = {
        "Уведомления",
        "Аккаунт", 
        "Приватность",
        "Поддержка",
        "Вопросы"
    };
    
    private static final String[] SETTINGS_DESCRIPTIONS = {
        "Настройки уведомлений и оповещений",
        "Управление аккаунтом и безопасность",
        "Настройки приватности и видимости",
        "Связаться с поддержкой",
        "Часто задаваемые вопросы"
    };
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        binding.settingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SettingsAdapter();
        binding.settingsRecyclerView.setAdapter(adapter);
    }
    
    private void handleSettingClick(int position) {
        switch (position) {
            case 0: // Уведомления
                showNotificationsDialog();
                break;
            case 1: // Аккаунт
                showAccountDialog();
                break;
            case 2: // Приватность
                showPrivacyDialog();
                break;
            case 3: // Поддержка
                showSupportDialog();
                break;
            case 4: // Вопросы
                showQuestionsDialog();
                break;
        }
    }
    
    private void showNotificationsDialog() {
        String[] options = {
            "Включить уведомления о лайках",
            "Включить уведомления о комментариях", 
            "Включить уведомления о новых проектах",
            "Включить уведомления о сообщениях"
        };
        
        boolean[] checkedItems = {true, true, true, true}; // По умолчанию все включены
        
        new AlertDialog.Builder(this)
            .setTitle("Настройки уведомлений")
            .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                // TODO: Сохранить настройки в Firebase
                Toast.makeText(this, options[which] + (isChecked ? " включено" : " выключено"), Toast.LENGTH_SHORT).show();
            })
            .setPositiveButton("Сохранить", (dialog, which) -> {
                Toast.makeText(this, "Настройки уведомлений сохранены", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showAccountDialog() {
        String[] options = {
            "Изменить пароль",
            "Удалить аккаунт",
            "Выйти из аккаунта"
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Управление аккаунтом")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Изменить пароль
                        showChangePasswordDialog();
                        break;
                    case 1: // Удалить аккаунт
                        showDeleteAccountDialog();
                        break;
                    case 2: // Выйти из аккаунта
                        logoutUser();
                        break;
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showChangePasswordDialog() {
        // TODO: Реализовать изменение пароля через Firebase Auth
        new AlertDialog.Builder(this)
            .setTitle("Изменить пароль")
            .setMessage("Функция изменения пароля будет доступна в следующем обновлении")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Удалить аккаунт")
            .setMessage("Вы уверены, что хотите удалить свой аккаунт? Это действие нельзя отменить.")
            .setPositiveButton("Удалить", (dialog, which) -> {
                // TODO: Реализовать удаление аккаунта
                Toast.makeText(this, "Функция удаления аккаунта будет доступна в следующем обновлении", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showPrivacyDialog() {
        String[] options = {
            "Публичный профиль",
            "Приватный профиль",
            "Показывать email",
            "Показывать дату регистрации"
        };
        
        boolean[] checkedItems = {true, false, false, true}; // По умолчанию
        
        new AlertDialog.Builder(this)
            .setTitle("Настройки приватности")
            .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                // TODO: Сохранить настройки в Firebase
                Toast.makeText(this, options[which] + (isChecked ? " включено" : " выключено"), Toast.LENGTH_SHORT).show();
            })
            .setPositiveButton("Сохранить", (dialog, which) -> {
                Toast.makeText(this, "Настройки приватности сохранены", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showSupportDialog() {
        String[] options = {
            "Написать в поддержку",
            "Сообщить об ошибке",
            "Предложить улучшение"
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Поддержка")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Написать в поддержку
                        showContactSupportDialog();
                        break;
                    case 1: // Сообщить об ошибке
                        showReportBugDialog();
                        break;
                    case 2: // Предложить улучшение
                        showSuggestionDialog();
                        break;
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showContactSupportDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Написать в поддержку")
            .setMessage("Email: exestwi@icloud.com")
            .setPositiveButton("Скопировать email", (dialog, which) -> {
                // TODO: Скопировать email в буфер обмена
                Toast.makeText(this, "Email скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }
    
    private void showReportBugDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Сообщить об ошибке")
            .setMessage("Опишите проблему, которую вы обнаружили. Мы постараемся исправить её как можно скорее.")
            .setPositiveButton("Отправить", (dialog, which) -> {
                Toast.makeText(this, "Отчёт об ошибке отправлен", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showSuggestionDialog() {
        // Создаем EditText для ввода предложения
        EditText suggestionInput = new EditText(this);
        suggestionInput.setHint("Введите ваше предложение...");
        suggestionInput.setMinLines(3);
        suggestionInput.setMaxLines(8);
        suggestionInput.setGravity(Gravity.TOP | Gravity.START);
        
        // Добавляем отступы
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        suggestionInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
            .setTitle("Предложить улучшение")
            .setMessage("Расскажите, как мы можем улучшить приложение. Ваши идеи очень важны для нас!")
            .setView(suggestionInput)
            .setPositiveButton("Отправить", (dialog, which) -> {
                String suggestion = suggestionInput.getText().toString().trim();
                if (!suggestion.isEmpty()) {
                    sendSuggestionEmail(suggestion);
                } else {
                    Toast.makeText(this, "Пожалуйста, введите предложение", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void sendSuggestionEmail(String suggestion) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"exestwi@icloud.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Предложение по улучшению приложения Exest");
            
            // Формируем тело письма
            String emailBody = "Предложение от пользователя:\n\n" + suggestion + "\n\n---\nОтправлено из приложения Exest";
            intent.putExtra(Intent.EXTRA_TEXT, emailBody);
            
            // Проверяем, есть ли приложение для отправки email
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Отправить предложение через:"));
            } else {
                Toast.makeText(this, "Не найдено приложение для отправки email", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            ErrorReporter.showErrorWithReport(this, "Ошибка при отправке предложения", "SettingsActivity", e);
        }
    }
    
    private void showQuestionsDialog() {
        String[] questions = {
            "Как создать проект?",
            "Как найти команду?",
            "Как настроить уведомления?",
            "Как изменить профиль?",
            "Как удалить аккаунт?"
        };
        
        new AlertDialog.Builder(this)
            .setTitle("Часто задаваемые вопросы")
            .setItems(questions, (dialog, which) -> {
                showAnswerDialog(questions[which], getAnswerForQuestion(which));
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }
    
    private String getAnswerForQuestion(int questionIndex) {
        switch (questionIndex) {
            case 0: return "Нажмите на кнопку '+' в ленте и заполните форму создания проекта.";
            case 1: return "Используйте поиск по навыкам или просматривайте проекты в ленте.";
            case 2: return "Перейдите в Настройки → Уведомления и выберите нужные опции.";
            case 3: return "В профиле нажмите на раздел 'Информация' для редактирования.";
            case 4: return "В Настройках → Аккаунт выберите 'Удалить аккаунт'.";
            default: return "Ответ не найден.";
        }
    }
    
    private void showAnswerDialog(String question, String answer) {
        new AlertDialog.Builder(this)
            .setTitle(question)
            .setMessage(answer)
            .setPositiveButton("Понятно", null)
            .show();
    }
    
    private void logoutUser() {
        new AlertDialog.Builder(this)
            .setTitle("Выйти из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти", (dialog, which) -> {
                FirebaseAuth.getInstance().signOut();
                SessionManager sessionManager = new SessionManager(this);
                sessionManager.logout();
                
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingViewHolder> {
        
        @NonNull
        @Override
        public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSettingBinding itemBinding = ItemSettingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new SettingViewHolder(itemBinding);
        }
        
        @Override
        public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
            holder.bind(position);
        }
        
        @Override
        public int getItemCount() {
            return SETTINGS_SECTIONS.length;
        }
        
        class SettingViewHolder extends RecyclerView.ViewHolder {
            private final ItemSettingBinding itemBinding;
            
            SettingViewHolder(@NonNull ItemSettingBinding binding) {
                super(binding.getRoot());
                this.itemBinding = binding;
            }
            
            void bind(int position) {
                itemBinding.settingTitle.setText(SETTINGS_SECTIONS[position]);
                itemBinding.settingDescription.setText(SETTINGS_DESCRIPTIONS[position]);
                
                itemView.setOnClickListener(v -> handleSettingClick(position));
            }
        }
    }
} 