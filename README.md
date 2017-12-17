# Lie_Detector
Lie Detection by voice and heart rate using Android Device and Microsoft Band 2.  

### Project Struture

### python/LD:
------
Python code for machine learning
### collect-speaker-heartrate-data.py:
python script for collecting audio and heartrate data from phone's microphone and microsoft band 2

### audioFeatureExtraction.py
From python library [pyAudioAnalysis](https://github.com/tyiannak/pyAudioAnalysis) for audio feature extraction

### utilities.py
From python library [pyAudioAnalysis](https://github.com/tyiannak/pyAudioAnalysis) for audio feature extraction

### features.py
python script for audio feature extraction from raw audio data, expected 8000 sample rate

### lie_detection_train.py
Cross Validation on Decision Tree Classifier with max depth 4 on collected data.

### CRF_all.py
Running SVMs from [PyStruct](https://pystruct.github.io/) on collected data.

### CRF_train.py
Cross Validation and Grid Search on structed learning model from [PyStruct](https://pystruct.github.io/).

### train_decision_tree.py
Cross Validation Grid Search on decision tree from sklearn and export best model in `pickel` to python/LD/training_output.

### train_logistic_regression.py
Corss Validation and Grid Search on logistic regression from sklearn

### lie_detection.py
Recieves data from server. Gets best model from `pickle` in python/LD/training_output, runs feature extraction and classification.

### Collected Data:
-----
### python/LD/data
Unlabelled data from data collection sessions. File name format is `type_of_data-speaker-number`

### python/LD/labelled-data
Labelled data from `python/LD/data` data.  0 - lie, 1 - truth.
