import createClient from "openapi-fetch";
import type { components, paths } from "../api/schema";
import { CLIENT_HEADER, endSession, getSession, refresh } from "./session";

export type Schemas = components["schemas"];
export type Problem = Schemas["Problem"];

/** A refusal from the API, as RFC 9457 ProblemDetail. `kind` is the type without `/problems/`. */
export class ApiError extends Error {
  readonly status: number;
  readonly kind: string;
  readonly fieldErrors: Record<string, string>;
  readonly requestId: string | undefined;

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? "Something went wrong");
    this.status = problem.status;
    this.kind = problem.type.replace(/^\/problems\//, "");
    this.fieldErrors = problem.errors ?? {};
    this.requestId = problem.requestId;
  }
}

function withToken(request: Request): Request {
  const session = getSession();
  if (session) {
    request.headers.set("Authorization", `Bearer ${session.token}`);
  }
  return request;
}

/**
 * Every call carries the access token. A 401 means it ran out (10 minutes): refresh once from the
 * cookie and retry; if that fails too, the session is over and the app goes back to Sign in.
 */
async function authFetch(input: Request): Promise<Response> {
  const retry = input.clone();
  const response = await fetch(withToken(input));
  if (response.status !== 401 || new URL(input.url).pathname.startsWith("/api/v1/auth/")) {
    return response;
  }
  if (await refresh()) {
    return fetch(withToken(retry));
  }
  endSession("expired");
  return response;
}

export const api = createClient<paths>({ baseUrl: window.location.origin, fetch: authFetch });

/** The cookie-authenticated auth calls need the client header. */
export const clientHeader = CLIENT_HEADER;

type Result<T> = { data?: T; error?: unknown; response: Response };

/** The data, or an {@link ApiError} thrown from the problem body. */
export async function call<T>(request: Promise<Result<T>>): Promise<T> {
  const { data, error, response } = await request;
  if (error !== undefined || !response.ok) {
    throw new ApiError(
      isProblem(error)
        ? error
        : { type: "/problems/unexpected", status: response.status, detail: "Something went wrong" },
    );
  }
  return data as T;
}

function isProblem(value: unknown): value is Problem {
  return typeof value === "object" && value !== null && "type" in value && "status" in value;
}

export function problemMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : "Something went wrong. Try again.";
}
