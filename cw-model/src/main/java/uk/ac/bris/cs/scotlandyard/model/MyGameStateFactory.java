package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	//-------------------------------------------------------------------------------------------//
	//get all players (this.everyone)
	private ImmutableList<Player> getAllPlayer(List<Player> detectives, Player mrX) {
		List<Player> allPlayer = new ArrayList<>(detectives);
		allPlayer.add(mrX);
		return ImmutableList.copyOf(allPlayer);
	}

	//-------------------------------------------------------------------------------------------//
	//getAvailableMove

	//get invalid destination
	private List<Integer> getInvalidDestination(List<Player> detectives) {
		return detectives.stream().map(Player::location).collect(Collectors.toList());
	}

	//Single move function for every player, will use it in getAvailableMoves
	private ImmutableSet<SingleMove> makeSingleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source) {
		final var singleMoves = new ArrayList<SingleMove>();
		for(int destination : setup.graph.adjacentNodes(source)) {
			// if the location is occupied, don't add to the list of moves to return
			if(!getInvalidDestination(detectives).contains(destination)) {
				for(Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
					// if it does, construct SingleMove and add it the list of moves to return
					if(player.has(t.requiredTicket())) {
						singleMoves.add(new SingleMove(player.piece(), player.location(), t.requiredTicket(), destination));
					}
				}
				// add moves to the destination via a secret ticket
				if(player.has(Ticket.SECRET)) {
					singleMoves.add(new SingleMove(player.piece(), player.location(), Ticket.SECRET, destination));
				}
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	//Double move function, will use it in getAvailableMoves
	private ImmutableSet<DoubleMove> makeDoubleMoves(
			GameSetup setup,
			List<Player> detectives,
			Player player,
			int source) {
		final var doubleMoves = new ArrayList<DoubleMove>();
		//find the first step list
		for(SingleMove firstStep : makeSingleMoves(setup, detectives, player, source)) {
			Player player1 = player.use(firstStep.ticket);
			//find the second step by using first step destination as source
			for(SingleMove secondStep : makeSingleMoves(setup, detectives, player1, firstStep.destination)) {
				doubleMoves.add(new DoubleMove(player.piece(), source, firstStep.ticket, firstStep.destination, secondStep.ticket, secondStep.destination));
			}
		}
		return ImmutableSet.copyOf(doubleMoves);
	}

	//make moves
	private ImmutableSet<Move> makeMoves(GameSetup setup, List<Player> detectives, ImmutableSet<Piece> remaining, ImmutableList<Player> everyone, ImmutableList<LogEntry> log, ImmutableSet<Piece> winner) {
		Set<Move> moves = new HashSet<>();
		for(Piece current : remaining) {
			for(Player player : everyone) {
				//find who is current player
				if(player.piece().webColour().equals(current.webColour())) {
					if(player.isMrX() && isNotGameOver(winner)) {
						//if round add 2 than still smaller than round.size(), it can do doubleMove
						if(player.has(Ticket.DOUBLE) && (log.size() + 2 <= setup.rounds.size())) {
							moves.addAll(makeDoubleMoves(setup, detectives, player, player.location()));
						}
						moves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
					}
					if(player.isDetective() && isNotGameOver(winner)) {
						moves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
					}
				}
			}
		}
		return ImmutableSet.copyOf(moves);
	}

	//-------------------------------------------------------------------------------------------//
	//getWinner

	//All round finished
	private Boolean allRoundFinished(GameSetup setup, ImmutableList<LogEntry> log, ImmutableSet<Piece> remaining, Player mrX) {
		return ((log.size() == setup.rounds.size()) && (remaining.contains(mrX.piece())));
	}

	//Detectives are stuck : no available move
	private Boolean DetectiveAreStuck (GameSetup setup, List<Player> detectives) {
		Set<Move> validMove = new HashSet<>();
		for(Player nextPlayer : detectives) {
			if(nextPlayer.isDetective()) {
				validMove.addAll(makeSingleMoves(setup, detectives, nextPlayer, nextPlayer.location()));
			}
		}
		return validMove.isEmpty();
	}

	//Mrx is captured : Game over when detectives moves to the same location with mrX, winner are detectives
	private Boolean MrxIsCaptured(List<Player> detectives, Player mrX, ImmutableSet<Piece> remaining, ImmutableList<Player> everyone) {
		List<Player> allRemaining = everyone.stream().filter(player -> remaining.contains(player.piece())).collect(Collectors.toList());
		//check last player
		if (allRemaining.stream().filter(Player::isDetective).count() == allRemaining.size()) {
			List<Player> alreadyMove = new ArrayList<>(detectives);
			alreadyMove.removeAll(allRemaining);
			//anyMatch means if there is a player at same location with mrX,than return true.
			return alreadyMove.stream().anyMatch(player -> player.location() == mrX.location());
		}
		//when remaining is mrX, we need to check last detectives is or not captured mrX
		else if(allRemaining.contains(mrX)) {
			return detectives.stream().anyMatch(player -> player.location() == mrX.location());
		}
		return false;
	}

	//Mrx is stuck : no available move
	private Boolean MrxIsStuck (GameSetup setup, ImmutableSet<Piece> remaining ,List<Player> detectives, Player mrX) {
		if(remaining.contains(mrX.piece())) {
			return (makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty());
		}
		return false;
	}

	//who is winner
	private ImmutableSet<Piece> whoIsWinner(GameSetup setup, List<Player> detectives, Player mrX, ImmutableList<LogEntry> log, ImmutableSet<Piece> remaining, ImmutableList<Player> everyone) {
		//Mrx is winner
		if((log.size() == 0 && DetectiveAreStuck(setup, detectives)) || DetectiveAreStuck(setup, detectives) || allRoundFinished(setup, log, remaining, mrX)) {
			return ImmutableSet.copyOf(Collections.singleton(mrX.piece()));//return a set which is only include one object
		}
		//Detectives are winner
		else if(MrxIsCaptured(detectives, mrX, remaining, everyone) || MrxIsStuck(setup, remaining, detectives, mrX)) {
			return ImmutableSet.copyOf(detectives.stream().map(Player::piece).collect(Collectors.toSet()));
		}
		//There are no winners yet
		else return ImmutableSet.copyOf(Collections.emptySet());//return a empty set
	}

	//when is GameOver
	private boolean isNotGameOver(ImmutableSet<Piece> winner) {
		return winner.isEmpty();
	}

	//-------------------------------------------------------------------------------------------//

	private final class MyGameState implements GameState {

		//Attributes
		private final GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private final ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private final ImmutableList<Player> everyone;
		private final ImmutableSet<Move> moves;
		private final ImmutableSet<Piece> winner;

		//Constructor
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.everyone = getAllPlayer(detectives, mrX);
			this.winner = whoIsWinner(setup, detectives, mrX, log, remaining, everyone);
			this.moves = makeMoves(setup, detectives, remaining, everyone, log, winner);

			//Check
			//check detective list are empty
			if(detectives.isEmpty()) { throw new NullPointerException("Detectives are null!"); }
			//check mrx is null
			if(mrX == null) { throw new NullPointerException("Mrx is null!"); }
			//check there is no mrX
			if(!everyone.contains(mrX)) { throw new IllegalArgumentException("No MrX!"); }
			//check mrx and detective are swapped
			if(mrX.isDetective()) { throw new IllegalArgumentException("Mrx and detective are swapped!"); }
			//check there are repeat players
			if(everyone.size() != new HashSet<>(everyone).size()) { throw new IllegalArgumentException("Repeat players"); }

			//detective check
			for(Player thisDetective : detectives){
				//check detective is empty
				if(thisDetective == null) { throw new NullPointerException("Any detective is null!"); }
				//check detective have Double ticket should throw
				if(thisDetective.has(Ticket.DOUBLE)){ throw new IllegalArgumentException("Detectives has Double ticket!"); }
				//check detective have Secret ticket should throw
				if(thisDetective.has(Ticket.SECRET)){ throw new IllegalArgumentException("Detectives has Secret ticket!"); }

				//check location
				List<Player> remainingPlayers = new ArrayList<>(detectives);
				remainingPlayers.remove(thisDetective);
				for(Player otherDetectives : remainingPlayers){
					//check two detectives are duplicate
					if(thisDetective == otherDetectives) { throw new IllegalArgumentException("There are duplicate detectives!"); }
					//check two detectives at same location
					if(thisDetective.location() == otherDetectives.location()){ throw new IllegalArgumentException("Two detectives in same location!"); }
				}
			}

			//check empty round should throw
			if(setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
			//check empty graph should throw
			if(setup.graph == null) throw new IllegalArgumentException("Graph is empty!");
		}

		//Function
		@Nonnull @Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getPlayers() {
			var allPiece = everyone.stream().map(Player::piece).distinct().collect(Collectors.toList());
			return ImmutableSet.copyOf(allPiece);
		}

		@Nonnull @Override
		//Optional means maybe returned object is null, which will avoid NullPointerException problem
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			return detectives.stream()
					.filter(eachDetective -> eachDetective.piece().webColour().equals(detective.webColour()))
					.map(Player::location).limit(1).findAny();
		}

		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for(Player eachPlayer : everyone) {
				if(eachPlayer.piece().webColour().equals(piece.webColour())) {
					Map<Ticket, Integer> allTicket = eachPlayer.tickets();
					TicketBoard ticketBoard = allTicket::get;//method references (ticket -> allTicket.get(ticket))
					return Optional.of(ticketBoard);
				}
			}
			return Optional.empty();
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		@Nonnull @Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			List<LogEntry> allLog = new ArrayList<>(log);
			List<Player> newDetectives = new ArrayList<>(detectives);
			Set<Piece> remainingDetectives = new HashSet<>(remaining);
			for (Player eachPlayer : everyone) {
				//find which player is moved
				if(eachPlayer.piece().webColour().equals(move.commencedBy().webColour())) {

					//-------------------------------------------------------------------------------------------//
					//update the ticket
					eachPlayer = eachPlayer.use(move.tickets());
					//give ticket after detective use
					if(eachPlayer.isDetective()) {
						mrX = mrX.give(move.tickets());
					}

					//-------------------------------------------------------------------------------------------//
					//get the newLocation
					int newLocation = move.visit(new Visitor<>() {
						@Override
						public Integer visit(SingleMove singleMove) {
							return singleMove.destination;
						}

						@Override
						public Integer visit(DoubleMove doubleMove) {
							return doubleMove.destination2;
						}
					});
					//update the player location
					eachPlayer = eachPlayer.at(newLocation);

					//-------------------------------------------------------------------------------------------//
					//update the mrX
					if(eachPlayer.isMrX()) {
						mrX = eachPlayer;
					}
					//update detectives
					if(eachPlayer.isDetective()) {
						for(Player eachDetective : detectives) {
							if(eachDetective.piece().webColour().equals(eachPlayer.piece().webColour())) {
								newDetectives.remove(eachDetective);
								newDetectives.add(eachPlayer);
								detectives = ImmutableList.copyOf(newDetectives);
								remainingDetectives.remove(eachDetective.piece());
							}
						}
					}
				}
			}

			//-------------------------------------------------------------------------------------------//
			//get log
			if(move.commencedBy().isMrX()) {
				allLog.addAll(move.visit(new Visitor<List<LogEntry>>(){
					@Override public List<LogEntry> visit(SingleMove singleMove){
						List<LogEntry> newLog1 = new ArrayList<>();
						if(setup.rounds.get(log.size())) {
							newLog1.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
						} else {
							newLog1.add(LogEntry.hidden(singleMove.ticket));
						}
						return newLog1;
					}

					@Override public List<LogEntry> visit(DoubleMove doubleMove){
						List<LogEntry> newLog2 = new ArrayList<>();
						if(setup.rounds.get(log.size())) {
							newLog2.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
						} else  {
							newLog2.add(LogEntry.hidden(doubleMove.ticket1));
						}
						if(setup.rounds.get(log.size() + 1 )) {
							newLog2.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
						} else {
							newLog2.add(LogEntry.hidden(doubleMove.ticket2));
						}
						return newLog2;
					}
				}));
			}

			//-------------------------------------------------------------------------------------------//
			//update remaining
			if(move.commencedBy().isMrX()) {
				Set<Piece> allDetectivePiece = new HashSet<>();
				for(Player everyDetective : detectives) {
					allDetectivePiece.add(everyDetective.piece());
				}
				remaining = ImmutableSet.copyOf(allDetectivePiece);
			}
			if(move.commencedBy().isDetective()){
				List<Player> fromPieceToPlayer = everyone.stream().filter(player -> remainingDetectives.contains(player.piece())).collect(Collectors.toList());
				if((remaining.size() <= 1) || DetectiveAreStuck(setup, fromPieceToPlayer)) {
					Set<Piece> mrxPiece = new HashSet<>();
					mrxPiece.add(mrX.piece());
					remaining = ImmutableSet.copyOf(mrxPiece);
				} else {
					remaining = ImmutableSet.copyOf(remainingDetectives);
				}
			}

			//return a new GameState
			return new MyGameState(setup, remaining, ImmutableList.copyOf(allLog), mrX, detectives);
		}
	}

	@Nonnull @Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}
