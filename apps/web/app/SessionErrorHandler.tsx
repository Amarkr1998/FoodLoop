"use client";

import { useEffect, useRef } from "react";
import { useSession, signOut } from "next-auth/react";
import { toast } from "sonner";
import { buildKeycloakLogoutUrl } from "@/lib/keycloak-logout";

/**
 * lib/auth.ts's jwt callback sets `error: "RefreshAccessTokenError"` once a
 * session's refresh token is permanently dead (expired SSO session, revoked
 * token, etc.) — middleware.ts catches this on the next navigation, but a
 * tab that's already loaded keeps calling the API with a token that will
 * never work until the user navigates somewhere. Without this, every
 * request just fails with an unhelpful generic "Something went wrong."
 * This catches it the moment NextAuth notices, instead of waiting for the
 * user to stumble into a failed mutation.
 */
export function SessionErrorHandler() {
  const { data: session } = useSession();
  const handled = useRef(false);

  useEffect(() => {
    if (session?.error !== "RefreshAccessTokenError" || handled.current) return;
    handled.current = true;

    toast.error("Your session expired — please sign in again.");
    const logoutUrl = buildKeycloakLogoutUrl(session.idToken);
    signOut({ redirect: false }).then(() => {
      window.location.href = logoutUrl;
    });
  }, [session]);

  return null;
}
