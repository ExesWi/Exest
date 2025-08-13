package wi.exest.exest.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import wi.exest.exest.R;
import wi.exest.exest.databinding.FragmentCoursesBinding;
import wi.exest.exest.models.Course;
import wi.exest.exest.utils.ErrorReporter;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class CoursesFragment extends Fragment {
    
    private FragmentCoursesBinding binding;
    private CoursesAdapter coursesAdapter;
    private List<Course> coursesList = new ArrayList<>();
    private String currentCategory = "all"; // all, programming, design, business, marketing
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        setupTabLayout();
        loadCourses();
    }
    
    private void setupRecyclerView() {
        coursesAdapter = new CoursesAdapter(coursesList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(coursesAdapter);
    }
    
    private void setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Все"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Программирование"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Дизайн"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Бизнес"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Маркетинг"));
        
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentCategory = "all"; break;
                    case 1: currentCategory = "programming"; break;
                    case 2: currentCategory = "design"; break;
                    case 3: currentCategory = "business"; break;
                    case 4: currentCategory = "marketing"; break;
                }
                filterCourses();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void loadCourses() {
        // Создаем статические курсы
        createStaticCourses();
        
        // Отображаем курсы
        filterCourses();
    }
    
    private void createStaticCourses() {
        coursesList.clear();
        
        // Mobile Developer
        Course mobileCourse = new Course();
        mobileCourse.courseId = "mobile_developer";
        mobileCourse.title = "Mobile Developer";
        mobileCourse.description = "Станьте профессиональным мобильным разработчиком. Изучите современные технологии и создавайте приложения для iOS и Android.";
        mobileCourse.category = "programming";
        mobileCourse.instructor = "Команда Exest";
        mobileCourse.isFree = true;
        mobileCourse.price = 0.0;
        mobileCourse.lessonsCount = 50;
        mobileCourse.difficulty = "beginner";
        coursesList.add(mobileCourse);
        
        // Android Developer
        Course androidCourse = new Course();
        androidCourse.courseId = "android_developer";
        androidCourse.title = "Android Developer";
        androidCourse.description = "Специализируйтесь на разработке приложений для Android. Изучите Kotlin, Java, Android SDK и создавайте качественные приложения.";
        androidCourse.category = "programming";
        androidCourse.instructor = "Команда Exest";
        androidCourse.isFree = true;
        androidCourse.price = 0.0;
        androidCourse.lessonsCount = 45;
        androidCourse.difficulty = "intermediate";
        coursesList.add(androidCourse);
        
        // iOS Developer
        Course iosCourse = new Course();
        iosCourse.courseId = "ios_developer";
        iosCourse.title = "iOS Developer";
        iosCourse.description = "Освойте разработку приложений для iPhone и iPad. Изучите Swift, SwiftUI, iOS SDK и создавайте приложения для App Store.";
        iosCourse.category = "programming";
        iosCourse.instructor = "Команда Exest";
        iosCourse.isFree = true;
        iosCourse.price = 0.0;
        iosCourse.lessonsCount = 40;
        iosCourse.difficulty = "intermediate";
        coursesList.add(iosCourse);
        
        // Desktop Developer
        Course desktopCourse = new Course();
        desktopCourse.courseId = "desktop_developer";
        desktopCourse.title = "Desktop Developer";
        desktopCourse.description = "Создавайте настольные приложения для Windows, macOS и Linux. Изучите C#, Java, Python и современные фреймворки.";
        desktopCourse.category = "programming";
        desktopCourse.instructor = "Команда Exest";
        desktopCourse.isFree = true;
        desktopCourse.price = 0.0;
        desktopCourse.lessonsCount = 35;
        desktopCourse.difficulty = "intermediate";
        coursesList.add(desktopCourse);
        
        // Frontend Developer
        Course frontendCourse = new Course();
        frontendCourse.courseId = "frontend_developer";
        frontendCourse.title = "Frontend Developer";
        frontendCourse.description = "Станьте экспертом в веб-разработке. Изучите HTML, CSS, JavaScript, React, Vue.js и создавайте современные веб-приложения.";
        frontendCourse.category = "programming";
        frontendCourse.instructor = "Команда Exest";
        frontendCourse.isFree = true;
        frontendCourse.price = 0.0;
        frontendCourse.lessonsCount = 60;
        frontendCourse.difficulty = "beginner";
        coursesList.add(frontendCourse);
        
        // Design
        Course designCourse = new Course();
        designCourse.courseId = "design";
        designCourse.title = "Design";
        designCourse.description = "Освойте современный дизайн. Изучите UI/UX, Figma, Adobe Creative Suite и создавайте красивые интерфейсы и графику.";
        designCourse.category = "design";
        designCourse.instructor = "Команда Exest";
        designCourse.isFree = true;
        designCourse.price = 0.0;
        designCourse.lessonsCount = 30;
        designCourse.difficulty = "beginner";
        coursesList.add(designCourse);
        
        // Business Management
        Course businessCourse = new Course();
        businessCourse.courseId = "business_management";
        businessCourse.title = "Business Management";
        businessCourse.description = "Изучите основы управления бизнесом. Стратегическое планирование, финансы, маркетинг и развитие компании.";
        businessCourse.category = "business";
        businessCourse.instructor = "Команда Exest";
        businessCourse.isFree = true;
        businessCourse.price = 0.0;
        businessCourse.lessonsCount = 40;
        businessCourse.difficulty = "intermediate";
        coursesList.add(businessCourse);
        
        // Entrepreneurship
        Course entrepreneurshipCourse = new Course();
        entrepreneurshipCourse.courseId = "entrepreneurship";
        entrepreneurshipCourse.title = "Entrepreneurship";
        entrepreneurshipCourse.description = "Создайте и развивайте собственный бизнес. От идеи до успешного стартапа с нуля.";
        entrepreneurshipCourse.category = "business";
        entrepreneurshipCourse.instructor = "Команда Exest";
        entrepreneurshipCourse.isFree = true;
        entrepreneurshipCourse.price = 0.0;
        entrepreneurshipCourse.lessonsCount = 35;
        entrepreneurshipCourse.difficulty = "beginner";
        coursesList.add(entrepreneurshipCourse);
        
        // Digital Marketing
        Course digitalMarketingCourse = new Course();
        digitalMarketingCourse.courseId = "digital_marketing";
        digitalMarketingCourse.title = "Digital Marketing";
        digitalMarketingCourse.description = "Освойте цифровой маркетинг. SMM, контекстная реклама, SEO, email-маркетинг и аналитика.";
        digitalMarketingCourse.category = "marketing";
        digitalMarketingCourse.instructor = "Команда Exest";
        digitalMarketingCourse.isFree = true;
        digitalMarketingCourse.price = 0.0;
        digitalMarketingCourse.lessonsCount = 45;
        digitalMarketingCourse.difficulty = "intermediate";
        coursesList.add(digitalMarketingCourse);
        
        // Content Marketing
        Course contentMarketingCourse = new Course();
        contentMarketingCourse.courseId = "content_marketing";
        contentMarketingCourse.title = "Content Marketing";
        contentMarketingCourse.description = "Создавайте качественный контент для привлечения аудитории. Копирайтинг, блоги, видео и инфографика.";
        contentMarketingCourse.category = "marketing";
        contentMarketingCourse.instructor = "Команда Exest";
        contentMarketingCourse.isFree = true;
        contentMarketingCourse.price = 0.0;
        contentMarketingCourse.lessonsCount = 25;
        contentMarketingCourse.difficulty = "beginner";
        coursesList.add(contentMarketingCourse);
        
        // Brand Management
        Course brandManagementCourse = new Course();
        brandManagementCourse.courseId = "brand_management";
        brandManagementCourse.title = "Brand Management";
        brandManagementCourse.description = "Создавайте и управляйте сильными брендами. Позиционирование, айдентика, репутация и лояльность клиентов.";
        brandManagementCourse.category = "marketing";
        brandManagementCourse.instructor = "Команда Exest";
        brandManagementCourse.isFree = true;
        brandManagementCourse.price = 0.0;
        brandManagementCourse.lessonsCount = 30;
        brandManagementCourse.difficulty = "intermediate";
        coursesList.add(brandManagementCourse);
    }
    
    private void filterCourses() {
        List<Course> filteredList = new ArrayList<>();
        
        if ("all".equals(currentCategory)) {
            filteredList.addAll(coursesList);
        } else {
            for (Course course : coursesList) {
                if (currentCategory.equals(course.category)) {
                    filteredList.add(course);
                }
            }
        }
        
        coursesAdapter.setData(filteredList);
        updateEmptyState(filteredList.isEmpty());
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Адаптер для курсов
    private static class CoursesAdapter extends RecyclerView.Adapter<CoursesAdapter.CourseViewHolder> {
        private List<Course> courses;
        
        CoursesAdapter(List<Course> courses) {
            this.courses = courses;
        }
        
        void setData(List<Course> newCourses) {
            this.courses = newCourses;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
            return new CourseViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            holder.bind(courses.get(position));
        }
        
        @Override
        public int getItemCount() {
            return courses.size();
        }
        
        static class CourseViewHolder extends RecyclerView.ViewHolder {
            private final View itemView;
            private TextView courseTitle;
            private TextView courseDescription;
            private MaterialButton courseButton;
            
            CourseViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                
                // Инициализация views
                courseTitle = itemView.findViewById(R.id.courseTitle);
                courseDescription = itemView.findViewById(R.id.courseDescription);
                courseButton = itemView.findViewById(R.id.courseButton);
            }
            
            void bind(Course course) {
                courseTitle.setText(course.title);
                courseDescription.setText(course.description);
                
                courseButton.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://grozrepair.ru/learning/index.html"));
                        itemView.getContext().startActivity(intent);
                    } catch (Exception e) {
                        ErrorReporter.showErrorWithReport(itemView.getContext(), 
                            "Ошибка открытия ссылки", "CoursesFragment", e);
                    }
                });
            }
        }
    }
} 