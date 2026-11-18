package com.example.nasugbubusinesslocator

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val startButton = findViewById<Button>(R.id.startButton)
        val subText = findViewById<TextView>(R.id.subText)

        // BFF, 2 SECONDS LOGO LANG, TAPOS LABAS SI START!
        Handler(Looper.getMainLooper()).postDelayed({
            subText.visibility = View.VISIBLE
            startButton.visibility = View.VISIBLE
        }, 2000)

        startButton.setOnClickListener {
            showRoleDialog()
        }
    }

    private fun showRoleDialog() {
        // BFF, ETO YUNG POP-UP! HINDI NA KAILANGAN NG IBANG XML FILE!
        val options = arrayOf("I'm a Business Owner", "I'm a User - Sign In", "Continue as Guest")

        AlertDialog.Builder(this)
            .setTitle("Who are you?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // BFF, OWNER! WALA PA TAYONG OWNER PAGE, KAYA TOAST MUNA!
                        Toast.makeText(this, "Owner Login - Coming Soon!", Toast.LENGTH_SHORT).show()
                        // BFF, PAG MAY OWNER PAGE NA KAYO, DITO NIYO ILALAGAY!
                        // startActivity(Intent(this, OwnerLoginActivity::class.java))
                    }
                    1 -> {
                        // BFF, USER!
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("USER_TYPE", "USER")
                        startActivity(intent)
                    }
                    2 -> {
                        // BFF, GUEST!
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("USER_TYPE", "GUEST")
                        startActivity(intent)
                    }
                }
            }
            .show()
    }
}