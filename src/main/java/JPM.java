import com.sun.org.apache.xerces.internal.dom.CommentImpl;
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
import java.util.function.Consumer;

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
        addDependency("junit", "junit", "4.13.2");
        addDependency("org.apache.commons", "commons-lang3", "3.12.0");
        //implementation("org.apache.commons:commons-lang3:3.12.0"); // Same as above but similar to Gradle DSL

        // Add some compiler arguments
        addCompilerArg("-Xlint:unchecked");
        addCompilerArg("-Xlint:deprecation");
    }

    public static void main(String[] args) throws Exception {
        ThisProject thisProject = new ThisProject(Arrays.asList(args));
        thisProject.generatePom();
        JPM.executeMaven("clean", "package"); // or JPM.executeMaven(args); if you prefer the CLI like "java JPM.java clean package"
    }
}



// 1JPM version 1.0.4 by Osiris-Team
// To upgrade JPM, replace the JPM class below with its newer version
public class JPM {
    // If you want to force a specific version of the wrapper replace "master" with "maven-wrapper-3.3.2" for example
    private static final String MAVEN_WRAPPER_URL_BASE = "https://github.com/apache/maven-wrapper/blob/master/maven-wrapper-distribution/src/resources/";

    public static void executeMaven(String... args) throws IOException, InterruptedException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        ProcessBuilder p = new ProcessBuilder();
        List<String> finalArgs = new ArrayList<>();
        File mavenWrapperFile = new File(System.getProperty("user.dir"),
                "mvnw" + (isWindows ? ".cmd" : ""));

        if (!mavenWrapperFile.exists()) {
            downloadMavenWrapper(isWindows);
        }

        finalArgs.add(mavenWrapperFile.getAbsolutePath());
        finalArgs.addAll(Arrays.asList(args));
        p.command(finalArgs);
        p.inheritIO();
        Process result = p.start();
        result.waitFor();
        if(result.exitValue() != 0)
            throw new RuntimeException("Maven finished with an error ("+result.exitValue()+").");
    }

    private static void downloadMavenWrapper(boolean isWindows) throws IOException {
        String wrapperScript = isWindows ? "mvnw.cmd" : "mvnw";
        String wrapperUrl = MAVEN_WRAPPER_URL_BASE + wrapperScript;

        System.out.println("Downloading Maven Wrapper: " + wrapperUrl);
        URL url = new URL(wrapperUrl);
        Files.copy(url.openStream(), new File(wrapperScript).toPath(), StandardCopyOption.REPLACE_EXISTING);

        if (!isWindows) {
            new File(wrapperScript).setExecutable(true);
        }
    }

    //
    // API and Models
    //

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
            String id = url.split("//")[0].split("/")[0].replace(".", "").replace("-", "");
            return new Repository(id, url);
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

        public void append(XML otherXML){
            root.appendChild(otherXML.root);
        }

        // Method to add a value to the XML document at the specified path.
        public void put(String key, String value) {
            Element currentElement = getOrCreateElement(key);
            currentElement.setTextContent(value);
        }

        // Method to add a comment to the XML document at the specified path.
        public void putComment(String key, String comment) {
            Element currentElement = getOrCreateElement(key);
            Node parentNode = currentElement.getParentNode();
            Node commentNode = document.createComment(comment);

            // Insert the comment before the specified element.
            parentNode.insertBefore(commentNode, currentElement);
        }

        public void putAttributes(String key, String... attributes) {
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
        }

        // Method to add attributes to an element in the XML document at the specified path.
        public void putAttributes(String key, Map<String, String> attributes) {
            Element currentElement = getOrCreateElement(key);

            // Set each attribute in the map on the element.
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                currentElement.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        // Helper method to traverse or create elements based on a path.
        private Element getOrCreateElement(String key) {
            String[] path = key.split("/");
            Element currentElement = root;

            for (String nodeName : path) {
                NodeList children = currentElement.getElementsByTagName(nodeName);
                if (children.getLength() > 0) {
                    currentElement = (Element) children.item(0);
                } else {
                    Element newElement = document.createElement(nodeName);
                    currentElement.appendChild(newElement);
                    currentElement = newElement;
                }
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
            xml.put("this/is/a/key", "value");
            xml.put("this/is/another/key", "another value");
            xml.putComment("this/is/another", "This is a comment for 'another'");
            Map<String, String> atr = new HashMap<>();
            atr.put("attr1", "value1");
            atr.put("attr2", "value2");
            xml.putAttributes("this/is/a/key", atr);
            System.out.println(xml.toString());
        }
    }

    public static class Plugin {
        public Consumer<Project> beforeGetConfiguration = (project) -> {};
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

        public Plugin withBeforeGetConfiguration(Consumer<Project> code){
            this.beforeGetConfiguration = code;
            return this;
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

        private void executeBeforeGetConfiguration(Project project) {
            beforeGetConfiguration.accept(project);
        }

        private void executeAfterGetConfiguration(Project project) {
            configuration.clear();
            executions.clear();
            dependencies.clear();
        }


        public String getConfiguration(Project project) {
            executeBeforeGetConfiguration(project);

            StringBuilder config = new StringBuilder();
            config.append("            <plugin>\n");
            config.append("                <groupId>").append(groupId).append("</groupId>\n");
            config.append("                <artifactId>").append(artifactId).append("</artifactId>\n");
            config.append("                <version>").append(version).append("</version>\n");

            if (!configuration.isEmpty()) {
                config.append("                <configuration>\n");
                for (Map.Entry<String, String> entry : configuration.entrySet()) {
                    // TODO toXMLTree(entry.getKey(), entry.getValue())
                    //config.append();
                }
                config.append("                </configuration>\n");
            }

            if (!executions.isEmpty()) {
                config.append("                <executions>\n");
                for (Execution execution : executions) {
                    config.append(execution.getConfiguration());
                }
                config.append("                </executions>\n");
            }

            if (!dependencies.isEmpty()) {
                config.append("                <dependencies>\n");
                for (Dependency dependency : dependencies) {
                    config.append("                    <dependency>\n");
                    config.append("                        <groupId>").append(dependency.groupId).append("</groupId>\n");
                    config.append("                        <artifactId>").append(dependency.artifactId).append("</artifactId>\n");
                    config.append("                        <version>").append(dependency.version).append("</version>\n");
                    if (dependency.scope != null) {
                        config.append("                        <scope>").append(dependency.scope).append("</scope>\n");
                    }
                    config.append("                    </dependency>\n");
                }
                config.append("                </dependencies>\n");
            }

            config.append("            </plugin>\n");

            executeAfterGetConfiguration(project);
            return config.toString();
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

        public String getConfiguration() {
            StringBuilder config = new StringBuilder();
            config.append("                    <execution>\n");
            config.append("                        <id>").append(id).append("</id>\n");
            if(phase != null && !phase.isEmpty())
                config.append("                        <phase>").append(phase).append("</phase>\n");

            if (!goals.isEmpty()) {
                config.append("                        <goals>\n");
                for (String goal : goals) {
                    config.append("                            <goal>").append(goal).append("</goal>\n");
                }
                config.append("                        </goals>\n");
            }

            if (!configuration.isEmpty()) {
                config.append("                        <configuration>\n");
                for (Map.Entry<String, String> entry : configuration.entrySet()) {
                    config.append("                            <").append(entry.getKey()).append(">")
                            .append(entry.getValue())
                            .append("</").append(entry.getKey()).append(">\n");
                }
                config.append("                        </configuration>\n");
            }

            config.append("                    </execution>\n");
            return config.toString();
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

        public Project() {
            addDefaultPlugins();
        }

        private void addDefaultPlugins() {
            plugins.add(new CompilerPlugin());
            plugins.add(new JarPlugin());
            plugins.add(new AssemblyPlugin());
            plugins.add(new SourcePlugin());
            plugins.add(new JavadocPlugin());
            plugins.add(new EnforcerPlugin());
        }

        public void addRepository(String url){
            repositories.add(Repository.fromUrl(url));
        }

        public void implementation(String s){
            String[] split = s.split(":");
            if(split.length < 3) throw new RuntimeException("Does not contain all required details: "+s);
            addDependency(split[0], split[1], split[2]);
        }

        public void addDependency(String groupId, String artifactId, String version) {
            dependencies.add(new Dependency(groupId, artifactId, version));
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
        /*
        XML pom = new XML("project");
            pom.putComment("project", "AUTO-GENERATED FILE, CHANGES SHOULD BE DONE IN ./JPM.java or ./src/main/java/JPM.java");
        // TODO
         */

        public void generatePom() throws IOException {


            StringBuilder pom = new StringBuilder();
            pom.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            pom.append("\n\n<!--  -->\n\n\n");
            pom.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            pom.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
            pom.append("    <modelVersion>4.0.0</modelVersion>\n\n");

            pom.append("    <groupId>").append(groupId).append("</groupId>\n");
            pom.append("    <artifactId>").append(artifactId).append("</artifactId>\n");
            pom.append("    <version>").append(version).append("</version>\n\n");

            pom.append("    <properties>\n");
            pom.append("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n");
            pom.append("    </properties>\n\n");

            if (!repositories.isEmpty()) {
                pom.append("    <repositories>\n");
                for (Repository rep : repositories) {
                    pom.append("        <repository>\n");
                    pom.append("            <id>").append(rep.id).append("</id>\n");
                    pom.append("            <url>").append(rep.url).append("</url>\n");
                    pom.append("        </repository>\n");
                }
                pom.append("    </repositories>\n\n");
            }

            if (!dependenciesManaged.isEmpty()) {
                pom.append("    <dependencyManagement>\n");
                for (Dependency dep : dependenciesManaged) {
                    pom.append("        <dependency>\n");
                    pom.append("            <groupId>").append(dep.groupId).append("</groupId>\n");
                    pom.append("            <artifactId>").append(dep.artifactId).append("</artifactId>\n");
                    pom.append("            <version>").append(dep.version).append("</version>\n");
                    pom.append("        </dependency>\n");
                }
                pom.append("    </dependencyManagement>\n\n");
            }

            if (!dependencies.isEmpty()) {
                pom.append("    <dependencies>\n");
                for (Dependency dep : dependencies) {
                    pom.append("        <dependency>\n");
                    pom.append("            <groupId>").append(dep.groupId).append("</groupId>\n");
                    pom.append("            <artifactId>").append(dep.artifactId).append("</artifactId>\n");
                    pom.append("            <version>").append(dep.version).append("</version>\n");
                    pom.append("        </dependency>\n");
                }
                pom.append("    </dependencies>\n\n");
            }

            pom.append("    <build>\n");
            if(!plugins.isEmpty()){
                pom.append("        <plugins>\n");
                for (Plugin plugin : plugins) {
                    pom.append(plugin.getConfiguration(this));
                }
                pom.append("        </plugins>\n\n");
            }

            pom.append("        <!-- Sometimes unfiltered resources cause unexpected behaviour, thus enable filtering. -->\n" +
                    "        <resources>\n" +
                    "            <resource>\n" +
                    "                <directory>src/main/resources</directory>\n" +
                    "                <filtering>true</filtering>\n" +
                    "            </resource>\n" +
                    "        </resources>\n\n");

            pom.append("    </build>\n");
            pom.append("</project>\n");

            File pomFile = new File(System.getProperty("user.dir")+"/pom.xml");
            try (FileWriter writer = new FileWriter(pomFile)) {
                writer.write(pom.toString());
            }
            System.out.println("Generated pom.xml file.");
        }
    }

    public static class CompilerPlugin extends Plugin {
        public CompilerPlugin() {
            super("org.apache.maven.plugins", "maven-compiler-plugin", "3.8.1");
            withBeforeGetConfiguration(project -> {
                addConfiguration("source", project.javaVersionSource);
                addConfiguration("target", project.javaVersionTarget);

                // Add compiler arguments from the project
                if (!project.compilerArgs.isEmpty()) {
                    StringBuilder args = new StringBuilder();
                    for (String arg : project.compilerArgs) {
                        args.append("<arg>").append(arg).append("</arg>");
                    }
                    addConfiguration("compilerArgs", args.toString());
                }
            });
        }
    }

    public static class JarPlugin extends Plugin {
        public JarPlugin() {
            super("org.apache.maven.plugins", "maven-jar-plugin", "3.2.0");
            withBeforeGetConfiguration(project -> {
                addConfiguration("archive manifest addClasspath", "true");
                addConfiguration("archive manifest mainClass", project.mainClass);
                addConfiguration("finalName", project.jarName.replace(".jar", ""));
            });
        }
    }

    public static class AssemblyPlugin extends Plugin {
        public AssemblyPlugin() {
            super("org.apache.maven.plugins", "maven-assembly-plugin", "3.3.0");
            withBeforeGetConfiguration(project -> {
                addConfiguration("descriptorRefs", "<descriptorRef>jar-with-dependencies</descriptorRef>");
                addConfiguration("archive manifest mainClass", project.mainClass);
                addConfiguration("finalName", project.fatJarName.replace(".jar", ""));
                addConfiguration("appendAssemblyId", "false");

                Execution execution = new Execution("make-assembly", "package");
                execution.addGoal("single");
                addExecution(execution);
            });
        }
    }

    public static class SourcePlugin extends Plugin {
        public SourcePlugin() {
            super("org.apache.maven.plugins", "maven-source-plugin", "3.2.1");
            withBeforeGetConfiguration(project -> {
                Execution execution = new Execution("attach-sources", null);
                addExecution(execution);
                execution.addGoal("jar");
            });
        }
    }

    public static class JavadocPlugin extends Plugin {
        public JavadocPlugin() {
            super("org.apache.maven.plugins", "maven-javadoc-plugin", "3.0.0");
            withBeforeGetConfiguration(project -> {
                Execution execution = new Execution("resource-bundles", "package");
                addExecution(execution);
                execution.addGoal("resource-bundle");
                execution.addGoal("test-resource-bundle");
                execution.addConfiguration("doclint", "none");
                execution.addConfiguration("detectOfflineLinks", "false");
            });
        }
    }

    public static class EnforcerPlugin extends Plugin {
        public EnforcerPlugin() {
            super("org.apache.maven.plugins", "maven-enforcer-plugin", "3.3.0");
            withBeforeGetConfiguration(project -> {
                Execution execution = new Execution("enforce", null);
                addExecution(execution);
                execution.addGoal("enforce");
                execution.addConfiguration("rules", "<dependencyConvergence/>");
            });
        }
    }
}
