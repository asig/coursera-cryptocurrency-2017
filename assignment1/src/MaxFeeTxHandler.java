// Copyright 2017 Andreas Signer. All rights reserved.

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double inputSum = 0;
        double outputSum = 0;

        Set<UTXO> seenUtxo = new HashSet<>();

        for (int i = 0; i < tx.getInputs().size(); i++) {
            Transaction.Input input = tx.getInputs().get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            //  (1)
            if (!utxoPool.contains(utxo)) {
                return false;
            }

            // (2)
            Transaction.Output out = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            // (3)
            if (seenUtxo.contains(utxo)) {
                return false;
            }
            seenUtxo.add(utxo);
            inputSum += out.value;
        }

        for (int i = 0; i < tx.getOutputs().size(); i++) {
            Transaction.Output out = tx.getOutput(i);
            // (4)
            if (out.value < 0) {
                return false;
            }
            outputSum += out.value;
        }

        // (5)
        if (inputSum < outputSum) {
            return false;
        }

        return true;
    }

    public double getFee(Transaction tx) {
        try {
            double inputSum = 0;
            double outputSum = 0;
            for (int i = 0; i < tx.getInputs().size(); i++) {
                Transaction.Input input = tx.getInputs().get(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                Transaction.Output out = utxoPool.getTxOutput(utxo);
                inputSum += out.value;
            }

            for (int i = 0; i < tx.getOutputs().size(); i++) {
                Transaction.Output out = tx.getOutput(i);
                outputSum += out.value;
            }
            return Math.max(inputSum - outputSum, 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    private void dumpTransactions(String title, List<Transaction> trx) {
        System.err.println("--------------------------------------------");
        System.err.println(title);
        for (int i = 0; i < trx.size(); i++) {
            System.err.println("trx[" + i + "]: valid = " + isValidTx(trx.get(i)) + "; fee = " + getFee(trx.get(i)));
        }
        System.err.println("--------------------------------------------");
    }

    private int findBest(List<Transaction> trx) {
        double maxFee = -Double.MAX_VALUE;
        int maxIdx = -1;
        for (int i = 0; i < trx.size(); i++) {
            Transaction t = trx.get(i);
            if (isValidTx(t)) {
                double fee = getFee(t);
                if (fee > maxFee) {
                    maxFee = fee;
                    maxIdx = i;
                }
            }
        }
        return maxIdx;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        List<Transaction> trx = new ArrayList<>();
        Collections.addAll(trx, possibleTxs);

        List<Transaction> res = new ArrayList<>();
        for (;;) {
            // Find the most rewarding valid trx
            int idx = findBest(trx);
            if (idx < 0) {
                break;
            }
            Transaction tx = trx.remove(idx);

            if (isValidTx(tx)) { // Always true
                res.add(tx);
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.getOutputs().size(); i++) {
                    Transaction.Output o = tx.getOutput(i);
                    utxoPool.addUTXO(new UTXO(tx.getHash(), i), o);
                }
            }
        }

        return res.toArray(new Transaction[res.size()]);
    }
}
