import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    static boolean logined = false;
    static final String[][] content = {
        // page 0 (main menu)
        {"Welcome to Book Ordering Management System",  // Title
        "Database Initialization",  // choice 1
        "Customer Operation",   // choice 2 ...
        "Bookstore Operation",
        "Quit"},

        // page 1 (Database Initialization)
        {"         Database Initialization         ",
        "Login to Database and Create Missing Tables",
        "Load from File",
        "Delete All Records",
        "Back to Menu",
        "Quit"},

        // page 2 (Customer Operation)
        {"            Customer Operation            ",
        "Book Search",
        "Place Order",  // (make order)
        "Check History Orders",
        "Back to Menu",
        "Quit"},

        // page 3 (Bookstore Operation)
        {"           Bookstore Operation           ",
        "Order Update",
        "Order Query",
        "Check Most Popular Books",
        "Back to Menu",
        "Quit"}
    };
    static void showMenu(int page){
        // clear screen
        for(int i = 0; i<100; i++)
            System.out.println();
        
        System.out.println("===== " + content[page][0] + " =====");
        System.out.println(" + System Date: " + LocalDate.now());
        System.out.print(" + Database Records: ");
        if(logined){
            // TODO: count record number
            System.out.println("...");
        }else{
            System.out.println("--Please login to the database--");
        }
        System.out.println("----------------------------------------------------");
        for(int i = 1; i<content[page].length; i++){
            System.out.println("> " + i + ". " + content[page][i]);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        int page = 0;
        int error = 0;
        while(page != 4){
            showMenu(page);

            // Read input
            switch(error){
                case 0:
                    System.out.println();
                    break;
                case 1:
                    System.out.println(">>> Input Error. Please Enter an integer.");
                    error = 0;
                    break;
                case 2:
                    System.out.println(">>> Input Error. Please Query from the Menu.");
                    error = 0;
                    break;
            }
            System.out.print(">>> Please Enter Your Query: ");
            Scanner myScanner = new Scanner(System.in);
            int input = -1;
            try{
                input = myScanner.nextInt();
            }catch(Exception e){
                error = 1;
                continue;
            }

            // process user choice: switch page or call function
            switch(page){
                case 0: // page 0 (main menu)
                    switch(input){
                        case 1: page = 1; break;
                        case 2: page = 2; break;
                        case 3: page = 3; break;
                        case 4: page = 4; break;
                        default:
                            error = 2;
                    } 
                    break;
                case 1: // page 1 (Database Initialization)
                    switch(input){
                        case 1: // Login to Database and Create Missing Tables
                            // TODO
                            break;
                        case 2: // Load from File
                            // TODO
                            break;
                        case 3: // Delete All Records
                            // TODO
                            break;
                        case 4: // Back to Menu
                            page = 0; 
                            break;
                        case 5: // Quit
                            page = 4;
                            break;
                        default:
                            error = 2; 
                    }
                    break;
                case 2: // page 2 (Customer Operation)
                    switch(input){
                        case 1: // Book Search
                            // TODO
                            break;
                        case 2: // Place Order
                            // TODO
                            break;
                        case 3: // Check History Orders
                            // TODO
                            break;
                        case 4: // Back to Menu
                            page = 0; 
                            break;
                        case 5: // Quit
                            page = 4;
                            break;
                        default:
                            error = 2; 
                    }
                    break;
                case 3: // page 3 (Bookstore Operation)
                    switch(input){
                        case 1: // Order Update
                            // TODO
                            break;
                        case 2: // Order Query
                            // TODO
                            break;
                        case 3: // Check Most Popular Books
                            // TODO
                            break;
                        case 4: // Back to Menu
                            page = 0; 
                            break;
                        case 5: // Quit
                            page = 4;
                            break;
                        default:
                            error = 2; 
                    }
                    break;

            }

        }
        System.out.println("Bye");
    }
}