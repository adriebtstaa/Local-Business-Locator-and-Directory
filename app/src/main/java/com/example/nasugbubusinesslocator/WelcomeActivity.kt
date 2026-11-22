package com.example.nasugbubusinesslocator

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nasugbubusinesslocator.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WelcomeActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        database = AppDatabase.getDatabase(this)

        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            showRoleDialog()
        }
    }

    private fun showRoleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_role_selection, null)
        val btnOwner = dialogView.findViewById<Button>(R.id.btnRoleOwner)
        val btnUser = dialogView.findViewById<Button>(R.id.btnRoleUser)
        val btnGuest = dialogView.findViewById<Button>(R.id.btnRoleGuest)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnOwner.setOnClickListener {
            dialog.dismiss()
            showBusinessOwnerRegistrationDialog()
        }

        btnUser.setOnClickListener {
            dialog.dismiss()
            showUserSignInDialog()
        }

        btnGuest.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Welcome Guest Explorer!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("USER_TYPE", "GUEST")
                putExtra("USER_NAME", "Guest Explorer")
                putExtra("USER_EMAIL", "guest@gmail.com")
            }
            startActivity(intent)
        }

        dialog.show()
    }

    private fun showUserSignInDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(context).apply { hint = "Full Name" }
        val emailInput = EditText(context).apply { hint = "Email or Contact Details" }

        layout.addView(nameInput)
        layout.addView(emailInput)

        AlertDialog.Builder(context)
            .setTitle("User Sign In")
            .setView(layout)
            .setPositiveButton("Explore Now") { _, _ ->
                val name = nameInput.text.toString().trim()
                val email = emailInput.text.toString().trim()

                if (name.isNotEmpty() && email.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.userDao().insertUser(
                            UserEntity(email = email, name = name, password = "", role = "USER")
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Welcome aboard, $name!", Toast.LENGTH_LONG).show()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra("USER_TYPE", "USER")
                                putExtra("USER_NAME", name)
                                putExtra("USER_EMAIL", email)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please enter your name and email/contact", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBusinessOwnerRegistrationDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val ownerNameInput = EditText(context).apply { hint = "Owner Name" }
        val emailInput = EditText(context).apply { hint = "Email or Contact" }
        val bizNameInput = EditText(context).apply { hint = "Business Name (e.g. Sunset Cove Resort)" }

        val categoryLabel = TextView(context).apply { text = "Select Business Category:"; setPadding(0, 10, 0, 0) }
        val categorySpinner = Spinner(context).apply {
            val categories = arrayOf("Resort", "Food", "Shop", "Cafe", "Pharmacy")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
        }

        val addressInput = EditText(context).apply { hint = "Address / Location (e.g. Brgy. Bucana)" }
        val phoneInput = EditText(context).apply { hint = "Phone Number" }

        layout.addView(ownerNameInput)
        layout.addView(emailInput)
        layout.addView(bizNameInput)
        layout.addView(categoryLabel)
        layout.addView(categorySpinner)
        layout.addView(addressInput)
        layout.addView(phoneInput)

        AlertDialog.Builder(context)
            .setTitle("Business Owner Registration")
            .setView(layout)
            .setPositiveButton("Register & Publish") { _, _ ->
                val ownerName = ownerNameInput.text.toString().trim()
                val email = emailInput.text.toString().trim()
                val bizName = bizNameInput.text.toString().trim()
                val category = categorySpinner.selectedItem.toString()
                val address = addressInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()

                if (ownerName.isNotEmpty() && email.isNotEmpty() && bizName.isNotEmpty() && address.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.userDao().insertUser(
                            UserEntity(email = email, name = ownerName, password = "", role = "Business Owner", businessNameOwned = bizName)
                        )
                        val newId = (System.currentTimeMillis() % 100000).toInt() + 200
                        val entity = BusinessEntity(
                            id = newId,
                            name = bizName,
                            ownerName = ownerName,
                            address = address,
                            barangay = "Nasugbu",
                            category = category,
                            phone = phone.ifEmpty { "N/A" },
                            imageName = "",
                            latitude = 14.0700 + (Math.random() * 0.02 - 0.01),
                            longitude = 120.6300 + (Math.random() * 0.02 - 0.01),
                            photosString = "",
                            hasBeachAccess = category == "Resort",
                            entranceFee = if (category == "Resort") 100.0 else 0.0,
                            cuisineType = if (category == "Food") "Local" else "",
                            openingHours = if (category == "Shop") "8AM - 8PM" else "",
                            specialty = if (category == "Cafe") "Coffee" else "",
                            hasWifi = category == "Cafe",
                            is24Hours = category == "Pharmacy"
                        )
                        database.businessDao().insertBusinesses(listOf(entity))

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Success! Business '$bizName' is now live!", Toast.LENGTH_LONG).show()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra("USER_TYPE", "OWNER")
                                putExtra("USER_NAME", ownerName)
                                putExtra("USER_EMAIL", email)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
