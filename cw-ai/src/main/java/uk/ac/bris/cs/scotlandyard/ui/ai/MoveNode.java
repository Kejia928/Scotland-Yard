package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MoveNode {

    private final int location;
    private int score;
    private final Set<Integer> linkedNode;
    private int security;
    private Move move;
    private final List<ScotlandYard.Ticket> ticketList = new ArrayList<>();


    public MoveNode(Move move, Board board) {
        this.move = move;
        this.score = 0;
        this.security = 1;
        this.location = move.visit(new Move.Visitor<>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }

            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.destination2;
            }
        });
        this.linkedNode = board.getSetup().graph.adjacentNodes(location);
        for(ScotlandYard.Ticket ticket : move.tickets()) {
            this.ticketList.add(ticket);
        }
    }

    public Move getMove() {
        return move;
    }

    public int getLocation() {
        return location;
    }

    public Set<Integer> getLinkedNodes() {
        return linkedNode;
    }

    public void setScore(int Score) {
        this.score = this.score + Score;
    }

    public int getScore() {
        return score;
    }

    public void setSecurity(int Security) {
        this.security = Security;
    }

    public int getSecurity() {
        return security;
    }

    public List<ScotlandYard.Ticket> getTicketList() {
        return ticketList;
    }

    public void setMove(Move newMove) {
        this.move = newMove;
    }


}
