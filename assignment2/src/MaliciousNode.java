import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class MaliciousNode implements Node {


    private final double pGraph;
    private final double pMalicious;
    private final double pTxDistribution;
    private final int numRounds;

    private int curRound;

    private Set<Integer> followees = new HashSet<>();
    private Set<Transaction> pendingTransactions;

    public MaliciousNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.pGraph = p_graph;
        this.pMalicious = p_malicious;
        this.pTxDistribution = p_txDistribution;
        this.numRounds = numRounds;
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
    }

    public Set<Transaction> sendToFollowers() {
        curRound++;
        if (curRound == numRounds - 1) {
            return pendingTransactions;
        }
        return new HashSet<>();
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
    }
}
