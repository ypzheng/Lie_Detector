# -*- coding: utf-8 -*-
"""
Created on Wed Sep 21 16:02:58 2016

@author: cs390mb

Assignment 3 : Speaker Identification

This is the solution script for training a model for identifying
speaker from audio data. The script loads all labelled speaker
audio data files in the specified directory. It extracts features
from the raw data and trains and evaluates a classifier to identify
the speaker.

"""

import os
import sys
import numpy as np
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from features import FeatureExtractor
from sklearn import cross_validation
from sklearn.metrics import confusion_matrix
import pickle
from sklearn.model_selection import GridSearchCV


# %%---------------------------------------------------------------------------
#
#		                 Load Data From Disk
#
# -----------------------------------------------------------------------------

data_dir = 'data' # directory where the data files are stored

output_dir = 'training_output' # directory where the classifier(s) are stored

if not os.path.exists(output_dir):
    os.mkdir(output_dir)

# the filenames should be in the form 'speaker-data-subject-1.csv', e.g. 'speaker-data-Erik-1.csv'. If they
# are not, that's OK but the progress output will look nonsensical

class_names = [] # the set of classes, i.e. speakers

data = np.zeros((0,8002)) #8002 = 1 (timestamp) + 8000 (for 8kHz audio data) + 1 (label)

for filename in os.listdir(data_dir):
    if filename.endswith(".csv") and filename.startswith("speaker-data"):
        filename_components = filename.split("-") # split by the '-' character
        speaker = filename_components[2]
        print("Loading data for {}.".format(speaker))
        if speaker not in class_names:
            class_names.append(speaker)
        speaker_label = class_names.index(speaker)
        sys.stdout.flush()
        data_file = os.path.join(data_dir, filename)
        data_for_current_speaker = np.genfromtxt(data_file, delimiter=',')
        print("Loaded {} raw labelled audio data samples.".format(len(data_for_current_speaker)))
        sys.stdout.flush()
        data = np.append(data, data_for_current_speaker, axis=0)

print("Found data for {} speakers : {}".format(len(class_names), ", ".join(class_names)))

# %%---------------------------------------------------------------------------
#
#		                Extract Features & Labels
#
# -----------------------------------------------------------------------------

# You may need to change this depending on how you compute your features
n_features = 626 # 20 formant features + 16 pitch contour features + 75 mfcc delta coefficients

print("Extracting features and labels for {} audio windows...".format(data.shape[0]))
sys.stdout.flush()

X = np.zeros((0,n_features))
y = np.zeros(0,)

# change debug to True to show print statements we've included:
feature_extractor = FeatureExtractor(debug=False)

for i,window_with_timestamp_and_label in enumerate(data):
    window = window_with_timestamp_and_label[1:-1]
    label = data[i,-1]
    print "Extracting features for window " + str(i) + "..."
    x = feature_extractor.extract_features(window)
    if (len(x) != X.shape[1]):
        print("Received feature vector of length {}. Expected feature vector of length {}.".format(len(x), X.shape[1]))
    X = np.append(X, np.reshape(x, (1,-1)), axis=0)
    y = np.append(y, label)

print("Finished feature extraction over {} windows".format(len(X)))
print("Unique labels found: {}".format(set(y)))
sys.stdout.flush()


print("\n")
print("---------------------- Grid Search -------------------------")
#grid_search
param_grid = [
    {
    'n_estimators':[350,400,450],
    'max_depth':[3, 5, 7, 9],
    'min_samples_split':[2, 4, 8, 16],
    'min_samples_leaf':[1, 8, 16],
    'random_state':[1]}
    ]

scores = ['precision', 'recall']

print '\n'
print "Starting Grid Search"

for score in scores:
    clf = GridSearchCV(RandomForestClassifier(), param_grid, cv=10,scoring="%s_macro"%score)
    clf.fit(X,y)
    print "Best Params with %s scoring"%score
    print(clf.best_params_)
    print "Best Score with %s scoring"%score
    mean= clf.cv_results_['mean_test_score']
    stds = clf.cv_results_['std_test_score']
    params = clf.cv_results_['params']
