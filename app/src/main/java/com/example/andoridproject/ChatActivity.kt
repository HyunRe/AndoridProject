package com.example.andoridproject

import ChatListItem
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.andoridproject.databinding.ChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatActivity : AppCompatActivity() {
    private val binding: ChatBinding by lazy {
        ChatBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val chatRoomsCollectionRef: CollectionReference by lazy {
        firestore.collection("chatRooms")
    }
    private val chatsCollectionRef: CollectionReference by lazy {
        firestore.collection("chats")
    }
    private val usersCollectionRef: CollectionReference by lazy {
        firestore.collection("users")
    }
    private var snapshotListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter
    private var chatItems = ArrayList<ChatItem>()
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private var chatRoom: ChatListItem? = null
    private var key: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        chatRoom = intent.getParcelableExtra("chatRoom")
        if (chatRoom == null) {
            key = intent.getLongExtra("receiveKey", 0L)
        } else {
            key = chatRoom?.key ?: 0L
        }

        Log.d("ChatActivity1111", "Key: $key")
        chatRoomsCollectionRef.whereEqualTo("key", key)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val chatListItem = document.toObject(ChatListItem::class.java)
                    if (chatListItem != null) {
                        if (myId != chatListItem.sellerId) {
                            usersCollectionRef.whereEqualTo("userId", chatListItem.sellerId)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot.documents) {
                                        val user = document.toObject(User::class.java)
                                        if (user != null) {
                                            binding.contactname.text = user.userNickname
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
                                            binding.contactname.text = user.userNickname
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

        binding.back.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("chatRoom", chatRoom)
            startActivity(intent)
        }

        binding.btnsubmit.setOnClickListener {
            putMessage()
        }

        chatItems.clear()
        chatItems = ArrayList()
        chatAdapter = ChatAdapter(chatItems)
        binding.messagesrecyclerView.layoutManager = LinearLayoutManager(this)
        binding.messagesrecyclerView.adapter = chatAdapter
    }

    override fun onStart() {
        super.onStart()

        // snapshot listener for specific chat room items
        snapshotListener = chatsCollectionRef.whereEqualTo("key", key)
            .addSnapshotListener { snapshot, error ->
                chatItems.clear()
                snapshot?.documents?.forEach { document ->
                    val chatItem = document.toObject(ChatItem::class.java)
                    chatItem?.let {
                        // chatItem 객체를 itemList에 추가
                        chatItems.add(it)
                    }
                }
                chatItems.sortBy { it.time }
                chatAdapter.notifyDataSetChanged()
            }
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }

    override fun onPause() {
        super.onPause()
        snapshotListener?.remove()
    }

    fun putMessage() {       //메시지 전송
        @Suppress("DEPRECATION")
        chatRoom = intent.getParcelableExtra("chatRoom")
        if (chatRoom == null) {
            key = intent.getLongExtra("receiveKey", 0L)
        } else {
            key = chatRoom?.key ?: 0L
        }

        Log.d("ChatActivity22222", "Key: $key")
        try {
            var message = ChatItem(getDateTimeString(), myId, binding.message.text.toString(), key)    //메시지 정보 초기화
            firestore.collection("chats")
                .add(message)
                .addOnSuccessListener {
                    Log.i("putMessage", "메시지 전송에 성공하였습니다.")
                    binding.message.text.clear()
                }
                .addOnFailureListener { e ->
                    Log.e("putMessage", "메시지 전송에 실패하였습니다", e)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("putMessage", "메시지 전송 중 오류가 발생하였습니다.")
        }
    }

    fun getDateTimeString(): String {
        try {
            val localDateTime = LocalDateTime.now()
            val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            return dateTimeFormatter.format(zonedDateTime)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("getTimeError")
        }
    }
}
