package com.kimjiun.ch21_firebase_db_fcm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.kimjiun.ch21_firebase_db_fcm.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    val TAG = "JIUNKIM"
    lateinit var db:FirebaseFirestore
    lateinit var storage: FirebaseStorage
    lateinit var binding: ActivityMainBinding

    val stroageUrl = "gs://kotlinprojects-f103e.appspot.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase FireStore
        db = FirebaseFirestore.getInstance()

        //insertFireStoreData()
        //getFireStoreData()

        // Firebase Storage
        storage = Firebase.storage
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //uploadFile()
        //downloadFile()
    }

    fun downloadFile(){
        val storageRef: StorageReference = storage.reference
        val imgRef: StorageReference = storageRef.child("images/a.jpg") // ????????? ????????? ????????? ????????? ??????????????? ????????????
        val imgRef2: StorageReference = storageRef.child("images/b.jpg")

        val ONE_MEGABYTE: Long = 1024 * 1024 // ???????????? ?????? ????????? ???

        // ????????? ????????????
        imgRef.getBytes(ONE_MEGABYTE)
            .addOnSuccessListener {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                //binding.imagePic2.setImageBitmap(bitmap)
            }
            .addOnFailureListener { Log.d(TAG, "DOWNLOAD FAIL") }

        // ?????? ????????????
        val localFile = File.createTempFile("images", "jpg")
        imgRef2.getFile(localFile)
            .addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                //binding.imagePic2.setImageBitmap(bitmap)
            }
            .addOnFailureListener { Log.d(TAG, "DOWNLOAD FAIL") }

        // downloadUrl ??? URL ??????
        imgRef2.downloadUrl
            .addOnSuccessListener {
                // firebase-ui-storage
                Glide.with(this)
                    .load(it)
                    .into(binding.imagePic2)
            }
            .addOnFailureListener { Log.d(TAG, "DOWNLOAD URL FAIL") }

    }

    @GlideModule
    class MyAppGlideModule: AppGlideModule(){
        override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
            registry.append(
                StorageReference::class.java, InputStream::class.java, FirebaseImageLoader.Factory()
            )
        }
    }

    fun uploadFile(){
        val storageRef: StorageReference = storage.reference
        val imgRef: StorageReference = storageRef.child("images/a.jpg") // ????????? ????????? ????????? ????????? ??????????????? ????????????
        val imgRef2: StorageReference = storageRef.child("images/b.jpg")
        val imgRef3: StorageReference = storageRef.child("images/c.jpg")

        // putBytes() - ???????????? ??????
        var bitmap = getBitmapFromView(binding.imagePic)
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        var uploadTask = imgRef.putBytes(data)
        uploadTask.addOnFailureListener { Log.d(TAG, "UPLOAD FAIL") }
            .addOnCompleteListener { Log.d(TAG, "UPLOAD SUCCESS") }

        // ???????????? ????????? URL ??????
        val urlTask = uploadTask.continueWithTask { task ->
            if(!task.isSuccessful){
                task.exception?.let{
                    throw it
                }
            }
            imgRef.downloadUrl
        }.addOnCompleteListener {  task ->
            if(task.isSuccessful){
                val downloadUri = task.result
                Log.d(TAG, "UPLOAD URL : ${downloadUri}")
            }
            else{
                Log.d(TAG, "UPLOAD URL FAIL")
            }
        }

        // putStream() ????????? ????????????
        val stream = FileInputStream(File(filesDir,"33.jpeg"))
        val uploadTask2 = imgRef2.putStream(stream).addOnFailureListener { Log.d(TAG, "UPLOAD2 FAIL") }
            .addOnCompleteListener { Log.d(TAG, "UPLOAD2 SUCCESS") }

        // putFile() ????????? ????????????
        val file = Uri.fromFile(File(filesDir,"33.jpeg"))
        val uploadTask3 = imgRef3.putFile(file).addOnFailureListener { Log.d(TAG, "UPLOAD3 FAIL") }
            .addOnCompleteListener { Log.d(TAG, "UPLOAD3 SUCCESS") }

        // ?????? ??????
        imgRef3.delete().addOnFailureListener { Log.d(TAG, "DELETE FAIL") }
            .addOnCompleteListener { Log.d(TAG, "DELETE SUCCESS") }
    }

    // ?????? ????????? ??????
    fun getBitmapFromView(view: View): Bitmap?{
        var bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun getFireStoreData(){
        // get() ????????? ???????????? ?????? ?????? ????????????
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    Log.d(TAG, "${doc.id} : ${doc.data}")
                }
            }
            .addOnFailureListener { e -> e.printStackTrace() }

        // ?????? ?????? ????????????
        val docRef = db.collection("users").document("ID01")
        docRef.get()
            .addOnSuccessListener { doc ->
                // ????????? ????????? ????????????
                val selectUser = doc.toObject(User2::class.java)
                Log.d(TAG, "OBJECT : ${selectUser?.name}, ${selectUser?.email}, ${selectUser?.isTop}, ${selectUser?.avg} ${selectUser?.isAdmin}")
            }
            .addOnFailureListener { e -> e.printStackTrace() }


        // whereXXX() ????????? ?????? ??????
        db.collection("users")
            .whereEqualTo("name", "kkang2")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    Log.d(TAG, "where : ${doc.id} : ${doc.data}")
                }
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    fun insertFireStoreData(){
        val user = mapOf("name" to "kkang", "email" to "a@a.com", "avg" to 10)
        val user2 = mapOf("name" to "kimjiun", "email" to "b@b.com", "avg" to 20)

        val colRef: CollectionReference = db.collection("users")
        val docRef: Task<DocumentReference> = colRef.add(user)
        docRef.addOnSuccessListener { documentRef -> Log.d(TAG, "ADD SUCCESS : ${documentRef.id}") }
        docRef.addOnFailureListener{ e -> Log.d(TAG, "ADD FAIL : ${e}") }

        db.collection("users")
            .add(user2)
            .addOnSuccessListener { documentRef -> Log.d(TAG, "ADD SUCCESS : ${documentRef.id}") }
            .addOnFailureListener{ e -> Log.d(TAG, "ADD FAIL : ${e}") }

        // ?????? ??????
        val user3 = User("Lee", "lee@k.com", 20, true, true)
        db.collection("users")
            .add(user3)

        // set() ????????? ????????? ??????
        val user4 = User("Park", "park@p.com", 40, false, false)
        db.collection("users")
            .document("ID01")
            .set(user4)

        // ??????????????? ??????
        db.collection("users")
            .document("ID01")
            //.update("email", "hello@k.com") // ?????? ?????????
            //.update(mapOf("email" to "hello2@k.com", "name" to "KIM")) // ?????????
            .update(mapOf("avg" to FieldValue.delete()))

    }

    class User(val name:String, val email:String, val avg:Int, @JvmField val isAdmin:Boolean, val isTop:Boolean)

    class User2{
        var name:String? = null
        var email:String? =null
        var avg:Int = 0
        var isAdmin:Boolean = false
        var isTop:Boolean = false
    }

}