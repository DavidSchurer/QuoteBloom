package com.example.quotebloom

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class QuoteData(
    val documentId: String,
    val quote: String,
    val author: String
)

@Composable
fun SavedQuotes(navController: NavHostController, mAuth: FirebaseAuth) {
    val user = mAuth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val savedQuotes = remember { mutableStateOf<List<QuoteData>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (user != null) {
            firestore.collection("users")
                .document(user.uid)
                .collection("savedQuotes")
                .get()
                .addOnSuccessListener { snapshot ->
                    val quotes = snapshot.documents.map {
                        QuoteData(
                            documentId = it.id,
                            quote = it.getString("quote") ?: "",
                            author = it.getString("author") ?: ""
                        )
                    }
                    savedQuotes.value = quotes
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Quotes",color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                backgroundColor = Color(0xFF232323)
            )
        },
        floatingActionButton = {
            if (savedQuotes.value.isNotEmpty()) {
                Button(
                    onClick = {
                        if (user != null) {
                            firestore.collection("users")
                                .document(user.uid)
                                .collection("savedQuotes")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot.documents.forEach { it.reference.delete() }
                                    savedQuotes.value = emptyList()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Delete All Saved Quotes", color = Color.White)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        backgroundColor = Color(0xFF121212)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (savedQuotes.value.isEmpty()) {
                Text("No saved quotes yet!", style = MaterialTheme.typography.h6, color = Color.White)
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(savedQuotes.value, key = {it.documentId }) { quoteData ->
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
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.End)
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
                                            .collection("savedQuotes")
                                            .document(quoteData.documentId)
                                            .delete()
                                            .addOnSuccessListener {
                                                savedQuotes.value = savedQuotes.value.filter { it.documentId != quoteData.documentId }
                                            }
                                    }
                                },
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .padding(8.dp)
                                    ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Quote", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

fun shareQuote(context: Context, quote: String, author: String) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "\"$quote\"\n\n— $author")
        type = "text/plain"
    }
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Share Quote via"
        )
    )
}