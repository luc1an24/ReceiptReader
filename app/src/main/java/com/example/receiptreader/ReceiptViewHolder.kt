package com.example.receiptreader

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptreader.vao.Receipt
import kotlinx.android.synthetic.main.item_receipt.view.*
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide

class ReceiptViewHolder(view: View) : RecyclerView.ViewHolder(view){
    private val picture = view.receiptImage
    private val shop = view.shop
    private val cost = view.cost
    //private val holder = view.receiptItemHolder

    fun bind(receipt: Receipt, context: Context){
        var picData = receipt.picture
        //val bitmap = BitmapFactory.decodeByteArray(picData, 0, picData.size)

        //picture.setImageBitmap(bitmap)

        Glide.with(itemView)
            .load(picData)
            .into(picture)
        shop.text = receipt.shop
        cost.text = receipt.cost.toString()
        /*holder.setOnClickListener {
            Toast.makeText(
                context,
                "Kliknil si na: ${weather.tempFrom} ${weather.timeStamp}",
                Toast.LENGTH_SHORT
            ).show()
        }*/
    }
}