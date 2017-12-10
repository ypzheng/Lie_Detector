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
from time import time

import os
import sys
import numpy as np
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from features import FeatureExtractor
from sklearn import cross_validation
from sklearn.metrics import confusion_matrix
from sklearn.cross_validation import train_test_split
from sklearn.svm import LinearSVC
from sklearn.linear_model import LogisticRegression

import pickle

from pystruct.models import ChainCRF

from pystruct.models import MultiClassClf
from pystruct.learners import (NSlackSSVM, OneSlackSSVM,
                               SubgradientSSVM, FrankWolfeSSVM)

from sklearn.metrics import hamming_loss
from sklearn.datasets import fetch_mldata
from sklearn.metrics import mutual_info_score
from scipy.sparse.csgraph import minimum_spanning_tree

from pystruct.learners import OneSlackSSVM
from pystruct.models import MultiLabelClf, ChainCRF
from pystruct.datasets import load_scene

import itertools
from sklearn.model_selection import GridSearchCV
# %%---------------------------------------------------------------------------
#
#		                 Load Data From Disk
#
# -----------------------------------------------------------------------------

data_dir = 'labelled-data' # directory where the data files are stored

output_dir = 'training_output' # directory where the classifier(s) are stored

if not os.path.exists(output_dir):
    os.mkdir(output_dir)

# the filenames should be in the form 'speaker-data-subject-1.csv', e.g. 'speaker-data-Erik-1.csv'. If they
# are not, that's OK but the progress output will look nonsensical

class_names = [] # the set of classes, i.e. speakers

data = np.zeros((0,8003)) #8003 = 1 (timestamp) + 8000 (for 8kHz audio data) + 1 (heart rate) + 1 label

for filename in os.listdir(data_dir):
    if filename.endswith(".csv") and filename.startswith("audio-caitlyn"):
        filename_components = filename.split("-") # split by the '-' character
        
        speaker = filename_components[1]
        number = filename_components[2]
        label = filename_components[3]

        print("Loading data for {}.".format(speaker))
        if speaker not in class_names:
            class_names.append(speaker)
        speaker_label = class_names.index(speaker)
        sys.stdout.flush()
        
        #Audio file
        audio_file = os.path.join(data_dir, filename)
        audio_for_current_speaker = np.genfromtxt(audio_file, delimiter=',')
        
        #Heart rate
        heart_file = os.path.join(data_dir, "ppg-caitlyn-"+number+"-"+label)
        heart_for_current_speaker = np.genfromtxt(heart_file, delimiter=",")

        len_audio = len(audio_for_current_speaker)
        len_heart = len(heart_for_current_speaker)
        
        print("Loaded {} raw audio data samples.".format(len_audio))
        print("Loaded {} raw heart rate data samples.".format(len_heart))
        sys.stdout.flush()
        
        #alignment for heart rate and audio file (same number of rows)
        temp_len = min(len_audio,len_heart)
        temp_range = np.arange(temp_len)
        #time + audio data 
        aligned_audio = audio_for_current_speaker[temp_range,:-1]
        #heart rate + label
        aligned_heart = heart_for_current_speaker[temp_range,1:3]
        
        #append the aligned datas
        aligned_data = np.append(aligned_audio,aligned_heart,axis=1)
        
        #finally append the rows from aligned data to data
        data=np.append(data, aligned_data, axis=0)


print("Found data for {} speakers : {}".format(len(class_names), ", ".join(class_names)))

# %%---------------------------------------------------------------------------
#
#		                Extract Features & Labels
#
# -----------------------------------------------------------------------------

# You may need to change this depending on how you compute your features
n_format = 55
n_pitch = 64
n_heart_rate = 1
n_mfcc = 0 #507
n_st_features = 8 + 13
n_features = n_format + n_pitch +n_mfcc +n_st_features+n_heart_rate# 20 formant features + 16 pitch contour features + 75 mfcc delta coefficients

print("Extracting features and labels for {} audio windows...".format(data.shape[0]))
sys.stdout.flush()

X = np.zeros((0,n_features))
y = np.zeros((0,1))

# change debug to True to show print statements we've included:
feature_extractor = FeatureExtractor(debug=False)

for i,window_with_timestamp_and_label in enumerate(data):
    window = window_with_timestamp_and_label[1:-2]
    label = data[i,-1]
    print "Extracting features for window " + str(i) + "..."
    x = feature_extractor.extract_features(window)
    if (len(x)+1 != X.shape[1]):
        print("Received feature vector of length {}. Expected feature vector of length {}.".format(len(x), X.shape[1]))
    
    #Add the heart rate features
    x = np.append(x, window_with_timestamp_and_label[-2])
   
    X = np.append(X, np.reshape(x, (1,-1)), axis=0)
    
    y = np.append(y, label)

print("Finished feature extraction over {} windows".format(len(X)))
print("Unique labels found: {}".format(set(y)))
sys.stdout.flush()



print("\n")
print("---------------------- Grid Search -------------------------")
#grid_search
# param_grid = [
#     {
#     'n_estimators':[450],
#     'max_depth':[5],
#     'min_samples_split':[8],
#     'min_samples_leaf':[1],
#     'random_state':[1]}
#     ]

param_grid = [{
    'penalty':['l2'],
    'tol':[1e-7,1e-5,1e-6],
    'C':[0.85,0.5,0.75],
    'solver':['newton-cg', 'lbfgs', 'liblinear', 'sag','saga']
}]

scores = ['precision', 'recall']

print '\n'
print "Starting Grid Search"

for score in scores:
    clf = GridSearchCV(LogisticRegression(), param_grid, cv=10,scoring="%s_macro"%score)
    clf.fit(X,y)
    print "Best Params with %s scoring"%score
    print(clf.best_params_)
    print "Best Score with %s scoring"%score
    print clf.best_score_
    mean= clf.cv_results_['mean_test_score']
    stds = clf.cv_results_['std_test_score']
    params = clf.cv_results_['params']
