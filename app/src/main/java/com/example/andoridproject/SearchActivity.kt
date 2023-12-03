package com.example.andoridproject

import SalesPost
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.andoridproject.databinding.SearchBinding
import com.example.andoridproject.databinding.SearchItemBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SearchActivity : AppCompatActivity() {
    private val binding: SearchBinding by lazy {
        SearchBinding.inflate(layoutInflater)
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private var snapshotListener: ListenerRegistration? = null
    private val itemsCollectionRef: CollectionReference by lazy {
        firestore.collection("items")
    }
    private lateinit var searchAdapter: SearchAdapter
    private var itemLists = ArrayList<SalesPost>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.searcrecyclerView.visibility = View.GONE

        itemLists = ArrayList()
        searchAdapter = SearchAdapter(itemLists)
        searchAdapter.setOnItemClickListener(object : SearchAdapter.OnItemClickListeners {
            override fun onItemClick(binding: SearchItemBinding, salesPost: SalesPost, position: Int) {
                val sellerId = salesPost.sellerId
                val intent = Intent(this@SearchActivity, SalesPostActivity::class.java)
                intent.putExtra("sellerId", sellerId)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })

        binding.searcrecyclerView.layoutManager = LinearLayoutManager(this)
        binding.searcrecyclerView.adapter = searchAdapter

        val searchViewTextListener: SearchView.OnQueryTextListener =
            object : SearchView.OnQueryTextListener {
                //검색버튼 입력시 호출, 검색버튼이 없으므로 사용하지 않음
                override fun onQueryTextSubmit(s: String): Boolean {
                    return false
                }

                //텍스트 입력/수정시에 호출
                override fun onQueryTextChange(s: String): Boolean {
                    searchAdapter.getFilter().filter(s)
                    binding.searcrecyclerView.visibility = View.VISIBLE
                    return false
                }
            }

        binding.searchView.setOnQueryTextListener(searchViewTextListener)
        searchAdapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()

        // snapshot listener for all items
        snapshotListener = itemsCollectionRef.addSnapshotListener { snapshot, error ->
            snapshot?.documents?.forEach { document ->
                val salesPost = document.toObject(SalesPost::class.java)
                salesPost?.let {
                    // SalesPost 객체를 itemList에 추가
                    itemLists.add(it)
                }
            }
            searchAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }
}