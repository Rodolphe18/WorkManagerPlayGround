package com.francotte.workmanagerplayground

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppScreen(modifier: Modifier=Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val ui by mainViewModel.uiState.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (ui.imagePath != null) {
                    ImageWithOptionalRedOverlay(
                        path = ui.imagePath ?: return@Box Text("Aucune image à afficher"),
                        showOverlay = ui.showRedOverlay || ui.status == UiStatus.Filtering,
                        contentDescription = "Image partagée"
                    )
                } else {
                    Text(text = "Aucune image à afficher")
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(text = ui.status.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


@Composable
private fun ImageWithOptionalRedOverlay(
    path: String,
    showOverlay: Boolean,
    contentDescription: String
) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x60FF0000))
            )
        }
    }
}