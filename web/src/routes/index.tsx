import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "../lib/session";

/** Admins start on the dashboard, members on Browse. */
export const Route = createFileRoute("/")({
  beforeLoad: () => {
    throw redirect({ to: getSession()?.isAdmin ? "/admin/dashboard" : "/browse" });
  },
});
