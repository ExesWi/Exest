package wi.exest.exest.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import wi.exest.exest.R;
import wi.exest.exest.adapter.UserAdapter;
import wi.exest.exest.databinding.FragmentSearchBinding;
import wi.exest.exest.models.User;


public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private DatabaseReference database;
    private UserAdapter userAdapter;
    private List<User> userList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance().getReference("users");
        userList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Настройка RecyclerView
        userAdapter = new UserAdapter(userList, user -> {
            // Переход в профиль пользователя
            Intent intent = new Intent(requireContext(), wi.exest.exest.activities.UserProfileActivity.class);
            intent.putExtra("userId", user.userId);
            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(userAdapter);
        binding.recyclerView.setVisibility(View.GONE); // Скрываем по умолчанию

        // Настройка поиска
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.emptyView.setVisibility(View.VISIBLE);
                } else {
                    searchUsers(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Кнопка очистки поиска
        binding.clearButton.setOnClickListener(v -> {
            binding.searchEditText.setText("");
        });
    }

    private void searchUsers(String query) {
        if (query.length() < 2) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
            return;
        }

        String lowerQuery = query.toLowerCase();

        Query searchQuery = database.orderByChild("username")
                .startAt(query)
                .endAt(query + "\uf8ff");

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        userList.add(user);
                    }
                }

                if (userList.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    userAdapter.updateData(userList);
                }

                Log.d("SearchDebug", "Query: " + lowerQuery + " | Results:" + userList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
                Log.e("SearchError", "Database error: " + error.getMessage());
            }
        });
    }
}