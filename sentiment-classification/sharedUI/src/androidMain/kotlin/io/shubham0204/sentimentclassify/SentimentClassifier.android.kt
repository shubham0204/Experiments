package io.shubham0204.sentimentclassify

import android.content.Context
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier

actual class SentimentClassifier(
    context: Context
) {

    private val classifier =
        BertNLClassifier.createFromFile(context, "mobilebert.tflite")

    actual fun getSentimentScore(text: String): Sentiment {
        val results: MutableList<Category?>? = classifier.classify(text)
        val negativeScore = results?.get(0)?.score ?: 0f
        val positiveScore = results?.get(1)?.score ?: 0f
        val score = negativeScore - positiveScore
        return when {
            score > 0.5f -> Sentiment.NEGATIVE
            score < -0.5f -> Sentiment.POSITIVE
            else -> Sentiment.NEUTRAL
        }
    }
}