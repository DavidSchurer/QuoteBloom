package com.example.quotebloom

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInActivity : ComponentActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Use your actual Web client ID
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            GoogleSignInScreen(onSignInSuccess = {
                // Navigate to MainActivity after successful sign-in
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            })
        }
    }

    // Trigger Google Sign-In intent
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle result of the sign-in attempt
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Firebase Authentication with Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign-in success, navigate to MainActivity
                val user = mAuth.currentUser
                Toast.makeText(baseContext, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(baseContext, "Authentication Failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun GoogleSignInScreen(onSignInSuccess: () -> Unit) {
        val mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser

        val backgroundColor = Color(0xFF121212)
        val textColor = Color.White
        val buttonColor = Color(0xFF383838)
        val navyColor = Color(0xFF1A2A3A)
        val greyColor = Color(0xFFB0B0B0)

        if (user != null) {
            // If user is signed in, navigate to MainActivity
            LaunchedEffect(user) {
                onSignInSuccess()
            }
        } else {
            // Show Google Sign-In Button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color(0xFF232323), shape = MaterialTheme.shapes.small)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Welcome to QuoteBloom",
                        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Main Content Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(navyColor, shape = MaterialTheme.shapes.small)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "QuoteBloom is the ultimate platform to explore a vast collection of quotes across various categories. Discover inspiring, motivational, and thought-provoking quotes, and engage with them by liking, commenting, and sharing your favorites. Save quotes that resonate with you and create your own personalized quotes to share with others. Whether you're seeking inspiration or looking to express your own thoughts, QuoteBloom is your go-to destination for the power of words.",
                            style = MaterialTheme.typography.body2.copy(
                                color = textColor,
                                fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(8.dp),
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Developed by David Schurer",
                        style = MaterialTheme.typography.body2.copy(color = greyColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { signIn() },
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("Sign in with Google", color = textColor)
                    }
                }
            }
        }
    }
}