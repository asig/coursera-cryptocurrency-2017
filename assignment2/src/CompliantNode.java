import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private final double pGraph;
    private final double pMalicious;
    private final double pTxDistribution;
    private final int numRounds;

    private int curRound;

    private Set<Integer> followees = new HashSet<>();
    private Set<Transaction> pendingTransactions;
    private Map<Transaction, Integer> seenCount;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.pGraph = p_graph;
        this.pMalicious = p_malicious;
        this.pTxDistribution = p_txDistribution;
        this.numRounds = numRounds;
        seenCount = new HashMap<>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees.clear();
        for (int i = 0; i < followees.length; i++) {
            if (followees[i]) {
                this.followees.add(i);
            }
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = new HashSet<>();
        this.pendingTransactions.addAll(pendingTransactions);
        for (Transaction tx : pendingTransactions) {
            seenCount.put(tx, 1);
        }
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> forward = new HashSet<>();

        int threshold = 1;
        curRound++;
        if (curRound == numRounds) {
            threshold = 1;
        }
        if (curRound > numRounds/3) {
            threshold = (int)(curRound * .3);
        }

        // Only send transactions that were seen above threshold
        for (Map.Entry<Transaction, Integer> e : seenCount.entrySet()) {
            if (e.getValue() >= threshold ) {
                forward.add(e.getKey());
            }
        }
        return forward;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        Set<Transaction> combined = new HashSet<>();
        for (Candidate c : candidates) {
            combined.add(c.tx);
        }
        for (Transaction tx : combined) {
            pendingTransactions.add(tx);
            seenCount.put(tx, seenCount.getOrDefault(tx, 0) + 1);
        }

    }
}
