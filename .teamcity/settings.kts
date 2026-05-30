import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.projectFeatures.*

// ============================================================================
// ГЛОБАЛЬНЫЕ НАСТРОЙКИ ПРОЕКТА
// ============================================================================

/*
 * Это основной файл, который TeamCity загружает при включении Versioned Settings.
 * Он определяет:
 * - Версию DSL (должна соответствовать версии TeamCity)
 * - Ссылку на VCS-репозиторий
 * - Корневой проект и его вложенные конфигурации
 */

version = "2026.1.1"  // Версия TeamCity, для которой генерируется конфигурация

project {
    // Связываем проект с текущим репозиторием (автоматически подставляется DslContext)
    vcsRoot(DslContext.settingsRoot)
    
    // Подключаем описание проекта и сборок из отдельных файлов
    subProject(Project)
    
    // Глобальные параметры для всех сборок
    params {
        // Параметры для подключения к Nexus (задаются в UI или через secrets)
        param("env.NEXUS_USER", "%nexus.user%")
        param("env.NEXUS_PASSWORD", "%nexus.password%")
        param("env.NEXUS_URL", "http://nexus:8081")
    }
}