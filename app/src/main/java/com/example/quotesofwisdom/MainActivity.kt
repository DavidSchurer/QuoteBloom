package com.example.quotesofwisdom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuotesApp()
        }
    }
}

@Composable
fun QuotesApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainPage(navController)
        }
        composable("search") {
            SearchQuotes(navController)
        }
    }
}

@Composable
fun MainPage(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quotes of Wisdom") })
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
            modifier = Modifier.fillMaxSize().padding(paddingValues),
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