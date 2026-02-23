//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    String book1 = "Java Basic";
    String book2 = "Python Intro";
    String temp = book1;
    book1 = book2;
    book2 = temp;

    System.out.println("Book1: " + book1);
    System.out.println("Book2: " + book2);

}
