package com.example.runmate


import android.content.Context
import com.example.runmate.utils.*
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.metrics.AddTrace

class Login : AppCompatActivity() {

    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginscreen)

        mAuth = Firebase.auth
        editEmail = findViewById(R.id.email)
        editPassword = findViewById(R.id.password)
        loginBtn = findViewById(R.id.btn_login)

        //check if the user is already logged in
        if(checkCurrentUser(mAuth)) {
            Toast.makeText(baseContext, "Benvenuto", Toast.LENGTH_LONG).show()
            val analytics = FirebaseAnalytics.getInstance(applicationContext)
            setUserProperties(applicationContext, analytics) //set analytics user's parameters

            loadTargetActivity()
        }
        else loadlogin()
    }

    /*
    Load login view
    */
    private fun loadlogin(){

        registerBtn = findViewById(R.id.btn_register)
        registerBtn.setOnClickListener{ view ->
            val intent = Intent(view.context, Registration::class.java)
            startActivity(intent)
            finish()
        }

        loginBtn.setOnClickListener{
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            if(email.isNullOrBlank() || password.isNullOrBlank()){
                Toast.makeText(this, "Inserisci tutti i campi", Toast.LENGTH_SHORT).show()
            }
            else{

                //Sign in with Google Firebase Authentication
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(baseContext, "Benvenuto in Runmate!", Toast.LENGTH_SHORT).show()
                        // load analytics and set user properties
                        val analytics = FirebaseAnalytics.getInstance(applicationContext)
                        setUserProperties(applicationContext, analytics)

                        loadTargetActivity()
                    } else {
                        Toast.makeText(baseContext, "Email o password non corretti", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    /*
    load main activity. Annotation is used to trace the performance of the function call,
    including nested functions.
     */
    @AddTrace(name = "loadMainFromLoginTrace", enabled = true)
    private fun loadMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /*
     Load TargetActivity to allow the user to insert their data after the registration or
     after the first login if the application was previously uninstalled and the account not cancelled.
     */
    private fun loadTargetActivity(){
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val sharedPref = getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)

        //Go to the 'User Details' page if no details and goals were defined during the registration phase.
        if(sharedPref.getInt("Height",0) == 0)
        {
            val intent = Intent(applicationContext,TargetActivity::class.java)
            startActivity(intent)
        }
        else {
            loadMainActivity()
        }
    }
}