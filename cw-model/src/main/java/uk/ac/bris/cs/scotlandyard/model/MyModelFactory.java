package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

/**
 * cw-model
 * Stage 2: Complete this class
 */

public final class MyModelFactory implements Factory<Model> {

	private static final class MyModel implements Model {

		//Attributes
		private ImmutableSet<Observer> observers;
		private GameState gameState;

		//Constructor
		private MyModel(
				final GameSetup setup,
				final Player mrX,
				final ImmutableList<Player> detectives,
				final ImmutableSet<Observer> observers){

			this.observers = observers;
			MyGameStateFactory state = new MyGameStateFactory();
			this.gameState = state.build(setup, mrX, detectives);

		}


		@Nonnull @Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observer.hashCode() == 0) {throw new NullPointerException("Invalid observe");}
			Set<Observer> currentObserve = new HashSet<>(observers);
			if(currentObserve.contains(observer)) {throw new IllegalArgumentException("Same observe");}
			currentObserve.add(observer);
			observers = ImmutableSet.copyOf(currentObserve);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(observer.hashCode() == 0) {throw new NullPointerException("Invalid observe");}
			Set<Observer> registerObserve = new HashSet<>(observers);
			if(!registerObserve.contains(observer)) {throw new IllegalArgumentException("Can not remove");}
			registerObserve.remove(observer);
			observers = ImmutableSet.copyOf(registerObserve);
		}

		@Nonnull @Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			for(Observer eachObserve : observers) {
				if(!gameState.getWinner().isEmpty()) {
					eachObserve.onModelChanged(gameState, Observer.Event.GAME_OVER);
				} else {
					eachObserve.onModelChanged(gameState, Observer.Event.MOVE_MADE);
				}
			}
		}
	}

	@Nonnull @Override public Model build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives, ImmutableSet.of());
	}
}
