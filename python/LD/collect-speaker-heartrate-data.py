# -*- coding: utf-8 -*-
"""
Created on Wed Sep  7 15:34:11 2016

Assignment A3 : Data Collection

@author: cs390mb

This Python script receives incoming labelled audio data through
the server and saves it in .csv format to disk.

"""

import socket
import sys
import json
import numpy as np
import os

# TODO: Replace the string with your user ID
user_id = "hv3u9sd28rgi3p6e"

# TODO: Change the filename of the output file.
# You should keep it in the format "speaker-data-<speaker>-#.csv"
audio_filename="audio-caitlyn-20.csv"#"SENSOR-NAME-#OFANSWER.csv"
heart_filename="ppg-caitlyn-20.csv"
# 0=Caitlyn, 1=Allen, 2=None, 3=Andy

# TODO: Change the label to match the speaker; it must be numeric
#label = 3

data_dir = "data"

if not os.path.exists(data_dir):
    os.mkdir(data_dir)

'''
    This socket is used to send data back through the data collection server.
    It is used to complete the authentication. It may also be used to send
    data or notifications back to the phone, but we will not be using that
    functionality in this assignment.
'''
send_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
send_socket.connect(("none.cs.umass.edu", 9999))

#################   Server Connection Code  ####################

'''
    This socket is used to receive data from the data collection server
'''
receive_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
receive_socket.connect(("none.cs.umass.edu", 8888))
# ensures that after 1 second, a keyboard interrupt will close
receive_socket.settimeout(1.0)

msg_request_id = "ID"
msg_authenticate = "ID,{}\n"
msg_acknowledge_id = "ACK"

def authenticate(sock):
    """
    Authenticates the user by performing a handshake with the data collection server.

    If it fails, it will raise an appropriate exception.
    """
    message = sock.recv(256).strip()
    if (message == msg_request_id):
        print("Received authentication request from the server. Sending authentication credentials...")
        sys.stdout.flush()
    else:
        print("Authentication failed!")
        raise Exception("Expected message {} from server, received {}".format(msg_request_id, message))
    sock.send(msg_authenticate.format(user_id))

    try:
        message = sock.recv(256).strip()
    except:
        print("Authentication failed!")
        raise Exception("Wait timed out. Failed to receive authentication response from server.")

    if (message.startswith(msg_acknowledge_id)):
        ack_id = message.split(",")[1]
    else:
        print("Authentication failed!")
        raise Exception("Expected message with prefix '{}' from server, received {}".format(msg_acknowledge_id, message))

    if (ack_id == user_id):
        print("Authentication successful.")
        sys.stdout.flush()
    else:
        print("Authentication failed!")
        raise Exception("Authentication failed : Expected user ID '{}' from server, received '{}'".format(user_id, ack_id))


try:
    print("Authenticating user for receiving data...")
    sys.stdout.flush()
    authenticate(receive_socket)

    print("Authenticating user for sending data...")
    sys.stdout.flush()
    authenticate(send_socket)

    print("Successfully connected to the server! Waiting for incoming data...")
    sys.stdout.flush()

    previous_json = ''

    audio_data=[]
    heart_data=[]

    while True:
        try:
            message = receive_socket.recv(1024).strip()
            json_strings = message.split("\n")
            json_strings[0] = previous_json + json_strings[0]
            for json_string in json_strings:
                try:
                    data = json.loads(json_string)
                except:
                    previous_json = json_string
                    continue
                previous_json = '' # reset if all were successful
                sensor_type = data['sensor_type']

                if (sensor_type == u"SENSOR_AUDIO"):
                    t = data['data']['t']
                    audio_buffer=data['data']['values']
                    print("Received audio data of length {}".format(len(audio_buffer)))
                    audio_instance = [t]
                    audio_instance.extend(audio_buffer)
                    #labelled_instance.append(label)
                    audio_data.append(audio_instance)
                if (sensor_type == u"SENSOR_HR"):
                    t = data['data']['t']
                    heart_buffer = data['data']['value']
                    print("Received heart rate data {}".format(heart_buffer))
                    heart_data.append([t, heart_buffer])


            sys.stdout.flush()
        except KeyboardInterrupt:
            # occurs when the user presses Ctrl-C
            print("User Interrupt. Quitting...")
            raise KeyboardInterrupt
            break
        except Exception as e:
            # ignore exceptions, such as parsing the json
            # if a connection timeout occurs, also ignore and try again. Use Ctrl-C to stop
            # but make sure the error is displayed so we know what's going on
            if (e.message != "timed out"):  # ignore timeout exceptions completely
                print(e)
            pass
except KeyboardInterrupt:
    # occurs when the user presses Ctrl-C
    print("User Interrupt. Saving labelled data...")
    audio_data = np.asarray(audio_data)
    with open(os.path.join(data_dir, audio_filename), "wb") as f:
        np.savetxt(f, audio_data, delimiter=",")

    heart_data = np.asarray(heart_data)
    with open(os.path.join(data_dir, heart_filename), "wb") as f:
        np.savetxt(f, heart_data, delimiter=",")
finally:
    print >>sys.stderr, 'closing socket for receiving data'
    receive_socket.shutdown(socket.SHUT_RDWR)
    receive_socket.close()

    print >>sys.stderr, 'closing socket for sending data'
    send_socket.shutdown(socket.SHUT_RDWR)
    send_socket.close()
