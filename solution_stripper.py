# -*- coding: utf-8 -*-
"""
Created on Mon May  8 10:55:31 2017

@author: snoran

CS390MB Solution Code Stripper

This file is used to maintain the My Activities solution and 
starter code in parallel, by stripping solution blocks out of 
the solution code and writing the remaining code to the 
starter codebase.

The source directory contains the solution code and has a 
corresponding private git repository. The desination directory 
will contain the starter code and has a corresponding 
public git repository.

Code blocks in the solution code are enclosed by the start tag 
<SOLUTION A#> and the end tag </SOLUTION A#>. <SOLUTION/ A#> also 
indicate in-line tags.

See http://stackoverflow.com/questions/1994488/copy-file-or-directories-recursively-in-python

"""

import os

root_src_dir = 'MyActivitiesSolution'    # Path/Location of the source directory
root_dst_dir = 'MyActivitiesStarter'  # Path to the destination folder

files_to_ignore = ['.git'] # don't overwrite starter git repo

for src_dir, dirs, files in os.walk(root_src_dir):
    dst_dir = src_dir.replace(root_src_dir, root_dst_dir, 1)
    # mkdir if does not exist
    if not os.path.exists(dst_dir):
        os.makedirs(dst_dir)
    for file_ in files:
        if file_ in files_to_ignore:
            continue
        src_file = os.path.join(src_dir, file_)
        dst_file = os.path.join(dst_dir, file_)
        if os.path.exists(dst_file):
            os.remove(dst_file)
        with open(src_file, "rb") as f:
            write_line = True
            current_line = 0
            lines = []
            for line in f:
                current_line += 1
                if "<SOLUTION/" in line:
                    if write_line == False:
                        print "WARNING : Found in-line tag within solution block ({} : line {})".format(src_file, current_line)
                    continue
                if "<SOLUTION A" in line:
                    if write_line == False:
                        print "WARNING : Expected closing tag; found opening tag ({} : line {})".format(src_file, current_line)
                    write_line = False
                if write_line:
                    lines.append(line)
                if "</SOLUTION A" in line:
                    if write_line == True:
                        print "WARNING : Expected opening tag; found closing tag ({} : line {})".format(src_file, current_line)
                    write_line = True
        if write_line == False:
            print "WARNING : Missing closing tag (EOF)"
        with open(dst_file, "wb") as f:
            for line in lines:
                f.write(line)