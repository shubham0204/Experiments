package io.shubham0204.sentimentclassify

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.util.CoreMap
import java.util.Properties


actual class SentimentClassifier {

    private val pipeline: StanfordCoreNLP

    init {
        val properties = Properties()
        properties.setProperty("tokenize.whitespace", "true")
        properties.setProperty("ssplit.eolonly", "true")
        properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
        pipeline = StanfordCoreNLP(properties)
    }

    actual fun getSentimentScore(text: String): Sentiment {
        val annotation = Annotation(text)
        pipeline.annotate(annotation)
        val sentence: CoreMap = annotation.get(SentencesAnnotation::class.java)[0]
        val sentiment = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree::class.java)
        return when (RNNCoreAnnotations.getPredictedClass(sentiment)) {
            0, 1 -> Sentiment.NEGATIVE
            3, 4 -> Sentiment.POSITIVE
            2 -> Sentiment.NEUTRAL
            else -> Sentiment.NEUTRAL
        }
    }
}