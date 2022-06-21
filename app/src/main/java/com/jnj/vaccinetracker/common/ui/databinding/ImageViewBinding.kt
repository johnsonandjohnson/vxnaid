package com.jnj.vaccinetracker.common.ui.databinding

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel

/**
 * @author maartenvangiel
 * @version 1
 */
object ImageViewBinding {

    @JvmStatic
    @BindingAdapter("imageUrl", "placeHolder")
    fun bindAsyncImage(imageView: ImageView, url: String?, placeHolderResource: Int) {
        if (url == null) {
            loadPlaceholder(imageView, placeHolderResource)
            return
        }
        var builder = Glide.with(imageView)
            .load(url)

        if (placeHolderResource == View.NO_ID) {
            builder = builder.placeholder(imageView.drawable)
        } else if (placeHolderResource != 0) {
            builder = builder.placeholder(placeHolderResource)
        }

        imageView.setImageDrawable(null)
        builder.into(imageView)
    }

    @JvmStatic
    private fun loadPlaceholder(imageView: ImageView, placeHolderResource: Int) {
        if (placeHolderResource == 0 || placeHolderResource == View.NO_ID) {
            imageView.setImageDrawable(null)
        } else {
            imageView.setImageResource(placeHolderResource)
        }
    }

    @JvmStatic
    @BindingAdapter("imageByteArray")
    fun bindImageByteArray(imageView: ImageView, image: ByteArray?) {
        if (image == null) {
            Glide.with(imageView)
                .clear(imageView)
            imageView.setImageDrawable(null)
        }

        Glide.with(imageView)
            .load(image)
            .into(imageView)
    }

    @JvmStatic
    @BindingAdapter("imageUiModel")
    fun bindImageByteArray(imageView: ImageView, image: ParticipantImageUiModel?) {
        bindImageByteArray(imageView, image?.byteArray)
    }

}