# 1JPM
1 Java Project Manager (1JPM), is a Maven/Gradle alternative with a twist. 
It's a single Java file itself, which should be edited by you to configure your project.

Meaning instead of writing XML (Maven) or Groovy (Gradle), your build file is Java code too.
**To be more exact, you [download/copy the JPM.java file](https://github.com/Osiris-Team/1JPM/releases/) into your project, open a terminal and execute:**

- Java 11 and above: `java JPM.java jar`
- Java 8 to 10:  `javac JPM.java && java -cp . JPM jar`
- Earlier Java versions are not supported

to build your project (`jar` is a task, which compiles and creates a jar file from your code).
If you want to include dependencies in the jar run `fatJar` instead.

1JPM works in a very similar way to Gradle, however
everything in 1JPM is a plugin (even all its tasks), 
and third-party plugins can be added simply by appending their Java code at the bottom of the JPM class
(must be written in Java 8 and not use external dependencies).

```java
class ThisProject extends JPM.Project {
  static{ // Task related examples (optional):
    JPM.ROOT.pluginsAfter.add(new JPM.Plugin("deploy").withExecute((project) -> {
      // Register custom task named "deploy", and run your tasks code here.
      // If this throws an exception the whole build stops.
    }));
    JPM.Build.GET.pluginsAfter.add(new JPM.Plugin("").withExecute((project) -> {
      // Run something after/before another task.
      // In this case after the "build" task.
    }));
  }

  public ThisProject(List<String> args) {
    // Override default configurations
    this.groupId = "com.mycompany";
    this.artifactId = "my-project";
    this.version = "1.0.0";
    this.mainClass = "com.mycompany.MyMainClass";
    this.jarName = "my-project.jar";
    this.fatJarName = "my-project-with-dependencies.jar";

    // Add some example dependencies
    addDependency("junit", "junit", "4.13.2");
    addDependency("org.apache.commons", "commons-lang3", "3.12.0");
    //implementation("org.apache.commons:commons-lang3:3.12.0"); // Same as above but similar to Gradle DSL

    // Add some compiler arguments
    addCompilerArg("-Xlint:unchecked");
    addCompilerArg("-Xlint:deprecation");
  }

  public static void main(String[] args) throws Exception {
    JPM.main(args);
  }
}

class ThirdPartyPlugins extends JPM.Plugins{
  // Add third party plugins below:
  // (If you want to develop a plugin take a look at "JPM.Clean" class further below to get started)
}


// 1JPM version 1.0.3 by Osiris-Team
// To upgrade JPM, replace the JPM class below with its newer version
public class JPM {
  //...
}
```

## Why a single file?
- IDEs should provide decent auto-complete when JPM.java is in the project root (where your pom.xml/build.gradle)
usually is.
- To access all your IDEs fancy features, you can also add JPM.java to ./src/main/java.
This also grants the rest of your project easy access to its data, like your projects version for example.
Just make sure that your working dir is still at ./ when executing tasks.
- Simple drag and drop installation.

## Progress
1JPM is a new project and currently an early-access/beta release,
thus does not contain all the functionalities of the other
major build tools like Maven and Gradle, however should provide the basic and most used functions.

Below you can see some Gradle tasks that are available in 1JPM (or planned).

### `build` ✅

- **`clean`**: ✅ Deletes the build directory.
- **`compileJava`**: ✅ Compiles Java source files.
    - Sub-task: `compileJava.options.compilerArgs`: ✅ Configures Java compiler arguments.
- **`processResources`**: ✅ Processes resource files (e.g., copying them to the output directory).
    - Sub-task: `processResources.expand(project.properties)`: ✅ Expands placeholders in resource files.
- **`classes`**: ✅ Assembles the compiled classes (depends on `compileJava` and `processResources`).
- **`compileTestJava`**: Compiles test Java source files.
- **`processTestResources`**: Processes test resource files.
- **`testClasses`**: Assembles the compiled test classes (depends on `compileTestJava` and `processTestResources`).
- **`test`**: Runs the unit tests (depends on `testClasses`).
    - Sub-task: `test.useJUnitPlatform()`: Configures JUnit Platform for testing.
- **`jar`**: ✅ Assembles the JAR file.
    - Sub-task: `jar.manifest`: ✅ Configures the JAR manifest.
- **`javadoc`**: Generates Javadoc for the main source code.
- **`assemble`**: ✅ Assembles the outputs of the project (depends on `classes` and `jar`).
- **`check`**: Runs all checks (depends on `test`).
- **`build`**: ✅ Aggregates all tasks needed to build the project (depends on `assemble` and `check`).

### `clean` ✅

- **`clean`**: ✅ Deletes the build directory.
- **`cleanTask`**: Deletes the output of a specific task (e.g., `cleanJar`, `cleanTest`).

### `test` (Future release)

- **`compileTestJava`**: Compiles test Java source files.
- **`processTestResources`**: Processes test resource files.
- **`testClasses`**: Assembles the compiled test classes.
- **`test`**: Runs the unit tests.
    - Sub-task: `test.include`: Specifies which test classes to run.
    - Sub-task: `test.exclude`: Specifies which test classes to exclude.
- **`integrationTest`**: Runs integration tests (custom task, needs configuration).

### `assemble` ✅

- **`compileJava`**: ✅ Compiles Java source files.
- **`processResources`**: ✅ Processes resource files.
- **`classes`**: ✅ Assembles the compiled classes.
- **`jar`**: ✅ Assembles the JAR file.
- **`fatJar`**: ✅ Creates a fat JAR with all dependencies (requires Shadow plugin).
- **`assemble`**: ✅ Aggregates `classes` and `jar` tasks.

### `check` (Future release)

- **`compileTestJava`**: Compiles test Java source files.
- **`processTestResources`**: Processes test resource files.
- **`testClasses`**: Assembles the compiled test classes.
- **`test`**: Runs the unit tests.
- **`checkstyle`**: Runs Checkstyle for code style checks (requires Checkstyle plugin).
- **`pmdMain`**: Runs PMD for static code analysis (requires PMD plugin).
- **`spotbugsMain`**: Runs SpotBugs for bug detection (requires SpotBugs plugin).
- **`check`**: Aggregates all verification tasks, including `test`, `checkstyle`, `pmdMain`, and `spotbugsMain`.

### `dependencies` ✅

- **`dependencies`**: ✅ Displays the dependencies of the project.
- **`dependencyInsight`**: Shows insight into a specific dependency.
- **`dependencyUpdates`**: ✅ Checks for dependency updates.

### `help` ✅

- **`help`**: ✅ Displays help information about the available tasks and command-line options.
- **`components`**: Displays the components produced by the project.

### `tasks` ✅

- **`tasks`**: ✅ Lists the tasks in the project.
- **`tasks --all`**: Lists all tasks, including task dependencies.

### `jar` ✅

- **`compileJava`**: ✅ Compiles Java source files.
- **`processResources`**: ✅ Processes resource files.
- **`classes`**: ✅ Assembles the compiled classes.
- **`jar`**: ✅ Assembles the JAR file.
    - Sub-task: `jar.manifest.attributes`: Sets manifest attributes.
    - Sub-task: `jar.from`: Includes additional files in the JAR.

### `publish` (Future release)

- **`generatePomFileForMavenPublication`**: Generates the POM file for Maven publication.
- **`publishMavenPublicationToMavenLocal`**: Publishes to the local Maven repository.
- **`publishMavenPublicationToMavenRepository`**: Publishes to a remote Maven repository.
- **`publish`**: Aggregates all publishing tasks.

### `eclipse` (Future release)

- **`eclipseClasspath`**: Generates the Eclipse classpath file.
- **`eclipseJdt`**: Generates the Eclipse JDT settings.
- **`eclipseProject`**: Generates the Eclipse project file.
- **`eclipse`**: Aggregates all Eclipse tasks.

### `idea` (Future release)

- **`ideaModule`**: Generates the IntelliJ IDEA module files.
- **`ideaProject`**: Generates the IntelliJ IDEA project file.
- **`ideaWorkspace`**: Generates the IntelliJ IDEA workspace file.
- **`idea`**: Aggregates all IDEA tasks.

### `run` (Future release)

- **`compileJava`**: Compiles Java source files.
- **`processResources`**: Processes resource files.
- **`classes`**: Assembles the compiled classes.
- **`run`**: Runs a Java application.
    - Sub-task: `run.main`: Specifies the main class to run.
    - Sub-task: `run.args`: Specifies command-line arguments.