import { queryOptions } from "@tanstack/react-query";
import { api, call } from "./api";

/** Query keys and fetchers shared across screens, so a change in one refreshes the others. */
export const keys = {
  events: ["events"] as const,
  event: (id: string) => ["events", id] as const,
  attendees: (id: string) => ["events", id, "attendees"] as const,
  myEvents: ["my-events"] as const,
  people: ["people"] as const,
  person: (id: string) => ["people", id] as const,
  dashboard: ["dashboard"] as const,
  me: ["me"] as const,
};

export function myEventsQuery(when: "UPCOMING" | "PAST") {
  return queryOptions({
    queryKey: [...keys.myEvents, when],
    queryFn: () => call(api.GET("/api/v1/me/registrations", { params: { query: { when } } })),
  });
}

export function eventQuery(id: string) {
  return queryOptions({
    queryKey: keys.event(id),
    queryFn: () => call(api.GET("/api/v1/events/{id}", { params: { path: { id } } })),
  });
}
