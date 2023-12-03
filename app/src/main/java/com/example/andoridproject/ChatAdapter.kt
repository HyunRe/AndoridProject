package com.example.andoridproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.andoridproject.databinding.ChatItemBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(private val chatItems: ArrayList<ChatItem>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val myId: String = auth.currentUser?.uid.orEmpty()

    inner class ChatViewHolder(private val binding: ChatItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chatItem: ChatItem) {
            val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(chatItem.time)!!
            )

            if (chatItem.senderId == myId) {
                binding.message.setBackgroundResource(R.drawable.background_talk_mine)
            } else {
                binding.message.setBackgroundResource(R.drawable.background_talk_others)
            }

            binding.message.text = chatItem.message
            binding.date.text = formattedTime
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChatItemBinding.inflate(inflater, parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatItems[position])
    }

    override fun getItemCount(): Int {
        return chatItems.size
    }
}

