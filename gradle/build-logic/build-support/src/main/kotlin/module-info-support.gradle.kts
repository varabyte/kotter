import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

// Extract "--patch-module" args into its own class to be configuration cache friendly
class PatchModuleArgProvider(
    @Input val moduleName: String,
    @InputFiles @PathSensitive(PathSensitivity.RELATIVE) val kotlinClasses: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments() = listOf(
        "--patch-module",
        "$moduleName=${kotlinClasses.asPath}"
    )
}

fun Project.extractModuleName(): String? {
    val moduleInfoFile = file("src/jvmMain/java/module-info.java")
    if (!moduleInfoFile.exists()) return null

    val moduleRegex = Regex("""module\s+([a-zA-Z0-9._]+)\s*\{""")
    val match = moduleRegex.find(moduleInfoFile.readText())

    return match?.groupValues?.get(1)
}

// Adapted from:
// https://kotlinlang.org/docs/gradle-configure-project.html#configure-with-java-modules-jpms-enabled
tasks.withType<JavaCompile>().configureEach {
    val moduleName = project.extractModuleName() ?: return@configureEach
    val kotlinCompileTask = tasks.named<KotlinCompile>("compileKotlinJvm")
    options.compilerArgumentProviders.add(
        PatchModuleArgProvider(
            moduleName,
            project.files(kotlinCompileTask.flatMap { it.destinationDirectory })
        )
    )
}

abstract class VerifyModuleTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    abstract val jvmVersion: Property<String>

    @get:Input
    abstract val moduleName: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modulePath: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        execOperations.exec {
            commandLine(
                "jdeps",
                "--module", moduleName.get(),
                "--multi-release", jvmVersion.get(),
                "--module-path", modulePath.asPath,
                "-summary",
                "-verbose"
            )
        }
    }
}
// In a convention plugin, you can inject services directly into the task registration
val execOps = project.serviceOf<ExecOperations>()

tasks.register<VerifyModuleTask>("verifyModuleInfo") {
    val jvmJarTask = tasks.named<Jar>("jvmJar")
    val jarFile = jvmJarTask.flatMap { it.archiveFile }
    modulePath.from(configurations.named("jvmRuntimeClasspath"))
    modulePath.from(jarFile)
    moduleName.set(project.provider { project.extractModuleName() ?: "unnamed" })

    val javaExtension = project.extensions.getByType<JavaPluginExtension>()
    jvmVersion.set(javaExtension.targetCompatibility.majorVersion)
}
