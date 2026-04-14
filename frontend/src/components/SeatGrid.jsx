export default function SeatGrid({ seats, selectedSeats, onToggle }) {
  const rows = [];
  for (let i = 0; i < seats.length; i += 4) {
    rows.push(seats.slice(i, i + 4));
  }

  return (
    <div className="seat-layout-wrap">
      <div className="driver-strip">Driver</div>
      <div className="seat-layout-grid">
        {rows.map((row, rowIndex) => (
          <div key={`row-${rowIndex}`} className="seat-row">
            <div className="seat-row-side">
              {row.slice(0, 2).map((seat) => {
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

            <div className="seat-aisle"></div>

            <div className="seat-row-side">
              {row.slice(2, 4).map((seat) => {
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
          </div>
        ))}
      </div>
    </div>
  );
}
