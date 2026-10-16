# TỔNG HỢP BÀI TẬP SESSION 12: CI/CD WORKFLOW NÂNG CAO VỚI GITHUB ACTIONS, DOCKER & REMOTE VPS

**Môn học:** IT209 - Quản trị Hạ tầng & Công nghệ Đóng gói Ứng dụng  
**Khóa học:** IT209  
**Sinh viên thực hiện:** Phạm Huỳnh Tiến Đạt  
**Mã sinh viên:** N24DTCN012  
**Thư mục làm việc:** `homework/session_12/`  

---

## 1. Cấu trúc nộp bài Session 12

```text
ss12/
├── .github/
│   └── workflows/
│       ├── ci-matrix.yml                       # Bài 1: Matrix Strategy JDK 17 & 21
│       ├── release.yml                         # Bài 2: Release Docker Image khi gắn Git Tag
│       ├── cd-multi-env.yml                    # Bài 3: Multi-Environment Staging & Production
│       └── deploy-vps.yml                      # Bài 4: Tự động deploy Container lên VPS qua SSH
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/DemoApplication.java
│   │   └── resources/application.properties
│   └── test/
│       └── java/com/example/demo/DemoApplicationTests.java
├── Dockerfile                                  # Multi-stage Dockerfile cho Spring Boot
├── .dockerignore
├── build.gradle                                # Khai báo Spring Boot 3.3.4, Web, Actuator
├── gradlew                                     # Gradle Wrapper cho Linux/macOS
├── gradlew.bat                                 # Gradle Wrapper cho Windows
├── settings.gradle
└── homework/
    └── session_12/
        ├── README.md                           # Tài liệu tổng quan Session 12
        ├── ex1/                                # Bài 1: Matrix Strategy (JDK 17 & 21)
        │   ├── .github/
        │   │   └── workflows/
        │   │       └── ci-matrix.yml
        │   └── README.md
        ├── ex2/                                # Bài 2: Docker Release on Git Tag
        │   ├── .github/
        │   │   └── workflows/
        │   │       └── release.yml
        │   └── README.md
        ├── ex3/                                # Bài 3: GitHub Environments (Staging & Production)
        │   ├── .github/
        │   │   └── workflows/
        │   │       └── cd-multi-env.yml
        │   └── README.md
        └── ex4/                                # Bài 4: Remote VPS Deployment via SSH
            ├── .github/
            │   └── workflows/
            │       └── deploy-vps.yml
            └── README.md
```

---

## 2. Tóm tắt nội dung 4 bài tập thực hành

| Bài tập | Mục tiêu chính | Công nghệ / Action sử dụng | Sản phẩm bàn giao |
| :--- | :--- | :--- | :--- |
| **Bài 1 (Ex1)** | Cấu hình ma trận kiểm thử tự động (*Matrix Strategy*) chạy song song trên JDK 17 và JDK 21, tối ưu tốc độ với Gradle Dependency Caching. | `strategy.matrix`<br>`actions/checkout@v5`<br>`actions/setup-java@v5` | `homework/session_12/ex1/.github/workflows/ci-matrix.yml`<br>`homework/session_12/ex1/README.md` |
| **Bài 2 (Ex2)** | Tự động hóa đóng gói và phát hành Docker Image chính thức lên GitHub Container Registry (GHCR) khi có sự kiện gán nhãn Release (`Git Tag v*.*.*`). | `docker/login-action@v3`<br>`docker/metadata-action@v5`<br>`docker/build-push-action@v5` | `homework/session_12/ex2/.github/workflows/release.yml`<br>`homework/session_12/ex2/README.md` |
| **Bài 3 (Ex3)** | Tách biệt luồng triển khai đa môi trường (*Staging* và *Production*) qua GitHub Environments; thiết lập quy tắc bảo vệ *Required Reviewers* phê duyệt thủ công trước khi đưa lên Production. | `environment: staging`<br>`environment: production`<br>`Environment Protection Rules` | `homework/session_12/ex3/.github/workflows/cd-multi-env.yml`<br>`homework/session_12/ex3/README.md` |
| **Bài 4 (Ex4)** | Tự động kéo Docker Image mới từ GHCR và khởi chạy lại Container Spring Boot trên máy chủ ảo VPS từ xa qua giao thức SSH an toàn. | `appleboy/ssh-action@v1.0.3`<br>`Repository Secrets`<br>`docker run / health check` | `homework/session_12/ex4/.github/workflows/deploy-vps.yml`<br>`homework/session_12/ex4/README.md` |

---

## 3. Sơ đồ tổng thể chu trình CI/CD Session 12

```text
                               +---------------------------------------+
                               |     Spring Boot Developer (Local)     |
                               +-------------------+-------------------+
                                                   |
         +-----------------------------------------+-----------------------------------------+
         |                                         |                                         |
(1) Push to 'main'                        (2) Push Git Tag 'v*.*.*'               (3) Push to 'develop'
         |                                         |                                         |
         v                                         v                                         v
+-----------------------+                 +-----------------------+                 +-----------------------+
| Workflow: ci-matrix   |                 | Workflow: release     |                 | Workflow: cd-multi-env|
|                       |                 |                       |                 |                       |
|  Matrix Strategy:     |                 |  1. Login GHCR        |                 |  Job: deploy-staging  |
|  - Test on Java 17 ✔  |                 |  2. Metadata Action   |                 |  - Environment:       |
|  - Test on Java 21 ✔  |                 |     Tag: :v1.0.0      |                 |    staging            |
|                       |                 |  3. Build & Push      |                 |  - Auto Deployed ✔    |
+-----------+-----------+                 +-----------+-----------+                 +-----------------------+
            |                                         |
            | (sau khi test thành công)               v
            v                             +-----------------------+
+-----------------------+                 | GitHub Packages (GHCR)|
| Workflow: deploy-vps  |                 | ghcr.io/<repo>:v1.0.0 |
|                       |                 +-----------------------+
|  Job 1: build-image   |
|  - Push latest image  |
|                       |
|  Job 2: deploy-vps    |
|  - SSH via Port 22    |
|  - Pull & Run Docker  |
|  - Healthcheck HTTP200|
+-----------+-----------+
            |
            v
+-----------------------+
| Remote Server (VPS)   |
| Container: my-app     |
| Port: 8080 (UP)       |
+-----------------------+
```

---

## 4. Hướng dẫn các câu lệnh kiểm thử và nộp bài

### Bài 1: Kiểm thử Matrix Strategy
```bash
git add .
git commit -m "feat(ex1): configure Java 17 and 21 matrix testing"
git push origin main
```
- Truy cập **Actions** tab -> Kiểm tra workflow `Java Spring Boot CI Matrix Test` phân nhánh 2 job:
  - `Test on Java 17` ✅
  - `Test on Java 21` ✅

### Bài 2: Kiểm thử Release Docker Tag
```bash
git tag v1.0.0
git push origin v1.0.0
```
- Truy cập **Actions** tab -> Workflow `Release Docker Image to GHCR` tự động chạy.
- Truy cập mục **Packages** trên GitHub -> Kiểm tra Docker image `ghcr.io/<owner>/<repo>:v1.0.0` đã được phát hành.

### Bài 3: Kiểm thử GitHub Environments & Manual Approval
1. **Tạo Environments:** Trên GitHub vào `Settings` -> `Environments` -> Tạo `staging` và `production` (bật `Required reviewers` cho `production`).
2. **Kiểm thử Staging:**
   ```bash
   git checkout -b develop
   git push origin develop
   ```
   -> Job `deploy-staging` tự động tích xanh.
3. **Kiểm thử Production Gate:**
   ```bash
   git checkout main
   git merge develop
   git push origin main
   ```
   -> Job `deploy-prod` dừng lại ở trạng thái **Waiting for review** -> Nhấp **Review deployments** -> Nhấn **Approve and deploy** -> Tích xanh thành công.

### Bài 4: Kiểm thử Triển khai VPS qua SSH
1. **Cấu hình Secrets:** Trên GitHub vào `Settings` -> `Secrets and variables` -> `Actions` -> Thêm `SSH_HOST`, `SSH_USER`, `SSH_KEY`.
2. **Kích hoạt:** Đẩy code lên `main` -> Workflow `Deploy Spring Boot Container to Remote VPS via SSH` tự động build và kết nối SSH để chạy container.
3. **Kiểm tra nghiệm thu:**
   - Mở trình duyệt truy cập: `http://<VPS_IP>:8080/`
   - Kiểm tra Health Endpoint: `http://<VPS_IP>:8080/actuator/health` -> Trả về `{"status":"UP"}`.

---

## 5. Tổng kết

Bộ bài tập Session 12 đã xây dựng hoàn chỉnh một hệ thống DevOps Pipeline hiện đại, chuẩn hóa theo các tiêu chuẩn công nghiệp:
1. **Tự động hóa kiểm thử đa phiên bản runtime (Matrix Strategy)** để bảo đảm tương thích hệ sinh thái Java.
2. **Quản lý phát hành tự động (Release Automation)** gắn chặt chẽ với Semantic Versioning Git Tag.
3. **Quản trị an toàn đa môi trường (Deployment Governance)** với hàng rào phê duyệt thủ công nghiêm ngặt trên môi trường Production.
4. **Triển khai ứng dụng container hóa tự động lên hạ tầng đám mây/máy chủ VPS thực tế**, khép kín vòng đời phát triển và vận hành phần mềm (*End-to-End DevOps Lifecycle*).
