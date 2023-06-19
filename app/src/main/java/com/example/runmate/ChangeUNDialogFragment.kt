package com.example.runmate

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

interface OnUsernameChangedListener { // Listener interface for username change
    fun onUsernameChanged(newUsername: String)
}

/*
This class displays a Dialog window for the username change.
Instantiated when user clicks on username button
 */
class ChangeUNDialogFragment : DialogFragment() {

    private var usernameChangedListener: OnUsernameChangedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nuovo username")

        val rootView = requireActivity().layoutInflater.inflate(R.layout.username_dialog, null)
        val usernameEditText = rootView.findViewById<EditText>(R.id.editText)
        builder.setView(rootView)

        builder.setPositiveButton("Conferma") { _, _ ->
            val userId = Firebase.auth.currentUser?.uid
            val newUsername = usernameEditText.text.toString()
            if (newUsername.isNotBlank()) {
                val sPref = requireContext().getSharedPreferences(
                    "${userId}UserPrefs",
                    Context.MODE_PRIVATE
                )
                val editor = sPref.edit()
                editor.putString("username", newUsername)
                editor.apply()

                Toast.makeText(context, "Username modificato correttamente", Toast.LENGTH_SHORT).show()

                //callback for changing the username message in the UI
                usernameChangedListener?.onUsernameChanged(newUsername)
            }
            else{
                Toast.makeText(context, "Username non accettato", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancella", null)

        return builder.create()
    }

    /*Set the username change listener. The listener will be the fragment that implements the interface
      OnUsernameChangedListener
    */
    fun setOnUsernameChangedListener(listener: OnUsernameChangedListener) {
        usernameChangedListener = listener
    }
}
