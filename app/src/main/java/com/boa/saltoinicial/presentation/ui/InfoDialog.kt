package com.boa.saltoinicial.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boa.saltoinicial.R

@Composable
fun InfoDialog(
    title: String? = "",
    desc: String? = "",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        InfoDialogContent(title = title, desc = desc, onDismiss = onDismiss)
    }
}

@Composable
private fun InfoDialogContent(
    title: String?,
    desc: String?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = Color.Transparent)
    ) {
        InfoDialogCard(title = title, desc = desc, onDismiss = onDismiss)
    }
}

@Composable
private fun InfoDialogCard(
    title: String?,
    desc: String?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(25.dp, 5.dp, 25.dp, 5.dp)
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        InfoDialogLogo()
        InfoDialogTextContent(title = title, desc = desc, onDismiss = onDismiss)
    }
}

@Composable
private fun InfoDialogLogo() {
    Image(
        painter = painterResource(id = R.mipmap.logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .height(180.dp)
            .fillMaxWidth(),
    )
}

@Composable
private fun InfoDialogTextContent(
    title: String?,
    desc: String?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        InfoDialogTitle(title = title)
        Spacer(modifier = Modifier.height(8.dp))
        InfoDialogDescription(desc = desc)
        Spacer(modifier = Modifier.height(24.dp))
        InfoDialogButton(onDismiss = onDismiss)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoDialogTitle(title: String?) {
    Text(
        text = title.orEmpty(),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 130.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun InfoDialogDescription(desc: String?) {
    Text(
        text = desc.orEmpty(),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 10.dp, start = 25.dp, end = 25.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun InfoDialogButton(onDismiss: () -> Unit) {
    Button(
        onClick = onDismiss,
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(
            text = "OK",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
