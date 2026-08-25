// Design tokens shared by apps/web and apps/admin. Plain values, not tied to
// any CSS-in-JS or component library — consume as JS values or via the CSS
// custom properties in ./tokens.css.

export const color = {
  brand50: "#f0faf4",
  brand100: "#d9f2e3",
  brand300: "#7dd3a8",
  brand500: "#2f9e63",
  brand600: "#227a4c",
  brand700: "#1a5e3b",
  neutral0: "#ffffff",
  neutral50: "#f7f8f7",
  neutral100: "#ecefec",
  neutral300: "#c7ccc8",
  neutral500: "#7a8078",
  neutral700: "#454943",
  neutral900: "#1c1e1b",
  danger500: "#d9483a",
  warning500: "#d99a2b",
} as const;

export const space = {
  xs: "4px",
  sm: "8px",
  md: "16px",
  lg: "24px",
  xl: "32px",
  xxl: "48px",
} as const;

export const radius = {
  sm: "4px",
  md: "8px",
  lg: "16px",
  full: "999px",
} as const;

export const fontSize = {
  xs: "12px",
  sm: "14px",
  md: "16px",
  lg: "20px",
  xl: "28px",
  xxl: "36px",
} as const;

export const fontWeight = {
  regular: 400,
  medium: 500,
  semibold: 600,
  bold: 700,
} as const;
