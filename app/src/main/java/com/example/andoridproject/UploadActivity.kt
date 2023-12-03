package com.example.andoridproject

import SalesPost
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Log.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.andoridproject.databinding.UploadBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UploadActivity: AppCompatActivity() {
    private val binding: UploadBinding by lazy {
        UploadBinding.inflate(layoutInflater)
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
    private var selectedUri: Uri? = null
    private val fileName = "${System.currentTimeMillis()}"

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
                    binding.imageView.setImageURI(uri)
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

        binding.btnimageupload.setOnClickListener {
            checkPermissionsAndOpenGallery()
        }

        binding.upload.setOnClickListener {
            uploaditems {
                val intent = Intent(this, MySalesPostActivity::class.java)
                intent.putExtra("fileName", fileName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private fun checkPermissionsAndOpenGallery() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
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
        storage.reference.child("items").child("imageUri").child(fileName)
            .putFile(uri)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    // 성공적으로 업로드되면 다운로드 URL 가져오기
                    storage.reference.child("items").child("imageUri").child(fileName)
                        .downloadUrl
                        .addOnSuccessListener {
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

    private fun uploaditems(callback: () -> Unit) {
        val title = binding.editposttitle.text.toString()
        val price = binding.editprice.text.toString()
        val content = binding.editcontent.text.toString()
        val sellerId = auth.currentUser?.uid.orEmpty()

        selectedUri?.let { imageUri ->
            uploadImage(imageUri)
            val uri = imageUri.toString()
            if (uri.isNotBlank()) {
                d("Firestore", "Image uploaded successfully. Download URL: $uri")
                uploadSalsPost(sellerId, title, price, content, fileName)
                callback.invoke()
            } else
                Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadSalsPost(sellerId: String, title: String, price: String, content: String, imageUrl: String) {
        val model = SalesPost(sellerId, title, System.currentTimeMillis(), "$price 원", content, imageUrl, ArrayList(), false)
        val salesPostsCollection = firestore.collection("items")
        salesPostsCollection.add(model)
            .addOnSuccessListener { documentReference ->
                d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                w("Firestore", "Error adding document", e)
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