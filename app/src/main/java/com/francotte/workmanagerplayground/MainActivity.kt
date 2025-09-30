package com.francotte.workmanagerplayground

import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.JobIntentService.enqueueWork
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OverwritingInputMerger
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.francotte.workmanagerplayground.ui.theme.WorkManagerPlayGroundTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkManagerPlayGroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
        intent?.let { handleIncomingIntent(it) }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        val url = extractUrlFromIntent(intent)
        when {
            url != null -> vm.startChainFromUrl(url)
            intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    vm.startChainFromUri(uri)
                }
            }
        }
    }

    private fun extractUrlFromIntent(intent: Intent): String? {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { return it.trim() }
        }
        if (intent.action == Intent.ACTION_VIEW && intent.dataString != null) {
            return intent.dataString
        }
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                val text = clip.getItemAt(i).text?.toString()
                if (!text.isNullOrBlank()) return text
                val uri = clip.getItemAt(i).uri
                if (uri != null && (uri.scheme == "http" || uri.scheme == "https")) {
                    return uri.toString()
                }
            }
        }
        return null
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkManagerPlayGroundTheme {
        Greeting("Android")
    }
}