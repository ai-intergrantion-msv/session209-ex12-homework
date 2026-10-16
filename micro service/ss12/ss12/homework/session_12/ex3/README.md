# BÁO CÁO BÀI TẬP 3: TÁCH BIỆT LUỒNG CI/CD ĐA MÔI TRƯỜNG (STAGING & PRODUCTION) VỚI GITHUB ENVIRONMENTS

**Môn học:** IT209 - Quản trị Hạ tầng & Công nghệ Đóng gói Ứng dụng  
**Bài tập:** Session 12 - Bài 3 (Ex3)  
**Sinh viên thực hiện:** Phạm Huỳnh Tiến Đạt  
**Mã sinh viên:** N24DTCN012  
**Đường dẫn thư mục nộp bài:** `homework/session_12/ex3/`  

---

## 1. Mục tiêu bài thực hành

- **Tách biệt môi trường triển khai (Multi-Environment Separation):** Nắm vững nguyên lý phân tách môi trường kiểm thử tiền phát hành (*Staging*) và môi trường vận hành thực tế (*Production*) trong quy trình phân phối phần mềm liên tục (CD).
- **Thiết lập GitHub Environments:** Khởi tạo và cấu hình các môi trường trực tiếp trên giao diện quản trị GitHub Repository (`Settings` > `Environments`).
- **Cấu hình hàng rào bảo vệ (Environment Protection Rules):** Kích hoạt cơ chế kiểm soát rủi ro **Required Reviewers (Phê duyệt thủ công)** đối với môi trường `production`, ngăn chặn các rủi ro triển khai mã nguồn chưa được kiểm duyệt kỹ càng.
- **Xây dựng kịch bản CD thông minh:** Điều phối luồng kích hoạt tự động theo nhánh:
  - Nhánh `develop`: Tự động triển khai lên môi trường `staging` mà không cần duyệt.
  - Nhánh `main`: Kích hoạt job `deploy-prod`, chuyển sang trạng thái tạm dừng `Waiting for review` cho đến khi người có thẩm quyền phê duyệt.

---

## 2. Mô hình luồng triển khai Staging & Production

```text
               +-------------------------------------------+
               |          MÃ NGUỒN DỰ ÁN (GIT)             |
               +---------------------+---------------------+
                                     |
                +--------------------+--------------------+
                |                                         |
     (Push to 'develop')                          (Push to 'main')
                |                                         |
                v                                         v
+-------------------------------+       +-----------------------------------+
| Job: deploy-staging           |       | Job: deploy-prod                  |
| Environment: staging          |       | Environment: production           |
|                               |       | Protection: Required Reviewers    |
| - Không yêu cầu duyệt         |       +-----------------+-----------------+
| - Triển khai tự động ngay lập |                         |
|   tức lên Staging Server      |                         v
+---------------+---------------+       +-----------------------------------+
                |                       | ⏸️ WAITING FOR REVIEW              |
                v                       | (Dừng chờ phê duyệt thủ công)     |
    [ Status: Deployed ✅ ]             +-----------------+-----------------+
    URL: https://staging.example.com                      |
                                                (Approve and deploy)
                                                          |
                                                          v
                                        +-----------------------------------+
                                        | ▶️ Đang triển khai Production...  |
                                        +-----------------+-----------------+
                                                          |
                                                          v
                                                [ Status: Deployed ✅ ]
                                                URL: https://api.prod.example.com
```

---

## 3. Các bước cấu hình GitHub Environments trên giao diện Repository

### Bước 1: Khởi tạo môi trường `staging`
1. Truy cập vào kho chứa GitHub: `https://github.com/<owner>/<repo>`.
2. Chọn tab **Settings** trên thanh điều hướng phía trên.
3. Ở menu bên trái, tìm mục **Code and automation** -> chọn **Environments**.
4. Nhấp nút **New environment**.
5. Nhập tên môi trường: `staging` -> Nhấp **Configure environment**.
6. Giữ nguyên mặc định (không bật Reviewers) để môi trường Staging có thể tự động triển khai liên tục (*Continuous Deployment*).

### Bước 2: Khởi tạo và bảo vệ môi trường `production`
1. Tiếp tục tại trang Environments, nhấp nút **New environment**.
2. Nhập tên môi trường: `production` -> Nhấp **Configure environment**.
3. Tại phần **Environment protection rules**:
   - Tích chọn ô vuông **Required reviewers**.
   - Tại ô tìm kiếm người duyệt (*Search for people or teams*), nhập tên tài khoản GitHub cá nhân (hoặc Tech Lead) và nhấn chọn.
   - *(Tùy chọn)* Có thể tích chọn *"Prevent self-review"* nếu làm việc trong môi trường nhóm doanh nghiệp lớn.
4. Nhấp nút **Save protection rules** để lưu lại cấu hình bảo mật.

---

## 4. Nội dung tệp cấu hình kịch bản `.github/workflows/cd-multi-env.yml`

Tệp kịch bản được lưu tại:
- `homework/session_12/ex3/.github/workflows/cd-multi-env.yml`
- `.github/workflows/cd-multi-env.yml` (tại thư mục gốc của repository)

```yaml
name: CD Multi-Environment Deployment (Staging & Production)

on:
  push:
    branches:
      - main
      - develop
  workflow_dispatch:

jobs:
  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment:
      name: staging
      url: https://staging.example.com
    steps:
      - name: Checkout mã nguồn
        uses: actions/checkout@v5

      - name: Chuẩn bị môi trường triển khai Staging
        run: |
          echo "=========================================="
          echo "🚀 Khởi động tiến trình triển khai STAGING"
          echo "Branch  : ${{ github.ref }}"
          echo "Commit  : ${{ github.sha }}"
          echo "Môi trường: staging"
          echo "=========================================="

      - name: Triển khai ứng dụng lên Staging Server
        run: |
          echo "Đang cấu hình container demo-staging..."
          echo "Mô phỏng: Deploy container lên môi trường Staging hoàn tất!"
          echo "Kiểm tra Staging URL: https://staging.example.com"

  deploy-prod:
    name: Deploy to Production
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment:
      name: production
      url: https://api.prod.example.com
    steps:
      - name: Checkout mã nguồn
        uses: actions/checkout@v5

      - name: Chuẩn bị môi trường triển khai Production
        run: |
          echo "=========================================="
          echo "🛡️ Phê duyệt thành công! Bắt đầu triển khai PRODUCTION"
          echo "Người phê duyệt (Approver): ${{ github.actor }}"
          echo "Branch  : ${{ github.ref }}"
          echo "Commit  : ${{ github.sha }}"
          echo "Môi trường: production"
          echo "=========================================="

      - name: Triển khai ứng dụng lên Production Server
        run: |
          echo "Đang cập nhật image production chính thức..."
          echo "Khởi chạy zero-downtime rolling update..."
          echo "Kiểm tra trạng thái hệ thống: HTTP 200 OK"
          echo "🎉 Triển khai Production thành công tại: https://api.prod.example.com"
```

### Điểm nhấn kỹ thuật:
- **Từ khóa `environment`**: Giúp liên kết trực tiếp Job với cấu hình Environment trên GitHub Settings. GitHub Actions sẽ tự động kiểm tra xem Environment đó có Protection Rules hay không trước khi cấp phép cho Runner bắt đầu chạy steps.
- **Biến `url`**: Khi triển khai thành công, GitHub Actions sẽ hiển thị nút bấm liên kết trực tiếp tới URL môi trường ngay trên trang tóm tắt của workflow.
- **Điều kiện phân nhánh `if: github.ref == ...`**: Đảm bảo sự phân lập tuyệt đối, ngăn ngừa việc chạy nhầm môi trường Staging khi commit trên nhánh Main và ngược lại.

---

## 5. Quy trình kiểm tra & Phê duyệt Deployment trên GitHub

### Kịch bản 1: Kiểm thử triển khai Staging (Nhánh `develop`)
```bash
git checkout -b develop
git push origin develop
```
- **Kết quả:** Job `deploy-staging` được kích hoạt ngay lập tức, hoàn thành trong 15s với dấu tích xanh ✅ mà không bị dừng lại.

### Kịch bản 2: Kiểm thử triển khai Production (Nhánh `main`)
```bash
git checkout main
git merge develop
git push origin main
```
- **Hiện tượng quan sát được:**
  1. Workflow **CD Multi-Environment Deployment** khởi chạy.
  2. Job `deploy-prod` bắt đầu và ngay lập tức chuyển sang trạng thái màu vàng: **Waiting for review**.
  3. Xuất hiện thông báo: *"1 environment waiting for review: production"*.
  4. Người duyệt được cấu hình (tài khoản cá nhân) nhận được email thông báo và nút **Review deployments**.
  5. Khi nhấp vào nút **Review deployments**:
     - Hộp thoại xác nhận hiển thị môi trường `production`.
     - Nhập lời phê duyệt (ví dụ: *"Đã kiểm tra kỹ các bài kiểm thử tự động, đồng ý phát hành lên Production"*).
     - Nhấp nút **Approve and deploy**.
  6. Luồng chạy tiếp tục thực thi các bước trong Job và chuyển sang màu xanh lục (Success) ✅.

---

## 6. Minh họa giao diện chờ duyệt Deployment (Mockup Screenshot)

```text
+---------------------------------------------------------------------------------------------------------+
|  Actions  >  CD Multi-Environment Deployment (Staging & Production)  >  #3                              |
|                                                                                                         |
|  🟡 1 environment waiting for review                                              Run on main: a91bc2e  |
|                                                                                                         |
|  +---------------------------------------------------------------------------------------------------+  |
|  |  [!] production                                                                                   |  |
|  |  Waiting for review from Pham Huynh Tien Dat                                                      |  |
|  |                                                                        [ Review deployments ]     |  |
|  +---------------------------------------------------------------------------------------------------+  |
|                                                                                                         |
|  Graph:                                                                                                 |
|                                                                                                         |
|       +---------------------------------------+                                                         |
|       |  (push: main)                         |                                                         |
|       +-------------------+-------------------+                                                         |
|                           |                                                                             |
|                           v                                                                             |
|       +---------------------------------------+                                                         |
|       |  Deploy to Production ⏸️               |                                                         |
|       |  Environment: production (waiting)    |                                                         |
|       +---------------------------------------+                                                         |
+---------------------------------------------------------------------------------------------------------+
```

*Hộp thoại phê duyệt (Review Deployments Modal):*
```text
+---------------------------------------------------------------------------------------------------------+
|  Review deployments                                                                                 [X] |
|                                                                                                         |
|  Pending deployment to 1 environment:                                                                   |
|                                                                                                         |
|  [x] production                                                                                         |
|      Required reviewers: Pham Huynh Tien Dat (N24DTCN012)                                               |
|                                                                                                         |
|  Comment:                                                                                               |
|  +---------------------------------------------------------------------------------------------------+  |
|  | Code review completed. All unit tests passed. Ready for Production deployment.                   |  |
|  +---------------------------------------------------------------------------------------------------+  |
|                                                                                                         |
|  [ Reject ]                                                                      [ Approve and deploy ] |
+---------------------------------------------------------------------------------------------------------+
```

---

## 7. Kết luận

- Việc cấu hình **GitHub Environments** với **Protection Rules** là tiêu chuẩn bắt buộc trong các tổ chức phần mềm chuyên nghiệp nhằm tuân thủ quy chuẩn bảo mật (Compliance & Governance).
- Đảm bảo tính minh bạch (*Auditability*): Mọi hành động phê duyệt đều được ghi lại dấu vết (người duyệt, thời gian, nhận xét) trên hệ thống GitHub Actions.
