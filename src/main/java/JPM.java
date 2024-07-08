import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ThisProject extends JPM.Project {
    static{
        JPM.ROOT.pluginsAfter.add(new JPM.Plugin("deploy").withExecute((project) -> { // Register custom task
            //deployToServer(project); // If it throws an exception the whole build stops
        }));
        JPM.Build.GET.pluginsAfter.add(new JPM.Plugin("").withExecute((project) -> {
            // Run something after/before another task
        }));
    }

    public ThisProject(List<String> argList) {
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

        // Add some compiler arguments
        addCompilerArg("-Xlint:unchecked");
        addCompilerArg("-Xlint:deprecation");
    }
}


// 1JPM version 1.0.0 by Osiris-Team
public class JPM {
    public static final Plugin ROOT = new Plugin("root");
    static{
        // Register all plugins/tasks, add your third party plugins here too
        System.out.print(Clean.GET);
        System.out.print(Compile.GET);
        System.out.print(ProcessResources.GET);
        System.out.print(CompileTest.GET);
        System.out.print(Test.GET);
        System.out.print(Jar.GET);
        System.out.print(FatJar.GET);
        System.out.print(Dependencies.GET);
        System.out.print(DependencyUpdate.GET);
        System.out.print(Help.GET);
        System.out.print(ResolveDependencies.GET);
        System.out.print(Build.GET);
        System.out.println();
    }
    public static void main(String[] args) throws Exception {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        if (argList.isEmpty()) {
            System.out.println("Usage: java JPM.java <task>");
            System.out.println("Use 'java JPM.java help' to see available tasks.");
            return;
        }

        // Execute tasks
        ThisProject thisProject = new ThisProject(argList);
        for (String arg : argList) {
            long startTime = System.currentTimeMillis();
            thisProject.executeRootTask(arg);
            long endTime = System.currentTimeMillis();
            System.out.println("Task '" + arg + "' completed in " + (endTime - startTime) + "ms");
        }
    }

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

    public static class Dependency {
        public String groupId;
        public String artifactId;
        public String version;

        public Dependency(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
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

        public void addDependency(String groupId, String artifactId, String version) {
            dependencies.add(new Dependency(groupId, artifactId, version));
        }

        public void addCompilerArg(String arg) {
            compilerArgs.add(arg);
        }
    }

    public static class Clean extends Plugin {
        public static Clean GET = new Clean();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    public static class Compile extends Plugin {
        public static Compile GET = new Compile();

        static {
            ROOT.pluginsAfter.add(GET);
        }

        public Compile() {
            super("compile");
            withExecute((project) -> {
                System.out.println("Compiling Java source files...");
                Files.createDirectories(Paths.get(project.classesDir));

                List<String> sourceFiles = getSourceFiles(project.srcDir);
                List<String> compileCommand = new ArrayList<>(Arrays.asList(
                        "javac", "-d", project.classesDir
                ));
                compileCommand.addAll(project.compilerArgs);
                compileCommand.addAll(getClasspath(project));
                compileCommand.addAll(sourceFiles);

                runCommand(compileCommand);
            });
            withPluginsBefore(Clean.GET);
        }
    }

    public static class ProcessResources extends Plugin {
        public static ProcessResources GET = new ProcessResources();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    public static class CompileTest extends Plugin {
        public static CompileTest GET = new CompileTest();

        static {
            ROOT.pluginsAfter.add(GET);
        }

        public CompileTest() {
            super("compileTest");
            withExecute((project) -> {
                System.out.println("Compiling test Java source files...");
                Files.createDirectories(Paths.get(project.testClassesDir));

                List<String> sourceFiles = getSourceFiles(project.testSrcDir);
                List<String> compileCommand = new ArrayList<>(Arrays.asList(
                        "javac", "-d", project.testClassesDir, "-cp",
                        project.classesDir + File.pathSeparator + String.join(File.pathSeparator, getClasspath(project))
                ));
                compileCommand.addAll(project.compilerArgs);
                compileCommand.addAll(sourceFiles);

                runCommand(compileCommand);
            });
            withPluginsBefore(Compile.GET);
        }
    }

    public static class Test extends Plugin {
        public static Test GET = new Test();

        static {
            ROOT.pluginsAfter.add(GET);
        }

        public Test() {
            super("test");
            withExecute((project) -> {
                System.out.println("Running tests...");
                List<String> command = new ArrayList<>(Arrays.asList(
                        "java", "-cp",
                        project.classesDir + File.pathSeparator +
                                project.testClassesDir + File.pathSeparator +
                                String.join(File.pathSeparator, getClasspath(project)),
                        "org.junit.platform.console.ConsoleLauncher",
                        "--scan-classpath",
                        "--reports-dir=" + project.buildDir + "/test-results"
                ));

                runCommand(command);
            });
            withPluginsBefore(CompileTest.GET);
        }
    }

    public static class Jar extends Plugin {
        public static Jar GET = new Jar();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    public static class FatJar extends Plugin {
        public static FatJar GET = new FatJar();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    public static class Dependencies extends Plugin {
        public static Dependencies GET = new Dependencies();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    public static class DependencyUpdate extends Plugin {
        public static DependencyUpdate GET = new DependencyUpdate();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    public static class Help extends Plugin {
        public static Help GET = new Help();

        static {
            ROOT.pluginsAfter.add(GET);
        }

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

    // Utility methods
    private static void deleteDirectory(Path path) throws IOException {
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

    private static List<String> getSourceFiles(String directory) throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".java"))
                    .collect(Collectors.toList());
        }
    }

    private static void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode);
        }
    }

    private static void addToJar(JarOutputStream jos, Path sourceDir, String parentPath) throws IOException {
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

    private static void addJarToFatJar(JarOutputStream jos, Path jarPath) throws IOException {
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

    private static List<String> getClasspath(Project project) throws IOException {
        List<String> classpath = new ArrayList<>();
        Path libDir = Paths.get(project.libDir);
        if (Files.exists(libDir)) {
            try (Stream<Path> walk = Files.walk(libDir)) {
                classpath.addAll(walk.filter(file -> file.toString().endsWith(".jar")).map(Path::toString)
                        .collect(Collectors.toList()));
            }
        }
        return classpath;
    }

    private static void downloadDependency(Dependency dep, Path libDir) throws IOException {
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

    public static class ResolveDependencies extends Plugin {
        public static ResolveDependencies GET = new ResolveDependencies();

        static {
            ROOT.pluginsAfter.add(GET);
        }

        public ResolveDependencies() {
            super("resolveDependencies");
            withExecute((project) -> {
                System.out.println("Resolving dependencies...");
                Path libDir = Paths.get(project.libDir);
                Files.createDirectories(libDir);

                for (Dependency dep : project.dependencies) {
                    downloadDependency(dep, libDir);
                }
            });
        }
    }

    public static class Build extends Plugin {
        public static Build GET = new Build();

        static {
            ROOT.pluginsAfter.add(GET);
        }

        public Build() {
            super("build");
            withExecute((project) -> {
                System.out.println("Building project...");
                // This task doesn't need to do anything as it depends on other tasks
            });
            withPluginsBefore(ResolveDependencies.GET, Compile.GET, ProcessResources.GET, CompileTest.GET, Test.GET, Jar.GET);
        }
    }

    //
    // Add third party plugins below
    //

}
