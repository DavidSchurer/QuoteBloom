package com.example.quotebloom

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CustomQuotesScreen(navController: NavHostController, mAuth: FirebaseAuth) {
    val context = LocalContext.current
    val user = mAuth.currentUser
    val firestore = FirebaseFirestore.getInstance()

    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var quoteContent by remember { mutableStateOf("") }
    val customQuotes = remember { mutableStateOf<List<QuoteData>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (user != null) {
            firestore.collection("users")
                .document(user.uid)
                .collection("customQuotes")
                .get()
                .addOnSuccessListener { snapshot ->
                    val quotes = snapshot.documents.map {
                        QuoteData(
                            documentId = it.id,
                            quote = it.getString("quote") ?: "",
                            author = it.getString("author") ?: "",
                            likes = it.getLong("likes")?.toInt() ?: 0,
                            dislikes = it.getLong("dislikes")?.toInt() ?: 0,
                            userLiked = it.get("userLiked") as? Boolean ?: false,
                            userDisliked = it.get("userDisliked") as? Boolean ?: false
                        )
                    }
                    customQuotes.value = quotes
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Quotes", color = Color.White) },
                backgroundColor = Color(0xFF232323),
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
            )
        },
        backgroundColor = Color(0xFF121212),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
                )
                {
                    Text("Create Custom Quotes", color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (customQuotes.value.isEmpty()) {
                Text("No Custom Quotes Created Yet!", style = MaterialTheme.typography.h6, color = Color.White)
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(customQuotes.value, key = { it.documentId.hashCode() }) { quoteData ->
                        Card(
                            elevation = 4.dp,
                            backgroundColor = Color(0xFF232323),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = quoteData.quote, style = MaterialTheme.typography.body1, color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = " - ${quoteData.author}",
                                        style = MaterialTheme.typography.body2,
                                        modifier = Modifier.align(Alignment.End),
                                        color = Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    shareQuote(context, quoteData.quote, quoteData.author)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Quote",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    if (user != null) {
                                        firestore.collection("users")
                                            .document(user.uid)
                                            .collection("customQuotes")
                                            .document(quoteData.documentId)
                                            .delete()
                                            .addOnSuccessListener {
                                                customQuotes.value =
                                                    customQuotes.value.filter { it.documentId != quoteData.documentId }
                                            }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Quote", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        backgroundColor = Color(0xFF232323),
                        title = { Text("Create a Custom Quote", color = Color.White) },
                        text = {
                            Column {
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name", color = Color.White) },
                                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent, focusedIndicatorColor = Color.White)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(
                                    value = quoteContent,
                                    onValueChange = { quoteContent = it },
                                    label = { Text("Quote Content", color = Color.White) },
                                    colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent, focusedIndicatorColor = Color.White)
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (user != null && name.isNotBlank() && quoteContent.isNotBlank()) {
                                    val quoteData = mapOf(
                                        "author" to name,
                                        "quote" to quoteContent
                                    )
                                    firestore.collection("users")
                                        .document(user.uid)
                                        .collection("customQuotes")
                                        .add(quoteData)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Custom Quote Saved!", Toast.LENGTH_SHORT).show()
                                            reloadCustomQuotes(user, firestore, customQuotes)
                                            name = ""
                                            quoteContent = ""
                                            showDialog = false
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed To Save Custom Quote.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
                            ) {
                                Text("Done", color = Color.White)
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialog = false },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    )
                }
    }
}

private fun reloadCustomQuotes(user: FirebaseUser?, firestore: FirebaseFirestore, customQuotes: MutableState<List<QuoteData>>) {
    if (user != null) {
        firestore.collection("users")
            .document(user.uid)
            .collection("customQuotes")
            .get()
            .addOnSuccessListener { snapshot ->
                val quotes = snapshot.documents.map {
                    QuoteData(
                        documentId = it.id,
                        quote = it.getString("quote") ?: "",
                        author = it.getString("author") ?: "",
                        likes = it.getLong("likes")?.toInt() ?: 0,
                        dislikes = it.getLong("dislikes")?.toInt() ?: 0,
                        userLiked = it.get("userLiked") as? Boolean ?: false,
                        userDisliked = it.get("userDisliked") as? Boolean ?: false
                    )
                }
                customQuotes.value = quotes
            }
    }
}