package com.mycompany.myproject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JPM {
    public static class ThisProject extends JPM.Project {
        public ThisProject() throws Exception {
            this(null);
        }
        public ThisProject(List<String> args) throws Exception {
            // Override default configurations
            this.groupId = "com.mycompany.myproject";
            this.artifactId = "my-project";
            this.version = "1.0.0";
            this.mainClass = groupId+".MyMainClass";
            this.jarName = artifactId+".jar";
            this.fatJarName = artifactId+"-with-dependencies.jar";

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












































    // 1JPM version 3.3.6 by Osiris-Team: https://github.com/Osiris-Team/1JPM
    // Do not edit anything below, since changes will be lost due to auto-updating.
    // You can also do this manually, by replacing everything below with its newer version and updating the imports.
    public static final List<Plugin> plugins = new ArrayList<>();
    public static final String mavenVersion = "3.9.8";
    public static final String mavenWrapperVersion = "3.3.2";
    public static final String mavenWrapperScriptUrlBase = "https://raw.githubusercontent.com/apache/maven-wrapper/maven-wrapper-"+ mavenWrapperVersion +"/maven-wrapper-distribution/src/resources/";
    public static final String mavenWrapperJarUrl = "https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper/"+ mavenWrapperVersion +"/maven-wrapper-"+ mavenWrapperVersion +".jar";
    public static final String mavenWrapperPropsContent = "distributionUrl=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/"+ mavenVersion +"/apache-maven-"+ mavenVersion +"-bin.zip";
    public static final String jpmLatestUrl = "https://github.com/Osiris-Team/1JPM/raw/main/src/main/java/com/mycompany/myproject/JPM.java";
    public static final String propsFileName = "JPM.properties";
    public static final Map<String, String> propsKeyValCache = new HashMap<>();
    public static boolean isJpmUpdated = false;

    /**
     * Running {@link #main(String[])} without arguments / empty arguments
     * should always result in a pom.xml file being created anyway. <br>
     * Passing over null instead of an arguments list should never create a pom.xml file.
     */
    public static Object expectation1 = new Object();

    static{
        updateSelfIfNeeded(); // Comment this to disable self-updating, remember to comment again after updating manually!
        // Init this once to ensure their plugins are added if they use the static constructor
        new ThirdPartyPlugins();

        if(isJpmUpdated){
            System.out.print("\n\n\n\n");
            System.out.println("!!! JPM WAS JUST UPDATED, THUS THIS RUN DOES NOT YET BENEFIT FROM THE NEWEST CHANGES !!!");
            System.out.println("--> RE-RUN THIS TO RUN WITH THE UPDATED JPM VERSION.");
            System.out.print("\n\n\n\n");
        }
    }

    /**
     * Bound by {@link #expectation1}.
     */
    public static void main(String[] args) throws Exception {
        new ThisProject(new ArrayList<>(Arrays.asList(args)));
    }

    public static void executeMaven(List<String> args) throws IOException, InterruptedException {
        executeMaven(args.toArray(new String[0]));
    }

    public static void executeMaven(String... args) throws IOException, InterruptedException {
        executeMaven(null, args);
    }

    public static void executeMaven(File userDir, String... args) throws IOException, InterruptedException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        ProcessBuilder p = new ProcessBuilder();
        List<String> finalArgs = new ArrayList<>();
        if(userDir == null) userDir = new File(System.getProperty("user.dir"));
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
        p.directory(userDir);
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
        URL url = toUrl(wrapperUrl);
        script.getParentFile().mkdirs();
        Files.copy(url.openStream(), script.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void downloadMavenWrapperJar(File jar) throws IOException {
        String wrapperUrl = mavenWrapperJarUrl;

        System.out.println("Downloading file from: " + wrapperUrl);
        URL url = toUrl(wrapperUrl);
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

    /**
     * Returns the value as string from the nearest JPM.properties file, or an empty string if not found/defined in the complete dir tree.<br>
     * <br>
     * By "nearest" we mean: If the value didn't exist in a JPM.properties file in the current working directory,
     * we check all JPM.properties files until the root of the disk.
     *
     * @param key The key to search for in the properties files.
     * @return The value associated with the key, or an empty string if not defined.
     */
    public static String $(String key) {
        // Check if the key is already in the cache
        if (propsKeyValCache.containsKey(key)) {
            return propsKeyValCache.get(key);
        }

        File currentDir = new File(System.getProperty("user.dir"));

        while (currentDir != null) {
            File propertiesFile = new File(currentDir, propsFileName);

            if (propertiesFile.exists() && propertiesFile.isFile()) {
                try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                    Properties properties = new Properties();
                    properties.load(fis);
                    String value = properties.getProperty(key);
                    if (value != null) {
                        propsKeyValCache.put(key, value); // Cache the result
                        return value;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // If there's an issue reading the file, continue to the parent directory
                }
            }

            currentDir = currentDir.getParentFile();
        }

        // If the key was not found in any properties file, return an empty string
        propsKeyValCache.put(key, ""); // Cache the empty result
        return "";
    }

    /**
     * This updates the JPM.java file in the current working dir by doing the following: <br>
     * 1. Downloads the latest JPM.java file into memory. <br>
     * 2. Checks its version by searching for a string like "// 1JPM version 3.2.0 by Osiris-Team:" and extracting the version from that. <br>
     * 3. Check the current version by doing the same for the current JPM.java file <br>
     * 4. If the downloaded JPM.java file is newer we update. <br>
     * 5. The actual update is done by extracting everything up to "// 1JPM version <version> by Osiris-Team:" from the current JPM.java file, <br>
     * then extracting everything starting at "// 1JPM version <version> by Osiris-Team:" (this line will also be included) until the end of the latest JPM.java file, <br>
     * then finally combining those 2 strings and overwriting the current JPM.java file, imports of both files will also be merged.
     */
    public static void updateSelfIfNeeded() {
        try{
            System.out.println("Downloading file from: " + jpmLatestUrl);
            File cwd = new File(System.getProperty("user.dir"));
            File jpmFile = new File(cwd + "/JPM.java");

            if (!jpmFile.exists()) {
                jpmFile = new File(cwd + "/src/main/java/JPM.java");
                if (!jpmFile.exists()) {
                    String jpmPackagePath = "/"+JPM.class.getPackage().getName().replace(".", "/");
                    jpmFile = new File(cwd+"/src/main/java"+jpmPackagePath+"/JPM.java");

                    // If still not found, search all subdirs by shallowest first
                    if (!jpmFile.exists()) {
                        jpmFile = searchForFileBreadthFirst(cwd, "JPM.java");
                        if (jpmFile == null) {
                            throw new FileNotFoundException("JPM.java file not found in the project directory.");
                        }
                    }
                }
            }

            URL url = toUrl(jpmLatestUrl);
            jpmFile.getParentFile().mkdirs();

            String jpmJavaContent = contentToString(url);
            String latestVersion = extractJPMVersion(jpmJavaContent);
            String currentJpmJavaContent = contentToString(jpmFile);
            String currentVersion = extractJPMVersion(currentJpmJavaContent);

            // Compare versions
            if (latestVersion.compareTo(currentVersion) > 0) {
                System.out.println("A newer JPM version is available ("+currentVersion+" -> "+latestVersion+"). Updating...");

                // Extract and merge imports
                String mergedImports = mergeJPMImports(currentJpmJavaContent, jpmJavaContent);

                // Extract parts for merging
                String currentHeader = extractCurrentJPMHead(currentJpmJavaContent);
                String latestBody = extractLatestJPMBody(jpmJavaContent);

                // Combine the header of the current file, merged imports, and the body of the latest file
                String updatedJpmJavaContent = mergedImports + "\n\n" + currentHeader + "\n" + latestBody;

                // Overwrite the current JPM.java file with the updated content
                try (FileWriter writer = new FileWriter(jpmFile)) {
                    writer.write(updatedJpmJavaContent);
                }

                isJpmUpdated = true;
                System.out.println("JPM update completed to version " + latestVersion+", please re-run to use the latest version!");
            } else {
                System.out.println("No JPM update needed. You are already on the latest version (" + currentVersion + ").");
            }
        } catch (Exception e) {
            System.err.println("Self-updating failed!");
            e.printStackTrace();
        }
    }

    private static File searchForFileBreadthFirst(File rootDir, String fileName) {
        Queue<File> directories = new LinkedList<>();
        directories.add(rootDir);

        while (!directories.isEmpty()) {
            File dir = directories.poll();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        directories.add(file);  // Add directories to the queue for further searching
                    } else if (file.getName().equals(fileName)) {
                        return file;  // Return the file if found
                    }
                }
            }
        }
        return null;  // File not found
    }


    private static String extractJPMVersion(String content) {
        Pattern pattern = Pattern.compile("//\\s*1JPM\\s*version\\s*(\\d+\\.\\d+\\.\\d+)\\s*by\\s*Osiris-Team:");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException("Could not find version information in the provided content.");
        }
    }

    /**
     * Starts after the imports.
     */
    private static String extractCurrentJPMHead(String content) {
        // Use regex to find the end of the imports section
        Pattern importPattern = Pattern.compile("^import\\s+.*?;$", Pattern.MULTILINE);
        Matcher importMatcher = importPattern.matcher(content);

        int lastImportEndIndex = 0;
        while (importMatcher.find()) {
            lastImportEndIndex = importMatcher.end();
        }

        // Start searching for the version comment after the imports
        Pattern versionPattern = Pattern.compile("//\\s*1JPM\\s*version\\s*\\d+\\.\\d+\\.\\d+\\s*by\\s*Osiris-Team:");
        Matcher versionMatcher = versionPattern.matcher(content);

        if (versionMatcher.find(lastImportEndIndex)) {
            int versionStartIndex = versionMatcher.start();
            return content.substring(lastImportEndIndex, versionStartIndex);
        } else {
            throw new IllegalStateException("Could not (extractCurrentJPMHead) find version information in the current content.");
        }
    }

    private static String extractLatestJPMBody(String content) {
        // Use regex to find the version comment line, matching any version number
        Pattern pattern = Pattern.compile("//\\s*1JPM\\s*version\\s*\\d+\\.\\d+\\.\\d+\\s*by\\s*Osiris-Team:");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            int index = matcher.start();
            return content.substring(index);
        } else {
            throw new IllegalStateException("Could not (extractBody) find version information in the latest content.");
        }
    }

    private static String mergeJPMImports(String currentContent, String latestContent) {
        Set<String> imports = new HashSet<>();
        Pattern importPattern = Pattern.compile("^import\\s+.*?;$", Pattern.MULTILINE);

        // Extract imports from the current content
        Matcher currentMatcher = importPattern.matcher(currentContent);
        while (currentMatcher.find()) {
            imports.add(currentMatcher.group());
        }

        // Extract imports from the latest content
        Matcher latestMatcher = importPattern.matcher(latestContent);
        while (latestMatcher.find()) {
            imports.add(latestMatcher.group());
        }

        // Combine all unique imports into a single string
        StringBuilder mergedImports = new StringBuilder();
        for (String imp : imports) {
            mergedImports.append(imp).append("\n");
        }

        return mergedImports.toString();
    }

    private static URL toUrl(String url) throws MalformedURLException {
        return URL.of(URI.create(url), null);
    }


    /**
     * This is going to download and copy the latest JPM.java file into all child projects it can find in this directory,
     * and also run that file to generate an initial pom.xml. The child projects name will be the same as its root directory name.<br>
     * <br>
     * A child project is detected if a src/main/java folder structure exists, and the parent folder of src/ is then used
     * as child project root. <br>
     * <br>
     * Note that a child project is expected to be directly inside a subdirectory of this project.<br>
     * <br>
     * Useful to quickly setup existing multi-module projects, since then {@link Project#isAutoParentsAndChildren} will work properly. <br>
     * <br>
     * Bound by {@link #expectation1}.
     */
    public static void portChildProjects() throws Exception {
        List<File> childProjectDirs = new ArrayList<>();
        File cwd = new File(System.getProperty("user.dir"));
        File[] subDirs = cwd.listFiles(File::isDirectory);
        if(subDirs != null)
            for (File subDir : subDirs) {
                fillSubProjectDirs(subDir, childProjectDirs);
            }

        if(childProjectDirs.isEmpty()) System.out.println("No child projects found in dir: "+cwd);
        else {
            for (File childProjectDir : childProjectDirs) {
                File jpmFile = new File(childProjectDir, "JPM.java");
                if(jpmFile.exists()) {
                    System.out.println("JPM.java file already exists for child project '"+childProjectDir.getName()+"'.");
                    if(!new File(childProjectDir, "pom.xml").exists()){
                        execJavaJpmJava(childProjectDir);
                    }
                    continue;
                }
                System.out.println("Downloading file from: " + jpmLatestUrl);
                URL url = toUrl(jpmLatestUrl);
                jpmFile.getParentFile().mkdirs();
                String jpmJavaContent = contentToString(url);
                jpmJavaContent = jpmJavaContent.replace(".myproject", "."+childProjectDir.getName())
                        .replace("my-project", childProjectDir.getName());
                Files.write(jpmFile.toPath(), jpmJavaContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Created JPM.java file for child project '"+childProjectDir.getName()+"'.");

                execJavaJpmJava(childProjectDir);
            }

            for (File childProjectDir : childProjectDirs) {
                System.out.println(childProjectDir);
            }
            System.out.println("Ported "+childProjectDirs.size()+" child projects successfully!");
        }
    }

    private static void execJavaJpmJava(File childProjectDir, String... additionalArgs) throws IOException, InterruptedException {
        ProcessBuilder p = new ProcessBuilder();
        List<String> list = new ArrayList<>();
        list.add("java");
        list.add("JPM.java");
        if(additionalArgs != null && additionalArgs.length > 0) list.addAll(Arrays.asList(additionalArgs));
        p.command(list);
        p.inheritIO();
        p.directory(childProjectDir);
        System.out.println("Executing in child project '"+ childProjectDir.getName()+"': java JPM.java");
        Process result = p.start();
        result.waitFor();
        if(result.exitValue() != 0){
            RuntimeException ex = new RuntimeException("Command finished with an error ("+result.exitValue()+"), while executing: java JPM.java");
            if(new File(childProjectDir, "pom.xml").exists()){
                System.err.println("IGNORED exception because pom.xml file was created. Make sure to look into this later:");
                ex.printStackTrace();
                return;
            }
            throw ex;
        }
    }

    private static void fillSubProjectDirs(File dir, List<File> childProjectDirs){
        File javaDir = new File(dir+"/src/main/java");
        if(javaDir.exists()){
            childProjectDirs.add(dir);
            File[] subDirs = dir.listFiles(File::isDirectory);
            if(subDirs != null)
                for (File subDir : subDirs) {
                    fillSubProjectDirs(subDir, childProjectDirs);
                }
        }
    }

    /**
     * Overloaded method to use UTF-8 as the default charset.
     *
     * @param url The URL to read from.
     * @return The content of the URL as a String.
     * @throws Exception If an I/O error occurs.
     */
    public static String contentToString(URL url) throws Exception {
        return contentToString(url, StandardCharsets.UTF_8);
    }

    /**
     * Reads the content of a URL as binary data and converts it to a String.
     *
     * @param url The URL to read from.
     * @param charset The charset to use for converting bytes to a String.
     * @return The content of the URL as a String.
     * @throws Exception If an I/O error occurs.
     */
    public static String contentToString(URL url, Charset charset) throws Exception {
        try (InputStream inputStream = url.openStream()){
            return contentToString(inputStream, charset);
        }
    }

    private static String contentToString(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            return contentToString(fis, StandardCharsets.UTF_8);
        }
    }

    public static String contentToString(InputStream in, Charset charset) throws IOException {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            return byteArrayOutputStream.toString(charset.name());
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

        public static Dependency fromGradleString(Project project, String s) {
            String[] split = s.split(":");
            String groupId = split.length >= 1 ? split[0] : "";
            String artifactId = split.length >= 2 ? split[1] : "";
            String versionId = split.length >= 3 ? split[2] : "";
            String scope = split.length >= 4 ? split[3] : "compile";
            Dependency dep = new Dependency(project, groupId, artifactId, versionId, scope);

            if (split.length < 3) {
                System.err.println("No version provided. This might cause issues. Dependency: " + s);
                suggestVersions(project, groupId, artifactId);
            }

            return dep;
        }

        private static void suggestVersions(Project project, String groupId, String artifactId) {
            Map<String, String> latestVersions = new HashMap<>();

            for (Repository _repo : project.repositories) {
                String repo = _repo.url;
                try {
                    URL url = toUrl(repo + "/" + groupId.replace('.', '/') + "/" + artifactId + "/maven-metadata.xml");
                    System.out.println("Checking repository: " + url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    if (connection.getResponseCode() == 200) {
                        List<String> versions = new ArrayList<>();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("<version>")) {
                                    String version = line.trim().replace("<version>", "").replace("</version>", "");
                                    versions.add(version);
                                }
                            }
                        }

                        if (!versions.isEmpty()) {
                            Collections.sort(versions, new VersionComparator());
                            latestVersions.put(repo, versions.get(0));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error checking repository " + repo + ": " + e.getMessage());
                }
            }

            if (!latestVersions.isEmpty()) {
                System.out.println("Latest versions for " + groupId + ":" + artifactId + " across repositories:");
                for (Map.Entry<String, String> entry : latestVersions.entrySet()) {
                    System.out.println("  - " + entry.getKey() + ": " + entry.getValue() +
                            (entry.getValue().contains("SNAPSHOT") ? " (SNAPSHOT)" : ""));
                }
            } else {
                System.out.println("No versions found for " + groupId + ":" + artifactId + " in any repository");
            }
        }

        private static class VersionComparator implements Comparator<String> {
            @Override
            public int compare(String v1, String v2) {
                String[] parts1 = v1.split("\\.");
                String[] parts2 = v2.split("\\.");
                for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
                    int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
                    int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
                    if (p1 != p2) {
                        return Integer.compare(p2, p1);  // Reverse order for latest version first
                    }
                }
                return v2.compareTo(v1);  // Reverse order for latest version first
            }

            private int parseVersionPart(String part) {
                try {
                    return Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }

        public Project project;
        public String groupId;
        public String artifactId;
        public String version;
        public String scope;
        public List<Dependency> transitiveDependencies;
        public List<Dependency> excludedDependencies = new ArrayList<>();
        public String type = "";
        /**
         * If provided builds this dependency before building the project.
         */
        public String localProjectPath = "";

        public Dependency(Project project, String groupId, String artifactId, String version) {
            this(project, groupId, artifactId, version, "compile", new ArrayList<>());
        }

        public Dependency(Project project, String groupId, String artifactId, String version, String scope) {
            this(project, groupId, artifactId, version, scope, new ArrayList<>());
        }

        public Dependency(Project project, String groupId, String artifactId, String version, String scope, List<Dependency> transitiveDependencies) {
            this.project = project;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
            this.transitiveDependencies = transitiveDependencies;
        }

        public Dependency exclude(String s){
            return exclude(Dependency.fromGradleString(project, s));
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
            String id = url.replaceAll("[\\\\/:\"<>|?*]", "");
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

        public XML(File file){
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(file);
                root = document.getDocumentElement();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXException e) {
                throw new RuntimeException(e);
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
            Element element = getElementOrNull(key);
            if (element != null && element.getParentNode() != null) {
                element.getParentNode().removeChild(element);
            }
            return this;
        }

        public XML rename(String oldKey, String newName) {
            Element element = getElementOrNull(oldKey);
            if (element != null) {
                document.renameNode(element, null, newName);
            }
            return this;
        }

        // Helper method to traverse or create elements based on a path.
        public Element getElementOrCreate(String key) {
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

        public Element getElementOrNull(String key) {
            if (key == null || key.trim().isEmpty()) return root;
            String[] path = key.split(" ");
            return getElementOrNull(path);
        }

        protected Element getElementOrNull(String[] path) {
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

        public String toStringAt(File file) throws IOException {
            String s = toString();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(s);
            }
            return s;
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


            // Create an XML object for the <plugin> element
            XML xml = new XML("plugin");

            Details details = new Details(this, project, projectXML, xml);
            executeBeforeToXML(details);

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
            public XML projectXml;
            public XML pluginXml;
            public Map<String, String> configuration = new HashMap<>();
            public List<Execution> executions = new ArrayList<>();
            public List<Dependency> dependencies = new ArrayList<>();

            public Details(Plugin plugin, Project project, XML projectXml, XML pluginXml) {
                this.plugin = plugin;
                this.project = project;
                this.projectXml = projectXml;
                this.pluginXml = pluginXml;
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
        /**
         * If true updates current pom, all parent and all child pom.xml
         * files with the respective parent details. <br>
         * <br>
         * This expects that the parent pom is always inside the parent directory,
         * otherwise a performant search is not possible since the entire disk would need to be checked.
         */
        public boolean isAutoParentsAndChildren = true;

        public Project() {
            repositories.add(Repository.fromUrl("https://repo.maven.apache.org/maven2"));
        }

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
            Dependency dep = addDependency(Dependency.fromGradleString(this, s));
            dep.scope = "test";
            return dep;
        }

        public Dependency implementation(String s){
            return addDependency(Dependency.fromGradleString(this, s));
        }

        public Dependency addDependency(String groupId, String artifactId, String version) {
            Dependency dep = new Dependency(this, groupId, artifactId, version);
            return addDependency(dep);
        }

        public Dependency addDependency(Dependency dep) {
            dependencies.add(dep);
            return dep;
        }

        public Dependency forceImplementation(String s){
            return forceDependency(Dependency.fromGradleString(this, s));
        }

        public Dependency forceDependency(String groupId, String artifactId, String version) {
            Dependency dep = new Dependency(this, groupId, artifactId, version);
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
            Dependency dep = Dependency.fromGradleString(this, s);
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

        /**
         * Writes the pom.xml file (see {@link #toXML()}) and also updates/re-generates parent/child poms. <br>
         * For any dependency that has localProjectPath set, it will try to update its pom and also run "maven install",
         * so that the dependency is built and ready to use in this project. <br>
         * @throws IOException
         */
        public void generatePom() throws IOException {
            XML pom = toXML();

            // Write to pom.xml
            File pomFile = new File(System.getProperty("user.dir") + "/pom.xml");
            try (FileWriter writer = new FileWriter(pomFile)) {
                writer.write(pom.toString());
            }
            System.out.println("Generated pom.xml file.");

            // If isAutoParentsAndChildren is true, handle parents and children automatically
            if (isAutoParentsAndChildren) {
                appendParentInfoAndRegen(pom);
                appendParentInfoToChildren();
            }

            Path cwd = Paths.get(System.getProperty("user.dir"));
            for (Dependency dependency : dependencies) {
                if(dependency.localProjectPath != null && !dependency.localProjectPath.isEmpty()){
                    Path projectPath = Paths.get(dependency.localProjectPath);
                    if(dependency.localProjectPath.startsWith(".")){
                        // Resolve the relative path against the current directory
                        projectPath = cwd.resolve(projectPath).normalize();
                    }
                    try {
                        execJavaJpmJava(projectPath.toFile());
                        executeMaven(projectPath.toFile(), "install");
                    } catch (Exception e) {
                        System.err.println("Failed to build dependency "+dependency.toString()+" with local project path: "+ projectPath);
                        e.printStackTrace();
                    }
                }
            }
        }

        protected void appendParentInfoAndRegen(XML currentPom) throws IOException {
            appendParentInfoAndRegen(currentPom, new File(System.getProperty("user.dir")), null);
        }

        protected void appendParentInfoAndRegen(XML currentPom, File currentDir, File forceStopAtDir) throws IOException {
            if(currentDir == null){
                System.out.println("Force end probably at disk root, because currentDir is null.");
                return;
            }
            File parentDir = currentDir.getParentFile();
            File parentPom = null;

            while (parentDir != null) {
                if(forceStopAtDir != null && forceStopAtDir.equals(currentDir)) {
                    System.out.println("Force end at: " + parentPom);
                    return;
                }

                parentPom = new File(parentDir, "pom.xml");
                if (parentPom.exists()) {
                    // Regen parent
                    try {
                        execJavaJpmJava(parentDir, "skipMaven"); // Only sync, no build);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Load and update parent pom.xml
                    System.out.println("Subproject '"+currentDir.getName()+"', found parent pom.xml at: " + parentPom.getAbsolutePath());
                    XML parent = new XML(parentPom);
                    Element parentGroup = parent.getElementOrNull("groupId");
                    Element parentArtifactId = parent.getElementOrNull("artifactId");
                    Element parentVersion = parent.getElementOrNull("version");
                    String parentPomRelativePath = "../pom.xml";
                    if(parentGroup == null) {
                        System.err.println("Ensure that this parent pom.xml contains a groupId! Cannot proceed!");
                        return;
                    }
                    if(parentArtifactId == null) {
                        System.err.println("Ensure that this parent pom.xml contains a artifactId! Cannot proceed!");
                        return;
                    }
                    if(parentVersion == null) {
                        System.err.println("Ensure that this parent pom.xml contains a version! Cannot proceed!");
                        return;
                    }
                    currentPom.put("parent groupId", parentGroup.getTextContent());
                    currentPom.put("parent artifactId", parentArtifactId.getTextContent());
                    currentPom.put("parent version", parentVersion.getTextContent());
                    currentPom.put("parent relativePath", parentPomRelativePath);
                    currentPom.toStringAt(new File(currentDir, "pom.xml"));
                    System.out.println("Updated pom.xml at: " + currentDir.getName());
                    currentPom = parent;
                    currentDir = parentPom;
                } else{
                    System.out.println("No more parents found. End at: " + parentPom.getAbsolutePath());
                    break;
                }
                parentDir = parentDir.getParentFile();
            }
        }

        protected void appendParentInfoToChildren() throws IOException {
            File currentDir = new File(System.getProperty("user.dir"));
            appendParentInfoToChildren(currentDir);
        }

        /**
         * @param currentDir assume that this contains a pom.xml file that already was updated,
         *                  now we want to check its sub-dirs for child projects.
         */
        protected void appendParentInfoToChildren(File currentDir) throws IOException {
            List<File> poms = new ArrayList<>();
            File[] subDirs = currentDir.listFiles(File::isDirectory);
            if(subDirs != null)
                for (File subDir : subDirs) {
                    fillChildPoms(subDir, poms);
                }

            // Inline sorting by file depth using separator counting
            // Sorting is done in descending order of depth (deepest first)
            poms.sort((f1, f2) -> {
                int depth1 = f1.getAbsolutePath().split(File.separator.equals("\\") ? "\\\\" : File.separator).length;
                int depth2 = f2.getAbsolutePath().split(File.separator.equals("\\") ? "\\\\" : File.separator).length;
                return Integer.compare(depth2, depth1);
            });


            HashSet<File> visitedFolders = new HashSet<>();
            for (File childPom : poms) {
                if(visitedFolders.contains(childPom.getParentFile()))
                    continue;

                // We either force stop at currentDir or at the already visited folder up in the file tree
                File forceStopAtDir = childPom;
                while (!visitedFolders.contains(forceStopAtDir)){
                    forceStopAtDir = forceStopAtDir.getParentFile();
                    if(forceStopAtDir == null){
                        forceStopAtDir = currentDir;
                        break;
                    }
                }

                // Update current child pom and all parent poms.
                XML pom = new XML(childPom);
                appendParentInfoAndRegen(pom, childPom.getParentFile(), forceStopAtDir);

                // Update visited list
                File folder = childPom.getParentFile();
                while (folder != null){
                    visitedFolders.add(folder);
                    folder = folder.getParentFile();
                }

            }
        }

        protected void fillChildPoms(File dir, List<File> poms){
            File pom = new File(dir, "pom.xml");
            if(pom.exists()){
                poms.add(pom);
                File[] subDirs = dir.listFiles(File::isDirectory);
                if(subDirs != null)
                    for (File subDir : subDirs) {
                        fillChildPoms(subDir, poms);
                    }
            }
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

    static {
        // This is opt-in meaning user must add it to the plugins, like shown below
        //plugins.add(PackagerPlugin.get);
    }
    /**
     * A plugin class for integrating JavaPackager into the 1JPM build system.
     * This class allows for configuration of the JavaPackager Maven plugin
     * through public fields and methods. It simplifies the setup by using
     * project defaults where applicable.
     */
    public static class PackagerPlugin extends Plugin {
        public static PackagerPlugin get = new PackagerPlugin();

        /**
         * The main class of the application to be packaged.
         * If not set, it defaults to the project's main class.
         */
        public String mainClass;

        /**
         * Indicates whether to bundle a JRE with the application.
         * Defaults to true, so that the user must not install Java separately.
         */
        public boolean bundleJre = true;

        /**
         * Generates a customized JRE, including only identified or specified modules. Otherwise, all modules will be included.
         * Defaults to true.
         */
        public boolean customizedJre = true;

        /**
         * The path to the JRE to be bundled with the application.
         * This is required if bundling with an existing JRE.
         */
        public String jrePath;

        /**
         * Path to a custom runnable JAR to be used instead of generating a new one.
         */
        public String runnableJar;

        /**
         * The target platform for the package (e.g., "windows", "linux", "mac").
         */
        public String platform;

        /**
         * Indicates whether to create a zip archive for the package.
         * Defaults to false.
         */
        public boolean createZipball = false;

        /**
         * Indicates whether to create a tarball archive for the package.
         * Defaults to false.
         */
        public boolean createTarball = false;

        /**
         * Indicates whether to generate an installer for the package.
         * Defaults to true.
         */
        public boolean generateInstaller = true;

        /**
         * The name of the package. If not set, it defaults to the project's artifact ID.
         */
        public String name;

        /**
         * Constructor for the PackagerPlugin class.
         * Sets up the plugin with its group ID, artifact ID, and version.
         * Automatically configures plugin options using project defaults where available.
         */
        public PackagerPlugin() {
            super("io.github.fvarrui", "javapackager", "1.7.6");
            onBeforeToXML(d -> {
                // Use project defaults where applicable
                if (mainClass != null && !mainClass.isEmpty()) {
                    d.putConfiguration("mainClass", mainClass);
                } else if (d.project.mainClass != null && !d.project.mainClass.isEmpty()) {
                    d.putConfiguration("mainClass", d.project.mainClass);
                }

                if (bundleJre) {
                    d.putConfiguration("bundleJre", "true");
                }

                if (!customizedJre) {
                    d.putConfiguration("customizedJre", "false");
                }

                if (jrePath != null && !jrePath.isEmpty()) {
                    d.putConfiguration("jrePath", jrePath);
                }

                if (runnableJar != null && !runnableJar.isEmpty()) {
                    d.putConfiguration("runnableJar", runnableJar);
                }

                if (platform != null && !platform.isEmpty()) {
                    d.putConfiguration("platform", platform);
                }

                if (createZipball) {
                    d.putConfiguration("createZipball", "true");
                }

                if (createTarball) {
                    d.putConfiguration("createTarball", "true");
                }

                if (!generateInstaller) {
                    d.putConfiguration("generateInstaller", "false");
                }

                if (name != null && !name.isEmpty()) {
                    d.putConfiguration("name", name);
                } else if (d.project.artifactId != null && !d.project.artifactId.isEmpty()) {
                    d.putConfiguration("name", d.project.artifactId);
                }
            });
        }

        /**
         * Adds a custom execution to the plugin.
         *
         * @param id The ID of the execution.
         * @param phase The phase of the build lifecycle to attach the execution to (e.g., "package").
         * @param executionConfig A Consumer function to configure the execution's goals and additional settings.
         * @return The current PackagerPlugin instance for chaining.
         */
        public PackagerPlugin addExecution(String id, String phase, Consumer<Execution> executionConfig) {
            Execution execution = new Execution(id, phase);
            executionConfig.accept(execution);
            this.onBeforeToXML(d -> d.addExecution(execution));
            return this;
        }

        /**
         * Adds multiple executions to the plugin, typically for creating packages
         * for different platforms (Windows, Linux, Mac).
         *
         * @return The current PackagerPlugin instance for chaining.
         */
        public PackagerPlugin addMultipleExecutions() {
            // Bundling for Windows
            addExecution("bundling-for-windows", "package", e -> {
                e.addGoal("package");
                e.putConfiguration("platform", "windows");
                e.putConfiguration("createZipball", "true");
            });

            // Bundling for Linux
            addExecution("bundling-for-linux", "package", e -> {
                e.addGoal("package");
                e.putConfiguration("platform", "linux");
                e.putConfiguration("createTarball", "true");
                if (jrePath != null && !jrePath.isEmpty()) {
                    e.putConfiguration("jdkPath", jrePath);
                }
            });

            // Bundling for Mac
            addExecution("bundling-for-mac", "package", e -> {
                e.addGoal("package");
                e.putConfiguration("platform", "mac");
                e.putConfiguration("createTarball", "true");
                if (jrePath != null && !jrePath.isEmpty()) {
                    e.putConfiguration("jdkPath", jrePath);
                }
            });

            return this;
        }
    }

    static {
        // This is opt-in meaning user must add it to the plugins, like shown below
        //plugins.add(NativeImagePlugin.get);
    }
    /**
     * This class integrates the GraalVM Native Image Maven plugin with 1JPM.
     * It provides fields to configure common options for building native images.
     * <p>
     * This plugin allows you to compile your Java application into a native executable
     * using GraalVM's native-image tool. It is highly configurable and supports various
     * advanced features such as building shared libraries, enabling debugging information,
     * and integrating with a metadata repository.
     * </p>
     */
    public static class NativeImagePlugin extends JPM.Plugin {

        /**
         * The name of the generated native image. Defaults to the artifactId of the project.
         */
        public String imageName;

        /**
         * The main class of the application. Defaults to the project's main class.
         */
        public String mainClass;

        /**
         * Whether to skip the native image build. Defaults to false.
         */
        public boolean skipNativeBuild = false;

        /**
         * Whether to skip native image tests. Defaults to false.
         */
        public boolean skipNativeTests = false;

        /**
         * Whether to enable debugging information in the native image. Defaults to false.
         */
        public boolean debug = false;

        /**
         * Whether to enable verbose output during the native image build. Defaults to false.
         */
        public boolean verbose = false;

        /**
         * Whether to build the native image as a shared library. Defaults to false.
         */
        public boolean sharedLibrary = false;

        /**
         * Whether to use an argument file for passing arguments to the native-image tool. Defaults to false.
         */
        public boolean useArgFile = false;

        /**
         * Whether to enable quick build mode. Defaults to false.
         */
        public boolean quickBuild = false;

        /**
         * A map of environment variables to be passed to the native-image tool.
         */
        public Map<String, String> environment = new HashMap<>();

        /**
         * A map of system properties to be passed to the native-image tool.
         */
        public Map<String, String> systemPropertyVariables = new HashMap<>();

        /**
         * A list of additional arguments to pass to the native-image tool.
         */
        public List<String> buildArgs = new ArrayList<>();

        /**
         * A list of JVM arguments to pass during the native-image build.
         */
        public List<String> jvmArgs = new ArrayList<>();

        /**
         * A list of directories where configuration files for native-image should be looked up.
         */
        public List<String> configurationFileDirectories = new ArrayList<>();

        /**
         * A custom classpath for the native image build.
         */
        public List<String> classpath = new ArrayList<>();

        /**
         * The directory containing the application's classes.
         */
        public String classesDirectory;

        /**
         * Configuration for the GraalVM native agent.
         */
        public GraalAgentConfig agentConfig;

        /**
         * Configuration for the GraalVM metadata repository.
         */
        public GraalMetadataRepositoryConfig metadataRepositoryConfig;

        public NativeImagePlugin() {
            super("org.graalvm.buildtools", "native-maven-plugin", "0.10.2");
            onBeforeToXML(d -> {
                // Use sensible defaults if not provided
                if (imageName == null) {
                    imageName = d.project.artifactId;  // Default to project's artifactId
                }
                if (mainClass == null) {
                    mainClass = d.project.mainClass;  // Default to project's mainClass
                }

                d.putConfiguration("imageName", imageName);
                d.putConfiguration("mainClass", mainClass);

                if (skipNativeBuild) d.putConfiguration("skipNativeBuild", "true");
                if (skipNativeTests) d.putConfiguration("skipNativeTests", "true");
                if (debug) d.putConfiguration("debug", "true");
                if (verbose) d.putConfiguration("verbose", "true");
                if (sharedLibrary) d.putConfiguration("sharedLibrary", "true");
                if (useArgFile) d.putConfiguration("useArgFile", "true");
                if (quickBuild) d.putConfiguration("quickBuild", "true");

                if (!environment.isEmpty()) {
                    XML envXml = new XML("environment");
                    for (Map.Entry<String, String> entry : environment.entrySet()) {
                        envXml.put(entry.getKey(), entry.getValue());
                    }
                    d.projectXml.add("configuration", envXml);
                }

                if (!systemPropertyVariables.isEmpty()) {
                    XML sysPropXml = new XML("systemPropertyVariables");
                    for (Map.Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                        sysPropXml.put(entry.getKey(), entry.getValue());
                    }
                    d.projectXml.add("configuration", sysPropXml);
                }

                if (!buildArgs.isEmpty()) {
                    XML buildArgsXml = new XML("buildArgs");
                    for (String arg : buildArgs) {
                        buildArgsXml.put("buildArg", arg);
                    }
                    d.projectXml.add("configuration", buildArgsXml);
                }

                if (!jvmArgs.isEmpty()) {
                    XML jvmArgsXml = new XML("jvmArgs");
                    for (String arg : jvmArgs) {
                        jvmArgsXml.put("arg", arg);
                    }
                    d.projectXml.add("configuration", jvmArgsXml);
                }

                if (!configurationFileDirectories.isEmpty()) {
                    XML configDirsXml = new XML("configurationFileDirectories");
                    for (String dir : configurationFileDirectories) {
                        configDirsXml.put("dir", dir);
                    }
                    d.projectXml.add("configuration", configDirsXml);
                }

                if (!classpath.isEmpty()) {
                    XML classpathXml = new XML("classpath");
                    for (String cp : classpath) {
                        classpathXml.put("param", cp);
                    }
                    d.projectXml.add("configuration", classpathXml);
                }

                if (classesDirectory != null) {
                    d.putConfiguration("classesDirectory", classesDirectory);
                }

                if (agentConfig != null) {
                    d.projectXml.add("configuration agent", agentConfig.toXML());
                }

                if (metadataRepositoryConfig != null) {
                    d.projectXml.add("configuration metadataRepository", metadataRepositoryConfig.toXML());
                }
            });
        }
    }


    /**
     * Configuration class for the GraalVM native agent.
     */
    public static class GraalAgentConfig {
        public boolean enabled = false;
        public String defaultMode;
        public Map<String, String> options = new HashMap<>();
        public Map<String, String> modes = new HashMap<>();
        public GraalMetadataCopyConfig metadataCopy;

        public XML toXML() {
            XML xml = new XML("agent");
            xml.put("enabled", String.valueOf(enabled));
            if (defaultMode != null) xml.put("defaultMode", defaultMode);

            if (!modes.isEmpty()) {
                for (Map.Entry<String, String> entry : modes.entrySet()) {
                    xml.put("modes " + entry.getKey(), entry.getValue());
                }
            }

            if (!options.isEmpty()) {
                for (Map.Entry<String, String> entry : options.entrySet()) {
                    xml.put("options " + entry.getKey(), entry.getValue());
                }
            }

            if (metadataCopy != null) {
                xml.add("metadataCopy", metadataCopy.toXML());
            }

            return xml;
        }
    }

    /**
     * Configuration class for handling the metadata copy process after the native agent execution.
     */
    public static class GraalMetadataCopyConfig {
        public boolean merge = false;
        public String outputDirectory;
        public Set<String> disabledStages = new HashSet<>();

        public XML toXML() {
            XML xml = new XML("metadataCopy");
            xml.put("merge", String.valueOf(merge));
            if (outputDirectory != null) xml.put("outputDirectory", outputDirectory);

            if (!disabledStages.isEmpty()) {
                XML stagesXml = new XML("disabledStages");
                for (String stage : disabledStages) {
                    stagesXml.put("stage", stage);
                }
                xml.add(stagesXml);
            }

            return xml;
        }
    }

    /**
     * Configuration class for managing the GraalVM metadata repository.
     */
    public static class GraalMetadataRepositoryConfig {
        public boolean enabled = true;
        public String localPath;
        public String url;
        public String version;
        public Map<String, GraalDependencyConfig> dependencies = new HashMap<>();

        public XML toXML() {
            XML xml = new XML("metadataRepository");
            xml.put("enabled", String.valueOf(enabled));
            if (localPath != null) xml.put("localPath", localPath);
            if (url != null) xml.put("url", url);
            if (version != null) xml.put("version", version);

            if (!dependencies.isEmpty()) {
                XML depsXml = new XML("dependencies");
                for (Map.Entry<String, GraalDependencyConfig> entry : dependencies.entrySet()) {
                    depsXml.add(entry.getValue().toXML());
                }
                xml.add(depsXml);
            }

            return xml;
        }
    }

    /**
     * Represents configuration for a specific dependency in the metadata repository.
     */
    public static class GraalDependencyConfig {
        public String groupId;
        public String artifactId;
        public boolean excluded = false;
        public String metadataVersion;

        public XML toXML() {
            XML xml = new XML("dependency");
            xml.put("groupId", groupId);
            xml.put("artifactId", artifactId);
            if (excluded) xml.put("excluded", "true");
            if (metadataVersion != null) xml.put("metadataVersion", metadataVersion);
            return xml;
        }
    }
}
