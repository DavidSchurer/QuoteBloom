package com.example.quotebloom

import android.util.Log
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchQuotes(navController: NavHostController, api: QuoteApiService) { // Use QuotesApi here
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
    )

    var query by remember { mutableStateOf("") }
    var filteredCategories by remember { mutableStateOf(allCategories) }
    var selectedCategory by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Quotes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar to search for quote categories
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    filteredCategories = allCategories.filter { category ->
                        category.contains(query, ignoreCase = true)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for categories...") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        coroutineScope.launch {
                            fetchQuoteByCategory(selectedCategory, api)?.let { (q, a) ->
                                quote = q
                                author = a
                            }
                        }
                    }
                })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display the filtered categories
            LazyColumn {
                items(filteredCategories) { category ->
                    ListItem(
                        text = { Text(category) },
                        modifier = Modifier.clickable {
                            selectedCategory = category
                            coroutineScope.launch {
                                // Fetch the quote for the selected category
                                fetchQuoteByCategory(selectedCategory, api)?.let { (q, a) ->
                                    quote = q
                                    author = a
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display the selected category and the quote for that specific category
            if (selectedCategory.isNotEmpty()) {
                Text("Selected Category: $selectedCategory", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(8.dp))

                LaunchedEffect(selectedCategory) {
                    Log.d("SearchQuotes", "LaunchedEffect triggered for category: $selectedCategory")
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
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = quote,
                                style = MaterialTheme.typography.body1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "- $author",
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                } else {
                    Text("Loading quote...", style = MaterialTheme.typography.body2)
                }
            }
        }
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