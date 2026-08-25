import type { ButtonHTMLAttributes, ReactNode } from "react";
import styles from "./Button.module.css";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger";
  children: ReactNode;
}

export function Button({ variant = "primary", className, children, ...rest }: ButtonProps) {
  const variantClass = styles[variant];
  return (
    <button className={[styles.button, variantClass, className].filter(Boolean).join(" ")} {...rest}>
      {children}
    </button>
  );
}
