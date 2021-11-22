package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class GraphNode {

    private final int[][] graph;

    public GraphNode(Board board) {

        GameSetup setup = board.getSetup();

        var graph = setup.graph;

        //Dijkstra Algorithm is used to get whole graph
        int [][] graphNodes = new int [graph.nodes().size() + 1][graph.nodes().size() + 1];
        for(int node : graph.nodes()) {
            Set<Integer> alreadyLinked = new HashSet<>();
            Set<Integer> restNodes = new HashSet<>(graph.nodes());
            Set<Integer> linkedNodes = new HashSet<>();
            //point to itself is zero
            graphNodes[node][node] = 0;
            restNodes.remove(node);
            alreadyLinked.add(node);
            for(int dis = 1; restNodes.size() != 0; dis++) {
                for(int eachNode : alreadyLinked) {
                    linkedNodes.addAll(graph.adjacentNodes(eachNode)) ;
                }
                alreadyLinked.clear();
                for(int thisNode : linkedNodes) {
                    if(graphNodes[node][thisNode] == 0) {
                        graphNodes[node][thisNode] = dis;
                    } else if(graphNodes[node][thisNode] > dis) {
                        graphNodes[node][thisNode] = dis;
                    }
                    alreadyLinked.add(thisNode);
                    restNodes.remove(thisNode);
                }
                linkedNodes.clear();
            }
        }
        this.graph = graphNodes;
    }

    //shortest distance between two point
    public int distance(int point1, int point2) {
        return graph[point1][point2];
    }

    //will used in GamePlayer
    public ImmutableMap<ScotlandYard.Ticket, Integer> getTicket(Board board, Piece piece) {
        var ticketType = ScotlandYard.Ticket.values();
        Map<ScotlandYard.Ticket, Integer> ticket = new HashMap<>();
        for(ScotlandYard.Ticket t : ticketType) {
            if(board.getPlayerTickets(piece).isPresent()) {
                ticket.put(t, board.getPlayerTickets(piece).get().getCount(t));
            }
        }
        return ImmutableMap.copyOf(ticket);
    }


}



