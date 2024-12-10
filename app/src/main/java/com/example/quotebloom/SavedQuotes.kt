package com.example.quotebloom

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val savedQuotes = remember { mutableStateListOf<Map<String, Any>>() }
    val user = mAuth.currentUser

    // Fetch the saved quotes for the current user
    LaunchedEffect(Unit) {
        user?.email?.let { email ->
            firestore.collection("quotes")
                .whereArrayContains("userSaved", email)
                .get()
                .addOnSuccessListener { documents ->
                    savedQuotes.clear()
                    for (document in documents) {
                        savedQuotes.add(document.data)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load saved quotes", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Quotes",color = Color.White) },
                backgroundColor = Color(0xFF232323),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
                },
                backgroundColor = Color(0xFF232323)
            ) {
                paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (savedQuotes.isEmpty()) {
                        Text(
                            text = "No saved quotes yet.",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(savedQuotes) { quote ->
                                SavedQuoteCard(quote, firestore, user?.email.orEmpty())
                            }
                        }
                    }
                }
    }
}

@Composable
fun SavedQuoteCard(
    quoteData: Map<String, Any>,
    firestore: FirebaseFirestore,
    currentUserEmail: String
) {
    val quoteText = quoteData["quoteText"] as? String ?: "Unknown Quote"
    val author = quoteData["author"] as? String ?: "Unknown Author"
    val likes = remember { mutableStateOf((quoteData["likes"] as? Long)?.toInt() ?: 0) }
    val dislikes = remember { mutableStateOf((quoteData["dislikes"] as? Long)?.toInt() ?: 0) }
    val userLiked = remember { mutableStateOf(false) }
    val userDisliked = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Real-time updates for likes and dislikes
    LaunchedEffect(quoteText) {
        firestore.collection("quotes").document(quoteText)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    likes.value = snapshot.getLong("likes")?.toInt() ?: 0
                    dislikes.value = snapshot.getLong("dislikes")?.toInt() ?: 0
                    val likedUsers = snapshot.get("userLiked") as? List<String> ?: emptyList()
                    val dislikedUsers = snapshot.get("userDisliked") as? List<String> ?: emptyList()
                    userLiked.value = currentUserEmail in likedUsers
                    userDisliked.value = currentUserEmail in dislikedUsers
                }
            }
    }

    Card(
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(vertical = 8.dp),
        backgroundColor = Color(0xFF232323)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = quoteText,
                style = MaterialTheme.typography.body1.copy(color = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "— $author",
                style = MaterialTheme.typography.body2.copy(color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        updateLikesDislikes(firestore, quoteText, currentUserEmail, isLike = true)
                    }
                ) {
                    Icon(
                        imageVector = if (userLiked.value) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (userLiked.value) Color.Blue else Color.White
                    )
                    Text(text = likes.value.toString(), color = Color.White, fontSize = 12.sp)
                }
                IconButton(
                    onClick = {
                        updateLikesDislikes(firestore, quoteText, currentUserEmail, isLike = false)
                    }
                ) {
                    Icon(
                        imageVector = if (userDisliked.value) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Dislike",
                        tint = if (userDisliked.value) Color.Red else Color.White
                    )
                    Text(text = dislikes.value.toString(), color = Color.White, fontSize = 12.sp)
                }
                IconButton(
                    onClick = {
                        shareQuote(context, quoteText, author)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
            }
        }
    }
}

// Helper function to update likes and dislikes
fun updateLikesDislikes(
    firestore: FirebaseFirestore,
    quoteId: String,
    userEmail: String,
    isLike: Boolean
) {
    firestore.runTransaction { transaction ->
        val quoteRef = firestore.collection("quotes").document(quoteId)
        val snapshot = transaction.get(quoteRef)

        val likedUsers = snapshot.get("userLiked") as? MutableList<String> ?: mutableListOf()
        val dislikedUsers = snapshot.get("userDisliked") as? MutableList<String> ?: mutableListOf()

        if (isLike) {
            if (!likedUsers.contains(userEmail)) {
                likedUsers.add(userEmail)
                dislikedUsers.remove(userEmail)
            }
        } else {
            if (!dislikedUsers.contains(userEmail)) {
                dislikedUsers.add(userEmail)
                likedUsers.remove(userEmail)
            }
        }

        transaction.update(quoteRef, "userLiked", likedUsers)
        transaction.update(quoteRef, "userDisliked", dislikedUsers)
        transaction.update(
            quoteRef,
            "likes",
            likedUsers.size.toLong(),
            "dislikes",
            dislikedUsers.size.toLong()
        )
    }
}

fun shareQuote(context: Context, quote: String, author: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "\"$quote\" - $author")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Quote via"))
}