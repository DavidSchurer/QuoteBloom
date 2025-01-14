package com.example.quotebloom

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class QuoteData(
    val documentId: String,
    val quote: String,
    val author: String,
    val likes: Int,
    val dislikes: Int,
    val userLiked: Boolean,
    val userDisliked: Boolean
)

@Composable
fun SavedQuotes(navController: NavHostController, mAuth: FirebaseAuth) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val savedQuotes = remember { mutableStateListOf<Map<String, Any>>() }
    val user = mAuth.currentUser
    var showComments by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current

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
                title = {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFB0B0B0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Saved Quotes", color = Color.Black)
                        }

                        },
                backgroundColor = Color(0xFF232323),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF2196F3),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                }
            )
        },
        backgroundColor = Color(0xFF121212),
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp) // Reserve space for the delete all saved quotes button
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Row for filter buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Add space between buttons
                        ) {
                            Button(
                                onClick = { filterType = "content"
                                          searchQuery = "" },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838)),
                                modifier = Modifier.weight(1f) // Ensure buttons fill available space equally
                            ) {
                                Text("Filter by Quote", color = Color.White)
                            }
                            Button(
                                onClick = { filterType = "author"
                                          searchQuery = "" },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838)),
                                modifier = Modifier.weight(1f) // Ensure buttons fill available space equally
                            ) {
                                Text("Filter by Author", color = Color.White)
                            }
                        }

                        // Remove Filter Button placed below filter buttons
                        Button(
                            onClick = {
                                filterType = ""
                                searchQuery = ""
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
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text("Remove Filter", color = Color.White)
                        }
                    }

                    // Search bar and search button layout
                    if (filterType.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                label = { Text(when (filterType) {
                                    "content" -> "Enter quote text"
                                    "author" -> "Enter quote author"
                                    else -> "Enter search term"
                                }) },
                                colors = TextFieldDefaults.textFieldColors(
                                    textColor = Color.White,
                                    backgroundColor = Color(0xFF1E1E1E),
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    user?.email?.let { email ->
                                        fetchFilteredQuotes(firestore, filterType, searchQuery, savedQuotes, email)
                                    }
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.padding(start = 8.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
                            ) {
                                Text("Search", color = Color.White)
                            }
                        }
                    }

                    // Display saved quotes
                    if (savedQuotes.isEmpty()) {
                        Text(
                            text = "No saved quotes yet.",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(savedQuotes) { quote ->
                                SavedQuoteCard(quote, firestore, user?.email.orEmpty()) { deletedQuote ->
                                    savedQuotes.removeIf { it["quoteText"] == deletedQuote["quoteText"] }
                                }
                            }
                        }
                    }
                }

                // Delete all saved quotes button at the bottom
                Button(
                    onClick = {
                        user?.email?.let { email ->
                            deleteAllSavedQuotes(firestore, email) {
                                savedQuotes.clear()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
                ) {
                    Text("Delete All Saved Quotes", color = Color.White)
                }
            }
        }
    )
}

fun fetchFilteredQuotes(
    firestore: FirebaseFirestore,
    filterType: String,
    searchQuery: String,
    savedQuotes: MutableList<Map<String, Any>>,
    userEmail: String
) {
    val normalizedQuery = searchQuery.trim().lowercase()

    firestore.collection("quotes")
        .whereArrayContains("userSaved", userEmail)
        .get()
        .addOnSuccessListener { documents ->
            savedQuotes.clear()
            for (document in documents) {
                val data = document.data
                val fieldValue = when (filterType) {
                    "content" -> data["quoteText"] as? String
                    "author" -> data["author"] as? String
                    else -> null
                }
                if (fieldValue?.lowercase()?.contains(normalizedQuery) == true) {
                    savedQuotes.add(data)
                }
            }
        }
        .addOnFailureListener {
            Log.e("FetchQuotes", "Error fetching filtered quotes", it)
    }
}

@Composable
fun SavedQuoteCard(
    quoteData: Map<String, Any>,
    firestore: FirebaseFirestore,
    currentUserEmail: String,
    onDelete: (Map<String, Any>) -> Unit // Callback to refresh the saved quotes after a quote is deleted
) {
    val quoteText = quoteData["quoteText"] as? String ?: "Unknown Quote"
    val author = quoteData["author"] as? String ?: "Unknown Author"
    val likes = remember { mutableStateOf((quoteData["likes"] as? Long)?.toInt() ?: 0) }
    val dislikes = remember { mutableStateOf((quoteData["dislikes"] as? Long)?.toInt() ?: 0) }
    val userLiked = remember { mutableStateOf(false) }
    val userDisliked = remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showComments by remember { mutableStateOf(false) }
    val navController = rememberNavController()

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
            Box(
                modifier = Modifier
                    .background(color = Color(0xFF1A2A3A), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = quoteText,
                        style = MaterialTheme.typography.body1.copy(color = Color.White),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                        Text(
                            text = "— $author",
                            style = MaterialTheme.typography.body2.copy(
                                fontStyle = FontStyle.Italic,
                                color = Color.White
                            ),
                            modifier = Modifier.align(Alignment.End)
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
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Like Icon Button
                    IconButton(
                        onClick = {
                            updateLikesDislikes(firestore, quoteText, currentUserEmail, isLike = true)
                        },
                        modifier = Modifier.size(48.dp),
                        enabled = true
                    ) {
                        Icon(
                            imageVector = if (userLiked.value) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = if (userLiked.value) Color.Green else Color.White
                        )
                    }

                    Text(text = likes.value.toString(), color = Color.White)

                    // Dislike Icon Button
                    IconButton(
                        onClick = {
                            updateLikesDislikes(firestore, quoteText, currentUserEmail, isLike = false)
                        },
                        modifier = Modifier.size(48.dp),
                        enabled = true
                    ) {
                        Icon(
                            imageVector = if (userDisliked.value) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Dislike",
                            tint = if (userDisliked.value) Color.Red else Color.White,
                            modifier = Modifier.rotate(180f)
                        )
                    }

                    Text(text = dislikes.value.toString(), color = Color.White)

                    IconButton(
                        onClick = {
                            shareQuote(context, quoteText, author)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            deleteQuote(firestore, quoteText, currentUserEmail)
                            onDelete(quoteData)
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comments Button
            Button(
                onClick = { showComments = !showComments },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
            ) {
                Text("Comments", color = Color.White)
            }

            if (showComments) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp) // Increase the height to display more comments
                        .padding(top = 16.dp)
                ) {
                    CommentSection(
                        quote = quoteText,
                        firestore = firestore,
                        user = FirebaseAuth.getInstance().currentUser,
                        navController = navController,
                        mAuth = FirebaseAuth.getInstance(),
                        onCloseCommentSection = { showComments = false }
                    )
                }
            }
        }
    }
}

// Helper function to delete the quote from the users saved quotes collection
fun deleteQuote(
    firestore: FirebaseFirestore,
    quoteId: String,
    userEmail: String
) {
    firestore.runTransaction { transaction ->
        val quoteRef = firestore.collection("quotes").document(quoteId)
        val snapshot = transaction.get(quoteRef)

        val userSavedList = snapshot.get("userSaved") as? MutableList<String> ?: mutableListOf()

        if (userSavedList.contains(userEmail)) {
            userSavedList.remove(userEmail)
            transaction.update(quoteRef, "userSaved", userSavedList)
        }
    }
}

fun deleteAllSavedQuotes(
    firestore: FirebaseFirestore,
    userEmail: String,
    onComplete: () -> Unit
) {
    firestore.collection("quotes")
        .whereArrayContains("userSaved", userEmail)
        .get()
        .addOnSuccessListener { documents ->
            val batch = firestore.batch()

            for (document in documents) {
                val quoteRef = document.reference
                batch.update(
                    quoteRef,
                    "userSaved", FieldValue.arrayRemove(userEmail)
                )
            }

            batch.commit()
                .addOnSuccessListener {
                    onComplete()
                }
                .addOnFailureListener {

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
        putExtra(Intent.EXTRA_TEXT, "\"$quote\"\n\n— $author")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Quote via"))
}