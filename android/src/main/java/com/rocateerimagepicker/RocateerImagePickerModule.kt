package com.rocateerimagepicker

import android.R.attr.data
import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = RocateerImagePickerModule.NAME)
class RocateerImagePickerModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener {


  companion object {
    const val NAME = "RocateerImagePicker"
    const val IMAGE_PICKER_REQUEST_CODE = 12345
  }

  private var pickerPromise: Promise? = null


  init {
    reactContext.addActivityEventListener(this)
  }

  override fun getName(): String = NAME

  /**
   * 이미지 피커 오픈
   */
  @ReactMethod
  fun openImagePicker(title: String, promise: Promise) {
    val currentActivity = currentActivity
    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "Current activity is null")
      return
    }
    pickerPromise = promise

    val intent = Intent(currentActivity, ImagePickerActivity::class.java)
    intent.putExtra("title", title)
    currentActivity.startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
  }

  override fun onActivityResult(
    activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?
  ) {
    if (requestCode == IMAGE_PICKER_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        val uris: ArrayList<String> = data?.getStringArrayListExtra("result_images") ?: arrayListOf()
        val resultArray: WritableArray = Arguments.createArray()
        for (uri in uris) {
          resultArray.pushString(uri)
        }
        pickerPromise?.resolve(resultArray)
      } else {
        pickerPromise?.reject("CANCELLED", "User cancelled image picker")
      }
      pickerPromise = null
    }
  }

  override fun onNewIntent(p0: Intent?) {
    TODO("Not yet implemented")
  }
}
