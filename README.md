# 1JPM
1 Java Project Manager (1JPM), is a Maven/Gradle alternative with a twist.
It's a single Java file itself, which should be edited by you to configure your project.
Meaning instead of writing XML or Groovy/DSL, your build file is Java code too.

**To build your project, simply [drag-and-drop the JPM.java file](https://github.com/Osiris-Team/1JPM/releases/) 
into your project, open a terminal and execute `java JPM.java` (Java 11 and above).**

<details>
<summary>Java 8 to 10 and JDK notes</summary>

- Execute:  `javac JPM.java && java -cp . JPM`
- Earlier Java versions are not supported
- Make sure you use a [globally installed JDK](https://adoptium.net/temurin/releases/?os=windows&package=jdk)
(not JRE) with JAVA_HOME set
</details>


Good to know:
- This repository functions as a template too
- 1JPM is Maven based, thus great IDE support by default
- 1JPM includes some extra plugins to increase runtime safety and provide additional features out of the box

```java
public class JPM {
    public static class ThisProject extends JPM.Project {
        public ThisProject() throws IOException, InterruptedException {
            this(null);
        }
        public ThisProject(List<String> args) throws IOException, InterruptedException {
            // Override default configurations
            this.groupId = "com.mycompany.myproject";
            this.artifactId = "my-project";
            this.version = "1.0.0";
            this.mainClass = "com.mycompany.myproject.MyMainClass";
            this.jarName = "my-project.jar";
            this.fatJarName = "my-project-with-dependencies.jar";

            // If there are duplicate dependencies with different versions force a specific version like so:
            //forceImplementation("org.apache.commons:commons-lang3:3.12.0");

            // Add dependencies
            implementation("org.apache.commons:commons-lang3:3.12.0");
            testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3");

            // Add compiler arguments
            addCompilerArg("-Xlint:unchecked");
            addCompilerArg("-Xlint:deprecation");

            // Add additional plugins
            //putPlugin("org.codehaus.mojo:exec-maven-plugin:1.6.0", d -> {
            //    d.putConfiguration("mainClass", this.mainClass);
            //});

            // Execute build
            if(args != null){
                generatePom();
                if(!args.contains("skipMaven"))
                    JPM.executeMaven("clean", "package");//, "-DskipTests"); 
                // or JPM.executeMaven(args); if you prefer the CLI, like "java JPM.java clean package"
            }
        }
    }

    public static class ThirdPartyPlugins extends JPM.Plugins{
        // Add third party plugins below, find them here: https://github.com/topics/1jpm-plugin?o=desc&s=updated
        // (If you want to develop a plugin take a look at "JPM.AssemblyPlugin" class further below to get started)
    }

    // 1JPM version 3.0.3 by Osiris-Team: https://github.com/Osiris-Team/1JPM
    // To upgrade JPM, replace everything below with its newer version
}
```

Above you can see the example configuration which runs the `clean package` tasks.
This compiles and creates a jar file from your code, and additionally creates the sources,
javadoc and with-dependencies jars.

### Additional goodies

#### 1JPM automatically resolves parent and child projects
<details>
<summary></summary>

See `project.isAutoParentsAndChildren`.
If true updates current pom, all parent and all child pom.xml
files with the respective parent details, adding seamless multi-module/project support.

This expects that the parent pom is always inside the parent directory,
otherwise a performant search is not possible since the entire disk would need to be checked.
</details>

#### 1JPM helps porting your multi-module project
<details>
<summary></summary>

Add JPM.java to your root project directory and add `JPM.portChildProjects();` before building.
This is going to download and copy the latest JPM.java file into all child projects it can find
in this directory, and also run it to generate an initial pom.xml for that child project.
The child projects name will be the same as its directory name.

A child project is detected
if a src/main/java folder structure exists, and the parent folder of src/ is then used as child project root.  
Note that a child project is expected to be directly inside a subdirectory of this project.

Now `project.isAutoParentsAndChildren` will work properly, since all needed pom.xml files should exist.

Do you also need something like global variables across those projects? 
Then the `String val = $("key");` function might be of help to you,
since it can easily retrieve values for props defined in the nearest JPM.properties file.

</details>

#### 1JPM can create native executables
<details>
<summary></summary>

GraalVM must be installed, then simply add `JPM.plugins.add(NativeImagePlugin.get);` before building.

The `NativeImagePlugin` in 1JPM is designed to integrate GraalVM's native image building capabilities into your Java project with minimal configuration. By default, it does the following:

1. **Image Generation**: It builds a native executable from your Java application using GraalVM. The generated executable is placed in the `target` directory.

2. **Default Configuration**:
    - **`imageName`**: Defaults to the project's `artifactId`.
    - **`mainClass`**: Automatically determined from the project’s main class configuration.
    - **`build-native` execution**: Compiles the project into a native image during the `package` phase.
    - **`test-native` execution**: Compiles and runs tests as native images during the `test` phase.

3. **Basic Options**: The plugin can be further configured with options like `verbose` output, additional `buildArgs`, or enabling debug information, but these are not set by default.

This setup allows you to seamlessly build native executables with GraalVM, leveraging its performance benefits and ahead-of-time (AOT) compilation, directly from your Maven build process.

For more details see [this GraalVM article](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).

</details>

#### 1JPM can create native installers
<details>
<summary></summary>

Simply add `JPM.plugins.add(PackagerPlugin.get);` before building.
With the default configuration, the `PackagerPlugin` in 1JPM:

- **Bundles a JRE** with the application package, ensuring the packaged application is self-contained and can run on any system without requiring an external JRE.
- **Uses the project's main class** as the entry point for the application, which is automatically set based on the project's configuration.
- **Generates a basic executable package** without additional platform-specific settings, tarballs, or zipballs.
- **Creates an installer** for the application by default for the current operating system.

This default setup is ideal for quickly packaging a Java application into a distributable format that includes everything needed to run the app.
For more details see [JavaPackager on GitHub](https://github.com/fvarrui/JavaPackager).

</details>

#### 1JPM is Maven based
<details>
<summary></summary>

Note that 1JPM is now using **Maven under the hood**, since the complexity as a fully independent build tool
(see version [1.0.3](https://github.com/Osiris-Team/1JPM/blob/1.0.3/src/main/java/JPM.java)) was too high for a single file. Besides, this gives us access to more features, a rich and mature plugin ecosystem, as well as **great IDE compatibility**. 1JPM will take care of generating the pom.xml, downloading the Maven-Wrapper, and then executing Maven as you can see above`.

</details>

#### 1JPM has plugins
<details>
<summary></summary>

A 1JPM plugin is basically a wrapper around a Maven plugin (its xml), providing easy access to its features, but can also be anything else to make building easier.
These third-party plugins can be added simply by appending their Java code inside the ThirdPartyPlugins class.
You can find a list here at [#1jpm-plugin](https://github.com/topics/1jpm-plugin?o=desc&s=updated).
(these must be written in Java 8 and not use external dependencies).
</details>


#### 1JPM saves you time
<details>
<summary></summary>

How many lines of relevant build code do we save compared to Maven?
- 1JPM: 128 lines (see [here](https://github.com/Osiris-Team/AutoPlug-Client/blob/bd580033dea4f0cb7399496e9a01bf8047fb5d88/src/main/java/JPM.java))
- Maven: 391 lines (see [here](https://github.com/Osiris-Team/AutoPlug-Client/blob/bd580033dea4f0cb7399496e9a01bf8047fb5d88/pom.xml))

Thus we write the same config with **263 lines less** code (which is a **3x** saving) when using 1JPM!
</details>

#### 1JPM is able to auto-update itself


## Why a single file?

#### Pros
- IDEs should provide decent auto-complete when JPM.java is in the project root (where your pom.xml/build.gradle)
usually is.
- To access all your IDEs fancy features, you can also add JPM.java to ./src/main/java.
This also grants the rest of your project easy access to its data, like your projects version for example.
Just make sure that your working dir is still at ./ when executing tasks.
- Simple drag and drop installation.
- Direct access to the source of 1JPM and what happens under the hood for those who like exploring or better
understanding how something works, which can be helpful if issues arise.

#### Cons / Todo
- Developing plugins is tricky (if you want them to be more complex) since you can't really use third-party dependencies at the moment.
A workaround for this would be developing a task like "minifyProject" which would merge a complete project into 1 line of code,

## Tipps
- You can use ChatGTP (or another LLM) to easily port your current Maven/Gradle based project over to 1JPM,
by sending it the above example `ThisProject` class and your current build details files (pom.xml/build.gradle),
then prompting it something like: "Port my current Maven/Gradle project to the JPM build tool, by modifing the ThisProject class accordingly".
If you have additional plugins also send it an example plugin from within the JPM class.

## Funding
I am actively maintaining this repository, publishing new releases and working 
on its codebase for free, so if this project benefits you and/or your company consider 
donating a monthly amount you seem fit. Thank you!

<a href="https://www.paypal.com/donate?hosted_button_id=JNXQCWF2TF9W4"><img src="https://github.com/andreostrovsky/donate-with-paypal/raw/master/blue.svg" height="40"></a>
