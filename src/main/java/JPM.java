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
import java.util.function.BiConsumer;

class ThisProject extends JPM.Project {

    public ThisProject(List<String> args) {
        // Override default configurations
        this.groupId = "com.mycompany";
        this.artifactId = "my-project";
        this.version = "1.0.0";
        this.mainClass = "com.mycompany.MyMainClass";
        this.jarName = "my-project.jar";
        this.fatJarName = "my-project-with-dependencies.jar";

        // Add some example dependencies
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3");
        implementation("org.apache.commons:commons-lang3:3.12.0");
        // If there are duplicate dependencies with different versions force a specific version like so:
        //forceImplementation("org.apache.commons:commons-lang3:3.12.0");

        // Add some compiler arguments
        addCompilerArg("-Xlint:unchecked");
        addCompilerArg("-Xlint:deprecation");
    }

    public static void main(String[] args) throws Exception {
        new ThisProject(Arrays.asList(args)).generatePom();
        JPM.executeMaven("clean", "package"); // or JPM.executeMaven(args); if you prefer the CLI, like "java JPM.java clean package"
    }
}

class ThirdPartyPlugins extends JPM.Plugins{
    // Add third party plugins below, find them here: https://github.com/topics/1jpm-plugin?o=desc&s=updated
    // (If you want to develop a plugin take a look at "JPM.Clean" class further below to get started)
}

// 1JPM version 2.1.1-IN_WORK by Osiris-Team: https://github.com/Osiris-Team/1JPM
// To upgrade JPM, replace the JPM class below with its newer version
public class JPM {
    public static final List<Plugin> plugins = new ArrayList<>();
    private static final String mavenVersion = "3.9.8";
    private static final String mavenWrapperVersion = "3.3.2";
    private static final String mavenWrapperScriptUrlBase = "https://raw.githubusercontent.com/apache/maven-wrapper/maven-wrapper-"+ mavenWrapperVersion +"/maven-wrapper-distribution/src/resources/";
    private static final String mavenWrapperJarUrl = "https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper/"+ mavenWrapperVersion +"/maven-wrapper-"+ mavenWrapperVersion +".jar";
    private static final String mavenWrapperPropsContent = "distributionUrl=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/"+ mavenVersion +"/apache-maven-"+ mavenVersion +"-bin.zip";

    public static void main(String[] args) throws Exception {
        ThisProject.main(args);
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

    private static void downloadMavenWrapper(File script) throws IOException {
        String wrapperUrl = mavenWrapperScriptUrlBase + script.getName();

        System.out.println("Downloading file from: " + wrapperUrl);
        URL url = new URL(wrapperUrl);
        script.getParentFile().mkdirs();
        Files.copy(url.openStream(), script.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void downloadMavenWrapperJar(File jar) throws IOException {
        String wrapperUrl = mavenWrapperJarUrl;

        System.out.println("Downloading file from: " + wrapperUrl);
        URL url = new URL(wrapperUrl);
        jar.getParentFile().mkdirs();
        Files.copy(url.openStream(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void createMavenWrapperProperties(File propertiesFile) throws IOException {
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
        public String groupId;
        public String artifactId;
        public String version;
        public String scope;
        public List<Dependency> transitiveDependencies;

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

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version + ":" + scope;
        }

        public XML toXML(){
            XML xml = new XML("dependency");
            xml.put("groupId", groupId);
            xml.put("artifactId", artifactId);
            xml.put("version", version);
            if (scope != null) {
                xml.put("scope", scope);
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
            return xml;
        }
    }

    public static class XML {
        private Document document;
        private Element root;

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
            Element parentElement = getOrCreateElement(key);
            Node importedNode = document.importNode(otherXML.root, true);
            parentElement.appendChild(importedNode);
            return this;
        }

        // Method to add a value to the XML document at the specified path.
        public XML put(String key, String value) {
            Element currentElement = getOrCreateElement(key);
            if(value != null && !value.isEmpty())
                currentElement.setTextContent(value);
            return this;
        }

        // Method to add a comment to the XML document at the specified path.
        public XML putComment(String key, String comment) {
            Element currentElement = getOrCreateElement(key);
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

            Element currentElement = getOrCreateElement(key);

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
            Element currentElement = getOrCreateElement(key);

            // Set each attribute in the map on the element.
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                currentElement.setAttribute(entry.getKey(), entry.getValue());
            }
            return this;
        }

        // Helper method to traverse or create elements based on a path.
        private Element getOrCreateElement(String key) {
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
        public List<BiConsumer<Project, XML>> beforeToXMLListeners = new CopyOnWriteArrayList<>();
        protected String groupId;
        protected String artifactId;
        protected String version;

        // Gets cleared after execute
        protected Map<String, String> configuration = new HashMap<>();
        // Gets cleared after execute
        protected List<Execution> executions = new ArrayList<>();
        // Gets cleared after execute
        protected List<Dependency> dependencies = new ArrayList<>();

        public Plugin(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public void addConfiguration(String key, String value) {
            configuration.put(key, value);
        }

        public void addExecution(Execution execution) {
            executions.add(execution);
        }

        public void addDependency(Dependency dependency) {
            dependencies.add(dependency);
        }

        public Plugin onBeforeToXML(BiConsumer<Project, XML> code){
            beforeToXMLListeners.add(code);
            return this;
        }

        private void executeBeforeToXML(Project project, XML projectXML) {
            for (BiConsumer<Project, XML> code : beforeToXMLListeners) {
                code.accept(project, projectXML);
            }
        }

        private void executeAfterToXML(Project project) {
            configuration.clear();
            executions.clear();
            dependencies.clear();
        }


        public XML toXML(Project project, XML projectXML) {
            executeBeforeToXML(project, projectXML);

            // Create an XML object for the <plugin> element
            XML xml = new XML("plugin");
            xml.put("groupId", groupId);
            xml.put("artifactId", artifactId);
            xml.put("version", version);

            // Add <configuration> elements if present
            if (!configuration.isEmpty()) {
                for (Map.Entry<String, String> entry : configuration.entrySet()) {
                    xml.put("configuration " + entry.getKey(), entry.getValue());
                }
            }

            // Add <executions> if not empty
            if (!executions.isEmpty()) {
                for (Execution execution : executions) {
                    xml.add("executions", execution.toXML());
                }
            }

            // Add <dependencies> if not empty
            if (!dependencies.isEmpty()) {
                for (Dependency dependency : dependencies) {
                    xml.add("dependencies", dependency.toXML());
                }
            }

            executeAfterToXML(project);
            return xml;
        }
    }

    public static class Execution {
        private String id;
        private String phase;
        private List<String> goals;
        private Map<String, String> configuration;

        public Execution(String id, String phase) {
            this.id = id;
            this.phase = phase;
            this.goals = new ArrayList<>();
            this.configuration = new HashMap<>();
        }

        public void addGoal(String goal) {
            goals.add(goal);
        }

        public void addConfiguration(String key, String value) {
            configuration.put(key, value);
        }

        public XML toXML() {
            // Create an instance of XML with the root element <execution>
            XML xml = new XML("execution");

            // Add <id> element
            xml.put("id", id);

            // Add <phase> element if it is not null or empty
            if (phase != null && !phase.isEmpty()) {
                xml.put("phase", phase);
            }

            // Add <goals> element if goals list is not empty
            if (!goals.isEmpty()) {
                xml.put("goals", ""); // Placeholder for <goals> element
                for (String goal : goals) {
                    xml.put("goals goal", goal);
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
        protected String jarName = "output.jar";
        protected String fatJarName = "output-fat.jar";
        protected String mainClass = "com.example.Main";
        protected String groupId = "com.example";
        protected String artifactId = "project";
        protected String version = "1.0.0";
        protected String javaVersionSource = "8";
        protected String javaVersionTarget = "8";
        protected List<Repository> repositories = new ArrayList<>();
        protected List<Dependency> dependenciesManaged = new ArrayList<>();
        protected List<Dependency> dependencies = new ArrayList<>();
        protected List<Plugin> plugins = new ArrayList<>();
        protected List<String> compilerArgs = new ArrayList<>();

        public void addRepository(String url){
            repositories.add(Repository.fromUrl(url));
        }

        public void testImplementation(String s){
            String[] split = s.split(":");
            if(split.length < 3) throw new RuntimeException("Does not contain all required details: "+s);
            addDependency(split[0], split[1], split[2]).scope = "test";
        }

        public void implementation(String s){
            String[] split = s.split(":");
            if(split.length < 3) throw new RuntimeException("Does not contain all required details: "+s);
            addDependency(split[0], split[1], split[2]);
        }

        public Dependency addDependency(String groupId, String artifactId, String version) {
            Dependency dep = new Dependency(groupId, artifactId, version);
            dependencies.add(dep);
            return dep;
        }

        public void forceImplementation(String s){
            String[] split = s.split(":");
            if(split.length < 3) throw new RuntimeException("Does not contain all required details: "+s);
            forceDependency(split[0], split[1], split[2]);
        }

        public void forceDependency(String groupId, String artifactId, String version) {
            dependenciesManaged.add(new Dependency(groupId, artifactId, version));
        }

        public void addCompilerArg(String arg) {
            compilerArgs.add(arg);
        }

        public void generatePom() throws IOException {
            // Create a new XML document with the root element <project>
            XML pom = new XML("project");
            pom.putComment("", "\n\n\n\nAUTO-GENERATED FILE, CHANGES SHOULD BE DONE IN ./JPM.java or ./src/main/java/JPM.java\n\n\n\n");
            pom.putAttributes("",
                    "xmlns", "http://maven.apache.org/POM/4.0.0",
                    "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
                    "xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
            );

            // Add <modelVersion> element
            pom.put("modelVersion", "4.0.0");

            // Add main project identifiers
            pom.put("groupId", groupId);
            pom.put("artifactId", artifactId);
            pom.put("version", version);

            // Add <properties> element
            pom.put("properties project.build.sourceEncoding", "UTF-8");

            // Add <repositories> if not empty
            if (!repositories.isEmpty()) {
                for (Repository rep : repositories) {
                    pom.add("repositories", rep.toXML());
                }
            }

            // Add <dependencyManagement> if there are managed dependencies
            if (!dependenciesManaged.isEmpty()) {
                for (Dependency dep : dependenciesManaged) {
                    pom.add("dependencyManagement dependencies", dep.toXML());
                }
            }

            // Add <dependencies> if there are dependencies
            if (!dependencies.isEmpty()) {
                for (Dependency dep : dependencies) {
                    pom.add("dependencies", dep.toXML());
                }
            }

            // Add <build> section with plugins and resources
            for (Plugin plugin : JPM.plugins) {
                pom.add("build plugins", plugin.toXML(this, pom));
            }
            for (Plugin plugin : plugins) {
                pom.add("build plugins", plugin.toXML(this, pom));
            }

            // Add resources with a comment
            pom.putComment("build resources", "Sometimes unfiltered resources cause unexpected behaviour, thus enable filtering.");
            pom.put("build resources resource directory", "src/main/resources");
            pom.put("build resources resource filtering", "true");

            // Write to pom.xml
            File pomFile = new File(System.getProperty("user.dir") + "/pom.xml");
            try (FileWriter writer = new FileWriter(pomFile)) {
                writer.write(pom.toString());
            }
            System.out.println("Generated pom.xml file.");
        }
    }

    static {
        plugins.add(CompilerPlugin.get);
    }
    public static class CompilerPlugin extends Plugin {
        public static CompilerPlugin get = new CompilerPlugin();
        public CompilerPlugin() {
            super("org.apache.maven.plugins", "maven-compiler-plugin", "3.8.1");
            onBeforeToXML((project, pom) -> {
                addConfiguration("source", project.javaVersionSource);
                addConfiguration("target", project.javaVersionTarget);

                // Add compiler arguments from the project
                if (!project.compilerArgs.isEmpty()) {
                    for (String arg : project.compilerArgs) {
                        addConfiguration("compilerArgs arg", arg);
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
            onBeforeToXML((project, pom) -> {
                addConfiguration("archive manifest addClasspath", "true");
                addConfiguration("archive manifest mainClass", project.mainClass);
                addConfiguration("finalName", project.jarName.replace(".jar", ""));
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
            onBeforeToXML((project, pom) -> {
                addConfiguration("descriptorRefs descriptorRef", "jar-with-dependencies");
                addConfiguration("archive manifest mainClass", project.mainClass);
                addConfiguration("finalName", project.fatJarName.replace(".jar", ""));
                addConfiguration("appendAssemblyId", "false");

                Execution execution = new Execution("make-assembly", "package");
                execution.addGoal("single");
                addExecution(execution);
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
            onBeforeToXML((project, pom) -> {
                Execution execution = new Execution("attach-sources", null);
                addExecution(execution);
                execution.addGoal("jar");
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
            onBeforeToXML((project, pom) -> {
                Execution execution = new Execution("resource-bundles", "package");
                addExecution(execution);
                execution.addGoal("resource-bundle");
                execution.addGoal("test-resource-bundle");
                execution.addConfiguration("doclint", "none");
                execution.addConfiguration("detectOfflineLinks", "false");
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
            onBeforeToXML((project, pom) -> {
                Execution execution = new Execution("enforce", null);
                addExecution(execution);
                execution.addGoal("enforce");
                execution.addConfiguration("rules dependencyConvergence", "");
            });
        }
    }
}
