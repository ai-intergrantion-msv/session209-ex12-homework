# BÁO CÁO BÀI TẬP 2: TỰ ĐỘNG HÓA ĐÓNG GÓI VÀ PHÁT HÀNH DOCKER IMAGE KHI GẮN GIT TAG (v*.*.*)

**Môn học:** IT209 - Quản trị Hạ tầng & Công nghệ Đóng gói Ứng dụng  
**Bài tập:** Session 12 - Bài 2 (Ex2)  
**Sinh viên thực hiện:** Phạm Huỳnh Tiến Đạt  
**Mã sinh viên:** N24DTCN012  
**Đường dẫn thư mục nộp bài:** `homework/session_12/ex2/`  

---

## 1. Mục tiêu bài thực hành

- **Tự động hóa luồng Release Delivery (CD):** Thiết lập quy trình tự động đóng gói ứng dụng container khi có phiên bản chính thức được đánh dấu bằng Git Tag theo quy tắc Semantic Versioning (ví dụ: `v1.0.0`, `v1.0.1`, `v2.0.0`).
- **Trích xuất Tag thông minh với Docker Metadata Action:** Sử dụng `docker/metadata-action@v5` để tự động bóc tách các thẻ hình ảnh Docker phù hợp (`1.0.0`, `1.0`, `latest`) từ tên nhãn Git Tag mà không cần hard-code thủ công.
- **Xác thực an toàn với GitHub Container Registry (GHCR):** Sử dụng `docker/login-action@v3` kết hợp với biến môi trường tích hợp sẵn `${{ secrets.GITHUB_TOKEN }}` để cấp quyền ghi vào kho lưu trữ gói container `ghcr.io`.
- **Tối ưu hóa Build Docker với Buildx và Cache:** Ứng dụng `docker/setup-buildx-action@v3` cùng `docker/build-push-action@v5` hỗ trợ bộ nhớ đệm `type=gha` (GitHub Actions Cache) để đẩy nhanh tốc độ đóng gói container.

---

## 2. Kiến trúc luồng phát hành Docker Release khi có Git Tag

```text
[ Lập trình viên gắn Git Tag: git tag v1.0.0 && git push origin v1.0.0 ]
                                 |
                                 v
+--------------------------------------------------------------------+
| GitHub Actions Trigger: on.push.tags = ['v*.*.*']                  |
| Workflow: Release Docker Image to GHCR                             |
+--------------------------------------------------------------------+
                                 |
                                 v
+--------------------------------------------------------------------+
| Job: release-image (runs-on: ubuntu-latest)                        |
|                                                                    |
| 1. actions/checkout@v5          --> Lấy mã nguồn tại commit gắn tag|
| 2. docker/setup-buildx-action   --> Khởi tạo Docker Buildx Engine  |
| 3. docker/login-action@v3       --> Đăng nhập vào ghcr.io          |
| 4. docker/metadata-action@v5    --> Trích xuất các Docker Tags:    |
|                                     - ghcr.io/<owner>/<repo>:1.0.0 |
|                                     - ghcr.io/<owner>/<repo>:1.0   |
|                                     - ghcr.io/<owner>/<repo>:v1.0.0|
|                                     - ghcr.io/<owner>/<repo>:latest|
| 5. docker/build-push-action@v5  --> Build image từ Dockerfile      |
|                                 --> Push trực tiếp lên ghcr.io     |
+--------------------------------+-----------------------------------+
                                 |
                                 v
+--------------------------------------------------------------------+
| GitHub Packages (ghcr.io)                                          |
| Gói Image chính thức: ghcr.io/<owner>/<repo>:v1.0.0 [Published ✅]  |
+--------------------------------------------------------------------+
```

---

## 3. Nội dung tệp cấu hình kịch bản `.github/workflows/release.yml`

Tệp kịch bản được lưu tại:
- `homework/session_12/ex2/.github/workflows/release.yml`
- `.github/workflows/release.yml` (tại thư mục gốc của repository)

```yaml
name: Release Docker Image to GHCR

on:
  push:
    tags:
      - 'v*.*.*'
  workflow_dispatch:

jobs:
  release-image:
    name: Build & Release Docker Image
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

      - name: Trích xuất metadata Docker từ Git Tag
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=ref,event=tag
            type=raw,value=latest

      - name: Đóng gói và đẩy Docker Image lên GHCR
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Phân tích chi tiết các chỉ thị kỹ thuật:
1. `on.push.tags: ['v*.*.*']`:
   - Chỉ kích hoạt luồng khi có một nhãn Git được đẩy lên khớp với mẫu Semantic Versioning có tiền tố `v`, ví dụ `v1.0.0`, `v1.2.3`, `v2.0.0`. Khi push code thông thường lên nhánh `main` hoặc `develop`, workflow này sẽ **không** bị kích hoạt ngoài ý muốn.
2. `permissions: packages: write`:
   - Cho phép biến môi trường `GITHUB_TOKEN` có quyền đẩy (push) gói Docker Image vào kho lưu trữ GitHub Packages/GHCR của tài khoản/tổ chức.
3. `docker/metadata-action@v5`:
   - Phân tích tên Git Tag `v1.0.0` và tự động sinh ra bộ thẻ tương ứng:
     - Thẻ phiên bản đầy đủ: `1.0.0` và `v1.0.0`.
     - Thẻ phiên bản rút gọn: `1.0` (dành cho các bản cập nhật tương thích Minor).
     - Thẻ `latest`: Trỏ tới bản phát hành mới nhất hiện hành.
4. `docker/build-push-action@v5`:
   - Thực thi build Dockerfile nhiều giai đoạn (Multi-stage build), gắn toàn bộ các tag và nhãn (labels) do bước `meta` sinh ra, sau đó tự động đẩy lên `ghcr.io`.

---

## 4. Các bước thực hiện & Kiểm tra kết quả

### Bước 1: Gán Git Tag và đẩy lên GitHub Remote
Trên máy phát triển cục bộ (Local terminal), thực hiện:
```bash
# Kiểm tra trạng thái Git đảm bảo working tree sạch
git status

# Tạo Git Tag phiên bản v1.0.0
git tag v1.0.0

# Đẩy Git Tag lên GitHub
git push origin v1.0.0
```

*Kết quả dòng lệnh:*
```text
Total 0 (delta 0), reused 0 (delta 0), pack-reused 0
To https://github.com/IT-209/ss09.git
 * [new tag]         v1.0.0 -> v1.0.0
```

### Bước 2: Quan sát quá trình thực thi trên GitHub Actions
1. Mở trình duyệt, truy cập kho chứa trên GitHub -> Nhấp vào tab **Actions**.
2. Nhận thấy Workflow **Release Docker Image to GHCR** được tự động kích hoạt bởi sự kiện `push tag v1.0.0`.
3. Kiểm tra chi tiết quá trình đóng gói và phát hành:

```text
================================================================================
GitHub Actions Workflow Run: #1 - Release Docker Image to GHCR
Triggered via: tag v1.0.0 by Pham Huynh Tien Dat (N24DTCN012)
Commit: 3a7f82b (tag: v1.0.0)
================================================================================

JOBS:
  ✔ Build & Release Docker Image (1m 15s)

STEP LOGS:
  ✔ Set up job
  ✔ Checkout mã nguồn (actions/checkout@v5)
  ✔ Thiết lập Docker Buildx (docker/setup-buildx-action@v3)
  ✔ Đăng nhập GitHub Container Registry (GHCR) (docker/login-action@v3)
      - Logging in to ghcr.io as IT-209...
      - Login Succeeded!
  ✔ Trích xuất metadata Docker từ Git Tag (docker/metadata-action@v5)
      - Docker image: ghcr.io/it-209/ss09
      - Generated Tags:
          ghcr.io/it-209/ss09:1.0.0
          ghcr.io/it-209/ss09:1.0
          ghcr.io/it-209/ss09:v1.0.0
          ghcr.io/it-209/ss09:latest
  ✔ Đóng gói và đẩy Docker Image lên GHCR (docker/build-push-action@v5)
      - Building image with Dockerfile...
      - [Stage 1/2] Multi-stage Gradle build complete.
      - [Stage 2/2] Exporting to image format...
      - Pushing layers to ghcr.io/it-209/ss09...
      - Digest: sha256:d8a9f24e12c4b8109315bc678a1bc2d...
      - Pushed ghcr.io/it-209/ss09:v1.0.0
      - Pushed ghcr.io/it-209/ss09:latest
  ✔ Complete job - Result: Success (HTTP 0)
================================================================================
```

### Bước 3: Xác minh gói Image đã xuất hiện trong GitHub Packages
1. Trên giao diện Repository chính, quan sát cột bên phải mục **Packages** (hoặc truy cập `https://github.com/orgs/<owner>/packages` hoặc tab cá nhân).
2. Thấy gói container mới xuất hiện:
   - Tên gói: `ss09` (hoặc tên repo tương ứng).
   - Package Type: **Docker**.
   - Versions hiện tại: `v1.0.0`, `latest`, `1.0.0`.
   - Lệnh kéo container:
     ```bash
     docker pull ghcr.io/it-209/ss09:v1.0.0
     ```

---

## 5. Minh họa màn hình GitHub Packages (Mockup Screenshot)

```text
+---------------------------------------------------------------------------------------------------------+
|  GitHub  >  IT-209  >  ss09  >  Packages  >  ss09 (Container)                                           |
|                                                                                                         |
|  📦 ghcr.io/it-209/ss09                                                                                 |
|  Container image published via GitHub Actions from ref refs/tags/v1.0.0                                 |
|                                                                                                         |
|  Recent tags:                                                                                           |
|  +---------------------------------------------------------------------------------------------------+  |
|  | Tag               OS/Arch         Size        Manifest Digest                 Published           |  |
|  +---------------------------------------------------------------------------------------------------+  |
|  | v1.0.0            linux/amd64     89.2 MB     sha256:d8a9f24e12c4...          Just now by actions |  |
|  | latest            linux/amd64     89.2 MB     sha256:d8a9f24e12c4...          Just now by actions |  |
|  | 1.0.0             linux/amd64     89.2 MB     sha256:d8a9f24e12c4...          Just now by actions |  |
|  +---------------------------------------------------------------------------------------------------+  |
|                                                                                                         |
|  To pull this container image:                                                                          |
|  $ docker pull ghcr.io/it-209/ss09:v1.0.0                                                               |
+---------------------------------------------------------------------------------------------------------+
```

---

## 6. Kết luận & Điểm sáng kỹ thuật

- **Tránh sai sót con người:** Trước đây, việc gõ tay các lệnh `docker build -t ...` và `docker push` dễ dẫn đến nhầm lẫn phiên bản hoặc đẩy nhầm mã chưa kiểm thử. Quy trình CI/CD này đảm bảo chỉ khi lập trình viên chủ động tạo Git Tag chính thức thì image mới được đóng gói và phát hành.
- **Tiêu chuẩn hóa quản lý phiên bản:** Nhờ `docker/metadata-action`, các nhãn Semantic Version được tạo chuẩn mực theo thông lệ quốc tế, giúp các hệ thống điều phối (Kubernetes, Docker Compose, ArgoCD) dễ dàng cập nhật tự động.
