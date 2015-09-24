/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.view.holder;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import catchla.yep.R;
import catchla.yep.adapter.iface.ItemClickListener;
import catchla.yep.model.Friendship;
import catchla.yep.model.User;

/**
 * Created by mariotaku on 15/4/29.
 */
public class FriendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final Fragment fragment;
    private final ItemClickListener listener;

    private final ImageView profileImageView;
    private final TextView nameView, timeView, descriptionView;

    public FriendViewHolder(final Fragment fragment, final ItemClickListener listener, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.listener = listener;
        profileImageView = (ImageView) itemView.findViewById(R.id.profile_image);
        nameView = (TextView) itemView.findViewById(R.id.name);
        timeView = (TextView) itemView.findViewById(R.id.update_time);
        descriptionView = (TextView) itemView.findViewById(R.id.description);
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (listener == null) return;
        listener.onItemClick(getAdapterPosition(), FriendViewHolder.this);
    }

    public void displaySample(int profileImage, String name, String time, String message) {
        profileImageView.setImageResource(profileImage);
        nameView.setText(name);
        timeView.setText(time);
        descriptionView.setText(message);
    }

    public void displayUser(final User user) {
        Glide.with(fragment)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_profile_image_default)
                .into(profileImageView);
        nameView.setText(user.getNickname());
//        timeView.setText();
        descriptionView.setText(user.getIntroduction());
    }

    public void displayFriendship(final Friendship friendship) {
        displayUser(friendship.getFriend());
    }
}