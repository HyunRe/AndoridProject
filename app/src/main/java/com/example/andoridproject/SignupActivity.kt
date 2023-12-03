package com.example.andoridproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.andoridproject.databinding.SignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private val binding: SignupBinding by lazy {
        SignupBinding.inflate(layoutInflater)
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnregister.setOnClickListener {
            val password = binding.password.text.toString()
            val passwordRe = binding.passwordRe.text.toString()

            if (password == passwordRe) {
                val userInfo = User(
                    userId = "",
                    userNickname = binding.userID.text.toString(),
                    userEmail = binding.email.text.toString(),
                    userName = binding.username.text.toString(),
                    birthDay = binding.birthday.text.toString(),
                    password = password,
                    imageUrl = ""
                )

                createAccount(userInfo)
            } else {
                binding.passwordMissmatch.text = "비밀번호가 일치하지 않습니다."
            }
        }
    }

    private fun createAccount(userInfo: User) {
        auth.createUserWithEmailAndPassword(userInfo.userEmail, userInfo.password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    userInfo.userId = user.uid
                    addUserDataToFirestore(userInfo)
                } else {
                    Log.e("SignupActivity", "사용자 정보가 null입니다.")
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                // 계정 생성 실패 시
                Log.e("SignupActivity", "사용자 생성 실패", e)
                binding.passwordMissmatch.text = "가입 오류: ${e.message}"
            }
    }

    private fun addUserDataToFirestore(user: User) {
        firestore.collection("users")
            .document(user.userId)
            .set(user)
            .addOnSuccessListener {
                Log.d("SignupActivity", "회원 정보 저장 완료")
                Toast.makeText(this, "회원 정보 저장 완료", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("SignupActivity", "회원 정보 저장 실패", e)
                Toast.makeText(this, "회원 정보 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }
}
