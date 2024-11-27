package com.example.quotesofwisdom

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchQuotes(navController: NavHostController) {
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
                    } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for quotes...") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        coroutineScope.launch {
                            // Perform the search
                            quote = fetchQuoteByCategory(selectedCategory)
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
                                quote = fetchQuoteByCategory(selectedCategory)
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
                if (quote.isNotEmpty()) {
                    Card(
                        elevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = quote,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.body1
                        )
                    }
                } else {
                    Text("No quote available for this category.", style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

suspend fun fetchQuoteByCategory(category: String): String {
    return try {
        val apiKey = "NDmlYdnYfqoqnD8jzYulNQ==Air34IWPfvH2FfG2"  // Replace with your actual API key
        val response = RetrofitInstance.api.searchQuotes(category, apiKey)
        if (response.results.isNotEmpty()) {
            response.results[0].content
        } else {
            "No Quote found for $category"
        }
    } catch (e: Exception) {
        "Error fetching quote."
    }
}