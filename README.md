Kifu Recorder
=============

Kifu Recorder is an app which aims to record go (weiqi, baduk) matches
in a simple and easy way. It uses a mobile device's camera to watch a go
game and creates a SGF game record as output. It works well in optimal
conditions (bright environment, good camera positioning), but it's still
a work in progress.

It can be downloaded for free on Google Play at
https://play.google.com/store/apps/details?id=br.edu.ifspsaocarlos.sdm.kifurecorder.

What's new
==========

### 1.2.0 - 25/08/2019

The main improvement in this release is dynamic board tracking. Up until
versions 1.1.X, board detection was done once and the board and camera had to
remain still for the remainder of the game recording. Any movement resulted
in detection errors. This version solves this problem for small camera or board
movements.

[image]

There is still a lot of room for improvement, so if you feel that board
tracking is not working well, it can be disabled by pressing the "Toggle board
tracking" button.

Requirements
============

In order to run this application, you must have OpenCV Manager installed
in your Android smartphone. You also must have the OpenCV4Android
library added to this project.

Your mobile device must have a moderately powerful processor and camera
for an optimal experience.
