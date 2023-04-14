// TODO delete: cmd: java -cp .\mysql-connector-j-8.0.32\mysql-connector-j-8.0.32.jar;. Main
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.Console;
import java.sql.*;
import java.io.File;
import java.io.FileNotFoundException;

public class Main {
    // global var
    static boolean connected = false;
    static Connection conn = null;
    static String newName;  // temp table name for testing
    static boolean newNameOK;
    static final String[] tableName = {"Book", "Customer", "Ordering"};
    static final HashMap<String, String> tableStruct = new HashMap<String, String>();
    static {
        tableStruct.put("Book", "(ISBN CHAR(13) NOT NULL CHECK(ISBN REGEXP '^[0-9\\-]+$' AND ISBN LIKE '_-____-____-_'), " +
                                    " Title VARCHAR(100) NOT NULL CHECK(Title NOT REGEXP '[%_]'), " + 
                                    " Authors VARCHAR(50) NOT NULL CHECK(Authors NOT REGEXP '[%_]'), " + 
                                    " Price INTEGER UNSIGNED NOT NULL, " + 
                                    " InventoryQuantity INTEGER UNSIGNED NOT NULL, " +
                                    " PRIMARY KEY (ISBN))");
        tableStruct.put("Customer", "(UID CHAR(10) NOT NULL, " +   
                                        " Name VARCHAR(50) NOT NULL CHECK(Name NOT REGEXP '[%_]'), " + 
                                        " Address VARCHAR(200) NOT NULL CHECK(Address NOT REGEXP '[%_]'), " + 
                                        " PRIMARY KEY (UID))");
        tableStruct.put("Ordering", "(OID CHAR(8) NOT NULL CHECK(OID REGEXP '^[0-9]+$' AND OID LIKE '________'), " + 
                                        " UID CHAR(10) NOT NULL, " +
                                        " OrderISBN CHAR(13) NOT NULL, " + 
                                        " OrderDate DATE NOT NULL, " + 
                                        " OrderQuantity INTEGER UNSIGNED NOT NULL, " + 
                                        " ShippingStatus VARCHAR(8) NOT NULL CHECK(ShippingStatus IN ('ordered','shipped','received')), " + 
                                        " PRIMARY KEY (OID, OrderISBN), " + 
                                        " FOREIGN KEY (UID) REFERENCES Customer(UID), " + 
                                        " FOREIGN KEY (OrderISBN) REFERENCES Book(ISBN))");
    }
    static final String[][] content = {
        // page 0 (main menu)
        {"================== Welcome to Book Ordering Management System ==================",  // Title
        "Database Initialization",  // choice 1
        "Customer Operation",   // choice 2 ...
        "Bookstore Operation",
        "Quit the System"},

        // page 1
        {"=========================== Database Initialization ============================",
        "Connect to Database and Create Missing Tables",
        "Load from File",
        "Delete All Records",
        "Back to Menu",
        "Quit the System"},

        // page 2
        {"============================== Customer Operation ==============================",
        "Book Search",
        "Place Order",  // (make order)
        "Check History Orders",
        "Back to Menu",
        "Quit the System"},

        // page 3
        {"============================== Bookstore Operation =============================",
        "Order Update",
        "Order Query",
        "Check Most Popular Books",
        "Back to Menu",
        "Quit the System"}
    };
    // (end of global var)
    
    // functions

    /**
     * clear screen
     * (by printing 100 '\n')
     */
    static void clrscr(){   // 
        for(int i = 0; i<100; i++)
            System.out.println();
    }

    /**
     * print message s and ask user to press enter
     * to ensure message are shown before flushed away
     */
    static void showMessage(String s){
        System.out.println(s);
        System.out.println("(Press Enter to continue)");
        Scanner myScanner = new Scanner(System.in);
        myScanner.nextLine();
    }

    /**
     * show the specific page
     */
    static void showMenu(int page){
        clrscr();
        
        // print header (title, date, database info.)
        System.out.println(content[page][0]);
        System.out.println("\n + System Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // TODO: show currect time?
        System.out.print(" + Database Records: ");
        if(!connected){
            System.out.println("--Fail to Connect to Database--");
        }else{
            for(int i = 0; i<tableName.length; i++){
                int count = -1;
                try{
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName[i]);
                    rs.next();
                    count = rs.getInt(1);
                }catch(Exception x){
                    System.out.print("Error: '" + tableName[i] + "' ");
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
                    System.out.print(count + ")" + (i<tableName.length-1 ? ", " : " "));
                }
            }
            System.out.println();
        }
        System.out.println("\n" + "-".repeat(80) + "\n");

        // print options
        for(int i = 1; i<8; i++){
            if(i<content[page].length)
                System.out.println("> " + i + ". " + content[page][i]);
            else
                System.out.println();
        }
        System.out.println();
    }

    /**
     * try to ask user for information and connect to database
     * modifying global var: conn, connected
     */
    static void connectDatabase(){
        clrscr();
        Scanner myScanner = new Scanner(System.in);
        Connection oldConn = conn;  // save the original connection
        String host, dbname, username, password;
        
        // return if fail to load the driver
        System.out.println("Loading MySQL JDBC Driver...");
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch(Exception x){
            showMessage("\nError: Unable to load the driver class!");
            return;
        }

        // ask user to input connection information
        System.out.print("Enter your host server address(press Enter for local host): ");
        host = myScanner.nextLine();
        if(host.equals("")) // default value of host if nothing entered
            host = "localhost";

        System.out.print("Enter your Database Name(press Enter for defalut name: BOOKORDERING)(create if not exist): ");
        dbname = myScanner.nextLine();
        if(dbname.equals(""))   // default value of dbname if nothing entered
            dbname = "BOOKORDERING";

        System.out.print("Enter your user name: ");
        username = myScanner.nextLine();
        
        Console console = System.console(); // try to mask password if possible
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
            conn = oldConn; // roll back the change
            showMessage("\nError: Fail to connect to host server / Invalid username and password");
            return;
        }

        // try to switch database
        try{    // try switching, got exception if database not exist
            conn.setCatalog(dbname);
        }catch(Exception x){
            try{    // try to create database as not exist
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE DATABASE " + dbname);
                conn.setCatalog(dbname);
            }catch(Exception y){
                conn = oldConn; // roll back the change
                showMessage("\nError: Fail to create database");
                return;
            }
        }

        // in order to check the schema of the existing table,
        // a new table name is randomly generated for comparison
        int failcount = 0;  // count the times of generated a repeated name
        while(!newNameOK){  // keep try generating until new name ok
            newNameOK = true;
            newName = "temp";
            for(int i = 0; i<40; i++)   // avoid by adding random char
                newName += (char)('A' + Math.random() * 26);
            
            try{    // try to create a table from newName, got exception if the table exist originally
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE " + newName + " (c char)");
                stmt.executeUpdate("DROP TABLE " + newName);
            }catch(Exception x){    // the name got repeated
                conn = oldConn; // roll back the change
                newNameOK = false;
                failcount++;
            }

            if(failcount > 1000000){    // stop generation (and give up) if the name always exist in the database
                conn = oldConn; // roll back the change
                showMessage("\nError: Please try another database");
                return;
            }
        }
        
        // check if the table exist, exist->check if the schema correct, not exist->create table
        for(String tname: tableName){
            try{    // try to create table in the table name, got exception if existed
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE " + tname + " " + tableStruct.get(tname));
            }catch(Exception x){    // table exist -> check schema
                try{
                    Statement stmt = conn.createStatement();    // create a table with correct schema
                    stmt.executeUpdate("CREATE TABLE " + newName + " " + tableStruct.get(tname));

                    /**
                     * TODO: add reference?
                     * Comparison Query modify from:
                     * https://dba.stackexchange.com/questions/75532/query-to-compare-the-structure-of-two-tables-in-mysql
                     */
                    Statement stmt2 = conn.createStatement();
                    ResultSet rs = stmt2.executeQuery("SELECT COUNT(1) FROM " + 
                                                        "(SELECT column_name,ordinal_position,data_type,column_type,COUNT(1) rowcount " +
                                                        "FROM information_schema.columns " + 
                                                        "WHERE table_schema=DATABASE() AND table_name IN ('" + tname + "','" + newName + "') " +
                                                        "GROUP BY column_name,ordinal_position,data_type,column_type "+
                                                        "HAVING COUNT(1)=1) A");
                    stmt.executeUpdate("DROP TABLE " + newName);

                    rs.next();
                    if(rs.getInt(1)>0)  // if the existing table do not match with the schema we want, throw exception
                        throw new Exception();
                }catch(Exception y){    // connection fail due to (the database exist a table with different schema) OR (any other exception got)
                    conn = oldConn; // roll back the change
                    System.out.println("\nError: Existing table not match \nPlease try another database or delete the existing table '" + tname + "'");
                    return;
                }
            }
        }
        connected = true;
    }

    // // sample use of the funciton:
    // try{
    //     Statement stmt = conn.createStatement();
    //     ResultSet rs = stmt.executeQuery("SELECT * FROM Book");
    //     int[] colW = {};
    //     showRs(rs, "book test", colW);
    // }catch(Exception e){
    //     e.printStackTrace();
    //     showMessage(e);
    // }
    /**
     * show the resultset rs in a formatted way
     * quit with 0:normal, 1:error, 2:back to menu, 3:quit program
     * parameter: 
     *      rs: the resultset to be shown, 
     *      tiltle: title that will be shown on top of rs,
     *      colW: the width of each column (auto gen if col # not match)
     * colW e.g.:{6, 5, 2}
     * +------+-----+--+
     * |test  |abcde|12|
     * |      |fg   |  |
     * +------+-----+--+
     */
    static int showRs(ResultSet rs, String tiltle, int[] colW){
        try{    // nearly the whole function is wrapped by try-catch
            // getting the label of each column
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();
            ArrayList<String> colName = new ArrayList<String>();
            List<ArrayList<String>> buffer = new ArrayList<ArrayList<String>>();
            for (int i = 1; i <= columnsNumber; i++) {
                colName.add(rsmd.getColumnLabel(i));
            }
            buffer.add(colName);

            // buffering all the record to better preform random access(page switching)
            while (rs.next()) {
                ArrayList<String> record = new ArrayList<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    record.add(rs.getString(i));
                }
                buffer.add(record);
            }
            
            // re-calculate colW if column number of inputted colW is not match with rs
            if(colW.length != columnsNumber){
                colW = new int[colName.size()];
                for (int i = 0; i < colName.size(); i++) {
                    colW[i] = colName.get(i).length();
                    for (ArrayList<String> row : buffer) {  // find max string length for each column
                        if (row.get(i).length() > colW[i]) {
                            colW[i] = row.get(i).length();
                        }
                    }
                }
                
                int sum = 0;
                for(int w: colW)
                    sum += w;
                int min = 5;
                while(sum+columnsNumber+1>80){  // reduce max colW by 1 if the total print width > 80
                    boolean sumChanged = false;
                    // decrease while keeping a min width
                    int maxindex = 0;
                    for(int i=1; i<columnsNumber; i++){
                        if(colW[i]>colW[maxindex]){
                            maxindex = i;
                        }
                    }
                    if(colW[maxindex]>min){
                        colW[maxindex]--;
                        sumChanged = true;
                    }
                    
                    // if impossible even width of each column = 1, give up to keep the total width in 80
                    if(!sumChanged){    
                        min--;
                        if(min==0){ // give up and set each width in 2
                            for(int i=0; i<columnsNumber; i++)
                                colW[i] = 2;
                            break;
                        }
                    }

                    // re-calculate sum of colW to check while-loop condition
                    sum = 0;
                    for(int w: colW)
                        sum += w;
                }
            }

            int page = 1;
            final int numberOfOnePage = 10; // number of records show in one page
            int maxPage = 1;
            while(maxPage*numberOfOnePage < buffer.size()-1)    // find the maximum page number
                maxPage++;
            int input = 0;

            while(input != 3){  // input==3 -> quit normally
                Scanner myScanner = new Scanner(System.in);
                clrscr();
                // print title in the center (80 width)
                System.out.printf("%" + ((80-tiltle.length())/2>0?(80-tiltle.length())/2:0) + "s%s\n", "", tiltle);
                
                if (buffer.size()==1){  //ResultSet is empty
                    System.out.println("\n" + " ".repeat(29) + "---No results found---\n");
                    showMessage("\n");
                    return 0;
                }

                // print upper +-------+--------+...
                for(int w : colW)
                    System.out.print("+" + "-".repeat(w));
                System.out.println("+");

                //print data(records)
                for(int r=0; r<=numberOfOnePage; r++){
                    ArrayList<String> row = new ArrayList<String>();
                    // load the print data
                    if(r==0){   // r==0 -> column label
                        row = buffer.get(0);
                    }else{  // load the required record in buffer to row
                        if((page-1)*numberOfOnePage + r >= buffer.size())
                            for (int i = 1; i <= columnsNumber; i++)
                                row.add("");
                        else
                            row = buffer.get((page-1)*numberOfOnePage + r);
                    }

                    // start printing
                    // copying from row to a hashmap
                    HashMap<Integer, String> remainingString = new HashMap<Integer, String>();
                    for(int i=0; i<columnsNumber; i++)
                        remainingString.put(i, row.get(i)==null?"":row.get(i));
                    while(!remainingString.isEmpty()){
                        for(int i=0; i<columnsNumber; i++){
                            String tmpS;
                            if(remainingString.containsKey(i)){ // move the string in map to tmpS
                                tmpS = remainingString.get(i);
                                remainingString.remove(i);
                            }else{
                                tmpS = "";
                            }

                            // cut the string if the length is too long
                            if(tmpS.length()>colW[i]){
                                char[] sp = {' ', ',', '-'};    // preferred seperator to try not to cut the words in middle
                                int[] pos = new int[sp.length];
                                for(int j=0; j<sp.length; j++)
                                    pos[j] = tmpS.lastIndexOf(sp[j], colW[i]-1);
                                int maxIndex = -1;
                                for (int p : pos) { // locate the preferred seperator
                                    if (p > maxIndex && p < colW[i]) {
                                        maxIndex = p;
                                    }
                                }
                                if(maxIndex == -1)  // cut the string in max length if no preferred seperator found
                                    maxIndex = colW[i];
                                else
                                    maxIndex++; // the location of cutting is one char after pos

                                // put back the remaining into the map
                                remainingString.put(i, tmpS.substring(maxIndex));
                                tmpS = tmpS.substring(0, maxIndex); // the string to print
                            }
                            // print the data
                            System.out.print("|" + tmpS + " ".repeat(colW[i]-tmpS.length()));
                        }
                        System.out.println("|");    // end of one row
                    }

                    // print middle +-------+--------+...
                    if(r==0){
                        for(int w : colW)
                            System.out.print("+" + "-".repeat(w));
                        System.out.println("+");
                    }
                }

                // print bottom +-------+--------+... (after all data)
                for(int w : colW)
                    System.out.print("+" + "-".repeat(w));
                System.out.println("+\n");
                
                // print page number and user options (center at 80)
                String pageS = "Page " + page + "/" + maxPage;
                System.out.print(page>1?"1. Previous Page  ":" ".repeat(18)); 
                System.out.print(" ".repeat(22-(pageS.length()+1)/2>0?22-(pageS.length()+1)/2:0));
                System.out.print(pageS + " ".repeat(20-pageS.length()/2>0?20-pageS.length()/2:0));
                System.out.println(page<maxPage?"  2. Next Page":" ");
                System.out.println("3. Finish" + " ".repeat(23) + "4. Back to Menu" + " ".repeat(15) + "5. Quit the System\n");
                
                switch(input){  //error message for wrong input
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

                // get user input
                try{
                    input = myScanner.nextInt();
                }catch(Exception e){
                    input = -1;
                    continue;
                }

                // process user input
                switch(input){
                    case 1: // previous page
                        input = page>1 ? 0 : 2;
                        if(page>1)
                            page--;
                        break;
                    case 2: // next page
                        input = page<maxPage ? 0 : 2;
                        if(page<maxPage)
                            page++;
                        break;
                    case 3: // finish (exit)
                        break;
                    case 4: // back to menu
                        return 2;
                    case 5: // quit the program
                        return 3;
                    default:    // others -> unknow(wrong) input
                        input = 2;
                }
            }
        }catch(Exception x){    // exception not expected (print out for debug)
            x.printStackTrace();
            System.out.println(x);
            Scanner myScanner = new Scanner(System.in);
            myScanner.nextLine();
            return 1;
        }
        return 0;
    }

    static void readFile(){
        while(true){
            clrscr();
            try {
                // Create a statement
                Statement stmt = conn.createStatement();
                
                // Get the path of file
                Scanner inputScanner = new Scanner(System.in);
                System.out.println("Enter your file path name(Empty to exit): [.txt / .csv]");
                String filePath = inputScanner.nextLine();

                if(filePath.equals("")){
                    break;
                }

                // Read data from file
                String isbnPattern = "\\d-\\d{4}-\\d{4}-\\d";
                String datePattern = "\\d{4}-\\d{2}-\\d{2}";
                String oidPattern = "\\d{8}";
                File file = new File(filePath);
                Scanner scanner = new Scanner(file);

                String createdTable = "";
                boolean finishRead = false;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    ArrayList<String> al = new ArrayList<String>();
        
                    StringBuilder sb = new StringBuilder();
                    boolean inQuotes = false;

                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == ',') {
                            if (!inQuotes) {
                                al.add(sb.toString());
                                sb = new StringBuilder();
                            } else {
                                sb.append(c);
                            }
                        } else if (c == '"') {
                            if (i < line.length() - 1 && line.charAt(i + 1) == '"') {
                                sb.append(c);
                                i++;
                            } else {
                                inQuotes = !inQuotes;
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                    al.add(sb.toString());

                    String[] parts = al.toArray(new String[al.size()]);

                    // if not created the temp table for testing error
                    if(createdTable.equals("") && (parts.length == 5||parts.length == 3||parts.length == 6)){    
                        try{    // create a temp table to test error
                            if (parts.length == 5) {
                                createdTable = "Book";
                            } else if (parts.length == 3) {
                                createdTable = "Customer";
                            } else if (parts.length == 6) {
                                createdTable = "Ordering";
                            }
                            Statement stmt2 = conn.createStatement();
                            stmt2.executeUpdate("CREATE TABLE " + newName + " " + tableStruct.get(createdTable));
                        }catch(Exception x){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                            showMessage("\nUnknown Error while connecting to database\n--- Disconnected to database ---");
                            stmt.executeUpdate("DROP TABLE " + newName);
                            connected = false;
                            return;
                        }
                    }

                    // Insert data into the table
                    if (parts.length == 5) {
                        // Book table
                        String isbn = parts[0].trim();
                        if(isbn.matches("\\d+") && isbn.length()==10)
                            isbn = isbn.substring(0, 1) + "-" + isbn.substring(1, 5) + "-" + 
                                    isbn.substring(5, 9) + "-" + isbn.substring(9);
                        String title = parts[1].trim();
                        String authors = parts[2].trim();
                        int price, quantity;
                        int error = 0;
                        try{
                            error = 1;
                            price = Integer.parseInt(parts[3].trim());
                            quantity = Integer.parseInt(parts[4].trim());

                            error = 2;
                            if(price <= 0 || quantity <=0)
                                throw new Exception();

                            error = 3;
                            if (!isbn.matches(isbnPattern))
                                throw new Exception();

                            error = 4;
                            String query = "INSERT INTO " + newName + " (ISBN, Title, Authors, Price, InventoryQuantity) VALUES ('" + isbn + "', '" + title + "', '" + authors + "', " + price + ", " + quantity + ")";
                            stmt.executeUpdate(query);
                        }catch(Exception e){
                            System.out.println("\nError occurred in record:");
                            System.out.println("ISBN: " + parts[0]);
                            System.out.println("Title: " + parts[1]);
                            System.out.println("Authors: " + parts[2]);
                            System.out.println("Price: " + parts[3]);
                            System.out.println("InventoryQuantity: " + parts[4]);
                            switch(error){
                                case 1:
                                    showMessage("\nPrice or Quantity is not an integer! Please check the file!");
                                    break;
                                case 2:
                                    showMessage("\nPrice or Quanity is less than 0! Please check the file!");
                                    break;
                                case 3:
                                    showMessage("\nISBN is in wrong format (X-XXXX-XXXX-X OR XXXXXXXXXX)! Please check the file!");
                                    break;
                                case 4:
                                    showMessage("\nFailure while inserting to database! Please check the file!");
                                    break;
                            }
                            throw new Exception("");
                        }
                        // if(authors.indexOf(",") != -1 ){
                        //     System.out.println("Author name contain (,)! Please check the file!");
                        //     return;
                        // }
                    } else if (parts.length == 3) {
                        // Customer table
                        String uid = parts[0].trim();
                        String name = parts[1].trim();
                        String address = parts[2].trim();
                        int error = 0;
                        try{
                            error = 1;
                            String query = "INSERT INTO " + newName + " (UID, Name, Address) VALUES ('" + uid + "', '" + name + "', '" + address + "')";
                            stmt.executeUpdate(query);
                        }catch(Exception e){
                            System.out.println("\nError occurred in record:");
                            System.out.println("UID: " + parts[0]);
                            System.out.println("Name: " + parts[1]);
                            System.out.println("Address: " + parts[2]);
                            switch(error){
                                case 1:
                                    showMessage("\nFailure while inserting to database! Please check the file!");
                                    break;
                            }
                            throw new Exception("");
                        }
                    } else if (parts.length == 6) {
                        // Ordering table
                        String oid = parts[0].trim();
                        String uid = parts[1].trim();
                        String isbn = parts[2].trim();
                        if(isbn.matches("\\d+") && isbn.length()==10)
                            isbn = isbn.substring(0, 1) + "-" + isbn.substring(1, 5) + "-" + 
                                    isbn.substring(5, 9) + "-" + isbn.substring(9);
                        String date = parts[3].trim();
                        if(date.matches("\\d+") && date.length()==8)
                            date = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6);
                        int quantity;
                        String status = parts[5].trim();
                        int error = 0;
                        try{
                            error = 1;
                            quantity = Integer.parseInt(parts[4].trim());

                            error = 2;
                            if(quantity <=0)
                                throw new Exception();

                            error = 3;
                            if (!isbn.matches(isbnPattern))
                                throw new Exception();

                            error = 4;
                            if (!date.matches(datePattern))
                                throw new Exception();

                            error = 5;
                            if (!oid.matches(oidPattern))
                                throw new Exception();
                            
                            error = 6;
                            List<String> stus = Arrays.asList("ordered", "shipped", "received");
                            if(!stus.contains(status))
                                throw new Exception();

                            error = 7;
                            try{    // check isbn
                                Statement stmt3 = conn.createStatement();
                                ResultSet rs3 = stmt3.executeQuery("SELECT COUNT(*) FROM Book WHERE ISBN = '" + isbn + "'");
                                rs3.next();
                                if(rs3.getInt(1)>0)
                                    error = 0;
                            }catch(Exception e){
                                e.printStackTrace();
                                showMessage("");
                            }finally{
                                if(error != 0){
                                    throw new Exception();
                                }
                            }

                            error = 8;
                            try{    // check uid
                                Statement stmt3 = conn.createStatement();
                                ResultSet rs3 = stmt3.executeQuery("SELECT COUNT(*) FROM Customer WHERE UID = '" + uid + "'");
                                rs3.next();
                                if(rs3.getInt(1)>0)
                                    error = 0;
                            }catch(Exception e){
                                e.printStackTrace();
                                showMessage("");
                            }finally{
                                if(error != 0){
                                    throw new Exception();
                                }
                            }

                            error = 9;
                            String query = "INSERT INTO " + newName + " (OID, UID, OrderISBN, OrderDate, OrderQuantity, ShippingStatus) VALUES ('" + oid + "', '" + uid + "', '" + isbn + "', '" + date + "', " + quantity + ", '" + status + "')";
                            stmt.executeUpdate(query);
                        }catch(Exception e){
                            System.out.println("\nError occurred in record:");
                            System.out.println("OID: " + parts[0]);
                            System.out.println("UID: " + parts[1]);
                            System.out.println("OrderISBN: " + parts[2]);
                            System.out.println("OrderDate: " + parts[3]);
                            System.out.println("OrderQuantity: " + parts[4]);
                            System.out.println("ShippingStatus: " + parts[5]);
                            switch(error){
                                case 1:
                                    showMessage("\nQuantity is not an integer! Please check the file!");
                                    break;
                                case 2:
                                    showMessage("\nQuanity is less than 0! Please check the file!");
                                    break;
                                case 3:
                                    showMessage("\nISBN is in wrong format (X-XXXX-XXXX-X OR XXXXXXXXXX)! Please check the file!");
                                    break;
                                case 4:
                                    showMessage("\nDATE(YYYY-MM-DD OR YYYYMMDD) is in wrong format! Please check the file!");
                                    break;
                                case 5:
                                    showMessage("\nOID(XXXXXXXX) is in wrong format! Please check the file!");
                                    break;
                                case 6:
                                    showMessage("\nUnknow ShippingStatus! Please check the file!");
                                    break;
                                case 7:
                                    showMessage("\nISBN not exist in Book record! Please check the file!");
                                    break;
                                case 8:
                                    showMessage("\nUID not exist in Customer record! Please check the file!");
                                    break;
                                case 9:
                                    System.out.println("hi");
                                    System.out.println(e.getMessage());
                                    showMessage("\nFailure while inserting to database! Please check the file!\n(UID and OID may not match with previous record)");
                                    break;
                            }
                            throw new Exception("");
                        }
                    } else {
                        showMessage("\nFile wrong format! Please check the file!");
                        throw new Exception("");
                    }
                }   // end read lines loop
                scanner.close();

                // write back from temp to correct one
                try{
                    conn.setAutoCommit(false);
                    Statement stmt2 = conn.createStatement();
                    ResultSet rs = stmt2.executeQuery("SELECT * FROM " + newName);
                    while(rs.next()){
                        if (createdTable.equals("Book")) {
                            stmt.executeUpdate("INSERT INTO " + createdTable + " VALUES ('" + rs.getString(1) + "', '" + rs.getString(2) + "', '" + rs.getString(3) + "', " + rs.getString(4) + ", " + rs.getString(5) + ")");
                        } else if (createdTable.equals("Customer")) {
                            stmt.executeUpdate("INSERT INTO " + createdTable + " VALUES ('" + rs.getString(1) + "', '" + rs.getString(2) + "', '" + rs.getString(3) + "')");
                        } else if (createdTable.equals("Ordering")) {
                            stmt.executeUpdate("INSERT INTO " + createdTable + " VALUES ('" + rs.getString(1) + "', '" + rs.getString(2) + "', '" + rs.getString(3) + "', '" + rs.getString(4) + "', " + rs.getString(5) + ", '" + rs.getString(6) + "')");
                        }
                    }
                    conn.commit();
                    conn.setAutoCommit(true);
                    finishRead = true;
                }catch(Exception e){
                    showMessage("\nSome records Duplicated");
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
                if(finishRead){
                    showMessage("\nInput success!");
                }
            }catch (FileNotFoundException e){
                showMessage("\nFile not found");
            }catch (SQLException e) {
                e.printStackTrace();
                showMessage("");
            } catch (Exception e) {
                if (!e.getMessage().equals("")){
                    e.printStackTrace();
                    showMessage("");
                }
            }finally{
                try{    // drop temp table after use 
                    Statement stmt2 = conn.createStatement();
                    stmt2.executeUpdate("DROP TABLE " + newName);
                }catch(Exception e){
                }
            }
        }   // end read file loop
    }

    public static void main(String[] args){
        int page = 0;
        int error = 0;
        connectDatabase();  // try to connect at the begining, if fail, need to go to 1-1. Connect to Database and Create Missing Tables
        while(page != 4){   // page==4 -> quit the program
            showMenu(page);

            // Read input
            switch(error){  // print error message
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

            // read input
            System.out.print(">>> Please Enter Your Query: ");
            int input = -1;
            Scanner myScanner = new Scanner(System.in);
            try{
                input = myScanner.nextInt();
            }catch(Exception e){
                error = 1;
                continue;
            }

            // process user choice: switch page OR call function OR process
            switch(page){
                // page 0 (main menu)
                case 0:
                    switch(input){
                        case 1: page = 1; break;
                        case 2: page = 2; break;
                        case 3: page = 3; break;
                        case 4: page = 4; break;
                        default:
                            error = 2;
                    } 
                    break;
                
                // page 1 (Database Initialization)
                case 1:
                    switch(input){
                        case 1: // Login to Database and Create Missing Tables
                            connectDatabase();
                            break;
                        case 2: // Load from File
                            readFile();
                            break;
                        case 3: // Delete All Records
                            clrscr();
                            
                            // ban if not connected
                            if(!connected){
                                clrscr();
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            
                            // ask for double check
                            System.out.println("Caution: Deleting ALL records at Books, Customers, and Orders");
                            System.out.println("This action cannot be recovered.");
                            System.out.print("Proceed? (Enter y|Y for Yes, any others for No): ");

                            // read input
                            Scanner myScanner2 = new Scanner(System.in);
                            String in = myScanner2.nextLine();
                            if(in.equals("Y") || in.equals("y")){
                                try{    // drop the whole table then create it back to delete all record
                                    Statement stmt = conn.createStatement();
                                    for(int i = tableName.length-1; i>=0; i--)
                                        stmt.executeUpdate("DROP TABLE " + tableName[i]);
                                    for(String tname: tableName){
                                        stmt.executeUpdate("CREATE TABLE " + tname + " " + tableStruct.get(tname));
                                    }
                                    showMessage("\nAll records has been deleted");
                                }catch(Exception x){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                                    showMessage("\nUnknown Error while deleting records\n--- Disconnected to database ---");
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
                        default:    // wrong input
                            error = 2; 
                    }
                    break;
                
                // page 2 (Customer Operation)
                case 2:
                    switch(input){
                        case 1: // Book Search
                            clrscr();

                            // ban if not connected
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }

                            // ask user to input the search information
                            System.out.println("Book Searching\n");
                            System.out.println("Please enter the ISBN, Book Title and Author Name:");
                            System.out.println("(Leave it empty to not specify)");
                            Scanner myScanner2 = new Scanner(System.in);
                            String isbn, title, aname;
                            System.out.print("ISBN: ");
                            isbn = myScanner2.nextLine();
                            if(isbn.matches("\\d+") && isbn.length()==10)
                                isbn = isbn.substring(0, 1) + "-" + isbn.substring(1, 5) + "-" + 
                                        isbn.substring(5, 9) + "-" + isbn.substring(9);
                            System.out.print("Book Title: ");
                            title = myScanner2.nextLine();
                            System.out.print("Author Name: ");
                            aname = myScanner2.nextLine();

                            // try to search the book and show the rs by showRs()
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
                            }catch(Exception e){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                                showMessage("\nUnknown Error while searching books\n--- Disconnected to database ---");
                                connected = false;
                            }
                            break;
                        case 2: // Place Order
                            clrscr();

                            // ban if not connected
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }

                            // ask user to input the order information
                            System.out.println("Placing Order\n");
                            String uid2;
                            Scanner myScanner4 = new Scanner(System.in);
                            System.out.print("Please enter your UID: ");
                            uid2 = myScanner4.nextLine();
                            
                            // check if empty
                            if(uid2.equals("")){
                                showMessage("\nUID cannot be empty");
                                continue;
                            }

                            // check if uid exist
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT *  " + 
                                    "FROM Customer " + 
                                    "WHERE uid = '" + uid2 + "' ");
                                if(!rs.next()){
                                    throw new Exception();
                                }
                            }catch(Exception e){
                                showMessage("\nUID '" + uid2 + "' not found");
                                continue;
                            }
                            
                            // find max oid, new oid = max+1
                            String newOID = null;
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT MAX(OID) AS MaxOID " + 
                                    "FROM Ordering ");
                                rs.next();
                                int oid = Integer.parseInt(rs.getString("MaxOID"));
                                if(oid+1 > 99999999)
                                    throw new Exception();
                                newOID = Integer.toString(oid+1);
                            }catch(Exception e){
                                showMessage("\nOID is full\nPlease empty or try another database");
                                continue;
                            }

                            // turn off autocommit
                            try{
                                conn.setAutoCommit(false);
                            }catch(Exception e){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                                showMessage("\nUnknown Error while searching books\n--- Disconnected to database ---");
                                connected = false;
                            }
                            
                            // ask input, check input
                            String isbn2 = "0", qty = "0";
                            while(!(isbn2.equals("") && qty.equals(""))){
                                clrscr();
                                System.out.println("\nUID: " + uid2 + " OID: " + newOID + "");
                                System.out.println("Please enter the required book's ISBN and quantity needed: ");
                                System.out.println("(Empty both of them to end ordering)");
                                System.out.print("Book ISBN: ");
                                isbn2 = myScanner4.nextLine();
                                if(isbn2.matches("\\d+") && isbn2.length()==10)
                                    isbn2 = isbn2.substring(0, 1) + "-" + isbn2.substring(1, 5) + "-" + 
                                            isbn2.substring(5, 9) + "-" + isbn2.substring(9);
                                System.out.print("Ordering quantity: ");
                                qty = myScanner4.nextLine();
                                
                                // check stock, insert value(temp)
                                if(!(isbn2.equals("") && qty.equals(""))){
                                    try{
                                        // check empty
                                        if(isbn2.equals("") && !qty.equals(""))
                                            throw new Exception("\nISBN cannot be empty (Empty the BOTH to end ordering)\nPlease try again");
                                        if(!isbn2.equals("") && qty.equals(""))
                                            throw new Exception("\nQuantity cannot be empty (Empty the BOTH to end ordering)\nPlease try again");
                                        
                                        // check isbn exist
                                        Statement stmt = conn.createStatement();
                                        ResultSet rs = stmt.executeQuery(
                                            "SELECT InventoryQuantity AS Stock " + 
                                            "FROM Book " + 
                                            "WHERE isbn = '" + isbn2 + "' ");
                                        if(!rs.next()){
                                            throw new Exception("\nNo recorded books with ISBN: " + isbn2 + " in the system\nPlease try again");
                                        }
                                        int stock = rs.getInt("Stock");

                                        // check qty input valid
                                        if(!qty.matches("\\d+"))
                                            throw new Exception("\nUnknow input for Ordering quantity\nPlease try again");
                                        
                                        // check enough inventory
                                        int orderQty = Integer.parseInt(qty);
                                        if(orderQty>stock)
                                            throw new Exception("\nInventory(" + stock + ") not enough for the ordering(" + orderQty + ")\nPlease try again");
                                        
                                        // check qty input valid
                                        if(orderQty<1)
                                            throw new Exception("\nInvalid quantity\nPlease try again");
                                        
                                        // find if user ordered the same book in the same oid
                                        rs = stmt.executeQuery(
                                            "SELECT OrderQuantity AS OrdQ " + 
                                            "FROM Ordering " + 
                                            "WHERE OrderISBN = '" + isbn2 + "' AND oid = '" + newOID + "'");
                                        if(rs.next()){  // yes -> updata record
                                            System.out.println("\nBook repeated. The quantity is added to the previous record.");
                                            stmt.executeUpdate(
                                                "UPDATE Ordering SET OrderQuantity = OrderQuantity + " + orderQty +  
                                                " WHERE OrderISBN = '" + isbn2 + "' AND oid = '" + newOID + "'"    
                                            );
                                        }else{  // no -> insert record
                                            stmt.executeUpdate(
                                                "INSERT INTO Ordering (OID, UID, OrderISBN, OrderDate, OrderQuantity, ShippingStatus) VALUES " + 
                                                "('" + newOID + "', '" + uid2 + "', '" + isbn2 + "', '" + 
                                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "', " + orderQty + ", 'ordered')"    
                                            );
                                        }

                                        // reduce inventory
                                        stmt.executeUpdate("UPDATE Book SET InventoryQuantity = InventoryQuantity - " + orderQty + 
                                            " WHERE isbn = '" + isbn2 + "'"
                                        );
                                        showMessage("\nOrder recorded");
                                    }catch(Exception x){
                                        showMessage(x.getMessage());
                                    }
                                }
                            }

                            // after all record entered -> confirm
                            int input2 = -1, price = -1;
                            try{
                                // check if empty order
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery("SELECT * FROM Ordering WHERE Ordering.OID = '" + newOID + "' ");
                                if(!rs.next()){
                                    clrscr();
                                    showMessage("\nNo order recorded");
                                    continue;
                                }

                                // get the total price
                                rs = stmt.executeQuery("SELECT SUM(Book.Price * Ordering.OrderQuantity) AS tPrice " + 
                                    "FROM Ordering " + 
                                    "JOIN Book ON Ordering.OrderISBN = Book.ISBN " + 
                                    "WHERE Ordering.OID = '" + newOID + "' ");
                                rs.next();
                                price = rs.getInt("tPrice");
                            }catch(Exception x){
                                showMessage(x.getMessage());
                                continue;
                            }
                                
                            // ask for confirmation of the order
                            while(input2 != 0){
                                clrscr();
                                System.out.println("Order confirmation\n");
                                System.out.println("> 1. Show order");
                                System.out.println("> 2. Confirm order (Total Price: " + price + ")");
                                System.out.println("> 3. Cancel order");

                                Scanner myScanner3 = new Scanner(System.in);
                                System.out.print("\n>>> Please Enter Your Option: ");
                                try{
                                    input2 = myScanner3.nextInt();
                                    if(input2<1 || input2>3)
                                        throw new Exception("\nPlease choose from the options");
                                }catch(Exception x){
                                    showMessage(x.getMessage());
                                    continue;
                                }
                                switch(input2){
                                    case 1:
                                        int r = 0;
                                        try{
                                            Statement stmt = conn.createStatement();
                                            ResultSet rs = stmt.executeQuery(
                                                "SELECT Ordering.OrderISBN AS ISBN, Book.Title AS 'Book Title', Ordering.OrderQuantity AS Ordered, " +
                                                "Book.Price AS Price, (Book.Price * Ordering.OrderQuantity) AS Subtotal " +
                                                "FROM Ordering " +
                                                "JOIN Book ON Ordering.OrderISBN = Book.ISBN " +
                                                "WHERE Ordering.OID = '" + newOID + "'"
                                            );
                                            int[] colW = {13, 50, 7, 5, 8};
                                            r = showRs(rs, "Order detail  OID: '" + newOID + "'  Total: " + price, colW);
                                            if(r==2||r==3){
                                                conn.rollback();
                                                conn.setAutoCommit(true);
                                                input2 = 0;
                                                clrscr();
                                            }
                                        }catch(Exception x){
                                            showMessage(x.getMessage());
                                            continue;
                                        }
                                        if(r==2){
                                            page = 0;
                                            showMessage("Order cancelled. Going back to Menu...");
                                        }
                                        if(r==3){
                                            page = 4; 
                                            showMessage("Order cancelled. Quiting the program...");
                                        }
                                        break;
                                    case 2:
                                        try{
                                            conn.commit();
                                            conn.setAutoCommit(true);
                                        }catch(Exception x){
                                            showMessage(x.getMessage());
                                            continue;
                                        }
                                        input2 = 0;
                                        clrscr();
                                        showMessage("\nOrder Placed, OID: " + newOID);
                                        break;
                                    case 3:
                                        try{
                                            conn.rollback();
                                            conn.setAutoCommit(true);
                                        }catch(Exception x){
                                            showMessage(x.getMessage());
                                            continue;
                                        }
                                        input2 = 0;
                                        clrscr();
                                        showMessage("\nOrder cancelled");
                                        break;
                                }
                            }
                            break;
                        case 3: // Check History Orders
                            clrscr();
                            
                            // ban if not connected
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }
                            
                            // ask user to input uid
                            System.out.println("Checking History Orders\n");
                            String uid;
                            Scanner myScanner3 = new Scanner(System.in);
                            System.out.print("Please enter your UID: ");
                            uid = myScanner3.nextLine();

                            // show by calling showRs() (even rs is empty set)
                            try{
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT Ordering.OrderDate AS 'Date', Ordering.oid AS 'OID', Ordering.OrderISBN AS 'ISBN', Ordering.OrderQuantity AS 'Quantity', Ordering.ShippingStatus AS 'Status' " + 
                                    "FROM Ordering " + 
                                    "WHERE uid = '" + uid + "' " + 
                                    "ORDER BY Ordering.OrderDate DESC;");
                                int[] colW = {10, 12, 15, 10, 10};
                                int r = showRs(rs, "History Orders for UID: " + uid, colW);
                                if(r==2)
                                    page = 0;
                                if(r==3)
                                    page = 4; 
                            }catch(Exception e){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                                showMessage("\nUnknown Error while checking history orders\n--- Disconnected to database ---");
                                connected = false;
                            }
                            break;
                        case 4: // Back to Menu
                            page = 0; 
                            break;
                        case 5: // Quit
                            page = 4;
                            break;
                        default:    // wrong input
                            error = 2; 
                    }
                    break;

                // page 3 (Bookstore Operation)
                case 3: 
                    switch(input){
                        case 1: // Order Update (one way update: ordered -> shipped -> received)
                            clrscr();

                            // ban if not connected
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }

                            // ask user to specify the order want to update
                            System.out.println("Order Updating\n");
                            System.out.println("Please enter the OID and the Order ISBN:");
                            System.out.println("(Leave ISBN empty to change all status in the order)");
                            Scanner myScanner2 = new Scanner(System.in);
                            String oid, isbn;
                            System.out.print("OID: ");
                            oid = myScanner2.nextLine();
                            System.out.print("ISBN: ");
                            isbn = myScanner2.nextLine();
                            if(isbn.matches("\\d+") && isbn.length()==10)
                                isbn = isbn.substring(0, 1) + "-" + isbn.substring(1, 5) + "-" + 
                                        isbn.substring(5, 9) + "-" + isbn.substring(9);

                            // check different conditon and throw the message wanted to print
                            try{
                                // check if oid empty
                                if(oid.equals("")){
                                    throw new Exception("\nOID cannot be empty");
                                }

                                // check if oid exist
                                Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery(
                                    "SELECT *  " + 
                                    "FROM Ordering " + 
                                    "WHERE oid = '" + oid + "' ");
                                if(!rs.next()){
                                    throw new Exception("\nOID " + oid + " not found");
                                }

                                // check if isbn empty: two conditions:
                                if(!isbn.equals("")){   // not empty -> at most one record to update -> check if that record exist
                                    rs = stmt.executeQuery(
                                        "SELECT * " + 
                                        "FROM Ordering " + 
                                        "WHERE oid = '" + oid + "' AND orderIsbn = '" + isbn + "' ");
                                    if(!rs.next()){
                                        throw new Exception("\nBook ISBN: " + isbn + " not found in OID: " + oid);
                                    }
                                }else{  // isbn empty -> more than one record to update -> check if all status equal
                                    rs = stmt.executeQuery(
                                        "SELECT COUNT(DISTINCT ShippingStatus) AS Num " + 
                                        "FROM Ordering " + 
                                        "WHERE oid = '" + oid + "' ");
                                    rs.next();

                                    // if there are more than one status in a single order, ban and ask if show
                                    if(rs.getInt("Num") > 1){   // more than one status, not updating
                                        System.out.println("\nThere are more than one status in OID: " + oid);
                                        System.out.println("Update failed. Show records? (Enter y|Y for Yes, any others for No)");
                                        Scanner myScanner3 = new Scanner(System.in);
                                        String in = myScanner3.nextLine();
                                        if(in.equals("Y")||in.equals("y")){
                                            rs = stmt.executeQuery(
                                                "SELECT OrderISBN AS ISBN, OrderQuantity AS Quantity, ShippingStatus AS Status " + 
                                                "FROM Ordering " + 
                                                "WHERE oid = '" + oid + "' ");
                                            int[] colW = {15, 10, 10};
                                            int r = showRs(rs, "Status in OID = " + oid, colW);
                                            if(r==2)
                                                page = 0;
                                            if(r==3)
                                                page = 4; 
                                        }
                                        // after showing OR no need to show, break out by throwing an exception (empty message to not print)
                                        throw new Exception("");
                                    }
                                }

                                // load the status that going to update
                                rs = stmt.executeQuery(
                                        "SELECT ShippingStatus AS status " + 
                                        "FROM Ordering " + 
                                        "WHERE oid = '" + oid + "' " + 
                                        (isbn.equals("")?"":("AND orderIsbn = '" + isbn + "' ")));
                                rs.next();
                                String cStatus = rs.getString("status");

                                // print current status
                                System.out.println("\nCurrent status: '" + cStatus + "'");

                                // load options
                                ArrayList<String> options = new ArrayList<String>();
                                switch(cStatus){
                                    case "ordered": // can update to 'shipped' OR 'received'
                                        options.add("shipped");
                                    case "shipped": // can only update to 'received'
                                        options.add("received");
                                        options.add("Cancel (Not updating)");
                                        break;
                                    default:    // cannot update 'recieved' (OR any other unknown status)
                                        throw new Exception("\nUpdate of current status '" + cStatus + "' is not supported");
                                }

                                // print options
                                System.out.println("Update to: ");
                                for (int i = 0; i < options.size(); i++) {
                                    System.out.println("> " + (i+1) + ". " + (i<options.size()-1?"'":"")  + options.get(i) + (i<options.size()-1?"'":""));
                                }

                                // ask input
                                System.out.print("\n>>> Please Enter Your Option: ");
                                Scanner myScanner3 = new Scanner(System.in);
                                int input2 = -1;
                                try{
                                    input2 = myScanner3.nextInt();  // will have exception for wrong input e.g. abc, #$%
                                    input2--;
                                    if(options.get(input2).equals("Cancel (Not updating)")) // will have exception for wrong input e.g. 0, -123
                                        throw new Exception("");    // cancelling normally, no message printed
                                    cStatus = options.get(input2);  // normal input -> save the status to update into cStatus
                                }catch(Exception x){
                                    if(x.getMessage().equals(""))   // continue throw exception for the normal cancel
                                        throw new Exception("");
                                    else    // throw exception for wrong input
                                        throw new Exception("\nUnknow input. Cancelling (No updates)");
                                }

                                // update the status to new status
                                stmt.executeUpdate("UPDATE Ordering " + 
                                                    "SET ShippingStatus = '" + cStatus + "' " + 
                                                    "WHERE OID = '" + oid + "' " + 
                                                    (isbn.equals("")?"":("AND orderIsbn = '" + isbn + "' ")));
                            }catch(Exception e){    // handle and print the error message thrown before
                                if(!e.getMessage().equals(""))  // (empty message to not print)
                                    showMessage(e.getMessage());
                                continue;
                            }
                            showMessage("\nStatus updated");
                            break;
                        case 2: // Order Query
                            clrscr();

                            // ban if not connected
                            if(!connected){
                                showMessage("Fail to Connect to Database");
                                continue;
                            }

                            // ask for the status to show
                            System.out.println("Order Querying\n");
                            System.out.println("Orders in shipping status:");
                            System.out.println("> 1. ordered");
                            System.out.println("> 2. shipped");
                            System.out.println("> 3. received\n");
                            Scanner myScanner3 = new Scanner(System.in);
                            System.out.print(">>> Please Enter Your Query: ");
                            int input2 = -1;
                            String status;
                            try{
                                input2 = myScanner3.nextInt();
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

                            // query and show rs
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
                            }catch(Exception e){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                                showMessage("\nUnknown Error while querying orders\n--- Disconnected to database ---");
                                connected = false;
                            }
                            break;
                        case 3: // Check Most Popular Books
                            clrscr();
                            
                            // ban if not connected
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
                            }catch(Exception e){    // unexceped exception catched, disconnect to database and force user to reconnect to solve the problem
                                showMessage("\nUnknown Error while checking most popular books\n--- Disconnected to database ---");
                                connected = false;
                            }
                            break;
                        case 4: // Back to Menu
                            page = 0; 
                            break;
                        case 5: // Quit
                            page = 4;
                            break;
                        default:    // wrong input
                            error = 2; 
                    }
                    break;
                // no default all page are included (page==4 -> quit program) and won't go out of expect
            }
        }
        clrscr();
        System.out.println("Bye");
    }
}
