package com.example.andoridproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.andoridproject.databinding.MyprofileItemBinding

class ProfileAdapter(private val itemList: List<MyTradeItem>) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {
    interface OnItemClickListeners {
        fun onItemClick(binding: MyprofileItemBinding, myTradeItem: MyTradeItem, position: Int)
    }

    private var onItemClickListeners: OnItemClickListeners? = null

    fun setOnItemClickListener(onItemClickListeners: OnItemClickListeners) {
        this.onItemClickListeners = onItemClickListeners
    }

    inner class ProfileViewHolder(private val binding: MyprofileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(myTradeItem: MyTradeItem) {
            binding.mydealimage.setImageResource(myTradeItem.imageResId)
            binding.mydealtext.text = myTradeItem.text

            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                binding.root.setOnClickListener {
                    onItemClickListeners?.onItemClick(binding, myTradeItem, position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = MyprofileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}
