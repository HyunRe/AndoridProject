package com.example.andoridproject

import ChatListItem
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.andoridproject.databinding.ChatlistBinding
import com.example.andoridproject.databinding.ChatlistItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatListActivity: Fragment() {
    private val binding: ChatlistBinding by lazy {
        ChatlistBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val chatRoomsCollectionRef: CollectionReference by lazy {
        firestore.collection("chatRooms")
    }
    private var snapshotListener: ListenerRegistration? = null
    private lateinit var chatListAdapter: ChatListAdapter
    private var chatListItems = ArrayList<ChatListItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = binding.root

        chatListItems = ArrayList()
        chatListAdapter = ChatListAdapter(chatListItems)
        chatListAdapter.setOnItemClickListener(object : ChatListAdapter.OnItemClickListeners {
            override fun onItemClick(binding: ChatlistItemBinding, chatListItem: ChatListItem, position: Int) {
                val receiveKey = chatListItem.key
                Log.d("ChatListActivity", "receiveKey: $receiveKey")
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("receiveKey", receiveKey)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        binding.chatlistrecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatlistrecyclerView.adapter = chatListAdapter

        return view
    }

    override fun onStart() {
        super.onStart()

        // snapshot listener for all items
        snapshotListener = chatRoomsCollectionRef.addSnapshotListener { snapshot, error ->
            chatListItems.clear()
            snapshot?.documents?.forEach { document ->
                val chatListItem = document.toObject(ChatListItem::class.java)
                chatListItem?.let {
                    // SalesPost 객체를 itemList에 추가
                    if (it.buyerId == myId || it.sellerId == myId) {
                        chatListItems.add(it)
                    }
                }
            }
            chatListAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }
}
