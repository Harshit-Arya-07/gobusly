export function getBusDisplayName(bus) {
  const busName = typeof bus?.busName === 'string' ? bus.busName.trim() : '';
  if (busName) {
    return busName;
  }

  return 'Unknown Bus';
}

export function getBusDisplayNumber(bus) {
  const busNumber = typeof bus?.busNumber === 'string' ? bus.busNumber.trim() : '';
  if (!busNumber) {
    return '';
  }

  const busName = typeof bus?.busName === 'string' ? bus.busName.trim() : '';
  if (busName && busName === busNumber) {
    return '';
  }

  return busNumber;
}