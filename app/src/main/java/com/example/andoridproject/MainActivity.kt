package com.example.andoridproject

import ChatListItem
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.andoridproject.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val myId: String = auth.currentUser?.uid.orEmpty()
    private var chatRoom: ChatListItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setBottomNavigationView()

        if (savedInstanceState == null) {
            binding.navigationView.selectedItemId = R.id.home
        }
    }

    fun setBottomNavigationView() {
        @Suppress("DEPRECATION")
        chatRoom = intent.getParcelableExtra("chatRoom")

        binding.navigationView.setOnItemSelectedListener { item ->
            val transaction = supportFragmentManager.beginTransaction()
            val bundle = Bundle()

            when (item.itemId) {
                R.id.home -> {
                    val homeFragment = HomeActivity()
                    transaction.replace(R.id.fragmentContainer, homeFragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.chatList -> {
                    val chatListFragment = ChatListActivity()
                    bundle.putParcelable("chatRoom", chatRoom)
                    chatListFragment.arguments = bundle

                    transaction.replace(R.id.fragmentContainer, chatListFragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.myPage -> {
                    val profileFragment = ProfileActivity()
                    bundle.putString("myId", myId)
                    profileFragment.arguments = bundle

                    transaction.replace(R.id.fragmentContainer, profileFragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}