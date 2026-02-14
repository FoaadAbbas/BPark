TO RUN THIS PROJECT IN ECLIPSE WITHOUT MAVEN:

1. Add all files in the /lib folder to the Build Path:
   Right-click project > Build Path > Configure Build Path > Libraries > Add JARs...

2. Make sure the following files exist in /lib:
   - jakarta.mail-1.6.7.jar
   - mysql-connector-java-8.0.33.jar
   - JavaFX SDK jars (from javafx-sdk-17.0.2/lib/*.jar)

3. Make sure src/ is a source folder in Eclipse:
   Right-click src > Build Path > Use as Source Folder

4. Right-click ServerUi.java > Run As > Java Application

5. If needed, add VM arguments:
   --module-path "/path/to/javafx-sdk-17.0.2/lib" --add-modules javafx.controls,javafx.fxml