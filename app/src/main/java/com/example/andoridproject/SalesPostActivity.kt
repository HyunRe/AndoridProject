package com.example.andoridproject

import ChatListItem
import SalesPost
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.andoridproject.databinding.SalaspostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesPostActivity: AppCompatActivity() {
    private val binding: SalaspostBinding by lazy {
        SalaspostBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val itemsCollectionRef: CollectionReference by lazy {
        firestore.collection("items")
    }
    private val usersCollectionRef: CollectionReference by lazy {
        firestore.collection("users")
    }
    private var itemsSnapshotListener: ListenerRegistration? = null
    private var usersSnapshotListener: ListenerRegistration? = null
    private var sellerId: String = ""
    private var imageUrl: String = ""
    private var chatRoom: ChatListItem? = null
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sellerId = intent.getStringExtra("sellerId") ?: ""
        imageUrl = intent.getStringExtra("imageUrl") ?: ""

        usersCollectionRef.whereEqualTo("userId", sellerId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // SalesPost 문서를 가져오기
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        val userimageRef = storage.reference.child("users")
                            .child("imageUri").child(sellerId)
                        binding.seller.text = user.userNickname
                        displayImageRef(userimageRef, binding.profileView)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 문서 조회 실패
                Log.w("Firestore", "Error getting documents.", e)
            }

        itemsCollectionRef.whereEqualTo("imageUrl", imageUrl)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // SalesPost 문서를 가져오기
                    val salesPost = document.toObject(SalesPost::class.java)
                    if (salesPost != null) {
                        val format = SimpleDateFormat("MM월 dd일", Locale.getDefault())
                        val date = Date(salesPost.createdAt)
                        val itemimageRef = storage.reference.child("items")
                            .child("imageUri").child(salesPost.imageUrl)

                        binding.content.text = salesPost.content
                        binding.title.text = salesPost.title
                        binding.date.text = format.format(date)
                        binding.price.text = salesPost.price
                        if (!salesPost.status) {
                            binding.sold.text = ""
                            binding.btnchatting.isEnabled = true
                            binding.btnchatting.text = "채팅 시작"
                        } else {
                            binding.sold.text = "판매 완료"
                            binding.btnchatting.isEnabled = false
                            binding.btnchatting.text = "채팅 불가"
                        }
                        displayImageRef(itemimageRef, binding.imageView)

                        val liked = myId in salesPost.like
                        binding.btnlike.setImageResource(if (liked) R.drawable.heart else R.drawable.emptyheart)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 문서 조회 실패
                Log.w("Firestore", "Error getting documents.", e)
            }

        if (myId != sellerId) {
            @Suppress("DEPRECATION")
            chatRoom = intent.getParcelableExtra("chatRoom")

            val key = chatRoom?.key ?: 0L
            Log.d("SalesPostActivity", "Key: $key")
            binding.btnchatting.setOnClickListener {
                saveChatRoomToFirestore {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("chatRoom", chatRoom)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }

            binding.btnlike.setOnClickListener {
                count++
                val liked = count % 2 == 1

                // 좋아요 상태에 따라 UI 업데이트
                binding.btnlike.setImageResource(if (liked) R.drawable.heart else R.drawable.emptyheart)

                // sellerId와 일치하는 문서를 검색
                itemsCollectionRef.whereEqualTo("sellerId", sellerId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            // SalesPost 문서의 ID 가져오기
                            val salesPostId = document.id

                            // SalesPost 문서의 like 필드 가져오기
                            val currentLikes =
                                document.get("like") as? ArrayList<String> ?: arrayListOf()

                            if (liked && myId !in currentLikes) {
                                // 좋아요 상태이고, 현재 사용자가 이미 좋아요하지 않았을 경우 myId를 like 배열에 추가
                                currentLikes.add(myId)
                            } else if (!liked && myId in currentLikes) {
                                // 좋아요 상태가 아니고, 현재 사용자가 이미 좋아요한 경우 myId를 like 배열에서 제거
                                currentLikes.remove(myId)
                            }

                            // SalesPost 문서의 like 필드 업데이트
                            itemsCollectionRef.document(salesPostId)
                                .update("like", currentLikes)
                                .addOnSuccessListener {
                                    // 업데이트 성공
                                    Log.d("Firestore", "문서 $salesPostId 업데이트 성공.")
                                }
                                .addOnFailureListener { e ->
                                    // 업데이트 실패
                                    Log.w("Firestore", "문서 $salesPostId 업데이트 중 오류 발생", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        // 문서 검색 실패
                        Log.w("Firestore", "문서 검색 중 오류 발생.", e)
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // items 컬렉션에 대한 SnapshotListener 등록
        itemsSnapshotListener = itemsCollectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("Firestore", "Listen failed.", error)
                return@addSnapshotListener
            }

            // 변경된 데이터 처리
            processItemsSnapshot(snapshot)
        }

        // users 컬렉션에 대한 SnapshotListener 등록
        usersSnapshotListener = usersCollectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("Firestore", "Listen failed.", error)
                return@addSnapshotListener
            }

            // 변경된 데이터 처리
            processUsersSnapshot(snapshot)
        }
    }

    override fun onStop() {
        super.onStop()

        // SnapshotListener 해제
        itemsSnapshotListener?.remove()
        usersSnapshotListener?.remove()
    }

    // items 컬렉션의 변경 사항을 처리하는 함수
    private fun processItemsSnapshot(snapshot: QuerySnapshot?) {
        snapshot?.documents?.forEach { document ->
            val salesPost = document.toObject(SalesPost::class.java)
            if (salesPost != null) {
                val format = SimpleDateFormat("MM월 dd일", Locale.getDefault())
                val date = Date(salesPost.createdAt)
                val itemimageRef = storage.reference.child("items")
                    .child("imageUri").child(salesPost.imageUrl)

                binding.content.text = salesPost.content
                binding.title.text = salesPost.title
                binding.date.text = format.format(date)
                binding.price.text = salesPost.price
                if (!salesPost.status) {
                    binding.sold.text = ""
                    binding.btnchatting.isEnabled = true
                    binding.btnchatting.text = "채팅 시작"
                } else {
                    binding.sold.text = "판매 완료"
                    binding.btnchatting.isEnabled = false
                    binding.btnchatting.text = "채팅 불가"
                }
                displayImageRef(itemimageRef, binding.imageView)

                val liked = myId in salesPost.like
                binding.btnlike.setImageResource(if (liked) R.drawable.heart else R.drawable.emptyheart)
            }
        }
    }

    // users 컬렉션의 변경 사항을 처리하는 함수
    private fun processUsersSnapshot(snapshot: QuerySnapshot?) {
        snapshot?.documents?.forEach { document ->
            val user = document.toObject(User::class.java)
            if (user != null) {
                val userimageRef = storage.reference.child("users")
                    .child("imageUri").child(sellerId)
                binding.seller.text = user.userNickname
                displayImageRef(userimageRef, binding.profileView)
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

    private fun saveChatRoomToFirestore(callback: () -> Unit) {
        @Suppress("DEPRECATION")
        chatRoom = intent.getParcelableExtra("chatRoom")
        // "chatRooms" 컬렉션에 ChatListItem 정보를 Firestore에 추가
        chatRoom?.let {
            firestore.collection("chatRooms")
                .add(it) // Automatically generates a unique document ID
                .addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "ChatRoom saved successfully with ID: ${documentReference.id}")
                    callback()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error saving chatRoom to Firestore", e)
                }
        }
    }
}