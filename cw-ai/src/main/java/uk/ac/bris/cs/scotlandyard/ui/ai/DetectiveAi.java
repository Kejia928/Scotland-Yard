package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DetectiveAi implements Ai {

    private Set<Integer> getMrxLocationRange(Board board) {
        List<LogEntry> log = board.getMrXTravelLog();
        List<Boolean> round = board.getSetup().rounds;
        Set<Integer> MrxRange = new HashSet<>(board.getSetup().graph.nodes());
        List<Integer> rounds = new ArrayList<>();
        //get round index when round is true
        for(int i = 0; i < round.size(); i++) {
            if(round.get(i)) {
                rounds.add(i);
            }
        }
        //from high to low
        rounds = rounds.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        //when round is true, the location can be known
        if(round.get(log.size() - 1) && log.get(log.size() - 1).location().isPresent()) {
            MrxRange.clear();
            MrxRange.add(log.get(log.size() - 1).location().get());
            return MrxRange;
        } else {
            //we need to find the close the log which is hold the location
            for(int r : rounds) {
                if(log.size() > r && log.get(r).location().isPresent()) {
                    MrxRange.clear();
                    MrxRange.add(log.get(r).location().get());
                    for(int j = r + 1; j < log.size(); j++) {
                        Set<Integer> locations = new HashSet<>();
                        for(int point : MrxRange) {
                            //loop the adjacent node of last location in log
                            for(int eachNode : board.getSetup().graph.adjacentNodes(point)) {
                                //based on which ticket used to find all possible location
                                for(ScotlandYard.Transport t : Objects.requireNonNull(board.getSetup().graph.edgeValueOrDefault(point, eachNode, ImmutableSet.of()))) {
                                    if(t.requiredTicket() == log.get(j).ticket()) {
                                        locations.add(eachNode);
                                    }
                                }
                            }
                        }
                        MrxRange.clear();
                        MrxRange.addAll(locations);
                    }
                    //direct return do not continue loop
                    return MrxRange;
                }
            }
        }
        return MrxRange;
    }

    private void scoring(MoveNode move, GraphNode graphNode, Set<Integer> mrxRange) {
        int distance = 0;
        for(int eachNode : mrxRange) {
            distance = distance + graphNode.distance(move.getLocation(), eachNode);
        }
        move.setScore(distance);
    }

    @Nonnull
    @Override
    public String name() { return "Detective AI"; }

    @Nonnull @Override
    public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        GraphNode graphNode = new GraphNode(board);
        var MrxRange = getMrxLocationRange(board);

        var moves = board.getAvailableMoves().asList();
        List<MoveNode> allMoveNodes = new ArrayList<>();

        //put move in to a moveNode object
        for(Move eachMove : moves) {
            MoveNode eachMoveNode = new MoveNode(eachMove, board);
            allMoveNodes.add(eachMoveNode);
        }

        for(MoveNode eachNode : allMoveNodes) {
            scoring(eachNode, graphNode, MrxRange);
        }

        int lowScore = 1;
        if(allMoveNodes.stream().map(MoveNode::getScore).collect(Collectors.toList()).stream().min(Integer::compare).isPresent()) {
            lowScore = allMoveNodes.stream().map(MoveNode::getScore).collect(Collectors.toList())
                    .stream().min(Integer::compare).get();
        }
        int lowestScore = lowScore;
        List<MoveNode> matchMove = allMoveNodes.stream().filter(moveNode -> moveNode.getScore() == lowestScore).collect(Collectors.toList());
        return matchMove.get(new Random().nextInt(matchMove.size())).getMove();

    }

}
