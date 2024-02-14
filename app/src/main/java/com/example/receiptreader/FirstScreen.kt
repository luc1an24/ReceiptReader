package com.example.receiptreader

import OnSwipeTouchListener
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.android.synthetic.main.activity_first_screen.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class FirstScreen : AppCompatActivity()  {

    private lateinit var auth: FirebaseAuth
    private lateinit var currUser: FirebaseUser
    private lateinit var storage: FirebaseStorage
    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    private val TAG = "PHOTO"

    private val PERMISSION_IMAGE_CODE = 1000
    private val REQUEST_IMAGE_CAPTURE = 1001
    private val PERMISSION_LOCATION_CODE = 1002

    private var latitude = 0.0
    private var longitude = 0.0
    private var image_uri: Uri? = null

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_screen)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btn_takePhoto.setOnClickListener {
            //if system os is Marshmallow or Above, we need to request runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkLocationPermissions()) {
                    if (isLocationEnabled()) {
                        mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                            val location: Location? = task.result
                            if (location == null) {
                                requestNewLocationData()
                            } else {
                                latitude = location.latitude
                                longitude = location.longitude
                            }
                        }
                    } else {
                        Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                }
                else{
                    requestLocationPermissions()
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                    //permission was not enabled
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_IMAGE_CODE)}
                else{
                    //permission already granted
                    openCamera()
                }
            }
            else{
                //system os is < marshmallow
                openCamera()
            }
        }

        btn_uploadPhoto.setOnClickListener {
            uploadPhoto()
        }

        firstScreen_ScrollView.setOnTouchListener(object : OnSwipeTouchListener(this@FirstScreen){
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.i(TAG, "Change of activity to Overview")
                val intent = Intent(this@FirstScreen, Overview::class.java)
                startActivity(intent)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        currUser = auth.currentUser!!
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            img_takenPic.setImageURI(image_uri)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when(requestCode){
            PERMISSION_IMAGE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG, "Permission for photo was granted")
                    //permission from popup was granted
                    openCamera()
                }
                else{
                    //permission from popup was denied
                    Log.i(TAG, "Permission for photo was denied")
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG, "Permission for locations was granted")
                    //permission from popup was granted
                    getLastLocation()
                }
                else{
                    //permission from popup was denied
                    Log.i(TAG, "Permission for locations was denied")
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openCamera() {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, currentTime)
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the ReceiptReader app")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun uploadPhoto() {
        if(img_takenPic.drawable == null){
            Toast.makeText(this, "Picture must be taken before uploading.", Toast.LENGTH_SHORT).show()
            return
        }
         val file : Uri = image_uri!!

        //put data through ml
        val mlFunctions = MlFunctions()

        val image : FirebaseVisionImage
        val detector = FirebaseVision.getInstance()
            .onDeviceTextRecognizer
        try {
            image = FirebaseVisionImage.fromFilePath(this, file)
            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    val meta = mlFunctions.processTextBlock(firebaseVisionText)
                    uploadImg(file, meta)
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun uploadImg(file: Uri, metadata: StorageMetadata){
        val userId = currUser.uid
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        val storageRef = storage.reference.child("pictures/$userId/$currentTime")

        val uploadTask = storageRef.putFile(file, metadata)

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener {
            Log.i(TAG, "Picture was not successfully uploaded!")
            Toast.makeText(this, "Picture was not successfully uploaded!", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {
            Log.i(TAG, "Picture was successfully uploaded!")
            Toast.makeText(this, "Picture was successfully uploaded.", Toast.LENGTH_SHORT).show()
            img_takenPic.setImageResource(android.R.color.transparent)
            loadingBarFirstScreen.visibility = View.GONE
        }.addOnProgressListener {
            loadingBarFirstScreen.visibility = View.VISIBLE
            val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
            println("Upload is $progress% done")
        }
    }


    private fun getLastLocation() {
        when (checkLocationPermissions()) {
            true -> {
                if (isLocationEnabled()) {

                    mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                        val location: Location? = task.result
                        if (location == null) {
                            requestNewLocationData()
                        } else {
                            latitude = location.latitude
                            longitude = location.longitude
                        }
                    }
                } else {
                    Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            }
            false -> {
                requestLocationPermissions()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0            //10000 ms
            fastestInterval = 0     //5000 ms
            numUpdates = 1
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            latitude = mLastLocation.latitude
            longitude = mLastLocation.longitude
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_LOCATION_CODE
        )
    }

    private fun checkLocationPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
}

