package io.shubham0204.sentimentclassify

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.shubham0204.sentimentclassify.theme.AppTheme
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) {
    // https://insert-koin.io/docs/reference/koin-compose/compose/#starting-koin-with-a-compose-app---koinapplication
    KoinMultiplatformApplication(
        config = createKoinConfiguration(),
    ) {
        AppTheme(onThemeChanged) {
            val sentimentClassifier = koinInject<SentimentClassifier>()
            var text by rememberSaveable { mutableStateOf("") }
            var sentiment by rememberSaveable{ mutableStateOf<Sentiment?>(null) }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .widthIn(max = 600.dp)
                        .align(Alignment.CenterHorizontally),
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text("Enter text to classify sentiment" )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    sentiment = sentimentClassifier.getSentimentScore(text)
                }) {
                    Text("Classify Sentiment")
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (sentiment != null) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally),
                        text = "Sentiment: $sentiment"
                    )
                }
            }
        }
    }
}
