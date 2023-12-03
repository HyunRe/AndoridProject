package com.example.andoridproject

import SalesPost
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.andoridproject.databinding.MypostlistItemBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostListAdapter(val myitemLists: ArrayList<SalesPost>) : RecyclerView.Adapter<MyPostListAdapter.ViewHolder>() {
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    inner class ViewHolder(private val binding: MypostlistItemBinding) :
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

            binding.btnfilter.setOnClickListener {
                val dialogBuilder = AlertDialog.Builder(binding.root.context)
                dialogBuilder.setTitle("게시글 관리")

                val options = arrayOf("게시글 수정", "게시글 삭제", "판매 완료")

                dialogBuilder.setItems(options) { _, which ->
                    val clickedPost = myitemLists[adapterPosition] // 위치를 명시적으로 가져옴
                    val itemsCollectionRef = firestore.collection("items")

                    when (which) {
                        0 -> {
                            // 값 전달
                            val intent = Intent(binding.root.context, UpdateActivity::class.java)
                            intent.putExtra("salesPost", clickedPost)
                            binding.root.context.startActivity(intent)
                        }
                        1 -> {
                            itemsCollectionRef.whereEqualTo("imageUrl", clickedPost.imageUrl)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot.documents) {
                                        // 찾은 문서를 삭제
                                        document.reference.delete()
                                            .addOnSuccessListener {
                                                myitemLists.remove(clickedPost)
                                                notifyDataSetChanged()
                                            }
                                            .addOnFailureListener { e ->
                                                // 삭제 실패 시의 동작
                                                Log.e("Firestore", "Error deleting document", e)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // 조회 실패 시의 동작
                                    Log.e("Firestore", "Error querying documents", e)
                                }
                        }
                        2 -> {
                            // update
                            val updateData = hashMapOf(
                                "status" to true,
                            )
                            itemsCollectionRef.whereEqualTo("imageUrl", clickedPost.imageUrl)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot.documents) {
                                        // 찾은 문서를 삭제
                                        document.reference.update(updateData as Map<String, Any>)
                                            .addOnSuccessListener {
                                                clickedPost.status = true
                                                binding.sold.text = "판매 완료"
                                            }
                                            .addOnFailureListener { e ->
                                                // 삭제 실패 시의 동작
                                                Log.e("Firestore", "Error deleting document", e)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // 조회 실패 시의 동작
                                    Log.e("Firestore", "Error querying documents", e)
                                }
                        }
                    }
                }

                val dialog = dialogBuilder.create()
                dialog.show()
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
        val binding = MypostlistItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(myitemLists[position])
    }

    override fun getItemCount(): Int {
        return myitemLists.size
    }
}