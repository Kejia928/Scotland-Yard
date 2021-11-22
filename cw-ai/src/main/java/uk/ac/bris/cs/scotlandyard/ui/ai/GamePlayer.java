package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GamePlayer {

    private Piece MrxPiece;
    private final Player Mrx;
    private final List<Player> detectives;

    public GamePlayer(Board board, int mrXLocation, GraphNode graphNode) {

        Set<Piece> temDetectivePiece = new HashSet<>();
        for(Piece piece : board.getPlayers()) {
            if(piece.isMrX()) {
                this.MrxPiece = piece;
            }
            if(piece.isDetective()) {
                temDetectivePiece.add(piece);
            }
        }

        this.Mrx = new Player(MrxPiece, graphNode.getTicket(board, MrxPiece), mrXLocation);

        List<Player> detectivePlayer = new ArrayList<>();
        for(Piece eachDetectivePiece : temDetectivePiece) {
            if(board.getDetectiveLocation((Piece.Detective) eachDetectivePiece).isPresent()) {
                Player detective = new Player(eachDetectivePiece, graphNode.getTicket(board, eachDetectivePiece), board.getDetectiveLocation((Piece.Detective) eachDetectivePiece).get());
                detectivePlayer.add(detective);
            }
        }
        this.detectives = detectivePlayer;

    }

    public Player getMrx() {
        return Mrx;
    }

    public List<Player> getDetectives() {
        return detectives;
    }


}

