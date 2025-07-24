package com.rocateerimagepicker

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(
  private val items: List<Uri>,
  private val selectedItems: Set<Uri>,
  private val onCameraClick: () -> Unit,
  private val onImageClick: (Uri) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  companion object {
    private const val VIEW_TYPE_CAMERA = 0
    private const val VIEW_TYPE_IMAGE = 1
  }

  override fun getItemCount() = items.size + 1  // +1 for camera button

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) VIEW_TYPE_CAMERA else VIEW_TYPE_IMAGE
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return if (viewType == VIEW_TYPE_CAMERA) {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_camera, parent, false)
      CameraViewHolder(view, onCameraClick)
    } else {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
      ImageViewHolder(view, onImageClick)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (holder is ImageViewHolder) {
      val uri = items[position - 1]
      holder.bind(uri, selectedItems.contains(uri))
    }
  }

  class CameraViewHolder(itemView: View, onCameraClick: () -> Unit) :
    RecyclerView.ViewHolder(itemView) {
    init {
      itemView.setOnClickListener { onCameraClick() }
    }
  }

  class ImageViewHolder(itemView: View, private val onImageClick: (Uri) -> Unit) :
    RecyclerView.ViewHolder(itemView) {
    private val imageView: AppCompatImageView = itemView.findViewById(R.id.image_view)
    private val overlay: View = itemView.findViewById(R.id.selection_View) // 반투명 오버레이뷰
    private val imageCheck: AppCompatImageView = itemView.findViewById(R.id.image_check)
    fun bind(uri: Uri, isSelected: Boolean) {
      Glide.with(imageView.context)
        .load(uri)
        .centerCrop()
        .into(imageView)

      overlay.isVisible = isSelected
      imageCheck.isVisible = isSelected

      itemView.setOnClickListener { onImageClick(uri) }
    }
  }

}
