package com.example.receiptreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "AUTH"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        if(auth.currentUser != null){
            updateUI(auth.currentUser)
        }

    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.i(TAG, "Change of activity to FirstScreen")
            val intent = Intent(this, FirstScreen::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Yikes", Toast.LENGTH_SHORT).show()
        }
    }

    fun btn_register_clicked(view: View) {
        if (!validateForm()) {
            return
        }

        val email = txt_mail.text.toString()

        val password = txt_password.text.toString()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful)
                {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                }
                else
                {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    fun btn_login_clicked(view: View) {
        if (!validateForm()) {
            return
        }

        val email = txt_mail.text.toString()
        val password = txt_password.text.toString()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful)
                {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                }
                else
                {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = txt_mail.text.toString()
        if (TextUtils.isEmpty(email)) {
            txt_mail.error = "Email and Password are required!"
            valid = false
        } else {
            txt_mail.error = null
        }

        val password = txt_password.text.toString()
        if (TextUtils.isEmpty(password)) {
            valid = false
        } else {
            txt_password.error = null
        }

        return valid
    }
}
