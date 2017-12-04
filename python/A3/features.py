# -*- coding: utf-8 -*-
"""
Created on Tue Sep 27 13:08:49 2016

@author: cs390mb

This is the solution code for extracting features of windows of audio data.
In particular, we look at formants, pitch and delta coefficients.

You will need to install the following dependencies:
    pip install python_speech_features
    pip install git+git://git.aubio.org/git/aubio
    pip install scikits.talkbox

And for extra credit, you will also need:
    pip install pocketsphinx
    pip install SpeechRecognition


"""

import numpy as np
import math
from scipy.signal import lfilter
from scikits.talkbox import lpc

from aubio import pitch
from python_speech_features import mfcc

# Uncomment to complete the speech recognition extra credit:
#import speech_recognition as sr
#from pocketsphinx.pocketsphinx import *
#from sphinxbase.sphinxbase import *

class FeatureExtractor():
    def __init__(self, debug=True):
        self.debug = debug

    def _compute_formants(self, audio_buffer):
        """
        Computes the frequencies of formants of the window of audio data, along with their bandwidths.

        A formant is a frequency band over which there is a concentration of energy.
        They correspond to tones produced by the vocal tract and are therefore often
        used to characterize vowels, which have distinct frequencies. In the task of
        speaker identification, it can be used to characterize a person's speech
        patterns.

        This implementation is based on the Matlab tutorial on Estimating Formants using
        LPC (Linear Predictive Coding) Coefficients:
        http://www.mathworks.com/help/signal/ug/formant-estimation-with-lpc-coefficients.html.

        Help porting this to Python was found here :
        http://stackoverflow.com/questions/25107806/estimate-formants-using-lpc-in-python.

        Why LPC? http://dsp.stackexchange.com/questions/2482/speech-compression-in-lpc-how-does-the-linear-predictive-filter-work-on-a-gene

        Here are some more details on why linear predictive analysis is a generally powerful tool
        in audio processing: http://iitg.vlab.co.in/?sub=59&brch=164&sim=616&cnt=1108.

        """

        # Get Hamming window. More on window functions can be found at https://en.wikipedia.org/wiki/Window_function
        # The idea of the Hamming window is to smooth out discontinuities at the edges of the window.
        # Simply multiply to apply the window.
        N = len(audio_buffer)
        Fs = 8000 # sampling frequency
        hamming_window = np.hamming(N)
        window = audio_buffer * hamming_window

        # Apply a pre-emphasis filter; this amplifies high-frequency components and attenuates low-frequency components.
        # The purpose in voice processing is to remove noise.
        filtered_buffer = lfilter([1], [1., 0.63], window)

        # Speech can be broken down into (1) The raw sound emitted by the larynx and (2) Filtering that occurs when transmitted from the larynx, defined by, for instance, mouth shape and tongue position.
        # The larynx emits a periodic function defined by its amplitude and frequency.
        # The transmission is more complex to model but is in the form 1/(1-sum(a_k * z^-k)), where the coefficients
        # a_k sufficiently encode the function (because we know it's of that form).
        # Linear Predictive Coding is a method for estimating these coefficients given a pre-filtered audio signal.
        # These value are called the roots, because the are the points at which the difference
        # from the actual signal and the reconstructed signal (using that transmission function) is closest to 0.
        # See http://dsp.stackexchange.com/questions/2482/speech-compression-in-lpc-how-does-the-linear-predictive-filter-work-on-a-gene.

        # Get the roots using linear predictive coding.
        # As a rule of thumb, the order of the LPC should be 2 more than the sampling frequency (in kHz).
        ncoeff = 2 + Fs / 1000
        A, e, k = lpc(filtered_buffer, ncoeff)
        roots = np.roots(A)
        roots = [r for r in roots if np.imag(r) >= 0]

        # Get angles from the roots. Each root represents a complex number. The angle in the
        # complex coordinate system (where x is the real part and y is the imaginary part)
        # corresponds to the "frequency" of the formant (in rad/s, however, so we need to convert them).
        # Note it really is a frequency band, not a single frequency, but this is a simplification that is acceptable.
        angz = np.arctan2(np.imag(roots), np.real(roots))

        # Convert the angular frequencies from rad/sample to Hz; then calculate the
        # bandwidths of the formants. The distance of the roots from the unit circle
        # gives the bandwidths of the formants (*Extra credit* if you can explain this!).
        unsorted_freqs = angz * (Fs / (2 * math.pi))

        # Let's sort the frequencies so that when we later compare them, we don't overestimate
        # the difference due to ordering choices.
        freqs = sorted(unsorted_freqs)

        # also get the indices so that we can get the bandwidths in the same order
        indices = np.argsort(unsorted_freqs)
        sorted_roots = np.asarray(roots)[indices]

        #compute the bandwidths of each formant
        bandwidths = -1/2. * (Fs/(2*math.pi))*np.log(np.abs(sorted_roots))

        if self.debug:
            print("Identified {} formants.".format(len(freqs)))

        return freqs, bandwidths

    def _compute_formant_features(self, window):
        """
        Computes the distribution of the frequencies of formants over the given window.
        Call _compute_formants to get the formats; it will return (frequencies,bandwidths).
        You should compute the distribution of the frequencies in fixed bins.
        This will give you a feature vector of length len(bins).
        """
        formants, _ = self._compute_formants(window)
        # TODO: compute features based on formants, return in a 1D array
        hist, _ = np.histogram(formants, bins = 55, range = (0, 5500))
        return hist

    def _compute_pitch_contour(self, window):
        """
        Computes the pitch contour of the audio data, along with the confidence curve.
        """
        win_s = 4096 # fft size
        hop_s = 512  # hop size
        samplerate=8000
        tolerance=0.8
        pitch_o = pitch("yin", win_s, hop_s, samplerate)
        pitch_o.set_unit("midi")
        pitch_o.set_tolerance(tolerance)

        INT_TO_FLOAT = 1. / 32768. #2^15 (15 not 16 because it's signed)


        # convert to an array of 32-bit floats in the range [-1,1]
        pitch_input = np.float32(window * INT_TO_FLOAT)

        pitch_contour = []
        confidence_curve = []

        index = 0
        while True:
            samples = pitch_input[index*hop_s:(index+1)*hop_s]
            pitch_output = pitch_o(samples)[0]
            confidence = pitch_o.get_confidence()
            pitch_contour += [pitch_output] # append to contour
            confidence_curve += [confidence]
            index += 1
            if (index+1)*hop_s > len(pitch_input): break # stop when there are no more frames

        if self.debug:
            print("Pitch contour has length {}.".format(len(pitch_contour)))
        return pitch_contour, confidence_curve

    def _compute_pitch_features(self, window):
        """
        Computes pitch features of the audio data. Use the _compute_pitch_contour
        method to get the pitch over time. Remember it also returns the confidence
        curve, which you won't need, but you could use for extra credit to improve
        performance (you have to be very clever!).

        You want to compute the distribution of the pitch values in fixed bins.
        This will give you a feature vector of length len(bins).

        You may also want to return the average pitch and standard deviation.
        """
        pitch_contour, _ = self._compute_pitch_contour(window)

        # TODO: compute features based on the pitch contour and return them in a 1D array
        hist, _ = np.histogram(pitch_contour, bins = 64, range = (0,128))
        return hist

    def _compute_mfcc(self, window):
        """
        Computes the MFCCs of the audio data. MFCCs are not computed over
        the entire 1-second window but instead over frames of between 20-40 ms.
        This is large enough to capture the power spectrum of the audio
        but small enough to be informative, e.g. capture vowels.

        The number of frames depends on the frame size and step size.
        By default, we choose the frame size to be 25ms and frames to overlap
        by 50% (i.e. step size is half the frame size, so 12.5 ms). Then the
        number of frames will be the number of samples (8000) divided by the
        step size (12.5) minus one because the frame size is too large for
        the last frame. Therefore, we expect to get 79 frames using the
        default parameters.

        The return value should be an array of size n_frames X n_coeffs, where
        n_coeff=13 by default.
        """
        mfccs = mfcc(window,8000,winstep=.0125)
        if self.debug:
            print("{} MFCCs were computed over {} frames.".format(mfccs.shape[1], mfccs.shape[0]))
        return mfccs

    def _compute_delta_coefficients(self, window, n=2):
        """
        Computes the delta of the MFCC coefficients. See the equation in the assignment details.

        The running-time is O(n_frames * n), so we generally want relatively small n. Default is n=2.

        """
        mfcc_feats = self._compute_mfcc(window)
        n_frames, n_coeffs = mfcc_feats.shape
        # TODO: implement delta coefficient calculation and return the result as 975-element 1D array
        temp_denominator=0
        for p in range(1,n+1):
            temp_denominator += p*p

        denominator = 2*temp_denominator

        results = []
        for i in range(n,(n_frames-n)):
            temp = np.zeros((1,n_coeffs))
            for j in range(1, n+1):
                temp = temp+j*(mfcc_feats[i+j,:]-mfcc_feats[i-j,:])

            delta_coef = temp/denominator
            results = np.append(results,delta_coef)
        return results

    def _compute_mean(self, window):
        return np.mean(window)

    def _compute_variance(self, window):
        return np.var(window)

    def _compute_median(self, window):
        return np.median(window)


    def _recognize_speech(window):
        """
        *Extra Credit*:

        Using one of the Speech Recognition APIs, such as
        Google text-to-speech, Microsoft's Bing Speech or IBM's Watson, etc.,
        this function will convert the audio window to spoken text. Due to
        API constraints that limit the number of calls per minute as well as
        the fact that speech-to-text over 1 second is unreliable and doesn't
        capture enough words to identify speaker, you should make calls to
        the speech recognition API less frequently, i.e. every 15 seconds.

        That means you can't use this data to make predictions on 1-second
        windows, unless you use the last 15 seconds to make predictions on
        the current 1-second window; however, the problem with this is that
        it assumes that the same person has been speaking for the last 15
        seconds. For this extra credit portion, simply make the assumption
        that we are in a situation where each speaker speaks for a longer
        period of time. This could be realistic in certain settings, such
        as during consecutive speeches or presentations.

        Here is how to use the speech recognition API:

            r = sr.Recognizer()
            # convert to 16-bit integers (default is 64 on most machines):
            audio_byte_array = np.int16(window)
            # pass in sampling rate (8000) and number of bytes per samples (2 = 16-bit audio data).
            audio=sr.AudioData(audio_byte_array, 8000, 2)

        To make the API calls, you need an API key. Google's speech-to-text, for example:

            # recognize speech using Google Speech Recognition
            GOOGLE_SPEECH_API_KEY = "REPLACE WITH YOUR KEY"
            try:
                # for testing purposes, we're just using the default API key
                print("Google Speech Recognition thinks you said " + r.recognize_google(audio,
                        key=GOOGLE_SPEECH_API_KEY))
            except sr.UnknownValueError:
                print("Google Speech Recognition could not understand audio")
            except sr.RequestError as e:
                print("Could not request results from Google Speech Recognition service; {0}".format(e))

        There is also other systems you can use, e.g. Wit.ai (r.recognize_wit()), Bing Speech
        (r.recognize_bing) and api.ai (r.recognize_api), houndify, IBM's Watson and Sphinx (which is the
        only one that can run locally!). From my experience, Bing Speech is the most accurate and Google
        Speech-to-text is relatively accurate as well. Wit.ai is decent. Watson and Api.ai aren't very
        accurate at all. And Sphinx doesn't work at all! Microsoft recently announced that it reached human-
        level speech recognition; I'm not entirely sure I believe it though, because Bing Speech, their
        previous version, is still pretty far from it. In any case, try them all out and see if it works for
        you!

        """

    def _compute_vocabulary_features(words_spoken):
        """
        *Extra Credit*:

        Compute the counts of each word as returned by the speech recognition
        API calls. Each feature then corresponds to a word in the English
        language. That means that your feature vector will be VERY BIG!
        Fortunately we don't have to store it all since very few of the features
        will be non-zero. Use a sparse matrix to do this. If you try out this
        feature, there are two things you need to consider: (1) Since speech
        is recognized less frequently than 1 second windows, e.g. once every 15s,
        we are working under the assumption that each speaker is talking for a
        relatively long period of time; and (2) in order to capture enough
        inter-speaker vocabulary diversity, you need A LOT of data. You may
        need to collect a significantly larger dataset in order for it to work.
        If you try this out, you don't need to collect a lot of data, but we'd
        still be interested in seeing your performance metrics.
        """

    def _compute_delta_delta_coefficients(self, window, n=2):
        """
        Computes the delta of the MFCC coefficients. See the equation in the assignment details.

        The running-time is O(n_frames * n), so we generally want relatively small n. Default is n=2.
        """
        delta_feats = self._compute_delta_coefficients(window)

        n_frames = delta_feats.shape[0]
        # TODO: implement delta coefficient calculation and return the result as 975-element 1D array
        temp_denominator=0
        for p in range(1,n+1):
            temp_denominator += p*p

        denominator = 2*temp_denominator
        results = []
        for i in range(n,(n_frames-n)):
            temp = 0
            for j in range(1, n+1):
                temp = temp+j*(delta_feats[i+j]-delta_feats[i-j])
            delta_delta_coef = temp/denominator
            results = np.append(results,delta_delta_coef)
        return results

    def extract_features(self, window, debug=True):
        """
        Here is where you will extract your features from the data in
        the given window.

        Make sure that x is a vector of length d matrix, where d is the number of features.

        """
        #n=30 88% n=25 88% n=20 88%  n=15 87% n=10 84% n=4 83%
        x = []

        x = np.append(x, self._compute_formant_features(window))
        x = np.append(x, self._compute_pitch_features(window))
        x = np.append(x, self._compute_delta_coefficients(window, n=20 ))
        # x = np.append(x, self._compute_delta_delta_coefficients(window, n=20))

        return x
