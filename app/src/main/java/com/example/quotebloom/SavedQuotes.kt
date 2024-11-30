package com.example.quotebloom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SavedQuotes(navController: NavHostController, mAuth: FirebaseAuth) {
    val user = mAuth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val savedQuotes = remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (user != null) {
            firestore.collection("users")
                .document(user.uid)
                .collection("savedQuotes")
                .get()
                .addOnSuccessListener { snapshot ->
                    val quotes = snapshot.documents.map {
                        Pair(it.getString("quote") ?: "", it.getString("author") ?: "")
                    }
                    savedQuotes.value = quotes
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Quotes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (savedQuotes.value.isEmpty()) {
                Text("No saved quotes yet!", style = MaterialTheme.typography.h6)
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    savedQuotes.value.forEach { (quote, author) ->
                        Card(
                            elevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = quote, style = MaterialTheme.typography.body1)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = " - $author", style = MaterialTheme.typography.body2, modifier = Modifier.align(Alignment.End))
                            }
                        }
                    }
                }
            }
        }

    }
}