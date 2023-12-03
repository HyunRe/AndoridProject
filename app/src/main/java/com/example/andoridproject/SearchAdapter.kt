package com.example.andoridproject

import SalesPost
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.andoridproject.databinding.SearchItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchAdapter(val itemlists: ArrayList<SalesPost>) : RecyclerView.Adapter<SearchAdapter.ViewHolder>(),
    Filterable {


    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    var filteredItem = ArrayList<SalesPost>()
    var itemFilter = ItemFilter()

    init {
        filteredItem.addAll(itemlists)
    }

    override fun getFilter(): Filter {
        return itemFilter
    }

    inner class ItemFilter : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterString = charSequence.toString()
            val results = FilterResults()

            // 검색이 필요 없을 경우를 위해 원본 배열을 복제
            val filteredList: ArrayList<SalesPost> = ArrayList<SalesPost>()
            // 공백제외 아무런 값이 없을 경우 -> 원본 배열
            if (filterString.trim { it <= ' ' }.isEmpty()) {
                results.values = itemlists
                results.count = itemlists.size
            } else {
                // 그 외의 경우(공백제외 2글자 초과) -> 이름/전화번호로 검색
                for (item in itemlists) {
                    if (item.title.contains(filterString)) {
                        filteredList.add(item)
                    }
                }
                results.values = filteredList
                results.count = filteredList.size
            }

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null) {
                itemlists.clear()
                itemlists.addAll(results.values as ArrayList<SalesPost>)
                notifyDataSetChanged()
            }
        }
    }
    interface OnItemClickListeners {
        fun onItemClick(view: SearchItemBinding, salesPost: SalesPost, position: Int)
    }

    private var onItemClickListeners: OnItemClickListeners? = null

    fun setOnItemClickListener(onItemClickListeners: OnItemClickListeners) {
        this.onItemClickListeners = onItemClickListeners
    }

    inner class ViewHolder(private val binding: SearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(salesPost: SalesPost) {
            val format = SimpleDateFormat("MM월 dd일", Locale.getDefault())
            val date = Date(salesPost.createdAt)
            val itemimageRef = storage.reference.child("items").child("imageUri").child(salesPost.imageUrl)

            binding.title.text = salesPost.title
            binding.uploaddate.text = format.format(date)
            binding.price.text = salesPost.price
            if (!salesPost.status)
                binding.sold.text = ""
            else
                binding.sold.text = "판매 완료"
            displayImageRef(itemimageRef, binding.imageView)

            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                binding.root.setOnClickListener {
                    onItemClickListeners?.onItemClick(binding, salesPost, position)
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
        val binding = SearchItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(itemlists[position])
    }

    override fun getItemCount(): Int {
        return itemlists.size
    }
}