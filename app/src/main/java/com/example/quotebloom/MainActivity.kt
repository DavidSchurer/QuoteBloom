package com.example.quotebloom

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser == null) {
            val intent = Intent(this, GoogleSignInActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            setContent {
                QuotesApp(mAuth)
            }
        }
    }
}

@Composable
fun QuotesApp(mAuth: FirebaseAuth) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainPage(navController, mAuth)
        }
        composable("search") {
            SearchQuotes(navController, api = RetrofitInstance.api)
        }
    }
}

@Composable
fun MainPage(navController: NavHostController, mAuth: FirebaseAuth) {
    val context = LocalContext.current

    val mGoogleSignInClient = GoogleSignIn.getClient(
        LocalContext.current,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QuoteBloom", style = MaterialTheme.typography.h6)
                        Text(
                            "Inspire, discover, share, and save quotes that mean something.",
                            style = MaterialTheme.typography.caption.copy(fontStyle = FontStyle.Italic),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        mGoogleSignInClient.signOut().addOnCompleteListener {
                            mGoogleSignInClient.revokeAccess().addOnCompleteListener {
                                mAuth.signOut()

                                val intent = Intent(context, GoogleSignInActivity::class.java)
                                context.startActivity(intent)
                                (context as? ComponentActivity)?.finish()
                            }
                        }
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("search") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search for Quotes")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            var quote by remember { mutableStateOf("Fetching quote...") }
            var author by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                val fetchedQuote = fetchRandomQuote()
                if (fetchedQuote != null) {
                    quote = fetchedQuote.first
                    author = fetchedQuote.second
                } else {
                    quote = "Error fetching quote."
                }
            }

            Card(
                elevation = 4.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = quote,
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = " - $author",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

interface QuoteApiService {
    @GET("quotes")
    suspend fun getRandomQuote(
        @Header("X-Api-Key") apiKey: String
    ): List<QuoteResponse>

    @GET("/quotes/search")
    suspend fun searchQuotes(
        @Query("query") query: String,
        @Header("X-Api-Key") apiKey: String
    ): QuoteSearchResponse

    @GET("quotes")
    suspend fun searchQuotesByCategory(
        @Query("category") category: String,
        @Header("X-Api-Key") apiKey: String
    ): List<Quote>

}

object RetrofitInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.api-ninjas.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: QuoteApiService = retrofit.create(QuoteApiService::class.java)
}

suspend fun fetchRandomQuote(): Pair<String, String>? {
    return try {
        val apiKey = "NDmlYdnYfqoqnD8jzYulNQ==Air34IWPfvH2FfG2"  // Replace with your actual API key
        val response = RetrofitInstance.api.getRandomQuote(apiKey)

        if (response.isNotEmpty()) {
            val quote = response[0]
            Pair(quote.quote, quote.author)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

data class QuoteSearchResponse(val results: List<QuoteResult>)
data class QuoteResult(val content: String)
data class QuoteResponse(
    val quote: String,
    val author: String
)