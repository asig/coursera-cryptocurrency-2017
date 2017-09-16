import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private final UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
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

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> res = new ArrayList<>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
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

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }
}
