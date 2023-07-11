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

package com.oltpbenchmark.benchmarks.ycsb;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.Procedure.UserAbortException;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.ycsb.procedures.*;
import com.oltpbenchmark.distributions.CounterGenerator;
import com.oltpbenchmark.distributions.ZipfianGenerator;
import com.oltpbenchmark.types.TransactionStatus;
import com.oltpbenchmark.util.TextGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

/**
 * YCSBWorker Implementation
 * I forget who really wrote this but I fixed it up in 2016...
 *
 * @author pavlo
 */
class YCSBWorker extends Worker<YCSBBenchmark> {

    private final ZipfianGenerator readRecord;
    private static CounterGenerator insertRecord;
    private final ZipfianGenerator randScan;

    private final char[] data;
    private final String[] params = new String[YCSBConstants.NUM_FIELDS];

    private final String[][] fixParams = new String[10][YCSBConstants.NUM_FIELDS];
    private final String[] results = new String[YCSBConstants.NUM_FIELDS];

    private final UpdateRecord procUpdateRecord;
    private final ScanRecord procScanRecord;
    private final ReadRecord procReadRecord;
    private final ReadModifyWriteRecord procReadModifyWriteRecord;
    private final InsertRecord procInsertRecord;
    private final DeleteRecord procDeleteRecord;

    private final ReadWriteRecord procReadWriteRecord;

    private final int totalRequest = 5; // totalRequest < 10
    private final double ratio1;
    private final double ratio2;
    private final double disRatio;
    private final int nodeCnt;

    private final int[] keys;

    public YCSBWorker(YCSBBenchmark benchmarkModule, int id, int init_record_count, double zipf, int[] keys, double ratio1, double ratio2, double disRatio, int nodeCnt) {
        super(benchmarkModule, id);
        this.data = new char[benchmarkModule.fieldSize];
        this.ratio1 = ratio1;
        this.ratio2 = ratio2;
        this.disRatio = disRatio;
        this.nodeCnt = nodeCnt;
        // TODO:
        this.readRecord = new ZipfianGenerator(rng(), init_record_count, zipf);// pool for read keys
        this.randScan = new ZipfianGenerator(rng(), YCSBConstants.MAX_SCAN);
        for (int i = 0; i < totalRequest; i++) {
            for (int j = 0; j < this.params.length; j++) {
                this.fixParams[i][j] = new String(TextGenerator.randomFastChars(rng(), this.data));
                this.fixParams[i][j] = this.fixParams[i][j].replaceAll(";", "!");
                this.fixParams[i][j] = this.fixParams[i][j].replaceAll("\"", ".");
                this.fixParams[i][j] = this.fixParams[i][j].replaceAll("'", "~");
            }
        }

        this.keys = keys;

        synchronized (YCSBWorker.class) {
            // We must know where to start inserting
            if (insertRecord == null) {
                insertRecord = new CounterGenerator(init_record_count);
            }
        }

        // This is a minor speed-up to avoid having to invoke the hashmap look-up
        // everytime we want to execute a txn. This is important to do on
        // a client machine with not a lot of cores
        this.procUpdateRecord = this.getProcedure(UpdateRecord.class);
        this.procScanRecord = this.getProcedure(ScanRecord.class);
        this.procReadRecord = this.getProcedure(ReadRecord.class);
        this.procReadModifyWriteRecord = this.getProcedure(ReadModifyWriteRecord.class);
        this.procInsertRecord = this.getProcedure(InsertRecord.class);
        this.procDeleteRecord = this.getProcedure(DeleteRecord.class);
        this.procReadWriteRecord = this.getProcedure(ReadWriteRecord.class);
    }

    @Override
    protected TransactionStatus executeWork(Connection conn, TransactionType nextTrans) throws UserAbortException, SQLException {
        Class<? extends Procedure> procClass = nextTrans.getProcedureClass();

        if (procClass.equals(DeleteRecord.class)) {
            deleteRecord(conn);
        } else if (procClass.equals(InsertRecord.class)) {
            insertRecord(conn);
        } else if (procClass.equals(ReadModifyWriteRecord.class)) {
            readModifyWriteRecord(conn);
        } else if (procClass.equals(ReadRecord.class)) {
            readRecord(conn);
        } else if (procClass.equals(ScanRecord.class)) {
            scanRecord(conn);
        } else if (procClass.equals(UpdateRecord.class)) {
            updateRecord(conn);
        } else if (procClass.equals(ReadWriteRecord.class)) {
            readWriteRecord(conn);
        }
        return (TransactionStatus.SUCCESS);
    }

    private void updateRecord(Connection conn) throws SQLException {

        int keyname = chooseUsefulKey(readRecord.nextInt());
        this.buildParameters();
        this.procUpdateRecord.run(conn, keyname, this.params);
    }

    private int chooseUsefulKey(int key) {
        if (key % 2 == 1) {
            key = key - 1;
        }
        if (keys[key] > 0) {
            return key;
        } else {
            for (int j = key; j >= 0; j -= 2) {
                if (keys[j] > 0) {
                    return j;
                }
            }

            return 0;
        }
    }

    private void scanRecord(Connection conn) throws SQLException {

        int keyname = readRecord.nextInt();
        int count = randScan.nextInt();
        this.procScanRecord.run(conn, keyname, count, new ArrayList<>());
    }

    private void readRecord(Connection conn) throws SQLException {

        int keyname = chooseUsefulKey(readRecord.nextInt());
        this.procReadRecord.run(conn, keyname, this.results);
    }

    private void readModifyWriteRecord(Connection conn) throws SQLException {

        int keyname = readRecord.nextInt();
        this.buildParameters();
        this.procReadModifyWriteRecord.run(conn, keyname, this.params, this.results);
    }

    private void insertRecord(Connection conn) throws SQLException {

        int keyname = insertRecord.nextInt();
        this.buildParameters();
        this.procInsertRecord.run(conn, keyname, this.params);
    }

    private void deleteRecord(Connection conn) throws SQLException {

        int keyname = readRecord.nextInt();
        this.procDeleteRecord.run(conn, keyname);
    }

    private void readWriteRecord(Connection conn) throws SQLException {
        int[] keyNames = new int[totalRequest];
        boolean flag;
        boolean is_distribute = Math.random() < disRatio;
        boolean is_new_node;
        int node_number = 0;
        for (int i = 0; i < totalRequest; i++) {
            while (true) {
                int keyname = readRecord.nextInt();
                flag = false;
                is_new_node = true;
                for (int j = 0; j < i; j++) {
                    if (keyname == keyNames[j]) {
                        flag = true;
                        break;
                    }
                    if (keyname % nodeCnt != keyNames[j]) {
                        is_new_node = false;
                    }
                }

                // if new node
                if (i > 0) {
                    if (is_distribute && node_number <= 1) {
                        if (!is_new_node)
                            continue;
                    }
                }

                if (!flag) {
                    keyNames[i] = keyname;
                    if (is_new_node)
                        node_number++;
                    keys[keyname]++;
                    break;
                }
            }
        }

        this.procReadWriteRecord.run(conn, keyNames, fixParams, ratio1, ratio2);

        for (int i = 0; i < totalRequest; i++) {
            keys[keyNames[i]]--;
        }
    }

    private void buildParameters() {
        for (int i = 0; i < this.params.length; i++) {
            this.params[i] = new String(TextGenerator.randomFastChars(rng(), this.data));
            this.params[i] = this.params[i].replaceAll(";", "!");
            this.params[i] = this.params[i].replaceAll("\"", ".");
            this.params[i] = this.params[i].replaceAll("'", "~");
        }
    }
}
