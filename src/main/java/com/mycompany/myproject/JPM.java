package com.mycompany.myproject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class JPM {
    public static class ThisProject extends JPM.Project {
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
            generatePom();
            if(!args.contains("skipMaven"))
                JPM.executeMaven("clean", "package"); // or JPM.executeMaven(args); if you prefer the CLI, like "java JPM.java clean package"
        }
    }

    public static class ThirdPartyPlugins extends JPM.Plugins{
        // Add third party plugins below, find them here: https://github.com/topics/1jpm-plugin?o=desc&s=updated
        // (If you want to develop a plugin take a look at "JPM.AssemblyPlugin" class further below to get started)
    }

    // 1JPM version 3.0.3 by Osiris-Team: https://github.com/Osiris-Team/1JPM
    // To upgrade JPM, replace everything below with its newer version
    public static final List<Plugin> plugins = new ArrayList<>();
    public static final String mavenVersion = "3.9.8";
    public static final String mavenWrapperVersion = "3.3.2";
    public static final String mavenWrapperScriptUrlBase = "https://raw.githubusercontent.com/apache/maven-wrapper/maven-wrapper-"+ mavenWrapperVersion +"/maven-wrapper-distribution/src/resources/";
    public static final String mavenWrapperJarUrl = "https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper/"+ mavenWrapperVersion +"/maven-wrapper-"+ mavenWrapperVersion +".jar";
    public static final String mavenWrapperPropsContent = "distributionUrl=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/"+ mavenVersion +"/apache-maven-"+ mavenVersion +"-bin.zip";

    static{
        // Init this once to ensure their plugins are added if they use the static constructor
        new ThirdPartyPlugins();
    }

    public static void main(String[] args) throws Exception {
        new ThisProject(new ArrayList<>(Arrays.asList(args)));
    }

    public static void executeMaven(String... args) throws IOException, InterruptedException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        ProcessBuilder p = new ProcessBuilder();
        List<String> finalArgs = new ArrayList<>();
        File userDir = new File(System.getProperty("user.dir"));
        File mavenWrapperFile = new File(userDir, "mvnw" + (isWindows ? ".cmd" : ""));
        File propertiesFile = new File(userDir, ".mvn/wrapper/maven-wrapper.properties");
        File mavenWrapperJarFile = new File(userDir, ".mvn/wrapper/maven-wrapper.jar");

        if (!mavenWrapperFile.exists()) {
            downloadMavenWrapper(mavenWrapperFile);
            if(!isWindows) mavenWrapperFile.setExecutable(true);
        }
        if(!mavenWrapperJarFile.exists()) downloadMavenWrapperJar(mavenWrapperJarFile);
        if (!propertiesFile.exists()) createMavenWrapperProperties(propertiesFile);

        finalArgs.add(mavenWrapperFile.getAbsolutePath());
        finalArgs.addAll(Arrays.asList(args));
        p.command(finalArgs);
        p.inheritIO();
        System.out.print("Executing: ");
        for (String arg : finalArgs) {
            System.out.print(arg+" ");
        }
        System.out.println();
        Process result = p.start();
        result.waitFor();
        if(result.exitValue() != 0)
            throw new RuntimeException("Maven ("+mavenWrapperFile.getName()+") finished with an error ("+result.exitValue()+"): "+mavenWrapperFile.getAbsolutePath());
    }

    public static void downloadMavenWrapper(File script) throws IOException {
        String wrapperUrl = mavenWrapperScriptUrlBase + script.getName();

        System.out.println("Downloading file from: " + wrapperUrl);
        URL url = new URL(wrapperUrl);
        script.getParentFile().mkdirs();
        Files.copy(url.openStream(), script.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void downloadMavenWrapperJar(File jar) throws IOException {
        String wrapperUrl = mavenWrapperJarUrl;

        System.out.println("Downloading file from: " + wrapperUrl);
        URL url = new URL(wrapperUrl);
        jar.getParentFile().mkdirs();
        Files.copy(url.openStream(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void createMavenWrapperProperties(File propertiesFile) throws IOException {
        // Create the .mvn directory if it doesn't exist
        File mvnDir = propertiesFile.getParentFile();
        if (!mvnDir.exists()) {
            mvnDir.mkdirs();
        }

        // Write default properties content to the file
        try (FileWriter writer = new FileWriter(propertiesFile)) {
            writer.write(mavenWrapperPropsContent);
        }
    }

    //
    // API and Models
    //

    public static class Plugins {
    }

    public static interface ConsumerWithException<T> extends Serializable {
        void accept(T t) throws Exception;
    }

    public static class Dependency {
        public static Dependency fromGradleString(String s){
            String[] split = s.split(":");
            String groupId = split.length >= 1 ? split[0] : "";
            String artifactId = split.length >= 2 ? split[1] : "";
            String versionId = split.length >= 3 ? split[2] : "";
            String scope = split.length >= 4 ? split[3] : "compile";
            Dependency dep = new Dependency(groupId, artifactId, versionId, scope);
            if(split.length < 3) System.err.println("No version provided. This might cause issues. Dependency: "+s);
            return dep;
        }

        public String groupId;
        public String artifactId;
        public String version;
        public String scope;
        public List<Dependency> transitiveDependencies;
        public List<Dependency> excludedDependencies = new ArrayList<>();
        public String type = "";

        public Dependency(String groupId, String artifactId, String version) {
            this(groupId, artifactId, version, "compile", new ArrayList<>());
        }

        public Dependency(String groupId, String artifactId, String version, String scope) {
            this(groupId, artifactId, version, scope, new ArrayList<>());
        }

        public Dependency(String groupId, String artifactId, String version, String scope, List<Dependency> transitiveDependencies) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
            this.transitiveDependencies = transitiveDependencies;
        }

        public Dependency exclude(String s){
            return exclude(Dependency.fromGradleString(s));
        }

        public Dependency exclude(Dependency dep){
            excludedDependencies.add(dep);
            return dep;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version + ":" + scope;
        }

        public XML toXML(){
            XML xml = new XML("dependency");
            xml.put("groupId", groupId);
            xml.put("artifactId", artifactId);
            if (version != null && !version.isEmpty()) xml.put("version", version);
            if (scope != null && !scope.isEmpty()) xml.put("scope", scope);
            if (type != null && !type.isEmpty()) xml.put("type", type);

            for (Dependency excludedDependency : excludedDependencies) {
                XML exclusion = new XML("exclusion");
                exclusion.put("groupId", excludedDependency.groupId);
                exclusion.put("artifactId", excludedDependency.artifactId);
                xml.add("exclusions", exclusion);
            }
            return xml;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return Objects.equals(groupId, that.groupId) &&
                    Objects.equals(artifactId, that.artifactId) &&
                    Objects.equals(version, that.version) &&
                    Objects.equals(scope, that.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId, version, scope);
        }
    }

    public static class Repository{
        public String id;
        public String url;
        public boolean isSnapshotsAllowed = true;

        public Repository(String id, String url) {
            this.id = id;
            this.url = url;
        }

        public static Repository fromUrl(String url){
            String id = url.split("//")[1].split("/")[0].replace(".", "").replace("-", "");
            return new Repository(id, url);
        }

        public XML toXML(){
            XML xml = new XML("repository");
            xml.put("id", id);
            xml.put("url", url);
            if(!isSnapshotsAllowed) xml.put("snapshots enabled", "false");
            return xml;
        }
    }

    public static class XML {
        public Document document;
        public Element root;

        // Constructor initializes the XML document with a root element.
        public XML(String rootName) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.newDocument();
                root = document.createElement(rootName);
                document.appendChild(root);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }

        // Method to append another XML object to this XML document's root.
        public XML add(XML otherXML) {
            Node importedNode = document.importNode(otherXML.root, true);
            root.appendChild(importedNode);
            return this;
        }

        // Method to append another XML object to a specific element in this XML document.
        public XML add(String key, XML otherXML) {
            Element parentElement = getElementOrCreate(key);
            Node importedNode = document.importNode(otherXML.root, true);
            parentElement.appendChild(importedNode);
            return this;
        }

        // Method to add a value to the XML document at the specified path.
        public XML put(String key, String value) {
            Element currentElement = getElementOrCreate(key);
            if(value != null && !value.isEmpty())
                currentElement.setTextContent(value);
            return this;
        }

        // Method to add a comment to the XML document at the specified path.
        public XML putComment(String key, String comment) {
            Element currentElement = getElementOrCreate(key);
            Node parentNode = currentElement.getParentNode();
            Node commentNode = document.createComment(comment);

            // Insert the comment before the specified element.
            parentNode.insertBefore(commentNode, currentElement);
            return this;
        }

        public XML putAttributes(String key, String... attributes) {
            if (attributes.length % 2 != 0) {
                throw new IllegalArgumentException("Attributes must be in key-value pairs.");
            }

            Element currentElement = getElementOrCreate(key);

            // Iterate over pairs of strings to set each attribute on the element.
            for (int i = 0; i < attributes.length; i += 2) {
                String attrName = attributes[i];
                String attrValue = attributes[i + 1];
                currentElement.setAttribute(attrName, attrValue);
            }
            return this;
        }

        // Method to add attributes to an element in the XML document at the specified path.
        public XML putAttributes(String key, Map<String, String> attributes) {
            Element currentElement = getElementOrCreate(key);

            // Set each attribute in the map on the element.
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                currentElement.setAttribute(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public XML remove(String key) {
            String[] path = key.split(" ");
            Element element = getElementOrNull(path);
            if (element != null && element.getParentNode() != null) {
                element.getParentNode().removeChild(element);
            }
            return this;
        }

        public XML rename(String oldKey, String newName) {
            String[] path = oldKey.split(" ");
            Element element = getElementOrNull(path);
            if (element != null) {
                document.renameNode(element, null, newName);
            }
            return this;
        }

        // Helper method to traverse or create elements based on a path.
        private Element getElementOrCreate(String key) {
            if (key == null || key.trim().isEmpty()) return root;
            String[] path = key.split(" ");
            Element currentElement = root;

            for (String nodeName : path) {
                Element childElement = null;
                NodeList children = currentElement.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(nodeName)) {
                        childElement = (Element) child;
                        break;
                    }
                }

                if (childElement == null) {
                    childElement = document.createElement(nodeName);
                    currentElement.appendChild(childElement);
                }
                currentElement = childElement;
            }

            return currentElement;
        }

        private Element getElementOrNull(String[] path) {
            Element currentElement = root;
            for (String nodeName : path) {
                NodeList children = currentElement.getChildNodes();
                boolean found = false;
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(nodeName)) {
                        currentElement = (Element) child;
                        found = true;
                        break;
                    }
                }
                if (!found) return null;
            }
            return currentElement;
        }

        // Method to convert the XML document to a pretty-printed string.
        public String toString() {
            try {
                javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
                javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();

                // Enable pretty printing with indentation and newlines.
                transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // Adjust indent space as needed

                javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
                java.io.StringWriter writer = new java.io.StringWriter();
                javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(writer);
                transformer.transform(domSource, result);

                return writer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public static void main(String[] args) {
            // Example usage of the XML class.
            XML xml = new XML("root");
            xml.put("this is a key", "value");
            xml.put("this is another key", "another value");
            xml.putComment("this is another", "This is a comment for 'another'");
            Map<String, String> atr = new HashMap<>();
            atr.put("attr1", "value1");
            atr.put("attr2", "value2");
            xml.putAttributes("this is a key", atr);
            System.out.println(xml.toString());
        }
    }

    public static class Plugin {
        public List<Consumer<Details>> beforeToXMLListeners = new CopyOnWriteArrayList<>();
        public String groupId;
        public String artifactId;
        public String version;

        public Plugin(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public Plugin onBeforeToXML(Consumer<Details> code){
            beforeToXMLListeners.add(code);
            return this;
        }

        private void executeBeforeToXML(Details details) {
            for (Consumer<Details> code : beforeToXMLListeners) {
                code.accept(details);
            }
        }

        /**
         * Usually you will override this.
         */
        public XML toXML(Project project, XML projectXML) {
            Details details = new Details(this, project, projectXML);
            executeBeforeToXML(details);

            // Create an XML object for the <plugin> element
            XML xml = new XML("plugin");
            xml.put("groupId", groupId);
            xml.put("artifactId", artifactId);
            if(version != null && !version.isEmpty()) xml.put("version", version);

            // Add <configuration> elements if present
            if (!details.configuration.isEmpty()) {
                for (Map.Entry<String, String> entry : details.configuration.entrySet()) {
                    xml.put("configuration " + entry.getKey(), entry.getValue());
                }
            }

            // Add <executions> if not empty
            if (!details.executions.isEmpty()) {
                for (Execution execution : details.executions) {
                    xml.add("executions", execution.toXML());
                }
            }

            // Add <dependencies> if not empty
            if (!details.dependencies.isEmpty()) {
                for (Dependency dependency : details.dependencies) {
                    xml.add("dependencies", dependency.toXML());
                }
            }
            return xml;
        }

        public static class Details {
            public Plugin plugin;
            public Project project;
            public XML xml;
            public Map<String, String> configuration = new HashMap<>();
            public List<Execution> executions = new ArrayList<>();
            public List<Dependency> dependencies = new ArrayList<>();

            public Details(Plugin plugin, Project project, XML xml) {
                this.plugin = plugin;
                this.project = project;
                this.xml = xml;
            }

            public Details putConfiguration(String key, String value) {
                configuration.put(key, value);
                return this;
            }

            public Execution addExecution(String id, String phase){
                Execution execution = new Execution(id, phase);
                executions.add(execution);
                return execution;
            }

            public Execution addExecution(Execution execution) {
                executions.add(execution);
                return execution;
            }

            public Details addDependency(Dependency dependency) {
                dependencies.add(dependency);
                return this;
            }
        }
    }

    public static class Execution {
        public String id;
        public String phase;
        public List<String> goals;
        public Map<String, String> configuration;

        public Execution(String id, String phase) {
            this.id = id;
            this.phase = phase;
            this.goals = new ArrayList<>();
            this.configuration = new HashMap<>();
        }

        public Execution addGoal(String goal) {
            goals.add(goal);
            return this;
        }

        public Execution putConfiguration(String key, String value) {
            configuration.put(key, value);
            return this;
        }

        public XML toXML() {
            // Create an instance of XML with the root element <execution>
            XML xml = new XML("execution");

            // Add <id> element
            if(id != null && !id.isEmpty()) xml.put("id", id);

            // Add <phase> element if it is not null or empty
            if (phase != null && !phase.isEmpty()) {
                xml.put("phase", phase);
            }

            // Add <goals> element if goals list is not empty
            if (!goals.isEmpty()) {
                for (String goal : goals) {
                    XML goalXml = new XML("goal");
                    goalXml.put("", goal);
                    xml.add("goals", goalXml);
                }
            }

            // Add <configuration> element if configuration map is not empty
            if (!configuration.isEmpty()) {
                xml.put("configuration", ""); // Placeholder for <configuration> element
                for (Map.Entry<String, String> entry : configuration.entrySet()) {
                    xml.put("configuration " + entry.getKey(), entry.getValue());
                }
            }

            // Return the XML configuration as a string
            return xml;
        }
    }

    public static class Project {
        public String jarName = "output.jar";
        public String fatJarName = "output-fat.jar";
        public String mainClass = "com.example.Main";
        public String groupId = "com.example";
        public String artifactId = "project";
        public String version = "1.0.0";
        public String javaVersionSource = "8";
        public String javaVersionTarget = "8";
        public List<Repository> repositories = new ArrayList<>();
        public List<Dependency> dependenciesManaged = new ArrayList<>();
        public List<Dependency> dependencies = new ArrayList<>();
        public List<Plugin> plugins = JPM.plugins;
        public List<String> compilerArgs = new ArrayList<>();
        public List<Project> profiles = new ArrayList<>();

        public Repository addRepository(String url, boolean isSnapshotsAllowed){
            Repository repository = addRepository(url);
            repository.isSnapshotsAllowed = isSnapshotsAllowed;
            return repository;
        }

        public Repository addRepository(String url){
            Repository repository = Repository.fromUrl(url);
            repositories.add(repository);
            return repository;
        }

        public Dependency testImplementation(String s){
            Dependency dep = addDependency(Dependency.fromGradleString(s));
            dep.scope = "test";
            return dep;
        }

        public Dependency implementation(String s){
            return addDependency(Dependency.fromGradleString(s));
        }

        public Dependency addDependency(String groupId, String artifactId, String version) {
            Dependency dep = new Dependency(groupId, artifactId, version);
            return addDependency(dep);
        }

        public Dependency addDependency(Dependency dep) {
            dependencies.add(dep);
            return dep;
        }

        public Dependency forceImplementation(String s){
            return forceDependency(Dependency.fromGradleString(s));
        }

        public Dependency forceDependency(String groupId, String artifactId, String version) {
            Dependency dep = new Dependency(groupId, artifactId, version);
            return forceDependency(dep);
        }

        public Dependency forceDependency(Dependency dep) {
            dependenciesManaged.add(dep);
            return dep;
        }

        /**
         * Adds the provided plugin or replaces the existing plugin with the provided plugin. <br>
         * This is a utility method to easily add plugins without needing to extend {@link Plugin}. <br>
         * If you want to modify an existing plugin do this by using the global reference like {@link AssemblyPlugin#get} directly. <br>
         *
         * @param s plugin details in Gradle string format: "groupId:artifactId:version"
         * @param onBeforeToXML executed before the xml for this plugin is generated, provides context details in parameter.
         */
        public Plugin putPlugin(String s, Consumer<Plugin.Details> onBeforeToXML){
            Dependency dep = Dependency.fromGradleString(s);
            Plugin pl = new Plugin(dep.groupId, dep.artifactId, dep.version);
            this.plugins.removeIf(pl2 -> pl2.groupId.equals(pl.groupId) && pl2.artifactId.equals(pl.artifactId));
            pl.onBeforeToXML(onBeforeToXML);
            this.plugins.add(pl);
            return pl;
        }

        public Profile addProfile(String id){
            Profile p = new Profile(id);
            return addProfile(p);
        }

        public Profile addProfile(Profile p){
            profiles.add(p);
            return p;
        }

        public void addCompilerArg(String arg) {
            compilerArgs.add(arg);
        }

        public XML toXML(){
            // Create a new XML document with the root element <project>
            XML xml = new XML("project");
            String jpmPackagePath = "/"+this.getClass().getPackage().getName().replace(".", "/");
            xml.putComment("", "\n\n\n\nAUTO-GENERATED FILE, CHANGES SHOULD BE DONE IN ./JPM.java or ./src/main/java"+
                    jpmPackagePath+"/JPM.java\n\n\n\n");
            xml.putAttributes("",
                    "xmlns", "http://maven.apache.org/POM/4.0.0",
                    "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
                    "xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
            );

            // Add <modelVersion> element
            xml.put("modelVersion", "4.0.0");

            // Add main project identifiers
            xml.put("groupId", groupId);
            xml.put("artifactId", artifactId);
            xml.put("version", version);

            // Add <properties> element
            xml.put("properties project.build.sourceEncoding", "UTF-8");

            // Add <repositories> if not empty
            if (!repositories.isEmpty()) {
                for (Repository rep : repositories) {
                    xml.add("repositories", rep.toXML());
                }
            }

            // Add <dependencyManagement> if there are managed dependencies
            if (!dependenciesManaged.isEmpty()) {
                for (Dependency dep : dependenciesManaged) {
                    xml.add("dependencyManagement dependencies", dep.toXML());
                }
            }

            // Add <dependencies> if there are dependencies
            if (!dependencies.isEmpty()) {
                for (Dependency dep : dependencies) {
                    xml.add("dependencies", dep.toXML());
                }
            }

            // Add <build> section with plugins and resources
            for (Plugin plugin : this.plugins) {
                xml.add("build plugins", plugin.toXML(this, xml));
            }

            // Add resources with a comment
            xml.putComment("build resources", "Sometimes unfiltered resources cause unexpected behaviour, thus enable filtering.");
            xml.put("build resources resource directory", "src/main/resources");
            xml.put("build resources resource filtering", "true");

            for (Project profile : profiles) {
                xml.add("profiles", profile.toXML());
            }
            return xml;
        }

        public void generatePom() throws IOException {
            XML pom = toXML();

            // Write to pom.xml
            File pomFile = new File(System.getProperty("user.dir") + "/pom.xml");
            try (FileWriter writer = new FileWriter(pomFile)) {
                writer.write(pom.toString());
            }
            System.out.println("Generated pom.xml file.");
        }
    }

    public static class Profile extends Project{
        public String id;

        public Profile(String id) {
            this.id = id;
            this.plugins = new ArrayList<>(); // Remove default plugins and have separate plugins list
        }

        @Override
        public XML toXML() {
            XML xml = new XML("profile");
            xml.put("id", id);

            // Add <repositories> if not empty
            for (Repository rep : repositories) {
                xml.add("repositories", rep.toXML());
            }

            // Add <dependencyManagement> if there are managed dependencies
            for (Dependency dep : dependenciesManaged) {
                xml.add("dependencyManagement dependencies", dep.toXML());
            }

            // Add <dependencies> if there are dependencies
            for (Dependency dep : dependencies) {
                xml.add("dependencies", dep.toXML());
            }

            // Add <build> section with plugins and resources
            for (Plugin plugin : this.plugins) {
                xml.add("build plugins", plugin.toXML(this, xml));
            }

            return xml;
        }
    }

    static {
        plugins.add(CompilerPlugin.get);
    }
    public static class CompilerPlugin extends Plugin {
        public static CompilerPlugin get = new CompilerPlugin();
        public CompilerPlugin() {
            super("org.apache.maven.plugins", "maven-compiler-plugin", "3.8.1");
            onBeforeToXML(d -> {
                d.putConfiguration("source", d.project.javaVersionSource);
                d.putConfiguration("target", d.project.javaVersionTarget);

                // Add compiler arguments from the project
                if (!d.project.compilerArgs.isEmpty()) {
                    for (String arg : d.project.compilerArgs) {
                        d.putConfiguration("compilerArgs arg", arg);
                    }
                }
            });
        }
    }

    static {
        plugins.add(JarPlugin.get);
    }
    public static class JarPlugin extends Plugin {
        public static JarPlugin get = new JarPlugin();
        public JarPlugin() {
            super("org.apache.maven.plugins", "maven-jar-plugin", "3.2.0");
            onBeforeToXML(d -> {
                d.putConfiguration("archive manifest addClasspath", "true");
                d.putConfiguration("archive manifest mainClass", d.project.mainClass);
                d.putConfiguration("finalName", d.project.jarName.replace(".jar", ""));
            });
        }
    }

    static {
        plugins.add(AssemblyPlugin.get);
    }
    public static class AssemblyPlugin extends Plugin {
        public static AssemblyPlugin get = new AssemblyPlugin();
        public AssemblyPlugin() {
            super("org.apache.maven.plugins", "maven-assembly-plugin", "3.3.0");
            onBeforeToXML(d -> {
                d.putConfiguration("descriptorRefs descriptorRef", "jar-with-dependencies");
                d.putConfiguration("archive manifest mainClass", d.project.mainClass);
                d.putConfiguration("finalName", d.project.fatJarName.replace(".jar", ""));
                d.putConfiguration("appendAssemblyId", "false");

                d.addExecution("make-assembly", "package")
                        .addGoal("single");
            });
        }
    }

    static {
        plugins.add(SourcePlugin.get);
    }
    public static class SourcePlugin extends Plugin {
        public static SourcePlugin get = new SourcePlugin();
        public SourcePlugin() {
            super("org.apache.maven.plugins", "maven-source-plugin", "3.2.1");
            onBeforeToXML(d -> {
                d.addExecution("attach-sources", null)
                        .addGoal("jar");
            });
        }
    }

    static {
        plugins.add(JavadocPlugin.get);
    }
    public static class JavadocPlugin extends Plugin {
        public static JavadocPlugin get = new JavadocPlugin();
        public JavadocPlugin() {
            super("org.apache.maven.plugins", "maven-javadoc-plugin", "3.0.0");
            onBeforeToXML(d -> {
                d.addExecution("resource-bundles", "package")
                        .addGoal("resource-bundle")
                        .addGoal("test-resource-bundle")
                        .putConfiguration("doclint", "none")
                        .putConfiguration("detectOfflineLinks", "false");
            });
        }
    }

    static {
        plugins.add(EnforcerPlugin.get);
    }
    public static class EnforcerPlugin extends Plugin {
        public static EnforcerPlugin get = new EnforcerPlugin();
        public EnforcerPlugin() {
            super("org.apache.maven.plugins", "maven-enforcer-plugin", "3.3.0");
            onBeforeToXML(d -> {
                d.addExecution("enforce", null)
                        .addGoal("enforce")
                        .putConfiguration("rules dependencyConvergence", "");
            });
        }
    }
}
