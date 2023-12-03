package com.example.andoridproject

import ChatListItem
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.andoridproject.databinding.ChatlistItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ChatListAdapter(val chatListItems: ArrayList<ChatListItem>) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val usersCollectionRef: CollectionReference by lazy {
        firestore.collection("users")
    }
    private val chatRoomsCollectionRef: CollectionReference by lazy {
        firestore.collection("chatRooms")
    }

    interface OnItemClickListeners {
        fun onItemClick(view: ChatlistItemBinding, chatListItem: ChatListItem, position: Int)
    }

    private var onItemClickListeners: OnItemClickListeners? = null

    fun setOnItemClickListener(onItemClickListeners: OnItemClickListeners) {
        this.onItemClickListeners = onItemClickListeners
    }

    inner class ViewHolder(private val binding: ChatlistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chatListItem: ChatListItem) {
            Log.d("ChatListAdapter", "Key: ${chatListItem.key}")
            chatRoomsCollectionRef.whereEqualTo("key", chatListItem.key)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val chatListItem = document.toObject(ChatListItem::class.java)
                        if (chatListItem != null) {
                            binding.title.text = chatListItem.title
                            if (myId != chatListItem.sellerId) {
                                usersCollectionRef.whereEqualTo("userId", chatListItem.sellerId)
                                    .get()
                                    .addOnSuccessListener { querySnapshot ->
                                        for (document in querySnapshot.documents) {
                                            val user = document.toObject(User::class.java)
                                            if (user != null) {
                                                binding.seller.text = user.userNickname
                                                val userimageRef = storage.reference.child("users")
                                                    .child("imageUri").child(chatListItem.sellerId)
                                                displayImageRef(userimageRef, binding.profile)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        // 문서 조회 실패
                                        Log.w("Firestore", "Error getting documents.", e)
                                    }
                            } else {
                                usersCollectionRef.whereEqualTo("userId", chatListItem.buyerId)
                                    .get()
                                    .addOnSuccessListener { querySnapshot ->
                                        for (document in querySnapshot.documents) {
                                            val user = document.toObject(User::class.java)
                                            if (user != null) {
                                                binding.seller.text = user.userNickname
                                                val userimageRef = storage.reference.child("users")
                                                    .child("imageUri").child(chatListItem.buyerId)
                                                displayImageRef(userimageRef, binding.profile)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        // 문서 조회 실패
                                        Log.w("Firestore", "Error getting documents.", e)
                                    }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // 문서 조회 실패
                    Log.w("Firestore", "Error getting documents.", e)
                }

            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                binding.root.setOnClickListener {
                    onItemClickListeners?.onItemClick(binding, chatListItem, position)
                }
            }
        }
    }

    private fun displayImageRef(imageRef: StorageReference?, view: ImageView) {
        imageRef?.getBytes(Long.MAX_VALUE)?.addOnSuccessListener {
            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
            view.setImageBitmap(bmp)
        }?.addOnFailureListener {
            // Failed to download the image
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChatlistItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(chatListItems[position])
    }

    override fun getItemCount(): Int {
        return chatListItems.size
    }
}