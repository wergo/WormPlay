The WORM file format ('.worm')


copyright by OeFAImusic 2002
http://www.oefai.at/music
VERSION 1.07

by Werner Goebl & Simon Dixon (15. Jan. 2004)
update by Maarten Grachten (13. Feb. 2008)

The WORM file format (.worm) is a text based format to store performance data of a 
certain performance of a musical piece as extracted from an audio file. The file 
contains header information (Piece, Performer, metrical description, meta infos) 
and file information (tempo, loudness, metrical structure). 

HEADER information
------------------
The WORM file starts with a upper case 'WORM' followed by the Version number.
Each header item is delimited by a colon and a tab (':	'). The order of 
the individual items is free, except 'Length' must be at the end of the header.

Information on the piece:
'Piece'............description of the piece (string)
'Composer'.........full name of composer    (string)
'Key'..............the key the piece is in (if there is one) (string)
'Indication'.......tempo indication or a special mood on top of the score (string)
'BeatLevel'........the beat as indicated in the score (denominator of time signature,
                   eg. '1/8') (string)
'TrackLevel'.......the metrical level tracked relative to the beat (see above), 
                   e.g. '1/2' (string)
'Upbeat'...........number of upBeats (relative to beat level, eg. '0.5') (string)
'BeatsPerBar'......number of beats per bar (numerator of time signature in the score) 
                   (integer)

Information on the performance:
'AudioFile'........the file location of the corresponding WAV file (string)
'Performer'........full name of performer (group/orchestra) (string)
'YearOfRecording'..year of recording (4 digits string)

Information on the Worm file:
'FrameLength'......the time span between the data lines in the file (in seconds, 
                   3 column file), when zero, there is a fourth column to indicate 
                   the time (%5.3f)
'XLabel'...........label to display along the x-axis of the worm (e.g.: "Tempo") (string)
'YLabel'...........label to display along the y-axis of the worm (e.g.: "Loudness") (string)
'XScale'...........whether the x axis is linear or logarithmic (e.g. 'lin' or 'log') (string)
'YScale'...........whether the y axis is linear or logarithmic (e.g. 'lin' or 'log') (string)
'LoudnessUnits'....units of the loudness information (eg. 'sone' or 'dB') (string) (deprecated by YUnits)
'XUnits'...........units of the x information (eg. 'BMP') (string)
'YUnits'...........units of the y information (eg. 'sone' or 'dB') (string)
'Axis'.............to specify the window for displaying the worm (four space 
                   delimited integers)
'Smoothing'........whether the file was smoothed ('gaussian') or contains raw data 
                   ('none').  In case of smoothing, you find left/right 
                   window in seconds (lefts rights), and left/right window in beats 
                   (leftb rightb)
'Length'...........number of lines in the file (always at the end of the header) 
                   (integer)
'StartBarNumber'...Bar number at which the Worm file starts accoring to the score 
                   (the same upbeat as in the beginning applies here as well)
'TempoLate'........Information, whether the first tempo value is doubled [1], or the
                   last [0].

file MATRIX information
-----------------------
'tempo'............in beats per minute relative to beat level as indicated in the 
                   header.
'loudness'.........in the units specified in the header.
'flags'............binary flags indicating (hierarchical) metrical information in the 
                   file
                   1.......track level (meaning: this line is a track)
                   2.......beat level
                   4.......bar level
                   8.......segmentation level 1 (shortest, e.g. 2-bar)
                  16.......segmentation level 2
                  32.......segmentation level 3 (e.g. 8-bar phrases)
                  64.......segmentation level 4 (e.g. 16-bar phrases)
'melody'...........0.......for accompaniment
                   1.......for melodic gesture start
                   2.......for melodic gesture end
                           (field melodyInfo = true created, if 5th column)

Version 1.0:
column     1          2          3
           tempo     loudness    flags

from Version 1.01 (with frameLength set to 0)
column     1          2          3          4
           time       tempo      loudness   flags        

from Version 1.07 (5th column with melody info)
column     1          2          3          4      5
           time       tempo      loudness   flags  melody       


Parser for Matlab
-----------------
(/raid/user/wernerg/m/)'readWorm.m' & 'writeWorm.m'
