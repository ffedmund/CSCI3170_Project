// cmd: java -cp .\mysql-connector-j-8.0.32\mysql-connector-j-8.0.32.jar;. Main
import java.time.LocalDate;
import java.util.Scanner;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.Console;
import java.sql.*;

public class Main {
    // global var
    static boolean connected = false;
    static Connection conn = null;
    static final String[] tableName = {"Book", "Customer", "Ordering"};
    static final HashMap<String, String> tableStruct = new HashMap<String, String>();
    static {
        tableStruct.put("Book", "(ISBN CHAR(13) not NULL, " +   //TODO: more detail?
                                    " Title VARCHAR(100), " + 
                                    " Authors VARCHAR(50), " + 
                                    " Price INTEGER, " + 
                                    " InventoryQuantity INTEGER, " +
                                    " PRIMARY KEY (ISBN))");
        tableStruct.put("Customer", "(UID CHAR(10) not NULL, " +   //TODO: more detail?
                                        " Name VARCHAR(50), " + 
                                        " Address VARCHAR(200), " + 
                                        " PRIMARY KEY (UID))");
        tableStruct.put("Ordering", "(OID CHAR(8) not NULL, " +   //TODO: more detail?
                                        " UID CHAR(10) not NULL, " +
                                        " OrderISBN CHAR(13) NOT NULL, " + 
                                        " OrderDate DATE, " + 
                                        " OrderQuantity INTEGER, " + 
                                        " ShippingStatus VARCHAR(8), " + 
                                        " PRIMARY KEY (OID, OrderISBN), " + 
                                        " FOREIGN KEY (UID) REFERENCES Customer(UID), " + 
                                        " FOREIGN KEY (OrderISBN) REFERENCES Book(ISBN))");
    }
    static final String[][] content = {
        // page 0 (main menu)
        {"===== Welcome to Book Ordering Management System =====",  // Title
        "Database Initialization",  // choice 1
        "Customer Operation",   // choice 2 ...
        "Bookstore Operation",
        "Quit the System"},

        // page 1 (Database Initialization)
        {"============== Database Initialization ==============",
        "Connect to Database and Create Missing Tables",
        "Load from File",
        "Delete All Records",
        "Back to Menu",
        "Quit the System"},

        // page 2 (Customer Operation)
        {"================= Customer Operation =================",
        "Book Search",
        "Place Order",  // (make order)
        "Check History Orders",
        "Back to Menu",
        "Quit the System"},

        // page 3 (Bookstore Operation)
        {"================ Bookstore Operation ================",
        "Order Update",
        "Order Query",
        "Check Most Popular Books",
        "Back to Menu",
        "Quit the System"}
    };
    
    // functions
    static void clrscr(){   // clear screen
        for(int i = 0; i<100; i++)
            System.out.println();
    }
    static void showMessage(String s){
        System.err.println(s);
        System.err.println("(Press Enter to continue)");
        Scanner myScanner = new Scanner(System.in);
        myScanner.nextLine();
    }
    static void showMenu(int page){
        clrscr();
        
        System.out.println(content[page][0]);
        System.out.println("\n + System Date: " + LocalDate.now());
        System.out.print(" + Database Records: ");
        if(connected){
            for(int i = 0; i<3; i++){
                int count = -1;
                try{
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName[i]);
                    rs.next();
                    count = rs.getInt(1);
                }catch(Exception x){
                    System.err.print("Error:'" + tableName[i] + "'");
                }
                if(count != -1){
                    switch(i){
                        case 0:
                            System.out.print("Books (");
                            break;
                        case 1:
                            System.out.print("Customers (");
                            break;
                        case 2:
                            System.out.print("Orders (");
                            break;
                    }
                    System.out.print(count + ")" + (i<2?", ":" "));
                }
            }
            System.out.println();
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
        String host, dbname, username, password;
        
        System.out.println("Loading MySQL JDBC Driver...");
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch(Exception x){
            showMessage("\nError: Unable to load the driver class!");
            return;
        }
        System.out.print("Enter your host server address(press Enter for local host): ");
        host = myScanner.nextLine();
        if(host.equals(""))
            host = "localhost";

        System.out.print("Enter your Database Name(press Enter for defalut name: BOOKORDING)(create if not exist): ");
        dbname = myScanner.nextLine();
        if(dbname.equals(""))
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
            showMessage("\nError: Fail to connect to host server / Invalid username and password");
            return;
        }

        // try to switch database
        try{
            conn.setCatalog(dbname);
        }catch(Exception x){
            try{    // try to create database if not exist
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE DATABASE " + dbname);
                conn.setCatalog(dbname);
            }catch(Exception y){
                showMessage("\nError: Fail to create database");
                return;
            }
        }

        String newName;
        boolean newNameOK;
        int failcount = 0;
        do{
            newNameOK = true;
            newName = "temp";
            for(int i = 0; i<40; i++)
                newName += (char)('A' + Math.random() * 26);
            try{
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE " + newName + " (c char)");
                stmt.executeUpdate("DROP TABLE " + newName);
            }catch(Exception x){
                newNameOK = false;
                failcount++;
            }   
            if(failcount > 1000000){
                showMessage("\nError: Please try another database");
                return;
            }
        }while(!newNameOK);
        
        // check if the table exist, exist->Exception x->check if the schema correct, not exist->create table book
        for(String tname: tableName){
            try{
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE " + tname + " " + tableStruct.get(tname));
            }catch(Exception x){    // table exist: check schema
                try{
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate("CREATE TABLE " + newName + " " + tableStruct.get(tname));
                    Statement stmt2 = conn.createStatement();
                    /* TODO: reference?
                    * Comparison Query modify from:
                    * https://dba.stackexchange.com/questions/75532/query-to-compare-the-structure-of-two-tables-in-mysql
                    */
                    ResultSet rs = stmt2.executeQuery("SELECT COUNT(1) FROM " + 
                                                        "(SELECT column_name,ordinal_position,data_type,column_type,COUNT(1) rowcount " +
                                                        "FROM information_schema.columns " + 
                                                        "WHERE table_schema=DATABASE() AND table_name IN ('" + tname + "','" + newName + "') " +
                                                        "GROUP BY column_name,ordinal_position,data_type,column_type "+
                                                        "HAVING COUNT(1)=1) A");
                    stmt.executeUpdate("DROP TABLE " + newName);
                    rs.next();
                    if(rs.getInt(1)>0)
                        throw new Exception();
                }catch(Exception y){
                    System.err.println("\nError: Existing table not match \nPlease try another database or delete the existing table '" + tname + "'");
                    return;
                }
            }
        }
        connected = true;
    }

    // try{
    //     Statement stmt = conn.createStatement();
    //     ResultSet rs = stmt.executeQuery("SELECT * FROM Book");
    //     int[] colW = {4,7,4,2};
    //     showRs(rs, "tututu title test", colW);
    // }catch(Exception e){
    //     e.printStackTrace();
    //     System.err.println(e);
    // }
    /* colW e.g.:{6, 5, 3}
     * +------+-----+---+
     * quit with 0:normal, 1:error, 2:back to menu, 3:quit program */
    static int showRs(ResultSet rs, String tiltle, int[] colW){
        try{
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            ArrayList<String> colName = new ArrayList<String>();
            List<ArrayList<String>> buffer = new ArrayList<ArrayList<String>>();
            for (int i = 1; i <= columnsNumber; i++) {
                colName.add(rsmd.getColumnLabel(i));
            }
            buffer.add(colName);
            while (rs.next()) {
                ArrayList<String> record = new ArrayList<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    record.add(rs.getString(i));
                }
                buffer.add(record);
            }
            
            if(colW.length != columnsNumber){
                colW = new int[colName.size()];
                for (int i = 0; i < colName.size(); i++) {
                    colW[i] = colName.get(i).length();
                    for (ArrayList<String> row : buffer) {
                        if (row.get(i).length() > colW[i]) {
                            colW[i] = row.get(i).length();
                        }
                    }
                }
                int sum = 0;
                for(int w: colW)
                    sum += w;
                int min = 5;
                while(sum+columnsNumber+1>80){
                    boolean sumChanged = false;
                    for(int i=0; i<columnsNumber; i++)
                        if(colW[i]>min){
                            colW[i]--;
                            sumChanged = true;
                        }
                    if(!sumChanged){
                        min--;
                        if(min==0){
                            for(int i=0; i<columnsNumber; i++)
                                colW[i] = 2;
                            break;
                        }
                    }
                    sum = 0;
                    for(int w: colW)
                        sum += w;
                }
            }

            int page = 1;
            final int numberOfOnePage = 10;
            int maxPage = 1;
            while(maxPage*numberOfOnePage < buffer.size()-1)
                maxPage++;
            int input = 0;

            while(input != 4){
                Scanner myScanner = new Scanner(System.in);
                clrscr();
                System.out.printf("%" + ((80-tiltle.length())/2>0?(80-tiltle.length())/2:0) + "s%s\n", "", tiltle);
                
                if (buffer.size()==1){//ResultSet is empty
                    System.out.println("\n" + " ".repeat(29) + "---No results found---\n");
                    showMessage("\n");
                    return 0;
                }

                // print upper +-------+--------+...
                for(int w : colW)
                    System.out.print("+" + "-".repeat(w));
                System.out.println("+");

                HashMap<Integer, String> remainingString = new HashMap<Integer, String>();
                for(int r=0; r<=numberOfOnePage; r++){
                    ArrayList<String> row;
                    if(r==0){
                        row = buffer.get(0);
                    }else{
                        if((page-1)*numberOfOnePage + r >= buffer.size())
                            break;
                        row = buffer.get((page-1)*numberOfOnePage + r);
                    }
                    for(int i=0; i<columnsNumber; i++){
                        String tmpS = row.get(i);
                        if(tmpS==null)
                            tmpS = "";
                        if(tmpS.length()>colW[i]){
                            char[] sp = {' ', ',', '-'};
                            int[] pos = new int[sp.length];
                            for(int j=0; j<sp.length; j++)
                                pos[j] = tmpS.lastIndexOf(sp[j], colW[i]-1);
                            int maxIndex = -1;
                            for (int p : pos) {
                                if (p > maxIndex && p < colW[i]) {
                                    maxIndex = p;
                                }
                            }
                            if(maxIndex == -1)
                                maxIndex = colW[i];
                            else
                                maxIndex++;
                            remainingString.put(i, tmpS.substring(maxIndex));
                            tmpS = tmpS.substring(0, maxIndex);
                        }
                        System.out.print("|" + tmpS + " ".repeat(colW[i]-tmpS.length()));
                    }
                    System.out.println("|");
                    while(!remainingString.isEmpty()){
                        for(int i=0; i<columnsNumber; i++){
                            if(!remainingString.containsKey(i)){
                                System.out.print("|" + " ".repeat(colW[i]));
                                continue;
                            }
                            String tmpS = remainingString.get(i);
                            remainingString.remove(i);
                            if(tmpS.length()>colW[i]){
                                char[] sp = {' ', ',', '-'};
                                int[] pos = new int[sp.length];
                                for(int j=0; j<sp.length; j++)
                                    pos[j] = tmpS.lastIndexOf(sp[j], colW[i]-1);
                                int maxIndex = -1;
                                for (int p : pos) {
                                    if (p > maxIndex && p < colW[i]) {
                                        maxIndex = p;
                                    }
                                }
                                if(maxIndex == -1)
                                    maxIndex = colW[i];
                                else
                                    maxIndex++;
                                remainingString.put(i, tmpS.substring(maxIndex));
                                tmpS = tmpS.substring(0, maxIndex);
                            }
                            System.out.print("|" + tmpS + " ".repeat(colW[i]-tmpS.length()));
                        }
                        System.out.println("|");
                    }
                    if(r==0){   // print middle +-------+--------+...
                        for(int w : colW)
                            System.out.print("+" + "-".repeat(w));
                        System.out.println("+");
                    }
                }
                // print bottom +-------+--------+...
                for(int w : colW)
                    System.out.print("+" + "-".repeat(w));
                System.out.println("+\n");

                String pageS = "Page " + page + "/" + maxPage;
                System.out.print(page>1?"1. Previous Page  ":" ".repeat(18)); 
                System.out.print(" ".repeat(22-(pageS.length()+1)/2>0?22-(pageS.length()+1)/2:0));
                System.out.print(pageS + " ".repeat(20-pageS.length()/2>0?20-pageS.length()/2:0));
                System.out.println(page<maxPage?"  2. Next Page":" ");
                System.out.println("3. Finish" + " ".repeat(23) + "4. Back to Menu" + " ".repeat(15) + "5. Quit the System\n");
                
                switch(input){
                    case 0:
                        System.out.print(">>> Please Enter Your Query: ");
                        break;
                    case 2:
                        System.out.print(">>> Input Error. Please Query from above Options: ");
                        input = 0;
                        break;
                    case -1:
                        System.out.print(">>> Input Error. Please Enter an integer Query: ");
                        input = 0;
                        break;
                }
                try{
                    input = myScanner.nextInt();
                }catch(Exception e){
                    input = -1;
                    continue;
                }
                switch(input){
                    case 1:
                        input = page>1 ? 0 : 2;
                        if(page>1)
                            page--;
                        break;
                    case 2:
                        input = page<maxPage ? 0 : 2;
                        if(page<maxPage)
                            page++;
                        break;
                    case 3:
                        break;
                    case 4:
                        return 2;
                    case 5:
                        return 3;
                    default:
                        input = 2;
                }
            }
        }catch(Exception x){
            x.printStackTrace();
            System.err.println(x);
            Scanner myScanner = new Scanner(System.in);
            myScanner.nextLine();
            return 1;
        }
        return 0;
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
            int input = -1;
            Scanner myScanner = new Scanner(System.in);
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
                            connectDatabase();
                            break;
                        case 2: // Load from File
                            // TODO
                            break;
                        case 3: // Delete All Records
                            clrscr();
                            if(!connected){
                                clrscr();
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            System.out.println("Caution: Deleting ALL records at Books, Customers, and Orders");
                            System.out.println("This action cannot be recovered.");
                            System.out.print("Proceed? (Enter y|Y for Yes, any others for No): ");
                            Scanner myScanner2 = new Scanner(System.in);
                            String in = myScanner2.nextLine();
                            if(in.equals("Y") || in.equals("y")){
                                try{
                                    Statement stmt = conn.createStatement();
                                    for(int i = tableName.length-1; i>=0; i--)
                                        stmt.executeUpdate("DROP TABLE " + tableName[i]);
                                    for(String tname: tableName){
                                        stmt.executeUpdate("CREATE TABLE " + tname + " " + tableStruct.get(tname));
                                    }
                                    showMessage("\nAll records has been deleted");
                                }catch(Exception x){
                                    showMessage("\nError while deleting records\n--- Disconnected to database ---");
                                    connected = false;
                                }
                            }
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
                            clrscr();
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            System.out.println("Book Searching\n");
                            System.out.println("Please enter the ISBN, Book Title and Author Name:");
                            System.out.println("(Leave it empty to not specify)");
                            Scanner myScanner2 = new Scanner(System.in);
                            String isbn, title, aname;
                            System.out.print("ISBN: ");
                            isbn = myScanner2.nextLine();
                            System.out.print("Book Title: ");
                            title = myScanner2.nextLine();
                            System.out.print("Author Name: ");
                            aname = myScanner2.nextLine();
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT Book.ISBN, Book.Title AS 'Book Title', Book.Authors AS 'Author', Book.Price, Book.InventoryQuantity AS 'Stock' " + 
                                    "FROM Book " + 
                                    "WHERE ISBN LIKE '%" + isbn + "%' AND Title LIKE '%" + title + "%' AND Authors LIKE '%" + aname + "%'");
                                int[] colW = {13, 31, 20, 5, 5};
                                int r = showRs(rs, "Book Search", colW);
                                if(r==2)
                                    page = 0;
                                if(r==3)
                                    page = 4; 
                            }catch(Exception e){
                                e.printStackTrace();
                                System.err.println(e);
                            }
                            break;
                        case 2: // Place Order
                            // TODO
                            break;
                        case 3: // Check History Orders
                            // TODO
                            clrscr();
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            System.out.println("Checking History Orders\n");
                            String uid;
                            Scanner myScanner3 = new Scanner(System.in);
                            System.out.print("Please enter your UID:");
                            uid = myScanner3.nextLine();
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT Ordering.oid AS 'OID', Ordering.OrderDate AS 'Date', Ordering.OrderISBN AS 'ISBN', Ordering.OrderQuantity AS 'Quantity', Ordering.ShippingStatus AS 'Status' " + 
                                    "FROM Ordering " + 
                                    "WHERE uid = '" + uid + "' " + 
                                    "ORDER BY Ordering.oid DESC;");
                                int[] colW = {10, 12, 15, 10, 10};
                                int r = showRs(rs, "History Orders for UID: " + uid, colW);
                                if(r==2)
                                    page = 0;
                                if(r==3)
                                    page = 4; 
                            }catch(Exception e){
                                e.printStackTrace();
                                System.err.println(e);
                            }
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
                            clrscr();
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            System.out.println("Order Querying\n");
                            System.out.println("Orders in shipping status:");
                            System.out.println("> 1. ordered");
                            System.out.println("> 2. shipped");
                            System.out.println("> 3. received\n");
                            Scanner myScanner2 = new Scanner(System.in);
                            System.out.print(">>> Please Enter Your Query: ");
                            int input2 = -1;
                            String status;
                            try{
                                input2 = myScanner2.nextInt();
                                switch(input2){
                                    case 1:
                                        status = "ordered";
                                        break;
                                    case 2:
                                        status = "shipped";
                                        break;
                                    case 3:
                                        status = "received";
                                        break;
                                    default:
                                        throw new Exception();
                                }
                            }catch(Exception e){
                                showMessage("\nError: Unknown input received");
                                continue;
                            }
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT Ordering.oid AS 'OID', Ordering.OrderDate AS 'Date', Ordering.UID, Ordering.OrderISBN AS 'ISBN', Ordering.OrderQuantity AS 'Quantity' " + 
                                    "FROM Ordering " +  
                                    "WHERE Ordering.ShippingStatus = '" + status + "' " +
                                    "ORDER BY Ordering.oid ASC;");
                                int[] colW = {10, 12, 12, 15, 10};
                                int r = showRs(rs, "Orders in status: " + status, colW);
                                if(r==2)
                                    page = 0;
                                if(r==3)
                                    page = 4; 
                            }catch(Exception e){
                                e.printStackTrace();
                                System.err.println(e);
                            }
                            break;
                        case 3: // Check Most Popular Books
                            clrscr();
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT DENSE_RANK() OVER (ORDER BY SUM(Ordering.OrderQuantity) DESC) AS 'Rank', Book.InventoryQuantity AS 'Inventory', " + 
                                    "Book.ISBN, Book.Title AS 'Book Title', SUM(Ordering.OrderQuantity) AS 'Total Orders', Book.Price " + 
                                    "FROM Book " + 
                                    "INNER JOIN Ordering ON Book.ISBN = Ordering.OrderISBN " + 
                                    "GROUP BY Book.ISBN " + 
                                    "ORDER BY 'Total Orders' DESC;");
                                int[] colW = {4, 9, 13, 30, 12, 5};
                                int r = showRs(rs, "Most Popular Books", colW);
                                if(r==2)
                                    page = 0;
                                if(r==3)
                                    page = 4; 
                            }catch(Exception e){
                                e.printStackTrace();
                                System.err.println(e);
                            }
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
