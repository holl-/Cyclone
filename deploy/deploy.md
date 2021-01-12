## Set up JDK with JAVAFX

For Java/JavaFX 14

set PATH_TO_FX="lib\javafx-sdk-14.0.1\lib"
set PATH_TO_FX_MODS="lib\javafx-jmods-14.0.1"
set JAVA_HOME="C:\Program Files\Java\jdk-14.0.1"

%JAVA_HOME%\bin\jlink --module-path %PATH_TO_FX_MODS% --add-modules java.se,javafx.fxml,javafx.web,javafx.media,javafx.swing --bind-services --output "out\jdkfx-14"

set JAVA_HOME="C:\Program Files\Java\jdkfx-14"


copy JDK and JavaFX modules to jdkfx-14\jmods


## Package Installer (Windows)

1. Build executable `JAR` file
2. Put `JIntellitype.dll`, `JIntellitype64.dll`, `Turn Off Monitor.exe` next to `JAR`
3. Run `jpackage`:

```
"D:\Program Files\Java\jdkfx-14\bin\jpackage" --name "Cyclone" --type "msi" --app-version "0.6" --win-menu --icon "deploy\icon.ico" --description "Cyclone Media Player" --file-associations "deploy\file-associations.properties" --input "out\artifacts\Cyclone_jar" --main-class player.fx.app.Launcher --main-jar Cyclone.jar
```

Additional installer options:
`--win-shortcut --win-dir-chooser`


## VM options for non-JavaFX build
--module-path "C:\Users\Philipp\IdeaProjects\Cyclone\javafx\javafx-sdk-14.0.1\lib"
--add-modules javafx.controls,javafx.fxml
--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED
--add-exports javafx.graphics/com.sun.glass.utils=ALL-UNNAMED
--add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED