package com.example.runmate

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File

// Class of the fragment of the Profile section
class UserFragment : Fragment(R.layout.fragment_user), OnUsernameChangedListener { //implements the listener in order to change the UI when username changes

    private lateinit var logoutBtn: Button
    private lateinit var usernameBtn: Button
    private lateinit var targetBtn: Button
    private lateinit var deleteAccountBtn: Button
    private val currentUser = Firebase.auth.currentUser
    private val uid = currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)
        val user_view = view.findViewById<TextView>(R.id.user_view)
        logoutBtn = view.findViewById(R.id.btn_logout)
        usernameBtn = view.findViewById(R.id.btn_username)
        targetBtn = view.findViewById(R.id.btn_target)

        // Retrieve username from SharedPreferences and display it in the user view
        val sPref = requireContext().getSharedPreferences("${uid}UserPrefs", Context.MODE_PRIVATE)
        val username = sPref.getString("username", "")
        user_view.text = "Ciao $username"

        deleteAccountBtn = view.findViewById(R.id.btn_delete_account)
        deleteAccountBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        targetBtn.setOnClickListener {
            val intent = Intent(activity, TargetActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        usernameBtn.setOnClickListener {
            val dialog = ChangeUNDialogFragment()
            dialog.setOnUsernameChangedListener(this) // Set the listener to this fragment
            dialog.show(parentFragmentManager, "change username dialog")
        }

        logoutBtn.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(activity, Login::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    /*
       implementation of OnUsernameChangedListener interface.
       This callback is called when username changes in order to update the UI
    */
    override fun onUsernameChanged(newUsername: String) {
        val userView = view?.findViewById<TextView>(R.id.user_view)
        userView?.text = "Ciao $newUsername"
    }

    // Display a confirmation dialog for account deletion
    private fun showDeleteConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Conferma eliminazione account")
        dialogBuilder.setMessage("Sei sicuro di voler eliminare il tuo account?")
        dialogBuilder.setPositiveButton("Conferma") { _, _ ->
            reauthenticateUser()
        }
        dialogBuilder.setNegativeButton("Annulla", null)
        dialogBuilder.show()
    }

    // Re-authenticate the user before deleting the account
    private fun reauthenticateUser() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Reinserisci le credenziali")
        dialogBuilder.setMessage("Per eliminare l'account, per favore digita la password")

        val rootView = requireActivity().layoutInflater.inflate(R.layout.delete_dialog, null)
        val passwordEditText = rootView.findViewById<EditText>(R.id.passwordEditText)
        dialogBuilder.setView(rootView)

        dialogBuilder.setPositiveButton("Conferma") { _, _ ->
            val password = passwordEditText.text.toString()

            if (password.isNotBlank()) {
                val credentials = EmailAuthProvider.getCredential(currentUser?.email!!, password)

                currentUser.reauthenticate(credentials)
                    ?.addOnSuccessListener {
                        deleteAccount()
                    }
                    ?.addOnFailureListener { exception ->
                        Log.e(TAG, "Errore durante la re-autenticazione", exception)
                        Toast.makeText(requireContext(), "Password errata", Toast.LENGTH_LONG).show()
                    }
            }
            else{
                Toast.makeText(requireContext(), "Password errata", Toast.LENGTH_LONG).show()
            }
        }

        dialogBuilder.setNegativeButton("Annulla", null)
        dialogBuilder.show()
    }

    // Delete the account and associated user data
    private fun deleteAccount() {
        currentUser?.delete()
            ?.addOnSuccessListener {
                deleteUserDataFromDatabase()
                navigateToLogin()
            }
            ?.addOnFailureListener { exception ->
                Log.e(TAG, "Errore durante l'eliminazione dell'account", exception)
            }
    }

    // Delete user data from the database
    private fun deleteUserDataFromDatabase() {
        // Delete local data stored in SharedPreferences
        val filename = "${uid}UserPrefs"
        val spFile = File(requireContext().applicationContext.filesDir.parent, "shared_prefs/$filename.xml")
        spFile.delete()
    }

    // Navigate to the login screen
    private fun navigateToLogin() {
        val intent = Intent(activity, Login::class.java)
        startActivity(intent)
        activity?.finish()
    }
}
