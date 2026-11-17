import { useSyncExternalStore } from "react";
import type { components } from "../api/schema";

export type User = components["schemas"]["UserResponse"];
type AuthResponse = components["schemas"]["AuthResponse"];

export interface Session {
  token: string;
  user: User;
  isAdmin: boolean;
}

/** Why the session ended, for the sign-in page's notice. */
export type EndReason = "signed-out" | "expired";

/**
 * The signed-in user and their access token, in memory only: never localStorage, where any script
 * could read it. A reload gets a new token from the refresh cookie (see {@link refresh}).
 */
let current: Session | null = null;
let endReason: EndReason | null = null;
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((listener) => listener());
}

export function getSession(): Session | null {
  return current;
}

export function useSession(): Session | null {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => current,
  );
}

export function startSession(auth: AuthResponse) {
  current = { token: auth.accessToken, user: auth.user, isAdmin: auth.user.roles.includes("ADMIN") };
  endReason = null;
  emit();
}

export function updateUser(user: User) {
  if (current) {
    current = { ...current, user, isAdmin: user.roles.includes("ADMIN") };
    emit();
  }
}

export function endSession(reason: EndReason) {
  current = null;
  endReason = reason;
  emit();
}

/** Read once by the sign-in page. */
export function takeEndReason(): EndReason | null {
  const reason = endReason;
  endReason = null;
  return reason;
}

/**
 * The refresh endpoints authenticate by cookie, so they also require this header: a cross-site
 * form can't set it.
 */
export const CLIENT_HEADER = { "X-Requested-With": "XMLHttpRequest" } as const;

let refreshing: Promise<boolean> | null = null;

/**
 * Swaps the refresh cookie for a new access token. Concurrent callers share one request: the
 * server rotates the cookie on every use, so two parallel refreshes would race.
 */
export function refresh(): Promise<boolean> {
  // A timeout, so an unreachable API shows Sign in instead of a blank page.
  refreshing ??= fetch("/api/v1/auth/refresh", { method: "POST", headers: CLIENT_HEADER, signal: AbortSignal.timeout(8000) })
    .then(async (response) => {
      if (!response.ok) {
        return false;
      }
      startSession((await response.json()) as AuthResponse);
      return true;
    })
    .catch(() => false)
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}
