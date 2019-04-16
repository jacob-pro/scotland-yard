package uk.ac.bris.cs.scotlandyard.ui.controller;

import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.Set;
import java.util.function.Consumer;
import java.util.Date;


public class MLGBoardPlayers {

	public static abstract class MLGBoardPlayer implements Board.BoardPlayer {

		protected Colour colour;

		abstract public void showNotification(Date deadline);

	}

	public static class ThisPlayer extends MLGBoardPlayer {

		private static final String NOTIFY_TIMEOUT = "notify_timeout";
		private static final String NOTIFY_MOVE = "notify_move";

		public ThisPlayer(Colour colour) {
			this.colour = colour;
		}

		@Override
		public void makeMove(Board.HintedBoard board, ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {

			board.notifications().dismissAll();

			//If move is pass do it automatically
			if (moves.size() == 1 && moves.iterator().next() instanceof PassMove) {
				callback.accept(moves.iterator().next());
				return;
			}

			showNotificationAndAsk(location, moves, board, callback);
		}

		private void showNotificationAndAsk(
				int location,
				Set<Move> moves,
				Board.HintedBoard board,
				Consumer<Move> consumer) {
			Notifications.NotificationBuilder.Notification notifyMove = new Notifications.NotificationBuilder(
					"Its your turn, Please pick a move")
					.addAction("Scroll to player",
							() -> board.scrollToLocation(location))
					.create();
			board.notifications().show(NOTIFY_MOVE, notifyMove);
			board.showMoveHints(moves, move -> {
				board.hideMoveHints();
				board.notifications().dismiss(NOTIFY_TIMEOUT);
				board.notifications().dismiss(NOTIFY_MOVE);
				consumer.accept(move);
			});
		}

		@Override
		public void showNotification(Date deadline) {

		}
	}

	public static class RemotePlayer extends MLGBoardPlayer {

		private final String name;

		public RemotePlayer(Colour colour, String name) {
			this.colour = colour;
			this.name = name;
		}

		@Override
		public void makeMove(Board.HintedBoard board, ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {
			//This will never be called
		}

		@Override
		public void showNotification(Date deadline) {

		}
	}
}
