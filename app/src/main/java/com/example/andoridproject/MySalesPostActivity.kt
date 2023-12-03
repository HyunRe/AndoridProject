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
import androidx.core.os.bundleOf
import com.example.andoridproject.databinding.MysalespostBinding
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

class MySalesPostActivity: AppCompatActivity() {
    private val binding: MysalespostBinding by lazy {
        MysalespostBinding.inflate(layoutInflater)
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
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private var fileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        fileName = intent.getStringExtra("fileName") ?: ""
        Log.d("MyPostListActivity", "FileName: $fileName")

        usersCollectionRef.whereEqualTo("userId", myId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // SalesPost 문서를 가져오기
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        val userimageRef = storage.reference.child("users")
                            .child("imageUri").child(myId)
                        binding.seller.text = user.userNickname
                        displayImageRef(userimageRef.child(myId), binding.profileView)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 문서 조회 실패
                Log.w("Firestore", "Error getting documents.", e)
            }

        itemsCollectionRef.whereEqualTo("imageUrl", fileName)
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
                        if (!salesPost.status)
                            binding.sold.text = ""
                        else
                            binding.sold.text = "판매 완료"
                        displayImageRef(itemimageRef, binding.imageView)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 문서 조회 실패
                Log.w("Firestore", "Error getting documents.", e)
            }

        binding.btnchatting.visibility = View.GONE
        binding.btnlike.visibility = View.GONE
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
                if (!salesPost.status)
                    binding.sold.text = ""
                else
                    binding.sold.text = "판매 완료"
                displayImageRef(itemimageRef, binding.imageView)
            }
        }
    }

    // users 컬렉션의 변경 사항을 처리하는 함수
    private fun processUsersSnapshot(snapshot: QuerySnapshot?) {
        snapshot?.documents?.forEach { document ->
            val user = document.toObject(User::class.java)
            if (user != null) {
                val userimageRef = storage.reference.child("users")
                    .child("imageUri").child(myId)
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
}