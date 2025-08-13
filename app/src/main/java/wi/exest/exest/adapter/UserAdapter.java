package wi.exest.exest.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;

import wi.exest.exest.R;
import wi.exest.exest.models.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateData(List<User> newUsers) {
        userList = new ArrayList<>(newUsers);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameTextView;
        private final ImageView avatarImageView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.user_name);
            avatarImageView = itemView.findViewById(R.id.user_avatar);
        }

        public void bind(User user, OnUserClickListener listener) {
            // Отображаем ОРИГИНАЛЬНЫЙ username (не нижний регистр)
            usernameTextView.setText(user.username);

            // Загрузка аватара (если есть)
            if (user.avatarBase64 != null && !user.avatarBase64.isEmpty()) {
                byte[] decodedBytes = Base64.decode(user.avatarBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                avatarImageView.setImageBitmap(bitmap);
            } else {
                avatarImageView.setImageResource(R.drawable.ic_default_avatar);
            }

            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}