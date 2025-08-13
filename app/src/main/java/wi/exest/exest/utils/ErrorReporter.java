package wi.exest.exest.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ErrorReporter {
    
    private static final String ERROR_EMAIL = "exestwi@icloud.com";
    private static final String ERROR_SUBJECT = "Ошибка в приложении Exest";
    
    public static void sendErrorReport(Context context, String errorMessage, String activityName) {
        sendErrorReport(context, errorMessage, activityName, null);
    }
    
    public static void sendErrorReport(Context context, String errorMessage, String activityName, Exception exception) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ERROR_EMAIL});
            intent.putExtra(Intent.EXTRA_SUBJECT, ERROR_SUBJECT);
            
            // Формируем детальный отчет об ошибке
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("=== ОТЧЕТ ОБ ОШИБКЕ ===\n\n");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            emailBody.append("📅 Дата и время: ").append(sdf.format(new Date())).append("\n\n");
            
            emailBody.append("📱 Информация об устройстве:\n");
            emailBody.append("   Производитель: ").append(android.os.Build.MANUFACTURER).append("\n");
            emailBody.append("   Модель: ").append(android.os.Build.MODEL).append("\n");
            emailBody.append("   Android версия: ").append(android.os.Build.VERSION.RELEASE).append("\n");
            emailBody.append("   API уровень: ").append(android.os.Build.VERSION.SDK_INT).append("\n\n");
            
            emailBody.append("🚨 Информация об ошибке:\n");
            emailBody.append("   Экран/Активность: ").append(activityName).append("\n");
            emailBody.append("   Сообщение: ").append(errorMessage).append("\n\n");
            
            if (exception != null) {
                emailBody.append("🔍 Детали исключения:\n");
                emailBody.append("   Тип: ").append(exception.getClass().getSimpleName()).append("\n");
                emailBody.append("   Сообщение: ").append(exception.getMessage()).append("\n\n");
                
                // Stack trace (первые 15 строк)
                emailBody.append("📋 Stack Trace:\n");
                StackTraceElement[] stackTrace = exception.getStackTrace();
                int maxLines = Math.min(stackTrace.length, 15);
                for (int i = 0; i < maxLines; i++) {
                    emailBody.append("   ").append(i + 1).append(". ").append(stackTrace[i].toString()).append("\n");
                }
                if (stackTrace.length > 15) {
                    emailBody.append("   ... и еще ").append(stackTrace.length - 15).append(" строк\n");
                }
                emailBody.append("\n");
            }
            
            emailBody.append("---\n📧 Отправлено автоматически из приложения Exest");
            
            intent.putExtra(Intent.EXTRA_TEXT, emailBody.toString());
            
            // Проверяем, есть ли приложение для отправки email
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(Intent.createChooser(intent, "Отправить отчет об ошибке через:"));
            } else {
                Toast.makeText(context, "Не найдено приложение для отправки email", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка при отправке отчета: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    public static void showErrorWithReport(Context context, String errorMessage, String activityName) {
        showErrorWithReport(context, errorMessage, activityName, null);
    }
    
    public static void showErrorWithReport(Context context, String errorMessage, String activityName, Exception exception) {
        // Показываем Toast с ошибкой
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        
        // Предлагаем отправить отчет
        new android.app.AlertDialog.Builder(context)
            .setTitle("🚨 Произошла ошибка")
            .setMessage("Хотите отправить отчет об ошибке разработчикам? Это поможет улучшить приложение.")
            .setPositiveButton("📧 Отправить отчет", (dialog, which) -> {
                sendErrorReport(context, errorMessage, activityName, exception);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    // Упрощенный метод для быстрого логирования ошибок
    public static void logError(Context context, String errorMessage, String activityName, Exception exception) {
        // Можно добавить локальное логирование или отправку в Firebase Crashlytics
        // Пока просто показываем диалог с предложением отправить отчет
        showErrorWithReport(context, errorMessage, activityName, exception);
    }
} 