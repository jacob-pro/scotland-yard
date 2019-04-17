package uk.ac.bris.cs.scotlandyard.ui.controller;

import javafx.util.Duration;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.PassMove;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;


class MLGBoardPlayers {

	private static final String NOTIFY_TIMEOUT = "notify_timeout";
	private static final String NOTIFY_MOVE = "notify_move";

	public static abstract class MLGBoardPlayer implements Board.BoardPlayer {

		protected Colour colour;

		abstract public void makeMoveReplacementHack(Notifications notifications, Instant deadline);

		void showTimeoutNotification(Notifications notifications, Instant deadline) {

			if (deadline == null) return;

			java.time.Duration timeDur = java.time.Duration.between(Instant.now(), deadline);
			Duration fxDur = new Duration(timeDur.toMillis());

			Notifications.NotificationBuilder.Notification timed = new Notifications.NotificationBuilder("Turn Timer").create(fxDur, () -> {});
			notifications.show(NOTIFY_TIMEOUT, timed);
		}

	}

	public static class ThisPlayer extends MLGBoardPlayer {

		ThisPlayer(Colour colour) {
			this.colour = colour;
		}

		private Board.HintedBoard board;
		private int location;
		private Set<Move> moves;
		private Consumer<Move> callback;

		@Override
		public void makeMove(Board.HintedBoard board, ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			this.board = board;
			this.location = location;
			this.moves = moves;
			this.callback = callback;
		}

		@Override
		public void makeMoveReplacementHack(Notifications notifications, Instant deadline) {

			notifications.dismissAll();

			//If move is pass do it automatically
			if (moves.size() == 1 && moves.iterator().next() instanceof PassMove) {
				callback.accept(moves.iterator().next());
				return;
			}

			this.showTimeoutNotification(notifications, deadline);

			Notifications.NotificationBuilder.Notification notifyMove = new Notifications.NotificationBuilder(
					"Its your turn. Please pick a move")
					.addAction("Scroll to player",
							() -> board.scrollToLocation(location))
					.create();
			board.notifications().show(NOTIFY_MOVE, notifyMove);
			board.showMoveHints(moves, move -> {
				board.hideMoveHints();
				board.notifications().dismiss(NOTIFY_TIMEOUT);
				board.notifications().dismiss(NOTIFY_MOVE);
				callback.accept(move);
			});

		}
	}

	public static class RemotePlayer extends MLGBoardPlayer {

		private String name;

		RemotePlayer(Colour colour, String name) {
			this.colour = colour;
			this.name = name;
		}

		@Override
		public void makeMove(Board.HintedBoard board, ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			//This can never be called, because the Board does not support our unconventional Player
		}

		@Override
		public void makeMoveReplacementHack(Notifications notifications, Instant deadline) {

			notifications.dismissAll();

			this.showTimeoutNotification(notifications, deadline);

			Notifications.NotificationBuilder.Notification notifyMove = new Notifications.NotificationBuilder(
					String.format("Waiting for %s's turn ", this.name)).create();
			notifications.show(NOTIFY_MOVE, notifyMove);

		}
	}
}
