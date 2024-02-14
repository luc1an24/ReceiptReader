package com.example.receiptreader

import OnSwipeTouchListener
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_overview.*
import kotlinx.android.synthetic.main.activity_profile.*

class Profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var currUser: FirebaseUser
    private var numberOfReceipts: Int = 0

    private val TAG = "PROFILE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        auth = FirebaseAuth.getInstance()

        profileLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Profile){

            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.i(TAG, "Change of activity to Overview")
                val intent = Intent(this@Profile, Overview::class.java)
                startActivity(intent)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        currUser = auth.currentUser!!

        numberOfReceipts = intent.getIntExtra("numReceipts", 0)

        if(auth.currentUser != null){
            txt_profileMail.text = currUser.email
            txt_profileId.text = currUser.uid
            txt_profileNumReceipts.text = numberOfReceipts.toString()
        }
    }

    fun btn_logout_clicked(view: View) {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
