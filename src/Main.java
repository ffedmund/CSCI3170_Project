// cmd: java -cp .\mysql-connector-j-8.0.32\mysql-connector-j-8.0.32.jar;. Main
import java.time.LocalDate;
import java.util.Scanner;
import java.io.Console;
import java.sql.*;

public class Main {
    // global var
    static boolean connected = false;
    static Connection conn = null;
    static String host, dbname, username, password;
    static final String[][] content = {
        // page 0 (main menu)
        {"===== Welcome to Book Ordering Management System =====",  // Title
        "Database Initialization",  // choice 1
        "Customer Operation",   // choice 2 ...
        "Bookstore Operation",
        "Quit"},

        // page 1 (Database Initialization)
        {"============== Database Initialization ==============",
        "Login to Database and Create Missing Tables",
        "Load from File",
        "Delete All Records",
        "Back to Menu",
        "Quit"},

        // page 2 (Customer Operation)
        {"================= Customer Operation =================",
        "Book Search",
        "Place Order",  // (make order)
        "Check History Orders",
        "Back to Menu",
        "Quit"},

        // page 3 (Bookstore Operation)
        {"================ Bookstore Operation ================",
        "Order Update",
        "Order Query",
        "Check Most Popular Books",
        "Back to Menu",
        "Quit"}
    };
    
    // functions
    static void clrscr(){   // clear screen
        for(int i = 0; i<100; i++)
            System.out.println();
    }
    static void showMenu(int page){
        clrscr();
        
        System.out.println(content[page][0]);
        System.out.println("\n + System Date: " + LocalDate.now());
        System.out.print(" + Database Records: ");
        if(connected){
            // TODO: count record number
            System.out.println("...");
        }else{
            System.out.println("--Fail to Connect to Database--");
        }
        System.out.println("\n----------------------------------------------------\n");
        for(int i = 1; i<8; i++){
            if(i<content[page].length)
                System.out.println("> " + i + ". " + content[page][i]);
            else
                System.out.println();
        }
        System.out.println();
    }

    static void connectDatabase(){
        clrscr();
        Scanner myScanner = new Scanner(System.in);
        
        System.out.println("Loading MySQL JDBC Driver...");
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch(Exception x){
            System.err.println("\nError: Unable to load the driver class!");
            System.err.println("(Press Enter to continue)");
            myScanner.nextLine();
            return;
        }
        System.out.print("Enter your host server address(press Enter for local host): ");
        host = myScanner.nextLine();
        if(host == "")
            host = "localhost";

        System.out.print("Enter your Database Name(press Enter for defalut name: BOOKORDING)(create if not exist): ");
        dbname = myScanner.nextLine();
        if(dbname == "")
            dbname = "BOOKORDING";

        System.out.print("Enter your user name: ");
        username = myScanner.nextLine();
                
        Console console = System.console();
        if (console == null) {
            System.out.print("Enter your password: ");
            password = myScanner.nextLine();
        }else{
            password = new String(console.readPassword("Enter your password(masking): "));
        }

        // finish reading, try connection
        System.out.println("Connecting to MySQL Database server...");
        try{
            conn = DriverManager.getConnection("jdbc:mysql://" + host, username, password);
        }catch(Exception x){
            System.err.println("\nError: Fail to connect to host server / Invalid username and password");
            System.err.println("(Press Enter to continue)");
            myScanner.nextLine();
            return;
        }

        // try "use dbname" to switch database
        try{
            conn.setCatalog(dbname);
        }catch(Exception x){
            try{    // try to create database if not exist
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE DATABASE " + dbname);
                conn.setCatalog(dbname);
            }catch(Exception y){
                System.err.println("\nError: Fail to create database");
                System.err.println("(Press Enter to continue)");
                myScanner.nextLine();
                return;
            }
        }

        //TODO: try create table

        System.out.println("");
        myScanner.nextLine();

        //TODO
    }

    public static void main(String[] args){
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
                            connectDatabase();
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
