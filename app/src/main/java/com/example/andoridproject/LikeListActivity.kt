package com.example.andoridproject

import SalesPost
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.andoridproject.databinding.LikelistBinding
import com.example.andoridproject.databinding.LikelistItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LikeListActivity: AppCompatActivity() {
    private val binding: LikelistBinding by lazy {
        LikelistBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val itemsCollectionRef: CollectionReference by lazy {
        firestore.collection("items")
    }
    private var snapshotListener: ListenerRegistration? = null
    private lateinit var likeListAdapter: LikeListAdapter
    private var likeLists = ArrayList<SalesPost>()
    private val myId: String = auth.currentUser?.uid.orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        likeLists.clear()
        likeLists = ArrayList()
        likeListAdapter = LikeListAdapter(likeLists)
        likeListAdapter.setOnItemClickListener(object : LikeListAdapter.OnItemClickListeners {
            override fun onItemClick(binding: LikelistItemBinding, salesPost: SalesPost, position: Int) {
                val intent = Intent(this@LikeListActivity, SalesPostActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        binding.likelistrecyclerView.layoutManager = LinearLayoutManager(this)
        binding.likelistrecyclerView.adapter = likeListAdapter
    }

    override fun onStart() {
        super.onStart()

        // snapshotListener 설정
        snapshotListener = itemsCollectionRef.addSnapshotListener { snapshot, error ->
            likeLists.clear()
            snapshot?.documents?.forEach { document ->
                try {
                    val salesPost = document.toObject(SalesPost::class.java)
                    salesPost?.let {
                        // SalesPost의 like 필드 가져오기
                        val likeList: ArrayList<String> = document.get("like") as? ArrayList<String> ?: ArrayList()
                        // myId가 likeList에 포함되어 있는지 확인
                        if (likeList.contains(myId)) {
                            // 조건을 만족하는 경우 likeLists에 추가
                            likeLists.add(it)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            likeListAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }
}