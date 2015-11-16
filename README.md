Requirements
============

In order to run this application, you must have OpenCV Manager installed
in your Android smartphone. You also must have the OpenCV4Android
library added to this project.

Testing
=======

You can put JPG photos of Go boards in the res/drawable folder in order
to test the image processing pipeline. They must be named 'imagemX.jpg',
where X is a number. Then, in the application, choose the menu 'Test'
and input the desired image number.

Testing results in this version
-------------------------------

These results were obtained with the code and parameters of this
specific version of the repository. The images used in the testing will
be hosted in another place in the future. Take a look at the "logs" file
if you want to see what the software detected.

Image | Result | Description
----- | ------ | -----------
1     | V      | One of the detected board's corners is slighty off
2     | V      | One of the detected board's corners is slighty off
3     | V      | One of the detected board's corners is slighty off
4     | V      | One of the detected board's corners is slighty off
5     | V      | One of the detected board's corners is considerably off
6     | V      | No problem
7     | V      | No problem
8     | X      | Finds black stones where the board is darkened by a shadow
9     | V      | One of the detected board's corners is really off
10    | X      | Thinks the board is 13x13 instead of 9x9
11    | V      | No problem
12    | X      | Doesn't detect a white stone that is under a shadow
13    | V      | No problem
14    | X      | Sees a cluster of black stones in a shadow where there are no stones
15    | V      | No problem
16    | X      | Thinks the board is 13x13 instead of 9x9
17    | V      | No problem
18    | X      | Fails to detect white stones under a shadow
19    | V      | One of the detected board's corners is slighty off
20    | V      | One of the detected board's corners is slighty off
21    | X      | Fails to detect a white stone, one of the detected board's corners is very off
22    | V      | No problem
23    | V      | No problem
24    | V      | No problem
25    | V      | One of the detected board's corners is slighty off
26    | V      | No problem
27    | V      | One of the detected board's corners is slighty off
28    | X      | Thinks the board is 13x13 instead of 9x9
29    | X      | Thinks the board is 13x13 instead of 9x9
30    | X      | Thinks the board is 13x13 instead of 9x9
31    | V      | No problem
32    | V      | One of the detected board's corners is slighty off because there's a stone over it
33    | X      | Some white stones weren't detected, but all that were were correct and all the black stones were detected correctly
34    | X      | No board is detected
35    | V      | No board is detected
