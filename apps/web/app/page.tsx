import { redirect } from "next/navigation";
import { getSession } from "@/lib/api";

export default async function HomePage() {
  const session = await getSession();
  redirect(session ? "/dashboard" : "/login");
}
