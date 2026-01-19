package io.shubham0204.sentimentclassify

import org.koin.dsl.module

actual val targetModule = module {
    single { SentimentClassifier() }
}