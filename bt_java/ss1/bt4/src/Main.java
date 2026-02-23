//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    Scanner sc = new Scanner(System.in);
    System.out.print("Nhập giá sách (USD - kiểu double): ");
    double priceUSD = sc.nextDouble();
    System.out.print("Nhập tỷ giá (float): ");
    float rate = sc.nextFloat();
    double totalVND = priceUSD * rate;
    long roundedVND = (long) totalVND;
    System.out.println("Giá chính xác (số thực): " + totalVND);
    System.out.println("Giá làm tròn để thanh toán (long): " + roundedVND);
    sc.close();

}
