/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.view.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import catchla.yep.R
import catchla.yep.adapter.BaseRecyclerViewAdapter
import catchla.yep.adapter.iface.ItemClickListener
import catchla.yep.model.Friendship
import catchla.yep.model.User
import catchla.yep.util.ImageLoaderWrapper

/**
 * Created by mariotaku on 15/4/29.
 */
class FriendViewHolder(itemView: View,
                       val adapter: BaseRecyclerViewAdapter<RecyclerView.ViewHolder>,
                       val listener: ItemClickListener?) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    private val profileImageView: ImageView
    private val nameView: TextView
    private val timeView: TextView
    private val descriptionView: TextView

    init {
        profileImageView = itemView.findViewById(R.id.profile_image) as ImageView
        nameView = itemView.findViewById(R.id.name) as TextView
        timeView = itemView.findViewById(R.id.update_time) as TextView
        descriptionView = itemView.findViewById(R.id.description) as TextView
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (listener == null) return
        listener.onItemClick(adapterPosition, this@FriendViewHolder)
    }

    fun displaySample(profileImage: Int, name: String, time: String, message: String) {
        profileImageView.setImageResource(profileImage)
        nameView.text = name
        timeView.text = time
        descriptionView.text = message
    }

    fun displayUser(user: User) {
        val imageLoader = adapter.imageLoader
        imageLoader.displayProfileImage(user.avatarThumbUrl, profileImageView)
        nameView.text = user.nickname
        //        timeView.setText();
        descriptionView.text = user.introduction
    }

    fun displayFriendship(friendship: Friendship) {
        displayUser(friendship.friend)
    }
}