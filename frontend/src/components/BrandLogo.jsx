export default function BrandLogo({ size = 34 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="gobusly-logo-gradient" x1="8" y1="8" x2="56" y2="56" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#0b8f77" />
          <stop offset="1" stopColor="#13a78b" />
        </linearGradient>
      </defs>

      <rect x="6" y="6" width="52" height="52" rx="16" fill="url(#gobusly-logo-gradient)" />

      <rect x="14" y="24" width="36" height="18" rx="8" fill="white" fillOpacity="0.18" />
      <rect x="18" y="27" width="23" height="6" rx="3" fill="white" />
      <rect x="18" y="35" width="17" height="4" rx="2" fill="white" fillOpacity="0.88" />

      <circle cx="24" cy="44" r="3" fill="white" />
      <circle cx="42" cy="44" r="3" fill="white" />
    </svg>
  );
}
