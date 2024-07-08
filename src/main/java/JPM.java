import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ThisProject extends JPM {
    public ThisProject() {
        // Override default configurations
        this.srcDir = "src";
        this.testSrcDir = "test";
        this.mainClass = "com.mycompany.MyMainClass";
        this.jarName = "my-project.jar";
        this.fatJarName = "my-project-with-dependencies.jar";
    }

    @Override
    public void start(List<String> args) {
        JPM.add("deploy", () -> { // Register custom task
            deployToServer(); // If it throws an exception the whole build stops
        });
    }

    // Override build method to add custom steps
    @Override
    protected void build() throws IOException, InterruptedException {
        super.build();
        System.out.println("Running additional post-build steps...");
        // Add any additional build steps here
    }

    // Add custom tasks
    protected void deployToServer() throws IOException, InterruptedException {
        build();
        System.out.println("Deploying to server...");
        // Add deployment logic here
    }


}


// 1JPM version 1.0.0 by Osiris-Team
public class JPM {
    public interface Code extends Serializable{
        void run() throws Exception;
    }
    private static final Map<String, List<Code>> commandsAndCode = new ConcurrentHashMap<>();

    public static void add(String command, Code code){
        synchronized (commandsAndCode){
            List<Code> l = commandsAndCode.get(command);
            if(l == null) l = new ArrayList<>();
            l.add(code);
            commandsAndCode.put(command, l);
        }
    }

    public static void put(String command, Code code){
        synchronized (commandsAndCode){
            List<Code> l = new ArrayList<>();
            l.add(code);
            commandsAndCode.put(command, l);
        }
    }

    public static void remove(String command){
        synchronized (commandsAndCode){
            commandsAndCode.remove(command);
        }
    }

    public static void remove(Code code){
        synchronized (commandsAndCode){
            commandsAndCode.forEach((command1, list1) -> {
                list1.remove(code);
            });
        }
    }

    public static void main(String[] args_) throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(args_));
        if (args.isEmpty()) {
            System.out.println("Usage: java YourBuildClass <task>");
            System.out.println("Available tasks: clean, compile, test-compile, test, build, build-fat");
            return;
        }

        ThisProject thisProject = new ThisProject();
        add("clean", thisProject::clean);
        add("compile", thisProject::compile);
        add("test-compile", thisProject::testCompile);
        add("test", thisProject::test);
        add("build", thisProject::build);
        add("build-fat", thisProject::buildFat);

        thisProject.start(args);
        for (String arg : args) {
            thisProject.executeTask(arg);
        }
    }

    protected String srcDir = "src/main/java";
    protected String testSrcDir = "src/test/java";
    protected String buildDir = "build";
    protected String classesDir = buildDir + "/classes";
    protected String testClassesDir = buildDir + "/test-classes";
    protected String jarName = "output.jar";
    protected String fatJarName = "output-fat.jar";
    protected String mainClass = "com.example.Main";
    protected String libDir = "lib";

    protected void start(List<String> args){
    };

    protected void executeTask(String task) throws Exception {
        if(!commandsAndCode.containsKey(task)) {
            System.out.println("Unknown task: " + task);
            return;
        }
        for (String command : commandsAndCode.keySet()) {
            if(task.equals(command))
                for (Code code : commandsAndCode.get(command)) {
                    code.run();
                }
        }
    }

    protected void clean() throws IOException {
        System.out.println("Cleaning build directory...");
        Path buildPath = Paths.get(buildDir);
        if (Files.exists(buildPath)) {
            deleteDirectory(buildPath);
        }
    }

    protected void compile() throws IOException, InterruptedException {
        System.out.println("Compiling Java source files...");
        clean();
        Files.createDirectories(Paths.get(classesDir));

        List<String> sourceFiles = getSourceFiles(srcDir);
        List<String> compileCommand = new ArrayList<>(Arrays.asList(
                "javac", "-d", classesDir
        ));
        compileCommand.addAll(sourceFiles);

        runCommand(compileCommand);
    }

    protected void testCompile() throws IOException, InterruptedException {
        System.out.println("Compiling test source files...");
        compile();
        Files.createDirectories(Paths.get(testClassesDir));

        List<String> sourceFiles = getSourceFiles(testSrcDir);
        List<String> compileCommand = new ArrayList<>(Arrays.asList(
                "javac", "-d", testClassesDir, "-cp", classesDir
        ));
        compileCommand.addAll(sourceFiles);

        runCommand(compileCommand);
    }

    protected void test() throws IOException, InterruptedException {
        System.out.println("Running tests...");
        testCompile();
        // This is a simplified test runner. In a real-world scenario, you'd use a proper test framework like JUnit.
        List<String> command = Arrays.asList(
                "java", "-cp", classesDir + ":" + testClassesDir,
                "org.junit.runner.JUnitCore", "com.example.TestSuite"
        );
        runCommand(command);
    }

    protected void build() throws IOException, InterruptedException {
        System.out.println("Creating JAR file...");
        test();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(jarName)), manifest)) {
            Path classesDirPath = Paths.get(classesDir);
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
    }

    protected void buildFat() throws IOException, InterruptedException {
        System.out.println("Creating fat JAR...");
        build();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(fatJarName)), manifest)) {
            // Add project classes
            addToJar(jos, Paths.get(classesDir));

            // Add dependencies
            Path libDirPath = Paths.get(libDir);
            if (Files.exists(libDirPath)) {
                Files.walk(libDirPath)
                        .filter(path -> path.toString().endsWith(".jar"))
                        .forEach(jar -> addJarToFatJar(jos, jar));
            }
        }
    }

    // Utility methods
    private void deleteDirectory(Path path) throws IOException {
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

    private List<String> getSourceFiles(String directory) throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> f.endsWith(".java"))
                    .collect(Collectors.toList());
        }
    }

    private void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode);
        }
    }

    private void addToJar(JarOutputStream jos, Path sourceDir) throws IOException {
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

    private void addJarToFatJar(JarOutputStream jos, Path jarPath) {
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
