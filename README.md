# WormPlay
Interactive player for playing and rendering a Performance Worm from the WormFile format while sounding the hyperlinked audio file. 

![WormPlay Application](./wormplay.png)

This JAVA application implements the original Performance Worm concept as described in the following paper:

Langner, J., & Goebl, W. (2003). Visualizing expressive performance in tempo–loudness space. Computer Music Journal, 27(4), 69–83. https://doi.org/10.1162/014892603322730514

The application is available as a JAR file in the releases section of this repository. The source code is also available for those interested in exploring or modifying the application.

To install, simply download the content of the dist folder including the dist/lib subdirectory. You will additionally need to hav the Java Media Framework (jmf.jar) copied into the dist/lib folder. 

To run the application, cd into the dist folder and type the following command in the terminal:

```
java -jar WormPlay.jar myWormFile.worm -d -150 -s 50
```

This will open the application and load the specified WormFile. The -d and -s options specify the delay of the visual display relative to the audio and the sleep time in milliseconds for the update loop of the application, respectively. Adjust these parameters as needed for optimal performance on your system.

To show the command line options, simply run the application without any arguments:

```
java -jar WormPlay.jar -h
```

To convert the worm file to a series of image files, use the following command:

```java -jar WormPlay.jar myWormFile.worm movie gif```

This will create a series of gif files in the current directory, one for each frame of the worm animation. You can adjust the output format (gif, png, jpg) as needed.