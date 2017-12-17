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
        heart_file = os.path.join(data_dir, "ppg-"+speaker+"-"+number+"-"+label)
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
        
        #temp_variant = np.var(heart_for_current_speaker[temp_range,:1])
        #temp_variant = np.ones((temp_len,1))*temp_variant
        #append the aligned datas
        #aligned_data = np.append(aligned_audio,temp_variant,axis=1)
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
n_features = n_format + n_pitch + n_heart_rate+n_mfcc +n_st_features# 20 formant features + 16 pitch contour features + 75 mfcc delta coefficients

print("Extracting features and labels for {} audio windows...".format(data.shape[0]))
sys.stdout.flush()

X = np.zeros((0,n_features))
y = np.zeros(0,)

# change debug to True to show print statements we've included:
feature_extractor = FeatureExtractor(debug=False)

for i,window_with_timestamp_and_label in enumerate(data):
    window = window_with_timestamp_and_label[1:-2]
    label = data[i,-1]
    print "Extracting features for window " + str(i) + "..."
    x = feature_extractor.extract_features(window)
    if (len(x)+2 != X.shape[1]):
        print("Received feature vector of length {}. Expected feature vector of length {}.".format(len(x), X.shape[1]))
    
    #Add the heart rate features
    x = np.append(x, window_with_timestamp_and_label[-2])
    #x = np.append(x, window_with_timestamp_and_label[-3])

    X = np.append(X, np.reshape(x, (1,-1)), axis=0)
    
    y = np.append(y, label)

print("Finished feature extraction over {} windows".format(len(X)))
print("Unique labels found: {}".format(set(y)))
sys.stdout.flush()


# %%---------------------------------------------------------------------------
#
#		                Train & Evaluate Classifier
#
# -----------------------------------------------------------------------------

n = len(y)
n_classes = len(class_names)

print("\n")
print("---------------------- Decision Tree -------------------------")

trees = [] # various decision tree classifiers
trees.append(DecisionTreeClassifier(criterion="entropy", max_depth=4))

for tree_index, tree in enumerate(trees):

   total_accuracy = 0.0
   total_precision = [0.0, 0.0]
   total_recall = [0.0, 0.0]

   cv = cross_validation.KFold(n, n_folds=10, shuffle=True, random_state=None)
   for i, (train_indexes, test_indexes) in enumerate(cv):
       X_train = X[train_indexes, :]
       y_train = y[train_indexes]
       X_test = X[test_indexes, :]
       y_test = y[test_indexes]
       tree = DecisionTreeClassifier(criterion="entropy", max_depth=3)
       print("Fold {} : Training decision tree classifier over {} points...".format(i, len(y_train)))
       sys.stdout.flush()
       tree.fit(X_train, y_train)
       print("Evaluating classifier over {} points...".format(len(y_test)))

       # predict the labels on the test data
       y_pred = tree.predict(X_test)

       # show the comparison between the predicted and ground-truth labels
       conf = confusion_matrix(y_test, y_pred, labels=[0,1])

       accuracy = np.sum(np.diag(conf)) / float(np.sum(conf))
       precision = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=1).astype(float))
       recall = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=0).astype(float))

       total_accuracy += accuracy
       total_precision += precision
       total_recall += recall

       print("The accuracy is {}".format(accuracy))
       print("The precision is {}".format(precision))
       print("The recall is {}".format(recall))

       print("\n")
       sys.stdout.flush()

   print("The average accuracy is {}".format(total_accuracy/10.0))
   print("The average precision is {}".format(total_precision/10.0))
   print("The average recall is {}".format(total_recall/10.0))

   print("Training decision tree classifier on entire dataset...")
   tree.fit(X, y)
#    print("Saving decision tree visualization to disk...")
#    export_graphviz(tree, out_file='tree{}.dot'.format(tree_index), feature_names = feature_names)



