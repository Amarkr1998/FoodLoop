import { getToken } from "next-auth/jwt";
import { NextResponse, type NextRequest } from "next/server";

export async function middleware(request: NextRequest) {
  const token = await getToken({ req: request, secret: process.env.NEXTAUTH_SECRET });
  // A token can exist yet be useless: once its refresh_token is dead (e.g. a
  // session that outlived a Keycloak restart), lib/auth.ts's jwt callback
  // sets `error` but the cookie still decodes fine — without this check the
  // app "loads" while every downstream API call silently 401s underneath it.
  if (!token || token.error) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("callbackUrl", request.url);
    return NextResponse.redirect(loginUrl);
  }
  return NextResponse.next();
}

export const config = {
  // Everything requires sign-in except the NextAuth routes themselves and
  // the public registration page (POST /api/v1/auth/register has no
  // account to authenticate yet, by definition).
  matcher: ["/((?!api/auth|register|login|_next/static|_next/image|favicon.ico).*)"],
};
