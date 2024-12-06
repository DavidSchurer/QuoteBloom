package com.example.quotebloom

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchQuotes(navController: NavHostController, api: QuoteApiService, mAuth: FirebaseAuth) { // Use QuotesApi here
    val allCategories = listOf(
        "age", "alone", "amazing", "anger", "architecture", "art", "attitude", "beauty",
        "best", "birthday", "business", "car", "change", "communication", "computers",
        "cool", "courage", "dad", "dating", "death", "design", "dreams", "education",
        "environmental", "equality", "experience", "failure", "faith", "family", "famous",
        "fear", "fitness", "food", "forgiveness", "freedom", "friendship", "funny", "future",
        "god", "good", "government", "graduation", "great", "happiness", "health", "history",
        "home", "hope", "humor", "imagination", "inspirational", "intelligence", "jealousy",
        "knowledge", "leadership", "learning", "legal", "life", "love", "marriage", "medical",
        "men", "mom", "money", "morning", "movies", "success"
    ).sorted()

    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = mAuth.currentUser
    val context = LocalContext.current

    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Quotes", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                backgroundColor = Color(0xFF232323)
            )
        },
        backgroundColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Button to trigger popup for quote category selection
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF232323)  // Set the button background color here
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Search Quote by Category", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display the selected category and the quote for that specific category
            if (selectedCategory.isNotEmpty()) {
                Text("Selected Category: $selectedCategory", style = MaterialTheme.typography.h6, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                LaunchedEffect(selectedCategory) {
                    val fetchedQuote = fetchQuoteByCategory(selectedCategory, api)
                    if (fetchedQuote != null) {
                        quote = fetchedQuote.first
                        author = fetchedQuote.second
                    } else {
                        quote = "No quote found."
                        author = ""
                    }
                }

                if (quote.isNotEmpty()) {
                        Card(
                            elevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            backgroundColor = Color(0xFF232323)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = quote,
                                    style = MaterialTheme.typography.body1,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "- $author",
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.align(Alignment.End),
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                IconButton(
                                    onClick = {
                                        shareQuote(context = context, quote = quote, author = author)
                                    },
                                    modifier = Modifier.align(Alignment.Start)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Quote",
                                        tint = Color.White
                                    )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val newQuote = fetchQuoteByCategory(selectedCategory, api)
                                        if (newQuote != null) {
                                            quote = newQuote.first
                                            author = newQuote.second
                                        } else {
                                            quote = "No quote found."
                                            author = ""
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Shuffle Quote",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            saveQuoteToFirebase(
                                firestore = firestore,
                                userId = currentUser?.uid ?: "",
                                quote = quote,
                                author = author,
                                context = context
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF383838)
                        )
                    ) {
                        Text("Save This Quote", color = Color.White)
                    }
                } else {
                    Text("Loading quote...", style = MaterialTheme.typography.body2, color = Color.White)
                }
            }

            // Popup Dialog for quote category selection
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Select a Category", color = Color.White) },
                    text = {
                        LazyColumn {
                            items(allCategories) { category ->
                                Button(
                                    onClick = {
                                        selectedCategory = category
                                        showDialog = false
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF383838)
                                    )
                                ) {
                                    Text(category, textAlign = TextAlign.Center, color = Color.White)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Close", color = Color.White)
                        }
                    },
                    backgroundColor = Color(0xFF232323)
                )
            }
        }
    }
}

fun saveQuoteToFirebase(firestore: FirebaseFirestore, userId: String, quote: String, author: String, context: Context) {
    if (userId.isBlank()) {
        Log.e("SavedQuote", "User not authenticated.")
        return
    }

    val quoteData = hashMapOf(
        "quote" to quote,
        "author" to author
    )

        firestore.collection("users")
            .document(userId)
            .collection("savedQuotes")
            .add(quoteData)
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    "Quote saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("SavedQuote", "Error saving quote: ${e.message}", e)
            }
}

suspend fun fetchQuoteByCategory(category: String, api: QuoteApiService): Pair<String, String>? {
    Log.d("fetchQuoteByCategory", "Fetching quote for category: $category")  // Add this log statement

    return try {
        if (category.isBlank()) return null

        val apiKey = "NDmlYdnYfqoqnD8jzYulNQ==Air34IWPfvH2FfG2"  // Replace with your actual API key
        val baseUrl = "https://api.api-ninjas.com/v1/quotes"
        val url = "$baseUrl?category=$category"

        println("Constructed URl: $url")
        Log.d("fetchQuoteByCategory", "Constructed URL: $url")

        val response = api.searchQuotesByCategory(category, apiKey)

        Log.d("fetchQuoteByCategory", "Received response: $response")
        println("API Response: $response")

        if (response.isNotEmpty()) {
            val quote = response[0].quote
            val author = response[0].author
            Log.d("fetchQuoteByCategory", "Quote found: $quote")
            Pair(quote, author)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("fetchQuoteByCategory", "Error fetching quote: ${e.message}", e)
        e.printStackTrace()  // Log the exception for debugging
        null
    }
}

data class Quote(
    val quote: String,
    val author: String,
    val category: String
)