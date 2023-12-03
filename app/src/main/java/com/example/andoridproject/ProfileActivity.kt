package com.example.andoridproject

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.andoridproject.databinding.MyprofileBinding
import com.example.andoridproject.databinding.MyprofileItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity: Fragment() {
    private val binding: MyprofileBinding by lazy {
        MyprofileBinding.inflate(layoutInflater)
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
    private lateinit var profileAdapter: ProfileAdapter
    private var itemLists = ArrayList<MyTradeItem>()
    private var userSnapshotListener: ListenerRegistration? = null
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private val imageRef: StorageReference by lazy {
        storage.reference.child("users").child("imageUri").child(myId)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = binding.root

        val usersCollectionRef = firestore.collection("users")
        usersCollectionRef.whereEqualTo("userId", myId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // SalesPost 문서를 가져오기
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        binding.nickname.text = user.userNickname
                        displayImageRef(imageRef, binding.profile)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 문서 조회 실패
                Log.w("Firestore", "Error getting documents.", e)
            }

        itemLists = ArrayList<MyTradeItem>().apply {
            add(MyTradeItem(R.drawable.list, "판매목록"))
            add(MyTradeItem(R.drawable.emptyheart, "관심목록"))
        }

        profileAdapter = ProfileAdapter(itemLists)
        profileAdapter.setOnItemClickListener(object : ProfileAdapter.OnItemClickListeners {
            override fun onItemClick(binding: MyprofileItemBinding, myTradeItem: MyTradeItem, position: Int) {
                val intent: Intent = when (position) {
                    0 -> {
                        Intent(requireContext(), MyPostListActivity::class.java)
                    }
                    1 -> {
                        Intent(requireContext(), LikeListActivity::class.java)
                    }
                    else -> return
                }
                intent.putExtra("myId", myId)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        binding.mydealrecyclerview.layoutManager = LinearLayoutManager(context)
        binding.mydealrecyclerview.adapter = profileAdapter

        binding.btneditprofile.setOnClickListener {
            val myId = auth.currentUser?.uid.orEmpty()
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra("myId", myId)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.logout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        // users 컬렉션에 대한 SnapshotListener 등록
        userSnapshotListener = firestore.collection("users")
            .document(myId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Firestore", "Listen failed.", error)
                    return@addSnapshotListener
                }

                // 변경된 데이터 처리
                processUserSnapshot(snapshot)
            }
    }

    override fun onStop() {
        super.onStop()

        // SnapshotListener 해제
        userSnapshotListener?.remove()
    }

    // users 컬렉션의 변경 사항을 처리하는 함수
    private fun processUserSnapshot(snapshot: DocumentSnapshot?) {
        val user = snapshot?.toObject(User::class.java)
        if (user != null) {
            binding.nickname.text = user.userNickname
            displayImageRef(imageRef, binding.profile)
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