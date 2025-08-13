package wi.exest.exest.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class Post {
    public String postId;
    public String userId;
    public String username;
    public String type; // idea, project, post
    public String title; // для идеи/проекта
    public String description; // описание
    public String advantages; // преимущества (для идеи)
    public String prepared; // что подготовлено (для идеи)
    public String projectStatus; // статус проекта (для проекта)
    public String invite; // приглашение в команду (для проекта)
    public String documentUrl; // ссылка на документ (для проекта)
    public List<String> imageUrls; // ссылки на изображения
    public List<String> imageBase64List; // base64 изображения
    public String documentBase64; // base64 документ (pdf)
    public long timestamp;
    public List<String> tags;
    public String projectType; // тип проекта (коммерческий, социальный и т.д.)
    public java.util.Map<String, java.util.Map<String, Boolean>> reactions; // реакции: тип -> userId -> true
    public int commentsCount; // количество комментариев

    public static class Comment {
        public String commentId;
        public String userId;
        public String username;
        public String text;
        public long timestamp;
        public Comment() {}
    }
    public java.util.Map<String, Comment> comments; // комментарии

    public Post() {}
} 