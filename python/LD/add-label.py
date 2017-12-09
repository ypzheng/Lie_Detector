import csv
import sys

chunk1 = ['data/audio-', sys.argv[1], '-', sys.argv[2],'.csv']
chunk2 = ['labelled-data/audio-', sys.argv[1], '-', sys.argv[2],'-',sys.argv[3],'.csv']
with open(''.join(chunk1),'r') as csvinput:
    with open(''.join(chunk2), 'w') as csvoutput:
        writer = csv.writer(csvoutput)

        for row in csv.reader(csvinput):
            if "lie" in sys.argv[3]:
                writer.writerow(row+['0'])
            else:
                writer.writerow(row+['1'])
