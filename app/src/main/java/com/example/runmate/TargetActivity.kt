package com.example.runmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.runmate.utils.setUserProperties
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth

// Class of the activity used to set user info
class TargetActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_target)

        val confirm_btn = findViewById<Button>(R.id.confirm_target_button)
        val reset_btn = findViewById<Button>(R.id.reset_target_button)
        val height_edit = findViewById<EditText>(R.id.editTargetHeight)
        val weight_edit = findViewById<EditText>(R.id.editTargetWeight)
        val steps_edit = findViewById<EditText>(R.id.editTargetSteps)
        val kcal_edit = findViewById<EditText>(R.id.editTargetKcal)
        val meters_edit = findViewById<EditText>(R.id.editTargetMeter)
        val gender = findViewById<RadioGroup>(R.id.editGender)
        var check = 0

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val sharedPreferences = getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)

        //If there are any saved data, they are loaded.
        //Otherwise, they are set to 0 (gender field default value is "Male")

        height_edit.setText(sharedPreferences.getInt("Height",0).toString())
        weight_edit.setText(sharedPreferences.getInt("Weight",0).toString())
        steps_edit.setText(sharedPreferences.getInt("Steps",0).toString())
        kcal_edit.setText(sharedPreferences.getInt("Calories",0).toString())
        meters_edit.setText(sharedPreferences.getInt("Meters",0).toString())
        gender.check(R.id.male)
        val g = sharedPreferences.getString("Gender", "")
        if(g == "" || g == "Male") gender.check(R.id.male)
        else gender.check(R.id.female)

        //Reset button sets each field to 0 and resets gender field

        reset_btn.setOnClickListener {
            height_edit.setText("0")
            weight_edit.setText("0")
            steps_edit.setText("0")
            kcal_edit.setText("0")
            meters_edit.setText("0")
            gender.clearCheck()
        }

        //Character-by-character field validation

        //Height
        height_edit.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?)
            {
                val value = s.toString().toIntOrNull()
                if(value !=null && s != null && value > 350)
                {
                    //Notify the user, then block any value that would exceed the limit
                    Toast.makeText(applicationContext, "Altezza non valida, inserisci un valore tra 1 e 350", Toast.LENGTH_SHORT).show()
                    height_edit.text =(s.subSequence(0,s.length-1) as Editable)
                    height_edit.setSelection(s.length-1)
                }
            }
        })

        //Weight
        weight_edit.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?)
            {
                val value = s.toString().toIntOrNull()
                if(value !=null && s != null && value > 350)
                {
                    Toast.makeText(applicationContext, "Peso non valido, inserisci un valore tra 1 e 350", Toast.LENGTH_SHORT).show()
                    weight_edit.text =(s.subSequence(0,s.length-1) as Editable)
                    weight_edit.setSelection(s.length-1)
                }
            }
        })

        //Steps per day
        steps_edit.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?)
            {
                val value = s.toString().toIntOrNull()
                if(value !=null && s != null && value > 50000)
                {
                    Toast.makeText(applicationContext, "Numero di passi non valido, inserisci un valore tra 1 e 50000", Toast.LENGTH_SHORT).show()
                    steps_edit.text =(s.subSequence(0,s.length-1) as Editable)
                    steps_edit.setSelection(s.length-1)
                }
            }
        })

        //Calories per day
        kcal_edit.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?)
            {
                val value = s.toString().toIntOrNull()
                if(value !=null && s != null && value > 5000)
                {
                    Toast.makeText(applicationContext, "Numero di calorie non valido, inserisci un valore tra 1 e 5000", Toast.LENGTH_SHORT).show()
                    kcal_edit.text =(s.subSequence(0,s.length-1) as Editable)
                    kcal_edit.setSelection(s.length-1)
                }
            }
        })

        //Meters per day
        meters_edit.addTextChangedListener(object : TextWatcher
        {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?)
            {
                val value = s.toString().toIntOrNull()
                if(value !=null && s != null && value > 30000)
                {
                    Toast.makeText(applicationContext, "Numero di metri non valido, inserisci un valore tra 1 e 30000", Toast.LENGTH_SHORT).show()
                    meters_edit.text =(s.subSequence(0,s.length-1) as Editable)
                    meters_edit.setSelection(s.length-1)
                }
            }
        })

        //When the 'Confirm' button is pressed, each field is checked.
        //If a field is empty or equal to 0, it will be flagged, and the user will be prompted to enter a valid value.
        //Otherwise, the values are saved, and the user is redirected back to the initial page.

        confirm_btn.setOnClickListener {

            //Empty field check
            if(height_edit.text.isEmpty() || weight_edit.text.isEmpty() || steps_edit.text.isEmpty() || kcal_edit.text.isEmpty() || meters_edit.text.isEmpty() || (gender.checkedRadioButtonId != R.id.male && gender.checkedRadioButtonId != R.id.female))
                check=6
            else{
                //Check for field equal to 0
                if(Integer.parseInt(meters_edit.text.toString()) == 0)
                    check=1
                if(Integer.parseInt(kcal_edit.text.toString()) == 0)
                    check=2
                if(Integer.parseInt(steps_edit.text.toString()) == 0)
                    check=3
                if(Integer.parseInt(weight_edit.text.toString()) == 0)
                    check=4
                if(Integer.parseInt(height_edit.text.toString()) == 0)
                    check=5
            }

            when(check){
                0 -> {
                    val height = Integer.parseInt(height_edit.text.toString())
                    val weight = Integer.parseInt(weight_edit.text.toString())
                    val steps = Integer.parseInt(steps_edit.text.toString())
                    val calories = Integer.parseInt(kcal_edit.text.toString())
                    val meters = Integer.parseInt(meters_edit.text.toString())

                    val editor = sharedPreferences.edit()
                    editor.putInt("Height", height)
                    editor.putInt("Weight", weight)
                    editor.putInt("Steps", steps)
                    editor.putInt("Calories", calories)
                    editor.putInt("Meters", meters)


                    when (gender.checkedRadioButtonId) {
                        R.id.male -> editor.putString("Gender", "Male")
                        R.id.female -> editor.putString("Gender", "Female")
                    }
                    editor.apply()
                    Toast.makeText(this, "Campi modificati correttamente", Toast.LENGTH_SHORT).show()
                    val analytics = FirebaseAnalytics.getInstance(applicationContext)
                    setUserProperties(applicationContext, analytics)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                1 -> Toast.makeText(this, "Numero di metri non valido, inserisci un valore tra 1 e 30000", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(this, "Numero di calorie non valido, inserisci un valore tra 1 e 5000", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "Numero di passi non valido, inserisci un valore tra 1 e 50000", Toast.LENGTH_SHORT).show()
                4 -> Toast.makeText(this, "Peso non valido, inserisci un valore tra 1 e 350", Toast.LENGTH_SHORT).show()
                5 -> Toast.makeText(this, "Altezza non valida, inserisci un valore tra 1 e 350", Toast.LENGTH_SHORT).show()
                6 -> Toast.makeText(this, "Inserisci tutti i campi", Toast.LENGTH_SHORT).show()
            }

            check = 0
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