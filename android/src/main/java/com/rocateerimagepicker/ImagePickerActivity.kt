package com.rocateerimagepicker

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

class ImagePickerActivity : AppCompatActivity() {
  companion object {
    const val RESULT_IMAGES = "result_images"
  }

  private val PERMISSION_REQUEST_CODE = 1001
  private val CAMERA_REQUEST_CODE = 2001
  private lateinit var adapter: ImageAdapter
  private val images = mutableListOf<Uri>()
  private val selectedImages = mutableSetOf<Uri>()

  private lateinit var photoUri: Uri
  private lateinit var cameraLauncher: ActivityResultLauncher<Intent>


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_image_picker)

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    val barTitle = intent.getStringExtra("title") ?: "이미지 선택"
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      title = barTitle
    }

    toolbar.setNavigationOnClickListener {
      finish()
    }

    val recyclerView = findViewById<RecyclerView>(R.id.image_list)
    recyclerView.layoutManager = GridLayoutManager(this, 3)

    adapter = ImageAdapter(
      images,
      selectedImages,
      onCameraClick = { openCamera() },
      onImageClick = { uri -> selectImage(uri) }
    )
    recyclerView.adapter = adapter

    val btnDone = findViewById<AppCompatButton>(R.id.btn_done)
    btnDone.setOnClickListener {
      val resultIntent = Intent().apply {
        putStringArrayListExtra(
          "result_images",
          ArrayList(selectedImages.map { it.toString() })  // Uri 리스트를 문자열 리스트로 변환해서 전달
        )
      }
      setResult(RESULT_OK, resultIntent)
      finish()
    }

    // 권한 체크
    if (hasPermissions()) {
      loadImagesAsync()
    } else {
      requestPermissions() // 권한 요청
    }

    cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        // photoUri 에 사진이 저장됨
        // 갤러리 스캔 (Optional)
        addImageToGallery(photoUri)

        // 리스트에 추가 및 선택 상태 반영
        images.add(0, photoUri)
        selectedImages.add(photoUri)
        adapter.notifyItemInserted(0)
        recyclerView.scrollToPosition(0)
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode == PERMISSION_REQUEST_CODE) {
      if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
        // 권한 허용됨 → 이미지 로딩 또는 카메라 기능 활성화
        loadImagesAsync()
      } else {
        // 권한 거부됨 → 안내 메시지 띄우기 등 처리
      }
    }
  }

  /**
   * 권한 체크
   */
  fun hasPermissions(): Boolean {
    val cameraPermission =
      checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    val readStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      // Android 13 이상은 READ_MEDIA_IMAGES 권한 사용
      checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
      checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    return cameraPermission && readStoragePermission
  }

  /**
   * 권한 요청
   */
  fun requestPermissions() {
    val permissionsToRequest = mutableListOf<String>()
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.CAMERA)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
      }
    } else {
      if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
      }
    }

    if (permissionsToRequest.isNotEmpty()) {
      requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
    }
  }

  /**
   * 이미지 호출
   */
  private fun loadImagesAsync() {
    CoroutineScope(Dispatchers.IO).launch {
      val loadedImages = loadImages()
      withContext(Dispatchers.Main) {
        images.clear()
        images.addAll(loadedImages)
        adapter.notifyDataSetChanged()
      }
    }
  }

  /**
   * 이미지 불러오기
   */
  private fun loadImages(): List<Uri> {
    val imageUris = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    val query = contentResolver.query(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      projection,
      null,
      null,
      sortOrder
    )

    query?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

      while (cursor.moveToNext()) {
        val id = cursor.getLong(idColumn)
        val contentUri =
          Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
        imageUris.add(contentUri)
      }
    }
    return imageUris
  }


  /**
   * 카메라 호출
   */
  private fun openCamera() {
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (takePictureIntent.resolveActivity(packageManager) != null) {
      val photoFile: File = createImageFile()
      photoUri = FileProvider.getUriForFile(
        this,
        "${applicationContext.packageName}.fileprovider",
        photoFile
      )
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
      cameraLauncher.launch(takePictureIntent)
    } else {
      Toast.makeText(this, "카메라 앱 실행 불가", Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * 카메라 촬영후 저장
   */
  private fun createImageFile(): File {
    val timestamp = System.currentTimeMillis()
    val imageFileName = "JPEG_${timestamp}_"
    val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(imageFileName, ".jpg", storageDir)
  }

  /**
   * 갤러리 스캔
   */
  private fun addImageToGallery(uri: Uri) {
    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    intent.data = uri
    sendBroadcast(intent)
  }


  /**
   * 이미지 선택
   */
  private fun selectImage(uri: Uri) {
    if (selectedImages.contains(uri)) {
      selectedImages.remove(uri)
    } else {
      selectedImages.add(uri)
    }
    adapter.notifyDataSetChanged()
  }

}
