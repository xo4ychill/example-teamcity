import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Build : BuildType({

    name = "Maven Build"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {

        /*
         * Для master выполняем deploy
         */
        maven {
            name = "Deploy"

            goals = "clean deploy"

            runnerArgs = "-s settings.xml"

            executionMode = BuildStep.ExecutionMode.RUN_ON_SUCCESS

            conditions {
                equals(
                    "teamcity.build.branch",
                    "refs/heads/master"
                )
            }
        }

        /*
         * Для остальных веток выполняем тесты
         */
        maven {
            name = "Tests"

            goals = "clean test"

            conditions {
                doesNotEqual(
                    "teamcity.build.branch",
                    "refs/heads/master"
                )
            }
        }
    }

    triggers {
        vcs {
        }
    }

    artifactRules = """
        target/*.jar
    """.trimIndent()
})
