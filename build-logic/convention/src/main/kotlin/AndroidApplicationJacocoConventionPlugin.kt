import mega.privacy.android.gradle.extension.MegaJacocoPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

/**
 * Plugin to apply Jacoco configuration to Android application projects.
 */
class AndroidApplicationJacocoConventionPlugin : Plugin<Project> {

    private var userExcludedFiles: Set<String> = emptySet()
    private var userIncludedFiles: Set<String> = emptySet()
    private var excludedFiles: List<String> = emptyList()

    /**
     * Apply the Jacoco configuration to the project.
     *
     * @param target
     */
    override fun apply(target: Project) {
        println("AndroidApplicationJacocoConventionPlugin applied to project ${target.name}")
        with(target) {
            pluginManager.apply("jacoco")

            extensions.create("mega-jacoco", MegaJacocoPluginExtension::class.java)
            extensions.getByType<MegaJacocoPluginExtension>().defaultExcludedFiles =
                defaultExcludedFiles

            afterEvaluate {
                extensions.getByType<MegaJacocoPluginExtension>().let {
                    userExcludedFiles = it.excludedFiles
                    userIncludedFiles = it.includedFiles
                    excludedFiles = mergeExcludedFiles()
                }
            }

            val jacocoVersion = "0.8.11"

            dependencies {
                "jacocoAnt"("org.jacoco:org.jacoco.ant:$jacocoVersion:nodeps")
            }

            extensions.configure<JacocoPluginExtension> {
                toolVersion = jacocoVersion
            }

            tasks.register("instrumentClasses") {
                dependsOn("compileGmsDebugSources")
                val outputDir =
                    "${layout.buildDirectory.get()}/intermediates/classes-instrumented/gms/debug/"

                doLast {
                    println("Instrumenting classes")
                    println("Excluded files: ${excludedFiles.size}")
                    val excludesPattern = excludedFiles.joinToString()
                    val jacocoAntConfig = configurations.getByName("jacocoAnt")
                    ant.withGroovyBuilder {
                        "taskdef"(
                            "name" to "instrument",
                            "classname" to "org.jacoco.ant.InstrumentTask",
                            "classpath" to jacocoAntConfig.asPath
                        )
                        "instrument"("destdir" to outputDir) {
                            "fileset"(
                                "dir" to "${layout.buildDirectory.get()}/intermediates/javac/gmsDebug/compileGmsDebugJavaWithJavac/classes",
                                "excludes" to excludesPattern
                            )
                            "fileset"(
                                "dir" to "${layout.buildDirectory.get()}/tmp/kotlin-classes/gmsDebug",
                                "excludes" to excludesPattern
                            )
                        }
                    }
                    /* Add the instrumented classes to the beginning of classpath */
                    tasks.named("testGmsDebugUnitTest") {
                        if (hasProperty("classpath")) {
                            setProperty(
                                "classpath",
                                files(outputDir) + property("classpath") as FileCollection
                            )
                        }
                    }
                }
            }

            tasks.register("createUnitTestCoverageReport") {
                dependsOn("instrumentClasses", "testGmsDebugUnitTest")
                val jacocoAntConfig = configurations.getByName("jacocoAnt")
                val buildDirPath = layout.buildDirectory.get()
                doLast {
                    ant.withGroovyBuilder {
                        "taskdef"(
                            "name" to "report",
                            "classname" to "org.jacoco.ant.ReportTask",
                            "classpath" to jacocoAntConfig.asPath
                        )
                        "report" {
                            "executiondata" {
                                ant.withGroovyBuilder {
                                    "file"("file" to "${buildDirPath}/jacoco/testGmsDebugUnitTest.exec")
                                }
                            }
                            "structure"("name" to "Coverage") {
                                "classfiles" {
                                    "fileset"("dir" to "${buildDirPath}/intermediates/javac/gmsDebug/compileGmsDebugJavaWithJavac/classes")
                                    "fileset"("dir" to "${buildDirPath}/tmp/kotlin-classes/gmsDebug")
                                }
                                "sourcefiles" {
                                    "fileset"("dir" to "src/main/java")
                                    "fileset"("dir" to "src/test/java")
                                }
                            }
                            "html"("destdir" to "${buildDirPath}/coverage-report/html")
                            "csv"("destfile" to "${buildDirPath}/coverage-report/coverage.csv")
                            "xml"("destfile" to "${buildDirPath}/coverage-report/coverage.xml")
                        }
                    }
                }
            }
        }
    }

    private fun mergeExcludedFiles(): List<String> {
        val result: MutableSet<String> = defaultExcludedFiles.toMutableSet()
        result += userExcludedFiles
        result -= userIncludedFiles
        return result.toList()
    }

    private val defaultExcludedFiles = setOf(
        // data binding
        "android/databinding/**/*.class",
        "**/android/databinding/*Binding.class",
        "**/android/databinding/*",
        "**/androidx/databinding/*",
        "**/BR.*",
        // android
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        // dagger
        "**/*_MembersInjector.class",
        "**/Dagger*Component.class",
        "**/Dagger*Component\$Builder.class",
        "**/Dagger*Subcomponent*.class",
        "**/*Subcomponent\$Builder.class",
        "**/*Module_*Factory.class",
        "**/di/module/*",
        "**/*_Factory*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        // kotlin
        "**/*MapperImpl*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/BuildConfig.*",
        "**/*Component*.*",
        "**/*BR*.*",
        "**/Manifest*.*",
        "**/*\$Lambda$*.*",
//                "**/*Companion*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        "**/*MembersInjector*.*",
        "**/*_MembersInjector.class",
        "**/*_Factory*.*",
        "**/*_Provide*Factory*.*",
//                "**/*Extensions*.*",
        // sealed and data classes
        "**/*$Result.*",
        "**/*$Result$*.*",
        // adapters generated by moshi
        "**/*JsonAdapter.*",
        //entity in domain layer
        "**/domain/entity/*",
        // model in data layer
        "**/data/model/*",
    )

}