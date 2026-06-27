import gzip
import utils
import numpy as np
import csv
from scipy import stats
from sklearn.metrics import classification_report

# Load the dataset
with open("dataset/imdb/imdb_dataset.csv") as csvfile:
    reader = csv.reader(csvfile, delimiter=',')
    lines = list(reader)
    lines.pop(0)
comments = []
sentiments = []
for line in lines:
    comments.append(line[0])
    if line[1] == "positive":
        sentiments.append(1)
    elif line[1] == "negative":
        sentiments.append(-1)
comments = comments[:1000]
sentiments = sentiments[:1000]

# Load representative samples
samples, sample_classes = utils.samples_from_json("dataset/imdb_samples.json")
samples_c_len = [len(gzip.compress(x.encode())) for x in samples]
samples = np.array(samples)
samples_c_len = np.array(samples_c_len)
sample_classes = np.array(
    sample_classes
)

# Iterate through different values of 'k'
# to determine the optimal value
for k in range(5, 20):
    def predict(x):
        x = str(x)
        Cs2 = len(gzip.compress(x.encode()))
        distances = []
        for s, Cs1 in zip(samples, samples_c_len):
            Cs1s2 = len(gzip.compress((s + ' ' + x).encode()))
            ncd = (Cs1s2 - min(Cs1, Cs2)) / max(Cs1, Cs2)
            distances.append(ncd)
        sorted_indices = np.argsort(distances)
        top_k_class = sample_classes[sorted_indices[:k]]
        (predict_class, _) = stats.mode(top_k_class)
        return predict_class

    pred_sentiments = []
    for i, x in enumerate(comments):
        pred_class = predict(x)
        if pred_class is not None:
            pred_sentiments.append(pred_class)

    print("for k = ", k)
    print(classification_report(sentiments, pred_sentiments))