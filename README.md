# 🎓 Домашнее задание: Настройка CI/CD в TeamCity

## 📋 Оглавление

1. 🎯 Описание задания

3. 📁 Структура проекта
4. ⚙️ Требования и зависимости
5. 🚀 Быстрый старт
6. 📦 Этап 1: Подготовка окружения
7. 🌍 Этап 2: Развёртывание инфраструктуры (Terraform)
8. 🐳 Этап 3: Проверка Docker-контейнеров
9. 🔧 Этап 4: Установка Nexus через Ansible
10. ⚙️ Этап 5: Настройка TeamCity
11. 🔄 Этап 6: Настройка сборки и деплоя
12. 🌿 Этап 7: Работа с фича-ветками
13. ✅ Этап 8: Финальная проверка
14. 🔍 Диагностика и частые проблемы
15. 🗑️ Удаление инфраструктуры
16. 📎 Приложение: Полный код проекта

### 🎯 Описание задания
Реализовать CI/CD пайплайн на базе TeamCity с использованием:

|     Компонент    |                             Требования                             |                 Реализация                |
|:----------------:|:------------------------------------------------------------------:|:-----------------------------------------:|
| TeamCity Server  | VM 4CPU/4GB, Docker-образ jetbrains/teamcity-server                | Ubuntu 22.04 + Docker + контейнер         |
| TeamCity Agent   | VM 2CPU/4GB, Docker-образ jetbrains/teamcity-agent, SERVER_URL env | Ubuntu 22.04 + Docker + -e SERVER_URL=... |
| Nexus Repository | VM 2CPU/4GB, установка через Ansible playbook                      | Ubuntu 22.04 + Ansible + Nexus 3.14.0-04  |
| Сборка проекта   | Условная логика: master→deploy, feature/*→test                     | TeamCity UI + Execution conditions        |
| Артефакты        | Публикация .jar в Nexus и в артефакты сборки                       | Maven shade plugin + artifact rules       |

### 📁 Структура проекта

```
example-teamcity/
├── README.md                          # Этот файл — документация решения
├── terraform/
│   ├── providers.tf                   # Провайдеры и настройки
│   ├── variables.tf                   # Все входные переменные
│   ├── main.tf                        # Оркестрация модулей
│   ├── outputs.tf                     # Полезные выводы
│   └── modules/
│       ├── vpc/
│       │   ├── main.tf                # Сеть VPC
│       │   ├── variables.tf           # Переменные модуля VPC
│       │   └── outputs.tf             # Выходы модуля VPC
│       ├── security/
│       │   ├── main.tf                # Security Group
│       │   ├── variables.tf           # Переменные модуля security
│       │   └── outputs.tf             # Выходы модуля security
│       └── vm/
│           ├── main.tf                # Универсальный модуль ВМ
│           ├── variables.tf           # Переменные модуля VM
│           ├── outputs.tf             # Выходы модуля VM
│           └── cloud-init-templates/
│               ├── server.tpl         # TeamCity Server в Docker
│               ├── agent.tpl          # TeamCity Agent в Docker
│               └── generic.tpl        # Базовая ВМ без Docker (для Nexus)
├── app/
│   ├── src/main/java/plaindoll/
│   │   ├── HelloPlayer.java           # Точка входа приложения
│   │   └── Welcomer.java              # Класс с методом для "hunter"
│   ├── src/test/java/plaindoll/
│   │   └── WelcomerTest.java          # Тесты JUnit
│   ├── pom.xml                        # Maven конфигурация с Nexus
│   └── teamcity/
│       └── settings.xml               # Maven credentials для Nexus
├── ansible/
│   ├── ansible.cfg                    # Настройки Ansible (фикс ACL-ошибки)
│   ├── inventory/
│   │   └── cicd/
│   │       ├── hosts.yml              # Инвентарь для Nexus VM
│   │       └── group_vars/nexus.yml   # Переменные Nexus + фикс ACL
│   ├── templates/
│   │   ├── nexus.properties.j2        # Шаблон nexus.properties
│   │   ├── nexus.systemd.j2           # Шаблон systemd-сервиса
│   │   └── nexus.vmoptions.j2         # Шаблон JVM-параметров
│   └── site.yml                       # Playbook установки Nexus
└── .teamcity/                         # Kotlin DSL (для документации)
    ├── settings.kts
    ├── pom.xml
    └── project/
        ├── Project.kt
        └── BuildType.kt
```

### ⚙️ Требования и зависимости

Программное обеспечение:

|    Инструмент    |          Минимальная версия         |                                    Установка                                    |
|:----------------:|:-----------------------------------:|:-------------------------------------------------------------------------------:|
| Terraform        | ≥ 1.6.0                             | terraform.io                                                                    |
| Yandex Cloud CLI | latest                              | curl -sSL https://storage.yandexcloud.net/yandex-cloud-cli/latest/yc.sh \| bash |
| Ansible          | ≥ 2.14                              | pip install ansible или apt install ansible                                     |
| Git              | latest                              | apt install git                                                                 |
| Java             | 8 (для Nexus) / 17 (для разработки) | apt install openjdk-8-jdk-headless                                              |
| Maven            | ≥ 3.9                               | apt install maven                                                               |


### 🚀 Быстрый старт

```bash
# 1️⃣ Клонировать репозиторий
git clone https://github.com/xo4ychill/example-teamcity.git
cd example-teamcity

# 2️⃣ Настроить переменные окружения
export TF_VAR_service_account_key_file="~/.config/yandex-cloud/sa-key.json"
export TF_VAR_cloud_id="b1gxxxxxxxxxxxxxxxx"      # yc config list
export TF_VAR_folder_id="b1gxxxxxxxxxxxxxxxx"     # yc config list
export TF_VAR_ssh_public_key="$(cat ~/.ssh/id_ed25519.pub)"
export TF_VAR_allowed_ssh_cidr="YOUR.IP.ADDRESS/32"  # whatismyip.com

# 3️⃣ Развернуть инфраструктуру
cd terraform/
terraform init
terraform fmt -recursive
terraform validate          # ✅ Success!
terraform plan -out=tfplan
terraform apply tfplan

# 4️⃣ Сохранить полезные выводы
terraform output -raw teamcity_url > ~/tc-server-url.txt
terraform output -raw nexus_vm_external_ip > ~/nexus-ip.txt
terraform output -raw nexus_vm_internal_ip > ~/nexus-internal-ip.txt

# 5️⃣ Запустить Ansible для Nexus
cd ../ansible/
sed -i "s/<nexushost>/$(cat ~/nexus-ip.txt)/" inventory/cicd/hosts.yml
ansible-playbook -i inventory/cicd/hosts.yml site.yml -u yc-user --private-key ~/.ssh/id_ed25519 -v

# 6️⃣ Настроить TeamCity и проверить сборку (см. подробные этапы ниже)
```

### 📦 Этап 1: Подготовка окружения

- Установка инструментов
```bash
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Установка базовых утилит
sudo apt install -y git curl wget unzip jq apt-transport-https ca-certificates

# Установка Terraform
curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform -y

# Установка Yandex Cloud CLI
curl -sSL https://storage.yandexcloud.net/yandex-cloud-cli/latest/yc.sh | bash
source ~/.bashrc  # или ~/.zshrc

# Установка Ansible
pip3 install ansible  # или: sudo apt install ansible

# Установка Java и Maven (для локальной разработки)
sudo apt install -y openjdk-17-jdk-headless maven
```
- Аутентификация в Yandex Cloud
```bash
# Получение токена (если не настроен)
yc iam create-token

# Настройка конфигурации
yc config set token <ВАШ_ТОКЕН>
yc config set cloud-id <CLOUD_ID>
yc config set folder-id <FOLDER_ID>

# Проверка доступа
yc vpc network list --folder-id $TF_VAR_folder_id
```
- Экспорт переменных для Terraform
Создайте файл ~/.teamcity-env для удобства:
```bash
# ~/.teamcity-env
export TF_VAR_service_account_key_file="$HOME/.config/yandex-cloud/sa-key.json"
export TF_VAR_cloud_id="b1gxxxxxxxxxxxxxxxx"
export TF_VAR_folder_id="b1gxxxxxxxxxxxxxxxx"
export TF_VAR_ssh_public_key="$(cat ~/.ssh/id_ed25519.pub)"
export TF_VAR_allowed_ssh_cidr="YOUR.PUBLIC.IP/32"  # Узнайте на whatismyip.com
```
Загрузите переменные:
```bash
source ~/.teamcity-env
```

### 🌍 Этап 2: Развёртывание инфраструктуры (Terraform)
