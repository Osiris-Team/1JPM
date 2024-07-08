import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ThisProject extends JPM.Project {
    public ThisProject() {
        // Override default configurations
        this.groupId = "com.mycompany";
        this.artifactId = "my-project";
        this.version = "1.0.0";
        this.mainClass = "com.mycompany.MyMainClass";
        this.jarName = "my-project.jar";
        this.fatJarName = "my-project-with-dependencies.jar";
    }

    @Override
    public void start(List<String> args) {
        JPM.ROOT.pluginsAfter.add(new JPM.Plugin("deploy", (project) -> { // Register custom task
            //deployToServer(project); // If it throws an exception the whole build stops
        }));
        JPM.Build.GET.pluginsAfter.add(new JPM.Plugin("", (project) -> {
            // Run something after/before another task
        }));
    }
}


// 1JPM version 1.0.0 by Osiris-Team
public class JPM {
    public static interface ConsumerWithException<T> extends Serializable{
        void accept(T t) throws Exception;
    }
    public static class Plugin{
        public String id;
        public ConsumerWithException<Project> onExecute;
        public List<Plugin> pluginsBefore = new CopyOnWriteArrayList<>();
        public List<Plugin> pluginsAfter = new CopyOnWriteArrayList<>();

        public Plugin(String id, ConsumerWithException<Project> onExecute) {
            this.id = id;
            this.onExecute = onExecute;
        }

        public Plugin withPluginsBefore(Plugin... l){
            withPluginsBefore(Arrays.asList(l));
            return this;
        }

        public Plugin withPluginsBefore(List<Plugin> l){
            this.pluginsBefore = l;
            return this;
        }

        public Plugin withPluginsAfter(Plugin... l){
            withPluginsAfter(Arrays.asList(l));
            return this;
        }

        public Plugin withPluginsAfter(List<Plugin> l){
            this.pluginsAfter = l;
            return this;
        }

        public void execute(Project project) throws Exception{
            for (Plugin plugin : pluginsBefore) {
                plugin.execute(project);
            }
            onExecute.accept(project);
            for (Plugin plugin : pluginsAfter) {
                plugin.execute(project);
            }
        }
    }
    public static class Project{
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

        public void start(List<String> args){
        };

        public void executeRootTask(String task) throws Exception {
            for (Plugin plugin : ROOT.pluginsAfter) {
                if(plugin.id.equals("task")){
                    plugin.execute(this);
                    return;
                }
            }
            System.out.println("Unknown task: " + task);
        }
    }

    public static final Plugin ROOT = new Plugin("root", project -> {});

    public static void main(String[] args_) throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(args_));
        if (args.isEmpty()) {
            System.out.println("Usage: java YourBuildClass <task>");
            System.out.println("Available tasks: clean, compile, test-compile, test, build, build-fat");
            return;
        }

        ThisProject thisProject = new ThisProject();
        thisProject.start(args);
        for (String arg : args) {
            thisProject.executeRootTask(arg);
        }
    }

    public static class Clean extends Plugin{
        public static Clean GET = new Clean();
        static{
            ROOT.pluginsAfter.add(GET);
        }

        public Clean() {
            super("clean", (project) -> {
                System.out.println("Cleaning build directory...");
                Path buildPath = Paths.get(project.buildDir);
                if (Files.exists(buildPath)) {
                    deleteDirectory(buildPath);
                }
            });
        }
    }

    public static class Compile extends Plugin{
        public static Compile GET = new Compile();
        static{
            ROOT.pluginsAfter.add(GET);
        }

        public Compile() {
            super("compile", (project) -> {
                System.out.println("Compiling Java source files...");
                Files.createDirectories(Paths.get(project.classesDir));

                List<String> sourceFiles = getSourceFiles(project.srcDir);
                List<String> compileCommand = new ArrayList<>(Arrays.asList(
                        "javac", "-d", project.classesDir
                ));
                compileCommand.addAll(sourceFiles);

                runCommand(compileCommand);
            });
            withPluginsBefore(Clean.GET);
        }
    }

    public static class TestCompile extends Plugin{
        public static TestCompile GET = new TestCompile();
        static{
            ROOT.pluginsAfter.add(GET);
        }

        public TestCompile() {
            super("test-compile", (project) -> {
                System.out.println("Compiling test source files...");
                Files.createDirectories(Paths.get(project.testClassesDir));

                List<String> sourceFiles = getSourceFiles(project.testSrcDir);
                List<String> compileCommand = new ArrayList<>(Arrays.asList(
                        "javac", "-d", project.testClassesDir, "-cp", project.classesDir
                ));
                compileCommand.addAll(sourceFiles);

                runCommand(compileCommand);
            });
            withPluginsBefore(Compile.GET);
        }
    }

    public static class Test extends Plugin{
        public static Test GET = new Test();
        static{
            ROOT.pluginsAfter.add(GET);
        }

        public Test() {
            super("test", (project) -> {
                System.out.println("Running tests...");
                // This is a simplified test runner. In a real-world scenario, you'd use a proper test framework like JUnit.
                List<String> command = Arrays.asList(
                        "java", "-cp", project.classesDir + ":" + project.testClassesDir,
                        "org.junit.runner.JUnitCore", "com.example.TestSuite"
                );
                runCommand(command);
            });
            withPluginsBefore(TestCompile.GET);
        }
    }

    public static class Build extends Plugin{
        public static Build GET = new Build();
        static{
            ROOT.pluginsAfter.add(GET);
        }

        public Build() {
            super("build", (project) -> {
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
            withPluginsBefore(Test.GET);
        }
    }

    public static class BuildFat extends Plugin{
        public static BuildFat GET = new BuildFat();
        static{
            ROOT.pluginsAfter.add(GET);
        }

        public BuildFat() {
            super("build-fat", (project) -> {
                System.out.println("Creating fat JAR...");
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, project.mainClass);

                try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(project.fatJarName)), manifest)) {
                    // Add project classes
                    addToJar(jos, Paths.get(project.classesDir));

                    // Add dependencies
                    Path libDirPath = Paths.get(project.libDir);
                    if (Files.exists(libDirPath)) {
                        Files.walk(libDirPath)
                                .filter(path -> path.toString().endsWith(".jar"))
                                .forEach(jar -> addJarToFatJar(jos, jar));
                    }
                }
            });
            withPluginsBefore(Build.GET);
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

    private static void addToJar(JarOutputStream jos, Path sourceDir) throws IOException {
        Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                        jos.putNextEntry(new JarEntry(entryName));
                        Files.copy(file, jos);
                        jos.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void addJarToFatJar(JarOutputStream jos, Path jarPath) {
        try (java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(Files.newInputStream(jarPath))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (!entry.getName().startsWith("META-INF")) {
                    jos.putNextEntry(entry);
                    jis.transferTo(jos);
                    jos.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
