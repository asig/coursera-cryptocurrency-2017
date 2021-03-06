// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.HashMap;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private static class BlockNode {
        private final Block block;
        private final int height;
        private final UTXOPool utxoPool;

        public BlockNode(Block block, BlockNode parent, UTXOPool utxoPool) {
            this.block = block;
            this.height = parent == null ? 0 : parent.getHeight() + 1;
            this.utxoPool = utxoPool;
        }

        public Block getBlock() {
            return block;
        }

        public UTXOPool getUtxoPool() {
            return utxoPool;
        }

        public int getHeight() {
            return height;
        }
    }

    private final TransactionPool txPool;
    private final Map<byte[], BlockNode> blocks;

    private BlockNode maxHeightBlock;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseToUTXOPool(genesisBlock.getCoinbase(), utxoPool);
        TxHandler txHandler = new TxHandler(utxoPool);
        txHandler.handleTxs(genesisBlock.getTransactions().toArray(new Transaction[genesisBlock.getTransactions().size()]));

        BlockNode node = new BlockNode(genesisBlock, null, txHandler.getUTXOPool());

        txPool = new TransactionPool();
        blocks = new HashMap<>();
        blocks.put(node.getBlock().getHash(), node);
        maxHeightBlock = node;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightBlock.getBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightBlock.getUtxoPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] parentHash = block.getPrevBlockHash();
        if (parentHash == null) {
            return false;
        }

        BlockNode parentNode = blocks.get(parentHash);
        if (parentNode == null) {
            return false;
        }

        if (parentNode.getHeight() < maxHeightBlock.getHeight() - CUT_OFF_AGE) {
            return false;
        }

        UTXOPool newPool = new UTXOPool(parentNode.utxoPool);
        addCoinbaseToUTXOPool(block.getCoinbase(), newPool);
        TxHandler txHandler = new TxHandler(newPool);
        Transaction[] validTx = txHandler.handleTxs(block.getTransactions().toArray(new Transaction[block.getTransactions().size()]));
        if (validTx.length != block.getTransactions().size()) {
            return false;
        }

        for (Transaction tx : validTx) {
            txPool.removeTransaction(tx.getHash());
        }

        BlockNode node = new BlockNode(block, parentNode, txHandler.getUTXOPool());
        if (node.getHeight() > maxHeightBlock.getHeight()) {
            maxHeightBlock = node;
        }
        blocks.put(node.getBlock().getHash(), node);
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private void addCoinbaseToUTXOPool(Transaction coinbase, UTXOPool utxoPool) {
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
    }
}
