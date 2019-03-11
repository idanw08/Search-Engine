# Search_Engine
written in Java 1.8.
this search engine is designed to work on a given corpus. 
to fire the application, double click the SearchEngine_GUI.bat(!!!) file which can be found at 'out' directory.

*** NOTICE --> DO NOT RUN THE .JAR FILE, ONLY DOUBLE CLICK .BAT FILE ***

Steps to parse and index your corpus:
1. select the corpus directory. make sure the directory also contains 'stop-words.txt' file.
2. select the output directory.
3. press 'Parse and Index' button and patiently wait until a pop window will be shown to the screen. this pop screen will contain            information regarding the corpus processing. the inforamtion will contain:
   - total documents indexed
   - total terms created
   - total process time (seconds)
   - total process time (minutes)

* if you are trying to open the project in Intellij, makesure you auto enable maven updates to properly download all the dependencies.

Steps to retrieve information using the engine:
1. if not set, choose the corpus path.
2. if not set, choose the posting files and dictionary directory.
3. if step 1 or 2 accured, press the 'Load Dictionary' button. After that, you can press 'Display Dictionary' and it will be presented to you.
4. Enter your query, either manually or by using a queries file.
5. Press 'set result path directory' button to choose the directory where the result file will br created. (optional)
6. Choose cities to filter your query results. (optional)
7. press 'Run Run Run' button to initiate the search!

(C) Edan Ben Ivri & Idan Weizman.
