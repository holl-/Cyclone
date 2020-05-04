# set up JDK with JAVAFX

set PATH_TO_FX="lib\javafx-sdk-14.0.1\lib"
set PATH_TO_FX_MODS="lib\javafx-jmods-14.0.1"
set JAVA_HOME="C:\Program Files\Java\jdk-14.0.1"

%JAVA_HOME%\bin\jlink --module-path %PATH_TO_FX_MODS% --add-modules java.se,javafx.fxml,javafx.web,javafx.media,javafx.swing --bind-services --output "out\jdkfx-14"

set JAVA_HOME="C:\Program Files\Java\jdkfx-14"



# copy JDK and JavaFX modules to jdkfx-14\jmods

"C:\Program Files\Java\jdkfx-14\bin\jpackage" --name "Cyclone" --type "msi" --app-version "0.2" --win-menu --icon "deploy\icon.ico" --description "Media Player" --file-associations "deploy\file-associations.properties" --input "out\artifacts\Cyclone_jar" --main-class player.fx.app.Launcher --main-jar Cyclone.jar

--win-shortcut --win-dir-chooser

# How to add JIntellitype64.dll?