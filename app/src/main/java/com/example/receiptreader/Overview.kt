package com.example.receiptreader

import OnSwipeTouchListener
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.Slide
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptreader.vao.Receipt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import kotlinx.android.synthetic.main.activity_overview.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import java.text.SimpleDateFormat as SimpleDateFormat1

class Overview : AppCompatActivity() {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var filteredViewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var allWebReceipts : ArrayList<Receipt> = ArrayList()
    private var filteredWebReceipts : ArrayList<Receipt> = ArrayList()

    private lateinit var auth: FirebaseAuth
    private lateinit var currUser: FirebaseUser
    private lateinit var storage: FirebaseStorage

    private var numberOfReceipts: Int = 0

    private val TAG = "OVERVIEW"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        viewManager = LinearLayoutManager(this)
        viewAdapter = OverviewListAdapter(allWebReceipts, this)
        filteredViewAdapter = OverviewListAdapter(filteredWebReceipts, this)

        rv_overview.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        rv_overview.setOnTouchListener(object : OnSwipeTouchListener(this@Overview){
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.i(TAG, "Change of activity to Profile")
                val intent = Intent(this@Overview, Profile::class.java)
                intent.putExtra("numReceipts", numberOfReceipts)
                startActivity(intent)
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.i(TAG, "Change of activity to FirstScreen")
                val intent = Intent(this@Overview, FirstScreen::class.java)
                startActivity(intent)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        currUser = auth.currentUser!!

        val userId = currUser.uid
        var listRef = storage.reference.child("pictures/$userId")
        listRef.listAll().addOnSuccessListener { listResult ->
            numberOfReceipts = listResult.items.size
            convertDataToReceipt(listResult)
        }
    }

    private fun convertDataToReceipt(results: ListResult){
        results.items.forEach { item ->
            var count = 4

            var pic = ByteArray(1)
            //var lat = 0.0
            //var lon = 0.0
            var shopName = ""
            var totalCost = 0.0
            var date : Long = 0
            var readOk : Boolean = false

            val TEN_MEGABYTES: Long = 1024 * 1024 * 10
            item.getBytes(TEN_MEGABYTES).addOnSuccessListener {
                pic = it
                count--
                if(count == 0){
                    addItemToCollection(pic, shopName, totalCost, date, readOk)
                }
            }

            item.metadata.addOnSuccessListener {
                Log.i(TAG, "metadata jej $it")
                //var cal = Calendar.getInstance()
                //cal.timeInMillis = it.creationTimeMillis
                date = it.creationTimeMillis

                it.customMetadataKeys.forEach { key ->
                    var metaValue = it.getCustomMetadata(key)
                   when(key){
                       //"latitude" -> lat = metaValue!!.toDouble()
                       //"longitude" -> lon = metaValue!!.toDouble()
                       "shopName" -> shopName = metaValue!!.toString()
                       "price" -> totalCost = metaValue!!.toDouble()
                       "readOk" -> readOk = metaValue!!.toBoolean()
                   }
                    Log.i(TAG, "Received metadata: $key -> $metaValue")
                    count--
                    if(count == 0){
                        addItemToCollection(pic, shopName, totalCost, date, readOk)
                    }
                }
            }
        }
    }

    private fun addItemToCollection(pic: ByteArray, shopName: String, totalCost: Double, date: Long, ok: Boolean){
        val receiptItem = Receipt(pic, shopName, totalCost, 0.0, 0.0, date, ok)
        allWebReceipts.add(receiptItem)
        viewAdapter.notifyDataSetChanged()
        rv_overview.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    fun btn_apply_clicked(view: View) {
        val filterText = txt_filter.text
        if(filterText.isNullOrEmpty()){
            rv_overview.apply {
                layoutManager = viewManager
                adapter = viewAdapter
            }
        }else{
            filteredWebReceipts.clear()

            val splittedText = filterText.split(" ")
            if(splittedText.count() != 2){
                txt_filter.error = "Only command and option are allowed."
                return
            }
            val command = splittedText[0]
            val option = splittedText[1]
            when(command){
                "name" -> {
                    allWebReceipts.forEach{receipt ->
                        if(receipt.shop.toLowerCase().contains(option.toLowerCase())){
                            filteredWebReceipts.add(receipt)
                        }
                    }
                    rv_overview.apply {
                        layoutManager = viewManager
                        adapter = filteredViewAdapter
                    }
                }
                "cost" -> {
                    allWebReceipts.forEach{
                        if(option.toLowerCase() == "low"){
                            allWebReceipts.sortBy { it.cost }
                        }else if(option.toLowerCase() == "high"){
                            allWebReceipts.sortByDescending { it.cost }
                        }
                    }
                    rv_overview.apply {
                        layoutManager = viewManager
                        adapter = viewAdapter
                    }
                }
                "date" -> {
                    allWebReceipts.forEach{
                        if(option.toLowerCase() == "old"){
                            allWebReceipts.sortBy { it.timestamp }
                        }else if(option.toLowerCase() == "new"){
                            allWebReceipts.sortByDescending { it.timestamp }
                        }
                    }
                    rv_overview.apply {
                        layoutManager = viewManager
                        adapter = viewAdapter
                    }
                }
            }

        }
    }

}
