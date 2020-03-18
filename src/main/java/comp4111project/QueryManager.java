package comp4111project;

import java.sql.ResultSet;
import java.sql.Statement;

public class QueryManager {
    DBConnection dbCon;
    private Statement st;
    private ResultSet rs;

    private QueryManager() {
        dbCon = new DBConnection();
    }

    private static class BillPushSingleton {
        private static final QueryManager INSTANCE = new QueryManager();
    }

    public static QueryManager getInstance() {
        return BillPushSingleton.INSTANCE;
    }

    public int returnAndLoanBook(String bookID, Boolean isReturningBook) {
        String updateQuery;
        try {
            String searchQuery = "SELECT available from book WHERE id =" + " '" + bookID + "' ";
            rs = st.executeQuery(searchQuery);

            if (rs.next()) {
                if(rs.getBoolean("available") == (isReturningBook ? false : true)) {
                    System.out.println("ready to be returned/loaned");
                    try {
                        if(isReturningBook) {
                            updateQuery = "UPDATE book SET available = '1' WHERE id =" + " '" + bookID + "' ";
                        } else {
                            updateQuery = "UPDATE book SET available = '0' WHERE id =" + " '" + bookID + "' ";
                        }

                        int result = st.executeUpdate(updateQuery);

                        if(result == 1) {
                            return 0; // OK
                        } else {
                            return 2; // Bad Request
                        }

                    } catch(Exception ex) {
                        return 2; // Bad Request
                    }

                } else {
                    System.out.println("Book already returned");
                    return 2; // The book is already returned
                }
            } else {
                System.out.println("no record");
                return 1; // No book record
            }

        } catch(Exception ex) {
            System.out.println("error " + ex);
            return 2; // Bad Request
        }
    }
}
