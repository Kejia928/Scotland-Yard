package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MyAi implements Ai {

	//get all detective piece
	private Set<Piece.Detective> getDetectives(Board board) {
		return board.getPlayers().stream().filter(Piece::isDetective)
				.map(piece -> (Piece.Detective)piece).collect(Collectors.toSet());
	}

	//get detective linked node
	private Set<Integer> getDetectiveLinkedNodes(Board board) {
		Set<Piece.Detective> detectives = getDetectives(board);
		Set<Integer> detectiveLinkedNodes = new HashSet<>();
		for(Piece.Detective eachDetective : detectives) {
			if(board.getDetectiveLocation(eachDetective).isPresent()) {
				detectiveLinkedNodes.addAll(board.getSetup().graph.adjacentNodes(board.getDetectiveLocation(eachDetective).get()));
			}
		}
		return detectiveLinkedNodes;
	}

	private void scoring(MoveNode move, GraphNode graphNode, Board board) {
		//distance to all detective
		int distance = 0;
		for (Piece.Detective eachDetective : getDetectives(board)) {
			if(board.getDetectiveLocation(eachDetective).isPresent()) {
				distance = distance + graphNode.distance(move.getLocation(), board.getDetectiveLocation(eachDetective).get());
			}
		}

		//ticket score
		int ticketScore = 50;
		ticketScore = ticketScore - (move.getTicketList().size() * 10);
		if(move.getTicketList().contains(ScotlandYard.Ticket.SECRET)) {
			ticketScore = ticketScore - 10;
		}

		//if the move target location is in the detective move range, set security to zero
		if(getDetectiveLinkedNodes(board).contains(move.getLocation())) {
			move.setSecurity(0);
		}

		//if security is 1, the score is distance plus linking node that is mean freedom
		//but if the target location in the detective move range(security = 0), the score is zero, which is worst move
		move.setScore((distance + move.getLinkedNodes().size() + ticketScore) * move.getSecurity());

	}

	private MoveNode minimax(Board.GameState gameState, Move lastMove, GraphNode graphNode, int depth, Boolean max, int alpha, int beta, int numD) {
		if(depth == 0 || !gameState.getWinner().isEmpty()) {
			MoveNode node = new MoveNode(lastMove, gameState);
			scoring(node, graphNode, gameState);
			return node;
		}

		if(max) {
			//maximize
			int maxScore = -99999;
			MoveNode maxMove = null;
			for(Move move : gameState.getAvailableMoves()) {
				Board.GameState state = gameState.advance(move);
				MoveNode child = minimax(state, move, graphNode, depth - 1, false, alpha, beta, 0);
				if(child.getScore() > maxScore) {
					maxScore = child.getScore();
					maxMove = child;
				}

				//alpha - beta pruning
				if(alpha < child.getScore()) {
					alpha = child.getScore();
				}
				//pruning the unnecessary branch
				if(alpha >= beta) {
					break;
				}
			}
			return maxMove;

		} else {
			//minimize
			int minScore = 99999;
			MoveNode minMove = null;
			var detectiveSize = gameState.getPlayers().stream().filter(Piece::isDetective).collect(Collectors.toSet()).size();
			//when the last detective moved
			if(numD == detectiveSize - 1) {
				//back to maximize level
				for(Move move : gameState.getAvailableMoves()) {
					Board.GameState state = gameState.advance(move);
					MoveNode child = minimax(state, move, graphNode, depth - 1, true, alpha, beta, 0);
					if(child.getScore() < minScore) {
						minScore = child.getScore();
						minMove = child;
					}
					//alpha - beta pruning
					if(beta > child.getScore()) {
						beta = child.getScore();
					}
					//pruning the unnecessary branch
					if(alpha >= beta) {
						break;
					}
				}
			} else {
				//still in mini, because all detectives are not finished moves
				for(Move move : gameState.getAvailableMoves()) {
					Board.GameState state = gameState.advance(move);
					MoveNode child = minimax(state, move, graphNode, depth, false, alpha, beta, numD + 1);
					if(child.getScore() < minScore) {
						minScore = child.getScore();
						minMove = child;
					}
					//alpha - beta pruning
					if(beta > child.getScore()) {
						beta = child.getScore();
					}
					//pruning the unnecessary branch
					if(alpha >= beta) {
						break;
					}
				}
			}
			return minMove;
		}
	}

	@Nonnull
	@Override
	public String name() { return "Mrx AI"; }

	@Nonnull @Override
	public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		GraphNode graphNode = new GraphNode(board);
		GamePlayer gamePlayer = new GamePlayer(board, moves.get(0).source(), graphNode);
		MyGameStateFactory state = new MyGameStateFactory();
		Board.GameState gameState = state.build(board.getSetup(), gamePlayer.getMrx(), ImmutableList.copyOf(gamePlayer.getDetectives()));
		return minimax(gameState, null, graphNode, 1, true, -99999, +99999, 0).getMove();
	}
}