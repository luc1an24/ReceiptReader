package com.example.receiptreader

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptreader.vao.Receipt

class OverviewListAdapter(var receiptList: ArrayList<Receipt>, var context: Context): RecyclerView.Adapter<ReceiptViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)= ReceiptViewHolder (
        LayoutInflater.from(parent.context).inflate(R.layout.item_receipt, parent, false)
    )

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(receiptList[position], context)
    }

    override fun getItemCount(): Int = receiptList.size
}