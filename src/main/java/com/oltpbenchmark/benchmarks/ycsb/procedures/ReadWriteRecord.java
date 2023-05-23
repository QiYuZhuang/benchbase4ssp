package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;

import java.sql.*;
import java.util.Arrays;
import java.util.Random;

import static com.oltpbenchmark.benchmarks.ycsb.YCSBConstants.TABLE_NAME;

public class ReadWriteRecord extends Procedure {
    public final SQLStmt readStmt = new SQLStmt(
        "SELECT * FROM " + TABLE_NAME + " WHERE YCSB_KEY=?"
    );

    public final SQLStmt updateStmt = new SQLStmt(
        "UPDATE " + TABLE_NAME + " SET FIELD1=?,FIELD2=?,FIELD3=?,FIELD4=?,FIELD5=?," +
            "FIELD6=?,FIELD7=?,FIELD8=?,FIELD9=?,FIELD10=? WHERE YCSB_KEY=?"
    );

    private int[] ops = new int[10];

    /*
     * @param ratio1: <read transaction : write transaction> (read transaction represents read-only)
     * @param ratio2: <read operation : write operation> (in write transaction)
     */
    public void run(Connection conn, int[] keyname, String[][] vals, double ratio1, double ratio2) throws SQLException {
        /*
         * if ratio1 = 0, it is a read-only transaction;
         * if ratio2 = 0, the transaction's operations are read operation;
         */
        Arrays.sort(keyname);
        StringBuilder final_stmt = new StringBuilder();
        int len = keyname.length;
        int count = 0;

        double rand1 = new Random().nextDouble();
        for (int i = 0; i < len; i++) {
            double rand2 = new Random().nextDouble();

            if (rand1 >= ratio1 || rand2 >= ratio2) {
                // read operation
                ops[i] = 1;
                String readFormat = "SELECT * FROM " + TABLE_NAME + " WHERE YCSB_KEY=%d";
                final_stmt.append(readFormat.formatted(keyname[i]));
            } else {
                // write operation
                ops[i] = 2;
                String updateFormat = "UPDATE " + TABLE_NAME + " SET FIELD1=\"%s\",FIELD2=\"%s\",FIELD3=\"%s\",FIELD4=\"%s\",FIELD5=\"%s\"," +
                    "FIELD6=\"%s\",FIELD7=\"%s\",FIELD8=\"%s\",FIELD9=\"%s\",FIELD10=\"%s\" WHERE YCSB_KEY=%d";
                final_stmt.append(updateFormat.formatted(vals[i][0], vals[i][1],
                    vals[i][2], vals[i][3],
                    vals[i][4], vals[i][5],
                    vals[i][6], vals[i][7],
                    vals[i][8], vals[i][9],
                    keyname[i]));
            }
            final_stmt.append(";");
        }

        Statement stmt = conn.createStatement();
        try {
            Boolean rs = stmt.execute(final_stmt.toString());
        } catch (SQLException ex) {
            System.out.println(ex.toString());
        }

        // maybe use ops when searching result
    }
}
