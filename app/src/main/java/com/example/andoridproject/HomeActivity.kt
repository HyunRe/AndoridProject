package com.example.andoridproject

import ChatListItem
import SalesPost
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.andoridproject.databinding.HomeBinding
import com.example.andoridproject.databinding.HomeItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity: Fragment() {
    private val binding: HomeBinding by lazy {
        HomeBinding.inflate(layoutInflater)
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
    private lateinit var homeAdapter: HomeAdapter
    private var itemLists = ArrayList<SalesPost>()
    private val myId: String = auth.currentUser?.uid.orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = binding.root

        binding.btnfilter.setOnClickListener {
            val dialog = FilterDialog()
            dialog.setOnFilterListener(object : FilterDialog.OnFilterListener {
                override fun onFilter(appliedFilters: Filters) {
                    applyFilters(appliedFilters)
                }
            })
            dialog.show(parentFragmentManager, "FilterDialog")
        }

        binding.upload.setOnClickListener {
            val intent = Intent(requireContext(), UploadActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        itemLists.clear()
        itemLists = ArrayList()
        homeAdapter = HomeAdapter(itemLists)
        homeAdapter.setOnItemClickListener(object : HomeAdapter.OnItemClickListeners {
            override fun onItemClick(binding: HomeItemBinding, salesPost: SalesPost, position: Int) {
                val chatRoom = ChatListItem (
                    buyerId = myId,
                    sellerId = salesPost.sellerId,
                    key = System.currentTimeMillis(),
                    title = salesPost.title
                )
                val sellerId = salesPost.sellerId
                val imageUrl = salesPost.imageUrl

                if (myId != sellerId) {
                    val intent = Intent(requireContext(), SalesPostActivity::class.java)
                    intent.putExtra("chatRoom", chatRoom)
                    intent.putExtra("sellerId", sellerId)
                    intent.putExtra("imageUrl", imageUrl)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), MySalesPostActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        })

        binding.salelistrecyclerView.layoutManager = LinearLayoutManager(context)
        binding.salelistrecyclerView.adapter = homeAdapter
        homeAdapter.notifyDataSetChanged()

        return view
    }

    override fun onStart() {
        super.onStart()

        // snapshot listener for all items
        if (snapshotListener == null) {  // 기존에 리스너가 등록되어 있지 않을 때만 등록
            snapshotListener = itemsCollectionRef.addSnapshotListener { snapshot, error ->
                itemLists.clear()  // 기존 항목들을 clear하고 다시 추가
                snapshot?.documents?.forEach { document ->
                    val salesPost = document.toObject(SalesPost::class.java)
                    salesPost?.let {
                        // SalesPost 객체를 itemList에 추가
                        itemLists.add(it)
                    }
                }
                homeAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }

    private fun applyFilters(filters: Filters) {
        itemLists.clear()

        snapshotListener = itemsCollectionRef.addSnapshotListener { snapshot, error ->
            snapshot?.documents?.forEach { document ->
                val salesPost = document.toObject(SalesPost::class.java)
                salesPost?.let {
                    val priceString = it.price.replace("[^\\d]".toRegex(), "") // "2222 원"에서 "2222"만 남김
                    val price = if (priceString.isNotBlank()) priceString.toInt() else 0 // 공백이나 빈 문자열이면 0으로 처리하거나 다른 기본값으로 변경

                    val priceInRange = price in filters.minPrice.toInt()..filters.maxPrice.toInt()
                    val soldOutCondition = if (filters.excludeSoldOut) {
                        // excludeSoldOut이 true이면 판매 중인 상품만 포함시킴
                        !it.status
                    } else {
                        // excludeSoldOut이 false이면 모든 상품을 포함시킴
                        true
                    }

                    if (priceInRange && soldOutCondition) {
                        itemLists.add(it)
                    }
                }
            }

            val sortingCondition = when {
                filters.ascendingOrder -> Comparator { o1: SalesPost, o2: SalesPost ->
                    o1.price.compareTo(o2.price)
                }
                filters.descendingOrder -> Comparator { o1: SalesPost, o2: SalesPost ->
                    o2.price.compareTo(o1.price)
                }
                else -> null
            }

            sortingCondition?.let {
                itemLists.sortWith(it)
            }

            homeAdapter.notifyDataSetChanged()
        }
    }
}