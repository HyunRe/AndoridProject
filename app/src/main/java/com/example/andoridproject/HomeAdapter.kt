package com.example.andoridproject

import SalesPost
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.andoridproject.databinding.HomeItemBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeAdapter(val itemlists: ArrayList<SalesPost>) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    interface OnItemClickListeners {
        fun onItemClick(view: HomeItemBinding, salesPost: SalesPost, position: Int)
    }

    private var onItemClickListeners: OnItemClickListeners? = null

    fun setOnItemClickListener(onItemClickListeners: OnItemClickListeners) {
        this.onItemClickListeners = onItemClickListeners
    }

    inner class ViewHolder(private val binding: HomeItemBinding) :
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
        val binding = HomeItemBinding.inflate(
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
