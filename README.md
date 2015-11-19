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

The parameters used were:

Step         | Parameters
------------ | ----------
Dilation     | 2 times
Canny        | 30 and 90
approxPolyDP | 0.04

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

Ideia: A detecção de bordas parece estar OK, precisa checar o detector de quadriláteros para ver porque alguns tabuleiros
estão sendo detectados bem errados.

Anotar também as posições em que estão os 4 cantos. Isso vai dar trabalho mas é bom para verificar se a detecção do tabuleiro está
melhorando ou não

With different parameters:

Step         | Parameters
------------ | ----------
Dilation     | 3 times
Canny        | 30 and 90
approxPolyDP | 0.015

Image | Result | Description
----- | ------ | -----------
1     | V      | No problem
2     | V      | No problem
3     | V      | One of the detected board's corners is slighty off
4     | X      | No board is detected
5     | V      | One of the detected board's corners is considerably off
6     | V      | No problem
7     | V      | No problem
8     | X      | Finds black stones where the board is darkened by a shadow
9     | X      | Board is detected OK, but finds one black stone where there's a shadow
10    | V      | No problem
11    | V      | No problem
12    | X      | Doesn't detect a white stone that is under a shadow
13    | X      | Doesn't detect a white stone that is under a shadow
14    | X      | Sees a cluster of black stones in a shadow where there are no stones
15    | V      | No problem
16    | V      | No problem
17    | V      | One of the borders is off
18    | X      | Fails to detect white stones under a shadow
19    | V      | No problem
20    | V      | No problem
21    | X      | Fails to detect a white stone
22    | X      | Fails to detect a white stone
23    | X      | Fails to detect a white stone
24    | X      | Fails to detect a white stone
25    | X      | Fails to detect a white stone
26    | X      | Fails to detect two white stones, one for the corners is considerably off
27    | X      | Fails to detect a white stone
28    | V      | No problem
29    | V      | No problem
30    | V      | No problem
31    | V      | No problem
32    | V      | One of the detected board's corners is slighty off because there's a stone over it
33    | X      | Thinks the board is 13x13
34    | X      | Only 1 stone not correctly identified
35    | V      | No board is detected

18/11/2015

The best results obtained as of yet for the board detection. A small
contour filtering step was added that helps in reducing some noise.

Step         | Parameters
------------ | ----------
Dilation     | 1 time
Canny        | 30 and 100
approxPolyDP | 0.012

Image | Result | Description
----- | ------ | -----------
1     | V      | No problem
2     | V      | No problem
3     | V      | Corner slightly off
4     | X      | No board is detected
5     | V      | No problem
6     | V      | No problem
7     | V      | No problem
8     | X      | Finds black stones where the board is darkened by a shadow
9     | X      | Board is detected OK, but finds one black stone where there's a shadow
10    | V      | No problem
11    | V      | No problem, one of the corners very slightly off
12    | X      | Doesn't detect a white stone that is under a shadow
13    | V      | No problem
14    | X      | Sees a cluster of black stones in a shadow where there are no stones
15    | V      | No problem
16    | V      | No problem
17    | V      | No problem
18    | X      | Fails to detect white stones under a shadow
19    | V      | One of the corners is off
20    | V      | One of the corners is slightly off
21    | X      | Fails to detect a white stone
22    | V      | One of the corners is a little off
23    | X      | Fails to detect a white stone
24    | V      | No problem
25    | V      | One of the corners is a little off
26    | V      | No problem
27    | V      | One of the corners is slightly off
28    | V      | No problem
29    | V      | No problem
30    | V      | No problem
31    | X      | No board was detected
32    | V      | One of the detected board's corners is slighty off because there's a stone over it
33    | X      | All black stones detected correctly
34    | X      | Only 1 stone not correctly identified
35    | V      | No board is detected
