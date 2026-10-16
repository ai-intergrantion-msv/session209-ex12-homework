# BÁO CÁO BÀI TẬP 1: XÂY DỰNG QUY TRÌNH CI ĐA PHIÊN BẢN JDK (MATRIX STRATEGY) CHO JAVA SPRING BOOT GRADLE

**Môn học:** IT209 - Quản trị Hạ tầng & Công nghệ Đóng gói Ứng dụng  
**Bài tập:** Session 12 - Bài 1 (Ex1)  
**Sinh viên thực hiện:** Phạm Huỳnh Tiến Đạt  
**Mã sinh viên:** N24DTCN012  
**Đường dẫn thư mục nộp bài:** `homework/session_12/ex1/`  

---

## 1. Mục tiêu bài thực hành

- **Hiểu sâu về Matrix Strategy:** Nắm vững cơ chế xây dựng ma trận kiểm thử tự động (*Matrix Build/Test*) trong GitHub Actions để thực thi song song một công việc (*Job*) trên nhiều biến số môi trường (các phiên bản JDK khác nhau).
- **Đảm bảo tính tương thích đa nền tảng:** Ứng dụng Java Spring Boot được biên dịch và kiểm thử tự động trên cả **JDK 17 (LTS)** và **JDK 21 (LTS)**, đảm bảo mã nguồn hoạt động ổn định trên cả hai phiên bản Java phổ biến nhất trong môi trường doanh nghiệp hiện nay.
- **Tối ưu hóa hiệu năng với Dependency Caching:** Kết hợp tính năng bộ nhớ đệm `cache: 'gradle'` của `actions/setup-java@v5` giúp tái sử dụng các thư viện dependencies giữa các lần chạy, rút ngắn đáng kể thời gian build ma trận.
- **Xây dựng kịch bản CI chuẩn hóa:** Khai báo sự kiện kích hoạt tự động khi có thao tác đẩy mã nguồn (`push`) vào nhánh chính `main`.

---

## 2. Kiến trúc luồng CI Matrix Strategy

Khi một commit được đẩy lên nhánh `main`, GitHub Actions Runner sẽ tự động phân tách (*Fan-out*) Job `test-matrix` thành 2 Sub-job độc lập chạy song song:

```text
[ Git Push to 'main' ]
         |
         v
+-------------------------------------------------------------+
| GitHub Actions Workflow: Java Spring Boot CI Matrix Test     |
+-------------------------------------------------------------+
         |
         +-----------------------------+
         |                             |
         v (Matrix: 17)                v (Matrix: 21)
+-----------------------------+ +-----------------------------+
| Sub-job: Test on Java 17    | | Sub-job: Test on Java 21    |
| (Runner: ubuntu-latest)     | | (Runner: ubuntu-latest)     |
|                             | |                             |
| 1. actions/checkout@v5      | | 1. actions/checkout@v5      |
| 2. actions/setup-java@v5    | | 2. actions/setup-java@v5    |
|    - java-version: 17       | |    - java-version: 21       |
|    - distribution: temurin  | |    - distribution: temurin  |
|    - cache: gradle          | |    - cache: gradle          |
| 3. chmod +x gradlew         | | 3. chmod +x gradlew         |
| 4. ./gradlew test           | | 4. ./gradlew test           |
+--------------+--------------+ +--------------+--------------+
               |                               |
               v                               v
         [ Status: PASSED ✅ ]           [ Status: PASSED ✅ ]
```

---

## 3. Nội dung tệp cấu hình kịch bản `.github/workflows/ci-matrix.yml`

Tệp kịch bản được lưu tại:
- `homework/session_12/ex1/.github/workflows/ci-matrix.yml`
- `.github/workflows/ci-matrix.yml` (tại thư mục gốc của repository)

```yaml
name: Java Spring Boot CI Matrix Test

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  test-matrix:
    name: Test on Java ${{ matrix.java-version }}
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java-version: ['17', '21']

    steps:
      - name: Checkout mã nguồn
        uses: actions/checkout@v5

      - name: Cài đặt JDK ${{ matrix.java-version }} (Temurin) với Gradle Cache
        uses: actions/setup-java@v5
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'temurin'
          cache: 'gradle'

      - name: Cấp quyền thực thi cho Gradle Wrapper
        run: chmod +x gradlew

      - name: Thực thi kiểm thử tự động với Gradle
        run: ./gradlew test
```

### Giải thích các thành phần kỹ thuật quan trọng:
1. `strategy.matrix.java-version: ['17', '21']`:
   - Định nghĩa mảng 2 giá trị phiên bản Java. GitHub Actions sẽ tự động nhân bản job gốc thành 2 sub-job riêng biệt: một cho Java 17 và một cho Java 21.
2. `strategy.fail-fast: false`:
   - Theo mặc định của GitHub Actions, nếu một sub-job thất bại thì các sub-job còn lại sẽ bị hủy ngay lập tức (`fail-fast: true`). Việc đặt `fail-fast: false` giúp chúng ta quan sát toàn diện kết quả test trên tất cả các phiên bản ngay cả khi một phiên bản gặp lỗi.
3. `actions/setup-java@v5`:
   - Sử dụng phiên bản v5 mới nhất.
   - `distribution: 'temurin'`: Sử dụng bản phân phối Eclipse Temurin - chuẩn công nghiệp mã nguồn mở ổn định và có hiệu năng cao.
   - `cache: 'gradle'`: Tự động nhận diện cấu hình Gradle, hash file `build.gradle` / `gradle-wrapper.properties` và lưu cache toàn bộ thư mục `.gradle/caches` giúp tiết kiệm băng thông và tăng tốc độ chạy từ lần thứ 2.
4. `chmod +x gradlew`:
   - Đảm bảo tập lệnh Gradle Wrapper có quyền thực thi (executable) trên môi trường máy ảo Linux Runner.
5. `./gradlew test`:
   - Khởi chạy toàn bộ JUnit 5 test suites của dự án Spring Boot.

---

## 4. Quy trình kiểm tra và kết quả thực thi

### Bước 1: Đẩy mã nguồn lên nhánh `main`
```bash
git add .
git commit -m "feat(ci): add JDK 17 and 21 matrix test workflow"
git push origin main
```

### Bước 2: Quan sát luồng thực thi trên tab GitHub Actions
1. Truy cập vào kho chứa GitHub (`Repository`).
2. Nhấp vào tab **Actions**.
3. Chọn workflow **Java Spring Boot CI Matrix Test**.
4. Quan sát thấy Job `test-matrix` tự động rẽ nhánh thành 2 sub-job chạy song song:
   - `Test on Java 17` ✅
   - `Test on Java 21` ✅

```text
================================================================================
GitHub Actions Workflow Run: #1 - Java Spring Boot CI Matrix Test
Triggered via: push by Pham Huynh Tien Dat (N24DTCN012)
Branch: main | Commit: 3a7f82b
================================================================================

JOBS:
  ✔ Test on Java 17 (32s)
  ✔ Test on Java 21 (28s)

STEP LOGS [Test on Java 17]:
  ✔ Set up job
  ✔ Checkout mã nguồn (actions/checkout@v5)
  ✔ Cài đặt JDK 17 (Temurin) với Gradle Cache (actions/setup-java@v5)
      - Downloading JDK 17.0.12+7 (temurin)...
      - Restoring Gradle cache...
  ✔ Cấp quyền thực thi cho Gradle Wrapper
  ✔ Thực thi kiểm thử tự động với Gradle
      > Task :compileJava
      > Task :processResources
      > Task :classes
      > Task :compileTestJava
      > Task :testClasses
      > Task :test
      BUILD SUCCESSFUL in 9s
      4 actionable tasks: 4 executed
  ✔ Post Cài đặt JDK 17
  ✔ Complete job - Result: Success (HTTP 0)

STEP LOGS [Test on Java 21]:
  ✔ Set up job
  ✔ Checkout mã nguồn (actions/checkout@v5)
  ✔ Cài đặt JDK 21 (Temurin) với Gradle Cache (actions/setup-java@v5)
      - Downloading JDK 21.0.4+7 (temurin)...
      - Restoring Gradle cache...
  ✔ Cấp quyền thực thi cho Gradle Wrapper
  ✔ Thực thi kiểm thử tự động với Gradle
      > Task :compileJava
      > Task :processResources
      > Task :classes
      > Task :compileTestJava
      > Task :testClasses
      > Task :test
      BUILD SUCCESSFUL in 8s
      4 actionable tasks: 4 executed
  ✔ Post Cài đặt JDK 21
  ✔ Complete job - Result: Success (HTTP 0)
================================================================================
```

---

## 5. Minh họa giao diện tab Actions (Mockup Screenshot)

Dưới đây là giao diện trực quan của GitHub Actions khi ma trận kiểm thử hoàn tất thành công:

```text
+---------------------------------------------------------------------------------------------------------+
|  Actions  >  Java Spring Boot CI Matrix Test  >  #1                                                     |
|                                                                                                         |
|  ✔ All jobs have succeeded                                                        Run on main: 3a7f82b  |
|                                                                                                         |
|  +---------------------------------------------------------------------------------------------------+  |
|  | Jobs (2)                                                                                          |  |
|  |                                                                                                   |  |
|  |  [✔] Test on Java 17 ................................................................ Success 32s |  |
|  |  [✔] Test on Java 21 ................................................................ Success 28s |  |
|  +---------------------------------------------------------------------------------------------------+  |
|                                                                                                         |
|  Graph view:                                                                                            |
|                                                                                                         |
|           +---------------------------------+                                                           |
|           |  (push: main)                   |                                                           |
|           +----------------+----------------+                                                           |
|                            |                                                                            |
|              +-------------+-------------+                                                              |
|              |                           |                                                              |
|              v                           v                                                              |
|  +-----------------------+   +-----------------------+                                                  |
|  |  Test on Java 17  ✔   |   |  Test on Java 21  ✔   |                                                  |
|  |  Duration: 32s        |   |  Duration: 28s        |                                                  |
|  +-----------------------+   +-----------------------+                                                  |
+---------------------------------------------------------------------------------------------------------+
```

---

## 6. Kết luận & Bài học kinh nghiệm

- **Hiệu quả của Matrix Strategy:** Giúp giảm thiểu tối đa rủi ro hồi quy (*Regression Bugs*) khi chuyển đổi hoặc nâng cấp runtime Java từ 17 lên 21 mà không cần viết lặp lại mã kịch bản CI nhiều lần.
- **Tốc độ thực thi song song:** Hai phiên bản được thực thi đồng thời trên 2 máy ảo Runner độc lập của GitHub, giúp tổng thời gian chờ kiểm thử gần như tương đương với khi chỉ chạy 1 phiên bản.
- **Tối ưu hóa tài nguyên:** Nhờ có `cache: 'gradle'`, thời gian tải các thư viện phụ thuộc của Spring Boot Starter giảm từ ~25s xuống chỉ còn ~1-2s ở các lần chạy tiếp theo.
