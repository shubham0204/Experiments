package io.shubham0204.sentimentclassify

enum class Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

expect class SentimentClassifier {
    fun getSentimentScore(text: String): Sentiment
}