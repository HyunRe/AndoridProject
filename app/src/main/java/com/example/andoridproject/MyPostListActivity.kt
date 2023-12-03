package com.example.andoridproject

import SalesPost
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.andoridproject.databinding.MypostlistBinding
import com.example.andoridproject.databinding.MypostlistItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyPostListActivity: AppCompatActivity() {
    private val binding: MypostlistBinding by lazy {
        MypostlistBinding.inflate(layoutInflater)
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
    private lateinit var myPostListAdapter: MyPostListAdapter
    private var myitemLists = ArrayList<SalesPost>()
    private val myId: String = auth.currentUser?.uid.orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        myitemLists.clear()
        myitemLists = ArrayList()
        myPostListAdapter = MyPostListAdapter(myitemLists)

        binding.mypostlistrecyclerView.layoutManager = LinearLayoutManager(this)
        binding.mypostlistrecyclerView.adapter = myPostListAdapter
    }

    override fun onStart() {
        super.onStart()

        // snapshot listener for items with matching sellerId
        snapshotListener = itemsCollectionRef.whereEqualTo("sellerId", myId)
            .addSnapshotListener { snapshot, error ->
                myitemLists.clear()
                snapshot?.documents?.forEach { document ->
                    val salesPost = document.toObject(SalesPost::class.java)
                    salesPost?.let {
                        // SalesPost 객체를 itemList에 추가
                        myitemLists.add(it)
                    }
                }
                binding.count.text = myitemLists.size.toString()
                myPostListAdapter.notifyDataSetChanged()
            }
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }
}