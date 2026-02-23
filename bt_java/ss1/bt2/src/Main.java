//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    Scanner sc = new Scanner(System.in);
    System.out.print("Nhập số ngày chậm trễ (n): ");
    int n = sc.nextInt();

    System.out.print("Nhập số lượng sách mượn (m): ");
    int m = sc.nextInt();
    double total = n * m * 5000;
    if (n > 7 && m >= 3) {
        total = total * 1.2; // tăng thêm 20%
    }
    System.out.println("Tổng tiền phạt: " + total + " VND");
    boolean requireCardLock = total > 50000;
    System.out.println("Yêu cầu khóa thẻ: " + requireCardLock);

    sc.close();

}

