import csv
import os
import pickle
from sentence_transformers import SentenceTransformer
from sklearn.cluster import KMeans
import numpy as np
import utils
from collections import defaultdict

# Load the dataset from a remote source or from a cache (if already downloaded)  
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
embeddings = None
if not os.path.exists('dataset/sentiment_embeddings_2.pkl'):
    model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
    embeddings = model.encode(comments)
    with open("dataset/sentiment_embeddings_2.pkl", "wb") as f:
        pickle.dump(embeddings, f)
else:
    with open("dataset/sentiment_embeddings_2.pkl", "rb") as f:
        embeddings = pickle.load(f)
        
# Perform k-means clustering with k = 300 
n_clusters = 300
kmeans = KMeans(n_clusters=n_clusters)
labels = kmeans.fit_predict(embeddings)

# The cluster centers derived from k-means are not actual data-points from the dataset  
# Hence, we iterate through the dataset and find data-points closed to the cluster centres  
embeddings_map = defaultdict(list)
for i in range(len(labels)):
    embeddings_map[labels[i]].append(i)
actual_cluster_center_idx = []
for label, embeddings_idx in embeddings_map.items():
    center = kmeans.cluster_centers_[label]
    nearest_idx = embeddings_idx[0]
    nearest_dist = float('inf')
    for emb_idx in embeddings_idx:
        dist = np.sqrt(np.sum(np.square(embeddings[emb_idx] - center)))
        if dist < nearest_dist:
            nearest_dist = dist
            nearest_idx = emb_idx
    actual_cluster_center_idx.append(nearest_idx)

# Write the representative clusters/data-points and their cluster labels  
# to a JSON file  
utils.samples_to_json(
    [comments[p] for p in actual_cluster_center_idx],
    [sentiments[p] for p in actual_cluster_center_idx],
    "dataset/imdb_samples.json"
)

# Write the representative clusters as a C char* array
utils.write_as_c_string_array(
    "dataset/imdb_samples.json",
    "data.h"
)