import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    idToken?: string;
    tenantId?: string;
    roles: string[];
    error?: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    idToken?: string;
    tenantId?: string;
    roles?: string[];
    refreshToken?: string;
    accessTokenExpiresAt?: number;
    error?: string;
  }
}
