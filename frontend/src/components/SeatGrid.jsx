export default function SeatGrid({ seats, selectedSeats, onToggle }) {
  return (
    <div className="seat-grid">
      {seats.map((seat) => {
        const isSelected = selectedSeats.includes(seat.seatNumber);
        const isBooked = seat.isBooked;
        return (
          <button
            key={seat.id}
            type="button"
            className={`seat ${isBooked ? 'seat-booked' : isSelected ? 'seat-selected' : 'seat-available'}`}
            onClick={() => onToggle(seat.seatNumber, isBooked)}
            disabled={isBooked}
            aria-label={`Seat ${seat.seatNumber}`}
          >
            {seat.seatNumber}
          </button>
        );
      })}
    </div>
  );
}
