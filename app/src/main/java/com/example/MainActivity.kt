package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.ads.AdManager
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob Interstitial Ads
        AdManager.init(this)

        // Initialize Local SQLite Database via Room
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "smart_chat_db"
        )
        .fallbackToDestructiveMigration(true)
        .build()

        // Initialize Chat Repository with DAO
        repository = ChatRepository(database.chatDao())

        // Create the ChatViewModel utilizing our ViewModel Factory
        val factory = ChatViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
