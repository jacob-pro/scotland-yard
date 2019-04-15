package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Ticket;

public class TicketRequest {
	public Colour colour;
	public Ticket ticket;

	public TicketRequest(Colour colour, Ticket ticket) {
		this.colour = colour;
		this.ticket = ticket;
	}
}
