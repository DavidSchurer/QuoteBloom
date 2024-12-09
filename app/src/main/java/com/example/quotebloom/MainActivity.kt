package com.example.quotebloom

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser == null) {
            val intent = Intent(this, GoogleSignInActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            setContent {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))) {
                    QuotesApp(mAuth)
                }
            }
        }
    }
}

@Composable
fun QuotesApp(mAuth: FirebaseAuth) {
    val navController = rememberNavController()
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainPage(navController, mAuth)
            }
            composable("search") {
                SearchQuotes(navController, api = RetrofitInstance.api, mAuth)
            }
            composable("savedQuotes") {
                SavedQuotes(navController, mAuth)
            }
            composable("customQuotes") {
                CustomQuotesScreen(navController, mAuth)
            }
        }
    }
}

@Composable
fun MainPage(navController: NavHostController, mAuth: FirebaseAuth) {
    val context = LocalContext.current

    val mGoogleSignInClient = GoogleSignIn.getClient(
        LocalContext.current,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    )

    val user = mAuth.currentUser
    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QuoteBloom", color = Color.White, style = MaterialTheme.typography.h6)
                        Text(
                            "Inspire, discover, share, and save meaningful quotes.",
                            style = MaterialTheme.typography.subtitle2.copy(fontStyle = FontStyle.Italic, color = Color.White),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                },
                backgroundColor = Color(0xFF232323),
                actions = {
                    IconButton(onClick = {
                        mGoogleSignInClient.signOut().addOnCompleteListener {
                            mGoogleSignInClient.revokeAccess().addOnCompleteListener {
                                mAuth.signOut()

                                val intent = Intent(context, GoogleSignInActivity::class.java)
                                context.startActivity(intent)
                                (context as? ComponentActivity)?.finish()
                            }
                        }
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color(0xFF232323),
                contentColor = Color.White
            ) {
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Search for Quotes") },
                    label = { Text("Search") },
                    selected = false,
                    onClick = { navController.navigate("search") }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Saved Quotes") },
                    label = { Text("Saved") },
                    selected = false,
                    onClick = { navController.navigate("savedQuotes") }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Create, contentDescription = "Custom Quotes") },
                    label = { Text("Custom Quotes") },
                    selected = false,
                    onClick = { navController.navigate("customQuotes") }
                )
            }
        },
        backgroundColor = Color(0xFF121212)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            var quote by remember { mutableStateOf("Fetching quote...") }
            var author by remember { mutableStateOf("") }
            var shuffleTriggered by remember { mutableStateOf(false) }

            // Logic for fetching new random quote / loading current quote to display
            // for quote of the day
            LaunchedEffect(Unit) {
                val savedQuote = getQuoteFromPreferences(context)
                val currentTime = System.currentTimeMillis()

                if (savedQuote != null && currentTime - savedQuote.third < 24 * 60 * 60 * 1000) {
                    quote = savedQuote.first
                    author = savedQuote.second
                } else {
                    val fetchedQuote = fetchRandomQuote()
                    if (fetchedQuote != null) {
                        quote = fetchedQuote.first
                        author = fetchedQuote.second
                        //Add quote to firestore in order to track likes/dislikes
                        addQuoteToFirestoreIfNotExists(firestore, quote, author)
                        // Saved quote to preferences in order to display quote of the day
                        // for 24 hours and prevent quote changing on app restart/navigation
                        saveQuoteToPreferences(context, quote, author, currentTime)
                    } else {
                        quote = "Error fetching quote."
                    }
                }
            }

            // Logic for shuffling quotes
            LaunchedEffect(shuffleTriggered) {
                if (shuffleTriggered) {
                    val newQuote = fetchRandomQuote()
                    if (newQuote != null) {
                        quote = newQuote.first
                        author = newQuote.second
                        saveQuoteToPreferences(context, quote, author, System.currentTimeMillis())
                    } else {
                        quote = "Error fetching new quote."
                    }
                    shuffleTriggered = false
                }
            }

            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QOTD Heading
                Text(
                    text = "Quote of the Day",
                    style = MaterialTheme.typography.h5.copy(color = Color.White),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // QOTD Quote Card
                Card(
                    elevation = 4.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(16.dp),
                    backgroundColor = Color(0xFF232323)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = quote,
                            style = MaterialTheme.typography.body1.copy(color = Color.White),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = " - $author",
                            style = MaterialTheme.typography.body2.copy(fontStyle = FontStyle.Italic, color = Color.White),
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LikesDislikesButtons(quoteId = quote, firestore = firestore)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
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
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Quote",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                shuffleTriggered = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Shuffle Quote",
                                tint = Color.White
                            )
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
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF383838))
                        ) {
                            Text("Save This Quote", color = Color.White)
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun LikesDislikesButtons(quoteId: String, firestore: FirebaseFirestore) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val likes = remember { mutableStateOf(0) }
    val dislikes = remember { mutableStateOf(0) }
    val userLiked = remember { mutableStateOf(false) }
    val userDisliked = remember { mutableStateOf(false) }
    val userSaved = remember { mutableStateOf(false) }

    // If the quote doesn't exist in firebase, add it to the firebase database
    LaunchedEffect(quoteId) {
        firestore.collection("quotes").document(quoteId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    val quoteData = mapOf(
                        "likes" to 0,
                        "dislikes" to 0,
                        "quoteText" to quoteId,
                        "userSaved" to emptyList<String>()
                    )
                    firestore.collection("quotes").document(quoteId).set(quoteData)
                }
            }
    }

    // Real-time listener for likes, dislikes, and user interactions
    LaunchedEffect(quoteId) {
        firestore.collection("quotes").document(quoteId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    likes.value = snapshot.getLong("likes")?.toInt() ?: 0
                    dislikes.value = snapshot.getLong("dislikes")?.toInt() ?: 0
                    val userLikedList = snapshot.get("userLiked") as? List<String> ?: emptyList()
                    val userDislikedList = snapshot.get("userDisliked") as? List<String> ?: emptyList()
                    val userSavedList = snapshot.get("userSaved") as? List<String> ?: emptyList()
                    currentUser?.email?.let { email ->
                        userLiked.value = email in userLikedList
                        userDisliked.value = email in userDislikedList
                        userSaved.value = email in userSavedList
                    }
                }
            }
    }

    fun updateLikeDislike(isLike: Boolean) {
        val email = currentUser?.email ?: return
        firestore.runTransaction { transaction ->
            val quoteRef = firestore.collection("quotes").document(quoteId)
            val snapshot = transaction.get(quoteRef)
            val userLikedList = snapshot.get("userLiked") as? List<String> ?: emptyList()
            val userDislikedList = snapshot.get("userDisliked") as? List<String> ?: emptyList()

            val updates = mutableMapOf<String, Any>()
            if (isLike) {
                if (userLiked.value) { // Undo the like if already liked
                    updates["likes"] = (snapshot.getLong("likes") ?: 0) - 1
                    updates["userLiked"] = userLikedList - email
                } else { // Add like if not liked
                    updates["likes"] = (snapshot.getLong("likes") ?: 0) + 1
                    updates["userLiked"] = userLikedList + email
                    if (email in userDislikedList) {
                        updates["dislikes"] = (snapshot.getLong("dislikes") ?: 0) - 1
                        updates["userDisliked"] = userDislikedList - email
                    }
                }
            } else {
                if (userDisliked.value) { // Undo the dislike if already disliked
                    updates["dislikes"] = (snapshot.getLong("dislikes") ?: 0) - 1
                    updates["userDisliked"] = userDislikedList - email
                } else { // Add dislike if not disliked
                    updates["dislikes"] = (snapshot.getLong("dislikes") ?: 0) + 1
                    updates["userDisliked"] = userDislikedList + email
                    if (email in userLikedList) {
                        updates["likes"] = (snapshot.getLong("likes") ?: 0) - 1
                        updates["userLiked"] = userLikedList - email
                    }
                }
            }
            transaction.update(quoteRef, updates)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like Button
        IconButton(
            onClick = {
                updateLikeDislike(true)
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

        // Dislike Button
        IconButton(
            onClick = {
                updateLikeDislike(false)
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

    @GET("quotes")
    suspend fun searchQuotesByCategory(
        @Query("category") category: String,
        @Header("X-Api-Key") apiKey: String
    ): List<Quote>

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
        val apiKey = "NDmlYdnYfqoqnD8jzYulNQ==Air34IWPfvH2FfG2"
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

suspend fun addQuoteToFirestoreIfNotExists(
    firestore: FirebaseFirestore,
    quote: String,
    author: String
) {
    val quoteCollection = firestore.collection("quotes")
    val querySnapshot = quoteCollection.whereEqualTo("quote", quote).get().await()

    if (querySnapshot.isEmpty) {
        val newQuote = hashMapOf(
            "quote" to quote,
            "author" to author,
            "likes" to 0,
            "dislikes" to 0
        )
        quoteCollection.add(newQuote)
    }
}

fun saveQuoteToPreferences(context: Context, quote: String, author: String, timestamp: Long) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("QuotePrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString("quote", quote)
        putString("author", author)
        putLong("timestamp", timestamp)
        apply()
    }
}

fun getQuoteFromPreferences(context: Context): Triple<String, String, Long>? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("QuotePrefs", Context.MODE_PRIVATE)
    val quote = sharedPreferences.getString("quote", null)
    val author = sharedPreferences.getString("author", null)
    val timestamp = sharedPreferences.getLong("timestamp", 0)

    return if (quote != null && author != null) {
        Triple(quote, author, timestamp)
    } else {
        null
    }
}

fun saveQuoteToFirestore(firestore: FirebaseFirestore, userId: String, quoteId: String) {
    val savedQuote = hashMapOf(
        "quoteId" to quoteId,
        "userLiked" to false,
        "userDisliked" to false
    )

    firestore.collection("users")
        .document(userId)
        .collection("savedQuotes")
        .document(quoteId)
        .set(savedQuote)
        .addOnSuccessListener {
            Log.d("saveQuoteToFirestore", "Quote saved")
        }
        .addOnFailureListener { e ->
            Log.w("saveQuoteToFirestore", "Failed to save quote", e)
        }
}

data class QuoteSearchResponse(val results: List<QuoteResult>)
data class QuoteResult(val content: String)
data class QuoteResponse(
    val quote: String,
    val author: String
)