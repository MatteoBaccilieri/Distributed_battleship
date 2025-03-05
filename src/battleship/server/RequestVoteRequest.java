package battleship.server;

import java.io.Serializable;

public class RequestVoteRequest implements Serializable{
    private int term;          // Candidate's term number
    private int candidateId;   // Unique identifier of the candidate requesting the vote

    public RequestVoteRequest(int term, int candidateId) {
        this.term = term;
        this.candidateId = candidateId;
    }

    public int getTerm() {
        return term;
    }

    public int getCandidateId() {
        return candidateId;
    }

    @Override
    public String toString() {
        return "RequestVoteRequest{" +
                "term=" + term +
                ", candidateId=" + candidateId +
                '}';
    }
}
