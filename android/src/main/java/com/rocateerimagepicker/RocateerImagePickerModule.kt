package com.rocateerimagepicker

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.ReadableMap
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
   * 이미지 피커 오픈 (옵션 기반)
   * JS: RocateerImagePicker.open({ allowMultiple, maxSelection, checkboxTintColor })
   */
  @ReactMethod
  fun open(options: ReadableMap, promise: Promise) {
    val currentActivity = currentActivity
    if (currentActivity == null) {
      promise.reject("NO_ACTIVITY", "Current activity is null")
      return
    }
    pickerPromise = promise

    val allowMultiple = if (options.hasKey("allowMultiple")) options.getBoolean("allowMultiple") else false
    val maxSelection = if (options.hasKey("maxSelection") && !options.isNull("maxSelection")) options.getInt("maxSelection") else -1
    val checkboxTintColor = if (options.hasKey("checkboxTintColor") && !options.isNull("checkboxTintColor")) options.getString("checkboxTintColor") else null

    val intent = Intent(currentActivity, ImagePickerActivity::class.java).apply {
      putExtra("allow_multiple", allowMultiple)
      // 음수면 무제한으로 해석; Activity 쪽에서 처리
      putExtra("max_selection", maxSelection)
      if (checkboxTintColor != null) {
        putExtra("checkbox_tint_color", checkboxTintColor)
      }
    }
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
        // 취소되었을 때는 빈 배열로 resolve (JS 래퍼와 일치)
        val resultArray: WritableArray = Arguments.createArray()
        pickerPromise?.resolve(resultArray)
      }
      pickerPromise = null
    }
  }

  override fun onNewIntent(p0: Intent?) {
    TODO("Not yet implemented")
  }
}
