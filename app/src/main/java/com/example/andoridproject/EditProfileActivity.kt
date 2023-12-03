package com.example.andoridproject

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.andoridproject.databinding.EditmyprofileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileActivity: AppCompatActivity() {
    private val binding: EditmyprofileBinding by lazy {
        EditmyprofileBinding.inflate(layoutInflater)
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val imageRef: StorageReference by lazy {
        storage.reference.child("users").child("imageUri").child(myId)
    }
    private var selectedUri: Uri? = null
    private val myId: String = auth.currentUser?.uid.orEmpty()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "권한이 거부되었습니다. 이미지를 선택할 수 없습니다.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    private val getContentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    binding.profile.setImageURI(uri)
                    selectedUri = uri
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            requestSinglePermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        else
            requestSinglePermission(android.Manifest.permission.READ_MEDIA_IMAGES)

        val usersCollectionRef = firestore.collection("users")
        usersCollectionRef.whereEqualTo("userId", myId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // SalesPost 문서를 가져오기
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        binding.editId.text = Editable.Factory.getInstance().newEditable(user.userNickname)
                        displayImageRef(imageRef, binding.profile)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 문서 조회 실패
                Log.w("Firestore", "Error getting documents.", e)
            }

        binding.profile.setOnClickListener {
            checkPermissionsAndOpenGallery()
        }

        binding.btnsave.setOnClickListener {
            updateusers()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
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

    private fun checkPermissionsAndOpenGallery() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContentLauncher.launch(intent)
    }

    private fun uploadImage(uri: Uri) {
        // 사진을 Firebase 저장소에 업로드
        storage.reference.child("users").child("imageUri").child(myId)
            .putFile(uri)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    // 성공적으로 업로드되면 다운로드 URL 가져오기
                    storage.reference.child("users").child("imageUri").child(myId)
                        .downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.d("Firestore", "Image uploaded successfully. Download URL: $uri")
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "Failed to get download URL after successful upload", it)
                        }
                } else {
                    Log.e("Firestore", "Image upload failed", it.exception)
                }
            }
    }

    private fun updateusers() {
        val userNickname  = binding.editId.text.toString()
        selectedUri?.let { imageUri ->
            uploadImage(imageUri)
            val uri = imageUri.toString()
            if (uri.isNotBlank()) {
                Log.d("Firestore", "Image uploaded successfully. Download URL: $uri")
                updateUser(myId, mapOf("userNickname" to userNickname, "imageUrl" to uri))
            } else
                Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
        } ?: run {
            updateUser(myId, mapOf("userNickname" to userNickname))
        }
    }

    private fun updateUser(myId: String, fields: Map<String, Any>) {
        val userDocument = firestore.collection("users").document(myId)
        userDocument.update(fields)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot updated with ID: $myId")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating document", e)
            }
    }

    private fun requestSinglePermission(permission: String) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
            return

        val requestPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it == false) { // 권한이 허용되지 않았을 경우
                AlertDialog.Builder(this).apply {
                    setTitle("경고")
                    setMessage("권한이 필요합니다!")
                }.show()
            }
        }

        if (shouldShowRequestPermissionRationale(permission)) {
            // 권한이 필요한 이유에 대해 설명
            AlertDialog.Builder(this).apply {
                setTitle("이유")
                setMessage("권한이 필요합니다!")
                setPositiveButton("허용") { _, _ -> requestPermLauncher.launch(permission) }
                setNegativeButton("거부") { _, _ -> }
            }.show()
        } else {
            // onCreate()에서 호출해야 함
            requestPermLauncher.launch(permission)
        }
    }
}