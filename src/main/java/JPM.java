import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.security.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;

class ThisProject extends JPM.Project {
    static{ // Optional task related examples:
        JPM.ROOT.pluginsAfter.add(new JPM.Plugin("deploy").withExecute((project) -> {
            // Register custom task named "deploy", and run your tasks code here.
            // If this throws an exception the whole build stops.
        }));
        JPM.Build.GET.pluginsAfter.add(new JPM.Plugin("").withExecute((project) -> {
            // Run something after/before another task.
            // In this case after the "build" task
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


// 1JPM version 1.0.4 by Osiris-Team
// To upgrade JPM, replace the JPM class below with its newer version
public class JPM {
    public static final Plugin ROOT = new Plugin("root");

    public static void main(String[] args) throws Exception {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        if (argList.isEmpty()) {
            System.out.println("Usage: java JPM.java <task>");
            System.out.println("Use 'java JPM.java help' to see available tasks.");
            return;
        }

        // Load third party plugins
        new ThirdPartyPlugins();

        // Execute tasks
        ThisProject thisProject = new ThisProject(argList);
        for (String arg : argList) {
            long startTime = System.currentTimeMillis();
            thisProject.executeRootTask(arg);
            long endTime = System.currentTimeMillis();
            System.out.println("Task '" + arg + "' completed in " + (endTime - startTime) + "ms");
        }
        System.out.println("All relevant files can be found inside /build at "+Paths.get(thisProject.buildDir));
    }

    //
    // API and Models
    //

    public static interface ConsumerWithException<T> extends Serializable {
        void accept(T t) throws Exception;
    }

    public static class Plugin {
        public String id;
        public ConsumerWithException<Project> execute = (project) -> {};
        public List<Plugin> pluginsBefore = new CopyOnWriteArrayList<>();
        public List<Plugin> pluginsAfter = new CopyOnWriteArrayList<>();

        public Plugin(String id) {
            this.id = id;
        }

        public Plugin withExecute(ConsumerWithException<Project> code){
            this.execute = code;
            return this;
        }

        public Plugin withPluginsBefore(Plugin... l) {
            withPluginsBefore(Arrays.asList(l));
            return this;
        }

        public Plugin withPluginsBefore(List<Plugin> l) {
            this.pluginsBefore = l;
            return this;
        }

        public Plugin withPluginsAfter(Plugin... l) {
            withPluginsAfter(Arrays.asList(l));
            return this;
        }

        public Plugin withPluginsAfter(List<Plugin> l) {
            this.pluginsAfter = l;
            return this;
        }

        public void execute(Project project) throws Exception {
            for (Plugin plugin : pluginsBefore) {
                plugin.execute(project);
            }
            execute.accept(project);
            for (Plugin plugin : pluginsAfter) {
                plugin.execute(project);
            }
        }
    }

    public static class Plugins {
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

    public static class VersionRange {
        private final String lowerBound;
        private final boolean lowerInclusive;
        private final String upperBound;
        private final boolean upperInclusive;

        public VersionRange(String range) {
            Pattern pattern = Pattern.compile("([\\[\\(])([^,]+),([^\\]\\)]+)([\\]\\)])");
            Matcher matcher = pattern.matcher(range);
            if (matcher.matches()) {
                lowerInclusive = "[".equals(matcher.group(1));
                lowerBound = matcher.group(2);
                upperBound = matcher.group(3);
                upperInclusive = "]".equals(matcher.group(4));
            } else {
                lowerInclusive = true;
                lowerBound = range;
                upperBound = null;
                upperInclusive = true;
            }
        }

        public boolean includes(String version) {
            int lowerComparison = compareVersions(version, lowerBound);
            if (lowerComparison < 0 || (!lowerInclusive && lowerComparison == 0)) {
                return false;
            }
            if (upperBound == null) {
                return true;
            }
            int upperComparison = compareVersions(version, upperBound);
            return upperComparison < 0 || (upperInclusive && upperComparison == 0);
        }
    }

    public static class Project {
        protected String srcDir = "src/main/java";
        protected String testSrcDir = "src/test/java";
        protected String buildDir = "build";
        protected String classesDir = buildDir + "/classes";
        protected String testClassesDir = buildDir + "/test-classes";
        protected String jarName = "output.jar";
        protected String fatJarName = "output-fat.jar";
        protected String mainClass = "com.example.Main";
        protected String libDir = "lib";
        protected String groupId = "com.example";
        protected String artifactId = "project";
        protected String version = "1.0.0";
        protected List<Dependency> dependencies = new ArrayList<>();
        protected List<String> compilerArgs = new ArrayList<>();

        public void executeRootTask(String task) throws Exception {
            for (Plugin plugin : ROOT.pluginsAfter) {
                if (plugin.id.equals(task)) {
                    plugin.execute(this);
                    return;
                }
            }
            System.out.println("Unknown task: " + task);
        }

        public void implementation(String s){
            String[] split = s.split(":");
            if(split.length < 3) throw new RuntimeException("Does not contain all required details: "+s);
            addDependency(split[0], split[1], split[2]);
        }

        public void addDependency(String groupId, String artifactId, String version) {
            dependencies.add(new Dependency(groupId, artifactId, version));
        }

        public void addCompilerArg(String arg) {
            compilerArgs.add(arg);
        }
    }

    //
    // Utility methods
    //

    public static void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static List<String> getSourceFiles(String directory) throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".java"))
                    .collect(Collectors.toList());
        }
    }

    public static void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode);
        }
    }

    public static void addToJar(JarOutputStream jos, Path sourceDir, String parentPath) throws IOException {
        Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String entryName = parentPath + sourceDir.relativize(file).toString().replace('\\', '/');
                        jos.putNextEntry(new JarEntry(entryName));
                        Files.copy(file, jos);
                        jos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static void addJarToFatJar(JarOutputStream jos, Path jarPath) throws IOException {
        try (JarInputStream jis = new JarInputStream(Files.newInputStream(jarPath))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.isDirectory() && !entry.getName().startsWith("META-INF")) {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = jis.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                    jos.closeEntry();
                }
            }
        }
    }

    public static String getClasspathString(Project project, String... additionalPaths) throws IOException {
        return String.join(File.pathSeparator, getClasspath(project));
    }

    public static List<String> getClasspath(Project project, String... additionalPaths) throws IOException {
        List<String> classpath = new ArrayList<>();
        if(additionalPaths != null)
            for (String additionalPath : additionalPaths) {
                classpath.add(additionalPath.replace("\\", "/"));
            }

        Path libDir = Paths.get(project.libDir);
        if (Files.exists(libDir)) {
            try (Stream<Path> walk = Files.walk(libDir)) {
                classpath.addAll(walk.filter(file -> file.toString().endsWith(".jar")).map(path -> path.toString().replace("\\", "/"))
                        .collect(Collectors.toList()));
            }
        }
        return classpath;
    }

    @Deprecated
    public static void downloadDependency(Dependency dep, Path libDir) throws IOException {
        String mavenRepoUrl = "https://repo1.maven.org/maven2/";
        String artifactPath = dep.groupId.replace('.', '/') + '/' + dep.artifactId + '/' + dep.version + '/' +
                dep.artifactId + '-' + dep.version + ".jar";
        URL url = new URL(mavenRepoUrl + artifactPath);
        Path targetPath = libDir.resolve(dep.artifactId + '-' + dep.version + ".jar");

        System.out.println("Downloading: " + url);
        try (InputStream in = url.openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 1024;
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                outputStream.write(buf, 0, readLen);

            return outputStream.toByteArray();
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }

    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    }

    //
    // Internal plugins
    //

    static {
        ROOT.pluginsAfter.add(Clean.GET);
    }
    public static class Clean extends Plugin {
        public static Clean GET = new Clean();
        public Clean() {
            super("clean");
            withExecute((project) -> {
                System.out.println("Cleaning build directory...");
                Path buildPath = Paths.get(project.buildDir);
                if (Files.exists(buildPath)) {
                    deleteDirectory(buildPath);
                }
            });
        }
    }

    static {
        ROOT.pluginsAfter.add(Compile.GET);
    }
    public static class Compile extends Plugin {
        public static Compile GET = new Compile();
        public Compile() {
            super("compile");
            withExecute((project) -> {
                System.out.println("Compiling Java source files...");
                Files.createDirectories(Paths.get(project.classesDir));

                List<String> sourceFiles = getSourceFiles(project.srcDir);

                List<String> compileCommand = new ArrayList<>(Arrays.asList(
                        "javac", "-d", project.classesDir, "-cp", getClasspathString(project)
                ));
                compileCommand.addAll(project.compilerArgs);
                compileCommand.addAll(sourceFiles);

                runCommand(compileCommand);
            });
            withPluginsBefore(Clean.GET);
        }
    }

    static {
        ROOT.pluginsAfter.add(ProcessResources.GET);
    }
    public static class ProcessResources extends Plugin {
        public static ProcessResources GET = new ProcessResources();
        public ProcessResources() {
            super("processResources");
            withExecute((project) -> {
                System.out.println("Processing resource files...");
                Path resourcesDir = Paths.get("src/main/resources");
                Path outputDir = Paths.get(project.classesDir);

                if (Files.exists(resourcesDir)) {
                    Files.walk(resourcesDir)
                            .filter(Files::isRegularFile)
                            .forEach(source -> {
                                try {
                                    Path relativePath = resourcesDir.relativize(source);
                                    Path destination = outputDir.resolve(relativePath);
                                    Files.createDirectories(destination.getParent());
                                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to process resource: " + source, e);
                                }
                            });
                }
            });
            withPluginsBefore(Compile.GET);
        }
    }

    static {
        ROOT.pluginsAfter.add(CompileTest.GET);
    }
    public static class CompileTest extends Plugin {
        public static CompileTest GET = new CompileTest();
        public CompileTest() {
            super("compileTest");
            withExecute((project) -> {
                System.out.println("Compiling test Java source files...");
                Files.createDirectories(Paths.get(project.testClassesDir));

                List<String> sourceFiles = getSourceFiles(project.testSrcDir);
                List<String> compileCommand = new ArrayList<>(Arrays.asList(
                        "javac", "-d", project.testClassesDir, "-cp",
                        getClasspathString(project, project.classesDir)
                ));
                compileCommand.addAll(project.compilerArgs);
                compileCommand.addAll(sourceFiles);

                runCommand(compileCommand);
            });
            withPluginsBefore(Compile.GET);
        }
    }

    static {
        ROOT.pluginsAfter.add(Test.GET);
    }
    public static class Test extends Plugin {
        public static Test GET = new Test();
        public Test() {
            super("test");
            withExecute((project) -> {
                System.out.println("Running tests...");
                List<String> command = new ArrayList<>(Arrays.asList(
                        "java", "-cp", getClasspathString(project, project.classesDir, project.testClassesDir),
                        "org.junit.platform.console.ConsoleLauncher",
                        "--scan-classpath",
                        "--reports-dir=" + project.buildDir + "/test-results"
                ));

                runCommand(command);
            });
            withPluginsBefore(CompileTest.GET);
        }
    }

    static {
        ROOT.pluginsAfter.add(Jar.GET);
    }
    public static class Jar extends Plugin {
        public static Jar GET = new Jar();
        public Jar() {
            super("jar");
            withExecute((project) -> {
                System.out.println("Creating JAR file...");
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, project.mainClass);

                try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(project.jarName)), manifest)) {
                    Path classesDirPath = Paths.get(project.classesDir);
                    Files.walk(classesDirPath)
                            .filter(Files::isRegularFile)
                            .forEach(file -> {
                                try {
                                    String entryName = classesDirPath.relativize(file).toString().replace('\\', '/');
                                    jos.putNextEntry(new JarEntry(entryName));
                                    Files.copy(file, jos);
                                    jos.closeEntry();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            });
            withPluginsBefore(Compile.GET, ProcessResources.GET);
        }
    }

    static {
        ROOT.pluginsAfter.add(FatJar.GET);
    }
    public static class FatJar extends Plugin {
        public static FatJar GET = new FatJar();
        public FatJar() {
            super("fatJar");
            withExecute((project) -> {
                System.out.println("Creating fat JAR...");
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, project.mainClass);

                try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(project.fatJarName)), manifest)) {
                    // Add project classes
                    addToJar(jos, Paths.get(project.classesDir), "");

                    // Add dependencies
                    for (String dep : getClasspath(project)) {
                        Path depPath = Paths.get(dep);
                        if (Files.isRegularFile(depPath) && depPath.toString().endsWith(".jar")) {
                            addJarToFatJar(jos, depPath);
                        }
                    }
                }
            });
            withPluginsBefore(Compile.GET, ProcessResources.GET);
        }
    }

    static {
        ROOT.pluginsAfter.add(Dependencies.GET);
    }
    public static class Dependencies extends Plugin {
        public static Dependencies GET = new Dependencies();
        public Dependencies() {
            super("dependencies");
            withExecute((project) -> {
                System.out.println("Project dependencies:");
                for (Dependency dep : project.dependencies) {
                    System.out.println("- " + dep);
                }
            });
        }
    }

    static {
        ROOT.pluginsAfter.add(DependencyUpdate.GET);
    }
    public static class DependencyUpdate extends Plugin {
        public static DependencyUpdate GET = new DependencyUpdate();
        public DependencyUpdate() {
            super("dependencyUpdate");
            withExecute((project) -> {
                System.out.println("Checking for dependency updates...");
                for (Dependency dep : project.dependencies) {
                    String latestVersion = getLatestVersion(dep);
                    if (!latestVersion.equals(dep.version)) {
                        System.out.println(dep + " can be updated to " + latestVersion);
                    } else {
                        System.out.println(dep + " is up to date");
                    }
                }
            });
        }

        private String getLatestVersion(Dependency dep) throws IOException {
            String mavenMetadataUrl = String.format(
                    "https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml",
                    dep.groupId.replace('.', '/'), dep.artifactId
            );
            URL url = new URL(mavenMetadataUrl);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("<release>")) {
                        return line.replaceAll(".*<release>(.*)</release>.*", "$1").trim();
                    }
                }
            }
            return dep.version; // Return current version if unable to find latest
        }
    }

    static {
        ROOT.pluginsAfter.add(Help.GET);
    }
    public static class Help extends Plugin {
        public static Help GET = new Help();
        public Help() {
            super("help");
            withExecute((project) -> {
                System.out.println("Available tasks:");
                for (Plugin plugin : ROOT.pluginsAfter) {
                    System.out.println("- " + plugin.id);
                }
                System.out.println("\nUse 'java JPM.java <task>' to run a task.");
            });
        }
    }

    static {
        ROOT.pluginsAfter.add(ResolveDependencies.GET);
    }
    public static class ResolveDependencies extends Plugin {
        public static ResolveDependencies GET = new ResolveDependencies();
        public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
        public static final Set<String> REPOSITORIES = new LinkedHashSet<>(Arrays.asList(MAVEN_CENTRAL));
        public static final Map<String, Dependency> DEPENDENCY_CACHE = new ConcurrentHashMap<>();
        public static final Map<String, String> VERSION_CACHE = new ConcurrentHashMap<>();
        public static final Path LOCAL_REPO = Paths.get(System.getProperty("user.home"), ".m2", "repository");

        public ResolveDependencies() {
            super("resolveDependencies");
            withExecute((project) -> {
                updateCentralIndex();
                resolveDependencies(project);
                handleMultiProjectBuild(project);
                generateDependencyReport(new HashSet<>(project.dependencies), Paths.get(project.buildDir, "dependency-report.txt"));
            });
        }

        protected final Object lock = new Object();

        /**
         * Resolves dependencies recursively, handling version conflicts and downloading artifacts.
         * @throws Exception if resolution fails or timeout occurs
         */
        protected void resolveDependencies(Project project) throws Exception {
            System.out.println("Resolving dependencies...");
            Path libDir = Paths.get(project.libDir);
            Files.createDirectories(libDir);

            Set<Dependency> resolvedDependencies = new LinkedHashSet<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Dependency dep : project.dependencies) {
                resolveDependencyTree(futures, dep, resolvedDependencies, new LinkedHashSet<>(), new LinkedHashSet<>(REPOSITORIES));
            }

            int maxSeconds = 600 * 3;
            for (int i = 0; i < maxSeconds; i++) {
                Thread.sleep(1000);
                boolean isAllDone = true;
                for (CompletableFuture<Void> f : futures) {
                    if(!f.isDone()) {
                        isAllDone = false;
                        break;
                    }
                }
                if(isAllDone) break;
            }
            for (CompletableFuture<Void> f : futures) {
                if(!f.isDone()) throw new Exception("There is still a task running after "+maxSeconds+" seconds. Terminated due to timeout reached.");
            }

            handleDependencyConflicts(resolvedDependencies);

            for (Dependency dep : resolvedDependencies) {
                downloadDependency(dep, libDir);
            }

            generateBuildSignature(resolvedDependencies, project);
        }

        /**
         * Recursively resolves a dependency and its transitive dependencies.
         * Handles circular dependencies and caching.
         */
        protected void resolveDependencyTree(List<CompletableFuture<Void>> futures, Dependency dep,
                                             Set<Dependency> resolvedDependencies, Set<String> visitedDeps, Set<String> currentRepositories) {
            futures.add(CompletableFuture.runAsync(() -> {
                try{
                    synchronized (lock){
                        String depKey = dep.toString();
                        if (visitedDeps.contains(depKey)) {
                            if (!resolvedDependencies.contains(dep)) {
                                System.out.println("Already resolved dependency detected (possibly circular): " + depKey);
                                return;
                            }
                            return;
                        }
                        visitedDeps.add(depKey);

                        Dependency cachedDep = DEPENDENCY_CACHE.get(depKey);
                        if (cachedDep != null) {
                            resolvedDependencies.add(cachedDep);
                            return;
                        }
                    }

                    String pomContent = downloadPom(dep, currentRepositories);
                    if(pomContent.isEmpty()) return;
                    Document pomDoc = parsePom(dep, pomContent);
                    resolveVersion(dep, pomContent, pomDoc, resolvedDependencies, visitedDeps, currentRepositories);

                    // Parse repositories before resolving transitive dependencies
                    Set<String> updatedRepositories = new HashSet<>(currentRepositories);
                    updatedRepositories.addAll(parseRepositories(pomDoc));

                    List<Dependency> transitiveDeps = getDependenciesUnsafe(pomDoc);
                    for (Dependency tDep : transitiveDeps) {
                        resolveVersion(tDep, pomContent, pomDoc, resolvedDependencies, visitedDeps, currentRepositories);
                    }

                    synchronized (lock){
                        dep.transitiveDependencies = transitiveDeps;
                        if(dep.scope == null) dep.scope = "compile";
                        String depKey = dep.toString();
                        DEPENDENCY_CACHE.put(depKey, dep);
                        if (!dep.scope.equals("import")) {
                            resolvedDependencies.add(dep);
                        }
                    }

                    for (Dependency transitiveDep : transitiveDeps) {
                        resolveDependencyTree(futures, transitiveDep, resolvedDependencies, visitedDeps, updatedRepositories);
                    }
                } catch (Exception e) {
                    System.err.println("Error while 'resolveDependencyTree' for "+dep);
                    throw new CompletionException(e);
                }
            }));
        }

        /**
         * Downloads POM file from repositories or retrieves from local cache.
         * @return POM content as string, or empty string if scope is "provided" and POM not found
         * @throws IOException if POM cannot be retrieved
         */
        protected String downloadPom(Dependency dep, Set<String> repositories) throws IOException {
            Path localPomPath = getLocalArtifactPath(dep, "pom");
            if (Files.exists(localPomPath)) {
                System.out.println("Successfully fetched from cache: "+localPomPath);
                return new String(Files.readAllBytes(localPomPath));
            }

            for (String repo : repositories) {
                String pomUrl = String.format("%s%s/%s/%s/%s-%s.pom",
                        repo, dep.groupId.replace('.', '/'), dep.artifactId, dep.version, dep.artifactId, dep.version);
                try {
                    URL url = new URL(pomUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (InputStream in = conn.getInputStream()) {
                            byte[] pomBytes = readAllBytes(in);
                            Files.createDirectories(localPomPath.getParent());
                            Files.write(localPomPath, pomBytes);
                            System.out.println("Successfully fetched from: " + pomUrl);
                            return new String(pomBytes);
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                            || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                            || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        String newUrl = conn.getHeaderField("Location");
                        System.out.println("Redirected to: " + newUrl);
                        // Recursively call the method with the new URL
                        return downloadPomFromUrl(new URL(newUrl), localPomPath);
                    } else {
                        System.out.println("Failed to fetch dependency from URL: " + pomUrl + ". Response code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.out.println("Error fetching dependency from URL: " + pomUrl + ". " + e.getMessage());
                    // Try next repository
                }
            }
            if(dep.scope != null && dep.scope.equals("provided")){
                System.err.println("POM not found ignored since its scope is 'provided' for " + dep);
                return "";
            }
            throw new IOException("POM not found for " + dep);
        }

        private String downloadPomFromUrl(URL url, Path localPomPath) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);

            try (InputStream in = conn.getInputStream()) {
                byte[] pomBytes = readAllBytes(in);
                Files.createDirectories(localPomPath.getParent());
                Files.write(localPomPath, pomBytes);
                return new String(pomBytes);
            }
        }

        protected Set<String> parseRepositories(Document pomDoc) {
            Set<String> newRepositories = new HashSet<>();
            NodeList repositoryNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "repository");
            List<Element> els = new ArrayList<>();
            for (int i = 0; i < repositoryNodes.getLength(); i++) {
                els.add((Element) repositoryNodes.item(i));
            }
            NodeList snapshotRepositoryNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "snapshotRepository");
            for (int i = 0; i < snapshotRepositoryNodes.getLength(); i++) {
                els.add((Element) snapshotRepositoryNodes.item(i));
            }

            for (Element el : els) {
                String url = getElementContent(el, "url");
                if (url != null && !url.isEmpty()) {
                    if (!url.endsWith("/")) {
                        url += "/";
                    }
                    newRepositories.add(url);
                }
            }
            return newRepositories;
        }

        protected Document parsePom(Dependency dep, String pomContent) throws Exception {
            try{
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true); // Enable namespace awareness
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(new ByteArrayInputStream(pomContent.getBytes()));
            } catch (Exception e) {
                System.err.println("Error while 'parsePom' for "+dep);
                System.err.println("Content of pom.xml (with "+pomContent.length()+" chars): \n"+pomContent);
                throw e;
            }
        }

        protected void resolveVersion(Dependency dep, String pomContent, Document pomDoc, Set<Dependency> resolvedDependencies, Set<String> visitedDeps, Set<String> currentRepositories) {
            if(dep.version == null) { // VERSION EMPTY
                // Check parent pom if dep exists inside it and try to retrieve version from there
                NodeList parentNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "parent");
                if (parentNodes.getLength() <= 0) {
                    throw new NoParentException("Dep "+dep+" does not contain <parent> element! Content: \n"+pomContent);
                }
                Element parentElement = (Element) parentNodes.item(0);
                String parentGroupId = getElementContent(parentElement, "groupId");
                String parentArtifactId = getElementContent(parentElement, "artifactId");
                String parentVersion = getElementContent(parentElement, "version");
                try {
                    // Hope that the parent contains the version property
                    if(parentGroupId.equals("org.apache.maven") && parentArtifactId.equals("maven-parent"))
                        throw new NoParentException("Reached maven-parent, thus went through all parents without finding the version!");
                    Dependency parentDep = new Dependency(parentGroupId, parentArtifactId, parentVersion);
                    String parentPomContent = downloadPom(parentDep, currentRepositories);
                    if(parentPomContent.isEmpty()) throw new NullPointerException(parentPomContent);
                    Document parentPomDoc = parsePom(parentDep, parentPomContent);
                    if(parentArtifactId.equals("surefire") && dep.artifactId.equals("common-junit48"))
                        System.out.println("WOWS");
                    List<Dependency> dependencies = getDependenciesUnsafe(parentPomDoc);
                    for (Dependency depInParent : dependencies) {
                        if(depInParent.groupId.equals(dep.groupId) && depInParent.artifactId.equals(dep.artifactId)){
                            resolveVersion(depInParent, parentPomContent, parentPomDoc, resolvedDependencies, visitedDeps, currentRepositories);
                            dep.version = depInParent.version;
                            break;
                        }
                    }
                    if(dep.version == null){
                        // Go one up and try parent of parent (until there is no parent left)
                        resolveVersion(dep, parentPomContent, parentPomDoc, resolvedDependencies, visitedDeps, currentRepositories);
                        return;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error resolving dependency ("+dep+") version from parent POM ("+parentGroupId+":"+parentArtifactId+":"+parentVersion+")", e);
                }
                return;
            }

            if (!dep.version.startsWith("${") && !dep.version.startsWith("[") && !dep.version.contains(",")) {
                return;
            }

            String cacheKey = dep.groupId + ":" + dep.artifactId + ":" + dep.version;
            String cachedVersion = VERSION_CACHE.get(cacheKey);
            if (cachedVersion != null) {
                dep.version = cachedVersion;
                return;
            }

            String resolvedVersion;
            if (dep.version.startsWith("${")) {
                resolvedVersion = resolveVersionFromProperty(dep, dep.version, pomContent, pomDoc);
            } else {
                List<String> availableVersions = getAvailableVersions(pomDoc);
                Map<String, String> managedVersions = parseDependencyManagement(pomDoc);
                String key = dep.groupId + ":" + dep.artifactId;
                String priorityVersion = managedVersions.get(key);
                if(priorityVersion != null) availableVersions.add(priorityVersion);
                resolvedVersion = resolveVersionRange(dep.version, availableVersions);
            }

            if(resolvedVersion == null){ // VERSION AS PROPERTY
                // Check parent pom if contains property
                NodeList parentNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "parent");
                if (parentNodes.getLength() <= 0) {
                    throw new NoParentException("Error resolving property from parent POM, no <parent> element! Content: \n"+pomContent);
                }
                Element parentElement = (Element) parentNodes.item(0);
                String parentGroupId = getElementContent(parentElement, "groupId");
                String parentArtifactId = getElementContent(parentElement, "artifactId");
                String parentVersion = getElementContent(parentElement, "version");
                try {
                    // Hope that the parent contains the version property
                    if(parentGroupId.equals("org.apache.maven") && parentArtifactId.equals("maven-parent"))
                        throw new NoParentException("Reached maven-parent, thus went through all parents without finding the version!");
                    Dependency parentDep = new Dependency(parentGroupId, parentArtifactId, parentVersion);
                    String parentPomContent = downloadPom(parentDep, currentRepositories);
                    if(parentPomContent.isEmpty()) throw new NullPointerException(parentPomContent);
                    Document parentPomDoc = parsePom(parentDep, parentPomContent);
                    if(parentArtifactId.equals("surefire") && dep.artifactId.equals("common-junit48"))
                        System.out.println("WOWS");
                    resolvedVersion = resolveVersionFromProperty(dep, dep.version, parentPomContent, parentPomDoc);
                    if(resolvedVersion == null){
                        // Go one up into parent and try again
                        resolveVersion(dep, parentPomContent, parentPomDoc, resolvedDependencies, visitedDeps, currentRepositories);
                        return;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error resolving dependency ("+dep+") property version from parent POM ("+parentGroupId+":"+parentArtifactId+":"+parentVersion+")", e);
                }
            }

            dep.version = resolvedVersion;
            VERSION_CACHE.put(cacheKey, resolvedVersion);
        }

        /**
         * Resolves version from property references, handling nested properties and parent POMs.
         * @return resolved version or null if property not found
         */
        protected String resolveVersionFromProperty(Dependency dep, String propertyRef, String pomContent, Document pomDoc) {
            String propertyName = propertyRef.substring(2, propertyRef.length() - 1);
            String value = resolveVersionFromProperty1(dep, propertyName, pomContent, pomDoc);
            if (value == null) {
                System.err.println("Property not found: " + propertyName);
                return null;
            }
            return value.contains("${") ? resolveVersionFromProperty(dep, value, pomContent, pomDoc) : value;
        }

        protected String resolveVersionFromProperty1(Dependency dep, String propertyName, String pomContent, Document pomDoc) {
            String[] propertyParts = propertyName.split("\\.");

            // Check project properties
            NodeList propertiesNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "properties");
            for (int i = 0; i < propertiesNodes.getLength(); i++) {
                Element propertiesElement = (Element) propertiesNodes.item(i);
                NodeList propertyNodes = propertiesElement.getChildNodes();
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Node propertyNode = propertyNodes.item(j);
                    if (propertyNode.getNodeType() == Node.ELEMENT_NODE &&
                            propertyNode.getLocalName().equals(propertyName)) {
                        return propertyNode.getTextContent().trim();
                    }
                }
            }

            // Check nested project properties
            if(propertyParts[0].equals("project")) propertyParts = Arrays.copyOfRange(propertyParts, 1, propertyParts.length);
            Element projectElement = pomDoc.getDocumentElement();
            Element currentElement = projectElement;
            for (String part : propertyParts) {
                NodeList childNodes = currentElement.getChildNodes();
                currentElement = null;
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE &&
                            childNode.getLocalName().equals(part)) {
                        currentElement = (Element) childNode;
                        break;
                    }
                }
                if (currentElement == null) {
                    break;
                }
            }
            if (currentElement != null && !currentElement.equals(projectElement)) {
                return currentElement.getTextContent().trim();
            }

            return null;
        }

        private static class NoParentException extends RuntimeException{
            public NoParentException(String message) {
                super(message);
            }

            public NoParentException(String message, Throwable cause) {
                super(message, cause);
            }
        }

        /**
         * Expects dep.version to be either null or a property. <br>
         * Either way the parent pom will be fetched, then if dep.version null
         * we search for the dep in that poms dependencies,
         * if dep.version a property we check that poms properties. <br>
         * Both cases might fail, thus do the same with parent of parent, etc. until no parent is left.
         */
        protected List<String> getAvailableVersions(Document pomDoc) {
            List<String> availableVersions = new ArrayList<>();
            NodeList versionElements = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "version");
            for (int i = 0; i < versionElements.getLength(); i++) {
                Node item = versionElements.item(i);
                availableVersions.add(item.getTextContent().trim());
            }
            return availableVersions;
        }

        public enum DependencyType {
            REGULAR("dependencies"), MANAGED("dependencyManagement");
            public final String enclosingXMLTagName;

            DependencyType(String enclosingXMLTagName) {
                this.enclosingXMLTagName = enclosingXMLTagName;
            }
        }

        /**
         * Retrieves dependencies from POM, merging managed and regular dependencies.
         * @return List of dependencies with potentially unresolved versions
         */
        protected List<Dependency> getDependenciesUnsafe(Document pomDoc){
            List<Dependency> managed = getDependenciesUnsafe(pomDoc, DependencyType.MANAGED);
            List<Dependency> regular =  getDependenciesUnsafe(pomDoc, DependencyType.REGULAR);
            List<Dependency> merged = new ArrayList<>();
            merged.addAll(managed);
            for (Dependency dep : regular) {
                boolean containsManagedDep = false;
                for (Dependency managedDep : managed) {
                    if(managedDep.groupId.equals(dep.groupId) && managedDep.artifactId.equals(dep.artifactId)){
                        containsManagedDep = true;
                        break;
                    }
                }
                if(!containsManagedDep) merged.add(dep);
            }
            return merged;
        }
        /**
         * Unsafe because their versions are not retrieved.
         */
        protected List<Dependency> getDependenciesUnsafe(Document pomDoc, DependencyType dependencyType){
            List<Dependency> deps = new ArrayList<>();
            NodeList enclosingNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", dependencyType.enclosingXMLTagName);
            if (enclosingNodes.getLength() > 0) {
                Element el = (Element) enclosingNodes.item(0);
                NodeList nodes = el.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "dependency");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element depElement = (Element) nodes.item(i);
                    String groupId = getElementContent(depElement, "groupId");
                    String artifactId = getElementContent(depElement, "artifactId");
                    String version = getElementContent(depElement, "version");
                    String scope = getElementContent(depElement, "scope");
                    boolean optional = Boolean.parseBoolean(getElementContent(depElement, "optional"));
                    Dependency transitiveDependency = new Dependency(groupId, artifactId, version, scope);
                    deps.add(transitiveDependency);
                }
            }
            return deps;
        }

        protected Map<String, String> parseDependencyManagement(Document pomDoc) {
            Map<String, String> managedVersions = new HashMap<>();
            NodeList dependencyManagementNodes = pomDoc.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "dependencyManagement");

            if (dependencyManagementNodes.getLength() > 0) {
                Element dependencyManagementElement = (Element) dependencyManagementNodes.item(0);
                NodeList managedDependencyNodes = dependencyManagementElement.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "dependency");

                for (int i = 0; i < managedDependencyNodes.getLength(); i++) {
                    Element depElement = (Element) managedDependencyNodes.item(i);
                    String groupId = getElementContent(depElement, "groupId");
                    String artifactId = getElementContent(depElement, "artifactId");
                    String version = getElementContent(depElement, "version");

                    if (groupId != null && artifactId != null && version != null) {
                        String key = groupId + ":" + artifactId;
                        managedVersions.put(key, version);
                    }
                }
            }

            return managedVersions;
        }

        protected String getElementContent(Element parent, String tagName) {
            NodeList elements = parent.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", tagName);
            return elements.getLength() > 0 ? elements.item(0).getTextContent().trim() : null;
        }

        protected void downloadDependency(Dependency dep, Path libDir) throws IOException {
            Path localJarPath = getLocalArtifactPath(dep, "jar");
            Path targetPath = libDir.resolve(dep.artifactId + '-' + dep.version + ".jar");

            if (Files.exists(localJarPath)) {
                Files.copy(localJarPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            if (Files.exists(targetPath)) return;

            for (String repo : REPOSITORIES) {
                String jarUrl = String.format("%s%s/%s/%s/%s-%s.jar",
                        repo, dep.groupId.replace('.', '/'), dep.artifactId, dep.version, dep.artifactId, dep.version);
                try {
                    URL url = new URL(jarUrl);
                    System.out.println("Downloading: " + url);
                    try (InputStream in = url.openStream()) {
                        byte[] jarBytes = readAllBytes(in);
                        Files.createDirectories(localJarPath.getParent());
                        Files.write(localJarPath, jarBytes);
                        Files.copy(localJarPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    verifyChecksum(dep, "jar");
                    return;
                } catch (FileNotFoundException e) {
                    // Try next repository
                }
            }
            throw new IOException("JAR not found for " + dep);
        }

        protected Path getLocalArtifactPath(Dependency dep, String extension) {
            System.out.println(dep);
            return LOCAL_REPO.resolve(
                            dep.groupId.replace('.', File.separatorChar))
                    .resolve(dep.artifactId)
                    .resolve(dep.version)
                    .resolve(dep.artifactId + "-" + dep.version + "." + extension);
        }

        protected void verifyChecksum(Dependency dep, String extension) throws IOException {
            Path artifactPath = getLocalArtifactPath(dep, extension);
            Path checksumPath = Paths.get(artifactPath.toString() + ".sha1");

            if (!Files.exists(checksumPath)) {
                downloadChecksum(dep, extension);
            }

            String expectedChecksum = new String(Files.readAllBytes(checksumPath)).trim();
            String actualChecksum = calculateSHA1(artifactPath);

            if (!expectedChecksum.equals(actualChecksum)) {
                throw new IOException("Checksum verification failed for " + dep);
            }
        }

        protected void downloadChecksum(Dependency dep, String extension) throws IOException {
            for (String repo : REPOSITORIES) {
                String checksumUrl = String.format("%s%s/%s/%s/%s-%s.%s.sha1",
                        repo, dep.groupId.replace('.', '/'), dep.artifactId, dep.version, dep.artifactId, dep.version, extension);
                try {
                    URL url = new URL(checksumUrl);
                    Path checksumPath = getLocalArtifactPath(dep, extension + ".sha1");
                    try (InputStream in = url.openStream()) {
                        Files.copy(in, checksumPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return;
                } catch (FileNotFoundException e) {
                    // Try next repository
                }
            }
            throw new IOException("Checksum not found for " + dep);
        }

        protected String calculateSHA1(Path file) throws IOException {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] fileBytes = Files.readAllBytes(file);
                byte[] hashBytes = digest.digest(fileBytes);
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("SHA-1 algorithm not available", e);
            }
        }

        protected void handleDependencyConflicts(Set<Dependency> dependencies) {
            Map<String, SortedSet<Dependency>> dependencyVersions = new HashMap<>();
            for (Dependency dep : dependencies) {
                String key = dep.groupId + ":" + dep.artifactId;
                dependencyVersions.computeIfAbsent(key, k -> new TreeSet<>((d1, d2) -> compareVersions(d2.version, d1.version))).add(dep);
            }
            dependencies.clear();
            for (SortedSet<Dependency> versions : dependencyVersions.values()) {
                dependencies.add(versions.first()); // Add the highest version
            }
        }

        protected void generateBuildSignature(Set<Dependency> dependencies, Project project) throws Exception {
            List<String> depStrings = dependencies.stream()
                    .map(Dependency::toString)
                    .sorted()
                    .collect(Collectors.toList());

            String dependenciesString = String.join("\n", depStrings);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(dependenciesString.getBytes());
            String signature = Base64.getEncoder().encodeToString(digest);

            Path signaturePath = Paths.get(project.buildDir, "build-signature.txt");
            Files.write(signaturePath, signature.getBytes());
            System.out.println("Build signature generated: " + signature);
        }

        /**
         * Resolves version ranges against available versions.
         * @return Highest matching version or throws RuntimeException if no match found
         */
        protected String resolveVersionRange(String versionRange, List<String> availableVersions) {
            if (!versionRange.contains(",")) {
                return versionRange; // It's a specific version, not a range
            }

            List<VersionRange> ranges = parseVersionRanges(versionRange);
            return availableVersions.stream()
                    .filter(v -> ranges.stream().allMatch(r -> r.includes(v)))
                    .max(JPM::compareVersions)
                    .orElseThrow(() -> new RuntimeException("No version satisfies range: " + versionRange));
        }

        protected List<VersionRange> parseVersionRanges(String versionRange) {
            List<VersionRange> ranges = new ArrayList<>();
            String[] parts = versionRange.split(",");
            for (int i = 0; i < parts.length; i += 2) {
                String lower = parts[i].trim();
                String upper = (i + 1 < parts.length) ? parts[i + 1].trim() : "";
                ranges.add(new VersionRange(lower + "," + upper));
            }
            return ranges;
        }

        /**
         * Handles multi-project builds by resolving dependencies for all subprojects.
         * Replaces inter-project dependencies with project outputs.
         */
        protected void handleMultiProjectBuild(Project rootProject) throws Exception {
            List<Project> allProjects = new ArrayList<>();
            allProjects.add(rootProject);
            allProjects.addAll(findSubprojects(rootProject));

            for (Project project : allProjects) {
                resolveDependencies(project);
            }

            // Resolve inter-project dependencies
            for (Project project : allProjects) {
                for (Dependency dep : project.dependencies) {
                    Project dependencyProject = findProjectByArtifact(allProjects, dep);
                    if (dependencyProject != null) {
                        // Replace the dependency with the project's output
                        replaceWithProjectOutput(project, dep, dependencyProject);
                    }
                }
            }
        }

        protected List<Project> findSubprojects(Project rootProject) {
            // Implementation depends on your project structure
            // This is a placeholder implementation
            return new ArrayList<>();
        }

        protected Project findProjectByArtifact(List<Project> projects, Dependency dep) {
            return projects.stream()
                    .filter(p -> p.groupId.equals(dep.groupId) && p.artifactId.equals(dep.artifactId))
                    .findFirst()
                    .orElse(null);
        }

        protected void replaceWithProjectOutput(Project project, Dependency dep, Project dependencyProject) {
            // Implementation depends on your build system
            // This is a placeholder implementation
            System.out.println("Replacing " + dep + " with output from " + dependencyProject.artifactId);
        }

        protected void cacheArtifactLocally(Dependency dep, Path artifactPath) throws IOException {
            Path localPath = getLocalArtifactPath(dep, "jar");
            Files.createDirectories(localPath.getParent());
            Files.copy(artifactPath, localPath, StandardCopyOption.REPLACE_EXISTING);

            // Verify checksum
            verifyChecksum(dep, "jar");
        }

        protected boolean isArtifactCached(Dependency dep) {
            Path localPath = getLocalArtifactPath(dep, "jar");
            if (!Files.exists(localPath)) {
                return false;
            }
            try {
                verifyChecksum(dep, "jar");
                return true;
            } catch (IOException e) {
                System.out.println("Warning: Cached artifact for " + dep + " failed checksum verification. Will re-download.");
                return false;
            }
        }

        protected void updateCentralIndex() throws Exception {
            // In a real implementation, this would download and parse the Maven Central Index
            // For simplicity, we'll just print a message
            System.out.println("Updating central index...");
        }

        protected void cleanLocalRepository() throws IOException {
            System.out.println("Cleaning local repository...");
            Files.walk(LOCAL_REPO)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        /**
         * Generates a report of all dependencies and their transitive dependencies.
         * @param reportPath Path where the report will be written
         */
        protected void generateDependencyReport(Set<Dependency> dependencies, Path reportPath) throws IOException {
            List<String> reportLines = new ArrayList<>();
            reportLines.add("Dependency Report");
            reportLines.add("==================");
            for (Dependency dep : dependencies) {
                reportLines.addAll(generateDependencyReport1("", dep));
            }
            Files.write(reportPath, reportLines);
        }

        protected List<String> generateDependencyReport1(String spaces, Dependency dep) throws IOException {
            List<String> reportLines = new ArrayList<>();
            reportLines.add(spaces + dep.toString());
            for (Dependency transitive : dep.transitiveDependencies) {
                reportLines.addAll(generateDependencyReport1(spaces + "    ", transitive));
            }
            return reportLines;
        }
    }

    static {
        ROOT.pluginsAfter.add(Build.GET);
    }
    public static class Build extends Plugin {
        public static Build GET = new Build();
        public Build() {
            super("build");
            withExecute((project) -> {
                System.out.println("Building project...");
                // This task doesn't need to do anything as it depends on other tasks
            });
            withPluginsBefore(ResolveDependencies.GET, Compile.GET, ProcessResources.GET, CompileTest.GET, Test.GET, Jar.GET);
        }
    }
}
