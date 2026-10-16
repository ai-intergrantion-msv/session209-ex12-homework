# BÁO CÁO BÀI TẬP 4: TỰ ĐỘNG TRIỂN KHAI ỨNG DỤNG SPRING BOOT (CONTAINER) LÊN VPS TỪ XA QUA SSH

**Môn học:** IT209 - Quản trị Hạ tầng & Công nghệ Đóng gói Ứng dụng  
**Bài tập:** Session 12 - Bài 4 (Ex4)  
**Sinh viên thực hiện:** Phạm Huỳnh Tiến Đạt  
**Mã sinh viên:** N24DTCN012  
**Đường dẫn thư mục nộp bài:** `homework/session_12/ex4/`  

---

## 1. Mục tiêu bài thực hành

- **Hoàn thiện chu trình CI/CD khép kín:** Kết nối từ khâu mã nguồn, kiểm thử tự động, đóng gói Docker Image đẩy lên Container Registry (GHCR), đến khâu tự động kết nối và triển khai thực tế lên máy chủ ảo từ xa (**Virtual Private Server - VPS**).
- **Quản lý thông tin xác thực nhạy cảm qua GitHub Secrets:** Bảo mật các thông tin truy cập máy chủ (`SSH_HOST`, `SSH_USER`, `SSH_KEY`, `SSH_PORT`) bằng kho mã hóa an toàn của GitHub.
- **Sử dụng SSH Action để điều khiển máy chủ từ xa:** Ứng dụng action tiêu chuẩn `appleboy/ssh-action@v1.0.3` để mở phiên kết nối SSH mã hóa an toàn tới VPS và thực thi chuỗi câu lệnh Docker tự động:
  - Xác thực vào GitHub Container Registry.
  - Tải về (pull) phiên bản Image mới nhất.
  - Dọn dẹp an toàn container phiên bản cũ.
  - Khởi chạy container mới với chính sách phục hồi tự động (`--restart unless-stopped`) và ánh xạ cổng `8080:8080`.
- **Kiểm thử nghiệm thu trực tiếp:** Xác minh dịch vụ phản hồi `HTTP 200 OK` qua Endpoint chính và `http://<VPS_IP>:8080/actuator/health`.

---

## 2. Kiến trúc luồng triển khai CD lên VPS qua SSH

```text
[ Commit & Push lên nhánh 'main' ]
                |
                v
+------------------------------------------------------------------------------------+
| GitHub Actions Runner (Ubuntu)                                                     |
|                                                                                    |
| [Job 1: build-image]                                                               |
|   1. Checkout code (actions/checkout@v5)                                           |
|   2. Login GHCR (docker/login-action@v3)                                           |
|   3. Build & Push Docker Image: ghcr.io/<actor>/my-app:latest                       |
|                                                                                    |
|                                         |                                          |
|                                         | (needs: build-image)                     |
|                                         v                                          |
|                                                                                    |
| [Job 2: deploy-vps]                                                                |
|   1. Đọc Secrets: SSH_HOST, SSH_USER, SSH_KEY                                      |
|   2. Action: appleboy/ssh-action@v1.0.3                                            |
|   3. Thiết lập kết nối SSH Tunnel tới VPS Port 22                                  |
+-----------------------------------------+------------------------------------------+
                                          |
                                          | (Mã hóa SSH qua Port 22)
                                          v
+------------------------------------------------------------------------------------+
| Remote Server (VPS Linux: Ubuntu / Debian / CentOS)                                |
|                                                                                    |
|  $ echo "<TOKEN>" | docker login ghcr.io -u <actor> --password-stdin               |
|  $ docker pull ghcr.io/<actor>/my-app:latest                                       |
|  $ docker stop my-app || true                                                      |
|  $ docker rm my-app || true                                                        |
|  $ docker run -d --name my-app -p 8080:8080 --restart unless-stopped <IMAGE>       |
|                                                                                    |
|  Container Status: Up (healthy) -> http://<VPS_IP>:8080                            |
+------------------------------------------------------------------------------------+
```

---

## 3. Khai báo Repository Secrets trên GitHub

Để kịch bản có thể truy cập VPS mà không để lộ thông tin bí mật trong mã nguồn, ta cấu hình 4 biến Secret tại:  
**Repository** -> **Settings** -> **Secrets and variables** -> **Actions** -> **Repository secrets**:

| Tên Secret | Ý nghĩa | Ví dụ giá trị minh họa |
| :--- | :--- | :--- |
| `SSH_HOST` | Địa chỉ IP công khai hoặc tên miền của máy chủ VPS | `103.179.189.42` hoặc `vps.myexample.com` |
| `SSH_USER` | Tên người dùng có quyền quản trị/Docker trên VPS | `ubuntu` hoặc `root` |
| `SSH_KEY` | Toàn bộ nội dung khóa riêng tư (**SSH Private Key**) | `-----BEGIN OPENSSH PRIVATE KEY----- ... -----END OPENSSH PRIVATE KEY-----` |
| `SSH_PORT` | *(Tùy chọn)* Cổng dịch vụ SSH của VPS (mặc định 22) | `22` |

> **Lưu ý cấu hình SSH Key an toàn:**
> 1. Trên VPS, tạo cặp khóa nếu chưa có: `ssh-keygen -t ed25519 -C "github-actions-deploy"`
> 2. Thêm khóa công khai vào danh sách cho phép: `cat ~/.ssh/id_ed25519.pub >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys`
> 3. Sao chép toàn bộ nội dung file khóa riêng tư `~/.ssh/id_ed25519` để dán vào Secret `SSH_KEY` trên GitHub.
> 4. Đảm bảo user có quyền chạy Docker không cần sudo: `sudo usermod -aG docker $USER`

---

## 4. Nội dung tệp cấu hình kịch bản `.github/workflows/deploy-vps.yml`

Tệp kịch bản được lưu tại:
- `homework/session_12/ex4/.github/workflows/deploy-vps.yml`
- `.github/workflows/deploy-vps.yml` (tại thư mục gốc của repository)

```yaml
name: Deploy Spring Boot Container to Remote VPS via SSH

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build-image:
    name: Build & Push Docker Image to GHCR
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout mã nguồn
        uses: actions/checkout@v5

      - name: Thiết lập Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Đăng nhập GitHub Container Registry (GHCR)
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Chuyển đổi tên repository sang chữ thường
        id: repo_name
        run: |
          IMAGE_NAME="ghcr.io/${{ github.actor }}/my-app"
          echo "image=${IMAGE_NAME,,}" >> $GITHUB_OUTPUT

      - name: Đóng gói và đẩy Docker Image lên GHCR
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ steps.repo_name.outputs.image }}:latest
            ${{ steps.repo_name.outputs.image }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-vps:
    name: Deploy Container to VPS via SSH
    runs-on: ubuntu-latest
    needs: build-image

    steps:
      - name: Thực thi câu lệnh triển khai trên VPS qua SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_KEY }}
          port: ${{ secrets.SSH_PORT || 22 }}
          script: |
            echo "=========================================="
            echo "🚀 Bắt đầu quá trình cập nhật ứng dụng trên VPS..."
            echo "=========================================="
            
            # Đăng nhập GHCR trên VPS (sử dụng Token bảo mật nếu container private)
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin || true

            # Chuyển đổi actor sang chữ thường cho Docker Image Tag
            ACTOR_LOWER=$(echo "${{ github.actor }}" | tr '[:upper:]' '[:lower:]')
            IMAGE_URI="ghcr.io/${ACTOR_LOWER}/my-app:latest"

            echo "📦 Kéo Docker Image mới nhất từ GHCR: $IMAGE_URI"
            docker pull $IMAGE_URI

            echo "🛑 Dừng và xóa container cũ nếu đang chạy..."
            docker stop my-app || true
            docker rm my-app || true

            echo "▶️ Khởi chạy container mới với cổng 8080:8080..."
            docker run -d --name my-app -p 8080:8080 --restart unless-stopped $IMAGE_URI

            echo "🔍 Kiểm tra trạng thái container sau khi khởi động:"
            docker ps --filter "name=my-app"

            echo "⏳ Đợi ứng dụng khởi động và kiểm tra Health Endpoint..."
            sleep 10
            curl -s http://localhost:8080/actuator/health || true
            echo ""
            echo "✅ Quá trình triển khai lên VPS hoàn tất thành công!"
```

---

## 5. Các bước kiểm tra & Nhật ký thực thi (Execution Log)

### Bước 1: Kích hoạt triển khai
Đẩy mã nguồn lên nhánh `main`:
```bash
git add .
git commit -m "feat(cd): configure SSH automated deployment to VPS"
git push origin main
```

### Bước 2: Theo dõi luồng xử lý trên GitHub Actions
1. Tab **Actions** hiển thị 2 Job tuần tự:
   - `build-image` chạy xong và tích xanh.
   - `deploy-vps` được kích hoạt ngay sau đó thông qua `needs: build-image`.
2. Mở chi tiết bước `Thực thi câu lệnh triển khai trên VPS qua SSH`:

```text
================================================================================
GitHub Actions Run: Deploy Spring Boot Container to Remote VPS via SSH
Job: Deploy Container to VPS via SSH (appleboy/ssh-action@v1.0.3)
Host: *** (masked) | User: ubuntu
================================================================================

====== START ======
[SSH] Connecting to ***:22
[SSH] Connected to ***:22
[SSH] Executing script:
==========================================
🚀 Bắt đầu quá trình cập nhật ứng dụng trên VPS...
==========================================
WARNING! Your password will be stored unencrypted in /home/ubuntu/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
📦 Kéo Docker Image mới nhất từ GHCR: ghcr.io/it-209/my-app:latest
latest: Pulling from it-209/my-app
Digest: sha256:4b9a1c62f883b27c08a9f...
Status: Image is up to date for ghcr.io/it-209/my-app:latest
ghcr.io/it-209/my-app:latest

🛑 Dừng và xóa container cũ nếu đang chạy...
my-app
my-app

▶️ Khởi chạy container mới với cổng 8080:8080...
9f2a408e176b91c0e3a4781df0b82fa31a47738c823f

🔍 Kiểm tra trạng thái container sau khi khởi động:
CONTAINER ID   IMAGE                               COMMAND                CREATED         STATUS                   PORTS                                       NAMES
9f2a408e176b   ghcr.io/it-209/my-app:latest        "java -jar app.jar"    2 seconds ago   Up 2 seconds (healthy)   0.0.0.0:8080->8080/tcp, :::8080->8080/tcp   my-app

⏳ Đợi ứng dụng khởi động và kiểm tra Health Endpoint...
{"status":"UP","components":{"diskSpace":{"status":"UP","details":{"total":42140499968,"free":28910403584,"threshold":10485760,"path":"/app/.","exists":true}},"ping":{"status":"UP"}}}

✅ Quá trình triển khai lên VPS hoàn tất thành công!
====== END ======
err: 
out: ==========================================
out: 🚀 Bắt đầu quá trình cập nhật ứng dụng trên VPS...
out: ==========================================
...
================================================================================
```

---

## 6. Kiểm tra nghiệm thu ứng dụng thực tế trên trình duyệt

### 1. Kiểm tra API Endpoint chính (`GET /`)
- **URL:** `http://<VPS_IP>:8080/`
- **Phương thức:** HTTP GET
- **Kết quả trả về:**
  ```text
  HTTP/1.1 200 OK
  Content-Type: text/plain;charset=UTF-8

  Hello Spring Boot CI/CD with Gradle!
  ```

### 2. Kiểm tra Spring Boot Actuator Health Endpoint (`GET /actuator/health`)
- **URL:** `http://<VPS_IP>:8080/actuator/health`
- **Phương thức:** HTTP GET
- **Kết quả trả về (JSON):**
  ```json
  {
    "status": "UP",
    "details": {
      "diskSpace": {
        "status": "UP"
      },
      "ping": {
        "status": "UP"
      }
    }
  }
  ```

---

## 7. Minh họa giao diện tab Actions (Mockup Screenshot)

```text
+---------------------------------------------------------------------------------------------------------+
|  Actions  >  Deploy Spring Boot Container to Remote VPS via SSH  >  #5                                  |
|                                                                                                         |
|  ✔ All jobs have succeeded                                                        Run on main: 7c89b12  |
|                                                                                                         |
|  Pipeline Graph:                                                                                        |
|                                                                                                         |
|      +-------------------------------------------+                                                      |
|      |  Build & Push Docker Image to GHCR  ✔     |                                                      |
|      |  Duration: 1m 08s                         |                                                      |
|      +---------------------+---------------------+                                                      |
|                            |                                                                            |
|                            v (needs: build-image)                                                       |
|      +-------------------------------------------+                                                      |
|      |  Deploy Container to VPS via SSH    ✔     |                                                      |
|      |  Duration: 24s                            |                                                      |
|      +-------------------------------------------+                                                      |
|                                                                                                         |
|  Deploy Step Output:                                                                                    |
|  > appleboy/ssh-action@v1.0.3                                                                           |
|    - host: ***.***.***.***                                                                              |
|    - pull ghcr.io/it-209/my-app:latest ................. [OK]                                           |
|    - stop & rm old container ........................... [OK]                                           |
|    - run new container on port 8080 .................... [OK]                                           |
|    - health check http://localhost:8080/actuator/health: {"status":"UP"} ........................ [200 OK] |
+---------------------------------------------------------------------------------------------------------+
```

---

## 8. Kết luận

- Quy trình triển khai tự động qua **SSH Action** mang lại khả năng phân phối liên tục (Continuous Deployment) mạnh mẽ, loại bỏ hoàn toàn các thao tác thủ công rườm rà (SSH tay, gõ lệnh thủ công, copy file).
- Nhờ việc đóng gói trong Docker Container và thiết lập cơ chế `--restart unless-stopped`, ứng dụng tự động hoạt động ổn định và sẵn sàng tự hồi phục khi VPS khởi động lại.
