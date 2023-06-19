package com.example.runmate

import android.content.Context
import com.google.firebase.perf.FirebasePerformance
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Registration : AppCompatActivity() {

    private lateinit var editUsername: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var register_btn: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var back_btn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        mAuth = Firebase.auth
        editUsername = findViewById(R.id.username)
        editEmail = findViewById(R.id.email)
        editPassword = findViewById(R.id.password)
        editConfirmPassword = findViewById(R.id.confirm_password)
        register_btn = findViewById(R.id.btn_register)
        back_btn = findViewById(R.id.btn_back)

        /*get instance of Firebase Performance object. Used to trace performance of code snippets.
          Similar to @addTrace annotation but used to trace code snippets that are not necessarily a function
         */
        val fbPer = FirebasePerformance.getInstance()
        val regTrace = fbPer.newTrace("user_registration_trace")

        //Go back to Login page.

        back_btn.setOnClickListener {
            intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        }

        //If every field is correctly compiled, an account will be created.
        //Thus the user will be redirected to TargetActivity.

        register_btn.setOnClickListener {
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()
            val username = editUsername.text.toString()

            if(username.isNullOrBlank()){
                Toast.makeText(this, "Inserisci un nome utente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (username.length > 30){
                Toast.makeText(this, "Il nome utente può contenere al massimo 30 caratteri", Toast.LENGTH_SHORT).show()
            }
            if (email.isNullOrBlank()) {
                Toast.makeText(this, "Inserisci la tua email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isNullOrBlank()) {
                Toast.makeText(this, "Inserisci la password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isNullOrBlank()) {
                Toast.makeText(this, "Conferma la password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "La password non corrisponde", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if(password.length < 6) {
                Toast.makeText(this, "La password deve contenere almeno 6 caratteri", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            else {
                regTrace.start() //Trace the time needed by the (async) call to Firebase authentication
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this)
                { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        val uid = user!!.uid
                        saveUserData(uid, username, email)

                        Toast.makeText(this, "Benvenuto in Runmate!", Toast.LENGTH_SHORT).show()

                        intent = Intent(applicationContext, TargetActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else Toast.makeText(
                        baseContext,
                        "Registrazione fallita.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                regTrace.stop()
            }

            // overrides default back button behavior
            val onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    return
                }
            }
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }
    }

    /*
    Save user account into preferences.
     */
    private fun saveUserData( uid: String, username : String, email : String){
        val sPref = getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)
        val editor = sPref.edit()
        editor.putString("username", username)
        editor.putString("email", email)
        editor.putString("uid", uid)
        editor.apply()
    }
}