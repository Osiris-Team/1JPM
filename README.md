# 1JPM
1 Java Project Manager (1JPM), is a Maven/Gradle alternative with a twist.
It's a single Java file itself, which should be edited by you to configure your project.

Meaning instead of writing XML (Maven) or Groovy/DSL (Gradle), your build file is Java code too.
**Thus to build your project, [download/copy the JPM.java file](https://github.com/Osiris-Team/1JPM/releases/) into your project, open a terminal and execute:**

- Java 11 and above: `java JPM.java`
- Java 8 to 10:  `javac JPM.java && java -cp . JPM`
- Earlier Java versions are not supported
- Make sure you use a
[globally installed JDK](https://adoptium.net/temurin/releases/?os=windows&package=jdk) (not JRE)
with JAVA_HOME set

**You can also clone/download this repository since it functions as a template too.**

Below you can see the example configuration which runs the `clean package` tasks.
This compiles and creates a jar file from your code, and additionally creates the sources,
javadoc and with-dependencies jars.

```java
class ThisProject extends JPM.Project {

    public ThisProject(List<String> args) {
        // Override default configurations
        this.groupId = "com.mycompany";
        this.artifactId = "my-project";
        this.version = "1.0.0";
        this.mainClass = "com.mycompany.MyMainClass";
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
    }

    public static void main(String[] _args) throws Exception {
        List<String> args = Arrays.asList(_args);
        ThisProject project = new ThisProject(args);
        project.generatePom();
        JPM.executeMaven("clean", "package"); // or JPM.executeMaven(args); if you prefer the CLI, like "java JPM.java clean package"
    }
}

class ThirdPartyPlugins extends JPM.Plugins{
    // Add third party plugins below, find them here: https://github.com/topics/1jpm-plugin?o=desc&s=updated
    // (If you want to develop a plugin take a look at "JPM.AssemblyPlugin" class further below to get started)
}

// 1JPM version 3.0.0 by Osiris-Team: https://github.com/Osiris-Team/1JPM
// To upgrade JPM, replace the JPM class below with its newer version
public class JPM {
  //...
}
```

Note that 1JPM is now using **Maven under the hood**, since the complexity as a fully independent build tool
(see version [1.0.3](https://github.com/Osiris-Team/1JPM/blob/1.0.3/src/main/java/JPM.java)) was too high for a single file. Besides, this gives us access to more features, a rich and mature plugin ecosystem, as well as **great IDE compatibility**. 1JPM will take care of generating the pom.xml, downloading the Maven-Wrapper, and then executing Maven as you can see in `main()`.

A 1JPM plugin is basically a wrapper around a Maven plugin (its xml), providing easy access to its features, but can also be anything else to make building easier.
These third-party plugins can be added simply by appending their Java code inside the ThirdPartyPlugins class.
You can find a list here at [#1jpm-plugin](https://github.com/topics/1jpm-plugin?o=desc&s=updated).
(these must be written in Java 8 and not use external dependencies).

## 1JPM in Production
How many lines of relevant build code do we save compared to Maven?
- 1JPM: 128 lines (see [here](https://github.com/Osiris-Team/AutoPlug-Client/blob/bd580033dea4f0cb7399496e9a01bf8047fb5d88/src/main/java/JPM.java))
- Maven: 391 lines (see [here](https://github.com/Osiris-Team/AutoPlug-Client/blob/bd580033dea4f0cb7399496e9a01bf8047fb5d88/pom.xml))

Thus we write the same config with **263 lines less** code (which is a **3x** saving) when using 1JPM!

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
