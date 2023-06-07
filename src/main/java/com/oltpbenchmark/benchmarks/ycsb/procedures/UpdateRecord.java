/*
 * Copyright 2020 by OLTPBenchmark Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.ycsb.YCSBConstants;

import java.sql.*;

import static com.oltpbenchmark.benchmarks.ycsb.YCSBConstants.TABLE_NAME;

public class UpdateRecord extends Procedure {

    public final SQLStmt updateAllStmt = new SQLStmt(
            "UPDATE " + TABLE_NAME + " SET FIELD1=?,FIELD2=?,FIELD3=?,FIELD4=?,FIELD5=?," +
                    "FIELD6=?,FIELD7=?,FIELD8=?,FIELD9=?,FIELD10=? WHERE YCSB_KEY=?"
    );

    public void run(Connection conn, int keyname, String[] vals) throws SQLException {
        StringBuilder final_stmt = new StringBuilder();
        String updateFormat = "UPDATE " + TABLE_NAME + " SET FIELD1=\"%s\",FIELD2=\"%s\",FIELD3=\"%s\",FIELD4=\"%s\",FIELD5=\"%s\"," +
            "FIELD6=\"%s\",FIELD7=\"%s\",FIELD8=\"%s\",FIELD9=\"%s\",FIELD10=\"%s\" WHERE YCSB_KEY=%d";
        final_stmt.append(updateFormat.formatted(vals[0], vals[1],
            vals[2], vals[3],
            vals[4], vals[5],
            vals[6], vals[7],
            vals[8], vals[9],
            keyname));

        Statement stmt = conn.createStatement();
        try {
            stmt.execute(final_stmt.toString());
        } catch (SQLException ex) {
            System.out.println(ex.toString());
        }
    }
}

