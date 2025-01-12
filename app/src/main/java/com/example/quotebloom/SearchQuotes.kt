package com.example.quotebloom

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.firestore.FieldValue
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
    val user = mAuth.currentUser
    val context = LocalContext.current
    var showComments by remember { mutableStateOf(false) }

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
                        addQuoteToFirestoreIfNotExists(firestore, quote, author)
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
                                Box(modifier = Modifier
                                    .background(color = Color(0xFF1A2A3A),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF6E6E6E),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        LikesDislikesButtons(quoteId = quote, firestore = firestore, author = author)
                                        IconButton(
                                            onClick = {
                                                shareQuote(context = context, quote = quote, author = author)
                                            },
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
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Shuffle Quote",
                                                tint = Color.White
                                            )
                                        }

                                    }
                                }
                                Button(
                                    onClick = {
                                        if (user != null) {
                                            firestore.collection("quotes").document(quote)
                                                .get()
                                                .addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        val userSavedList =
                                                            document.get("userSaved") as? List<String>
                                                                ?: emptyList()
                                                        if (!userSavedList.contains(user.email)) {
                                                            firestore.collection("quotes").document(quote)
                                                                .update(
                                                                    "userSaved",
                                                                    FieldValue.arrayUnion(user.email)
                                                                )
                                                                .addOnSuccessListener {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Quote saved successfully",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "You have already saved this quote",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF383838)
                                    )
                                ) {
                                    Text("Save This Quote", color = Color.White)
                                }
                                Button(
                                    onClick = { showComments = !showComments },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
                                ) {
                                    Text("Comments", color = Color.White)
                                }
                            }
                            if (showComments) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 200.dp)
                                        .height(400.dp)
                                ) {
                                    CommentSection(
                                        quote = quote,
                                        firestore = firestore,
                                        user = mAuth.currentUser,
                                        navController = navController,
                                        mAuth = mAuth,
                                        onCloseCommentSection = { showComments = false }
                                    )
                                }
                            }

                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

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

        val apiKey = EnvironmentalVariables.QUOTES_API_KEY
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