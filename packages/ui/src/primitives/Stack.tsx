import type { CSSProperties, HTMLAttributes, ReactNode } from "react";
import { space } from "../tokens";

export interface StackProps extends HTMLAttributes<HTMLDivElement> {
  direction?: "row" | "column";
  gap?: keyof typeof space;
  align?: CSSProperties["alignItems"];
  justify?: CSSProperties["justifyContent"];
  children: ReactNode;
}

export function Stack({
  direction = "column",
  gap = "md",
  align,
  justify,
  style,
  children,
  ...rest
}: StackProps) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: direction,
        gap: space[gap],
        alignItems: align,
        justifyContent: justify,
        ...style,
      }}
      {...rest}
    >
      {children}
    </div>
  );
}
