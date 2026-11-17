import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { api, call } from "./api";
import { keys } from "./queries";

/** Register and Leave, from Browse, the event page or My events; every list showing it refreshes. */
export function useRegistration(eventId: string, title: string) {
  const queryClient = useQueryClient();
  const refreshAll = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: keys.events }),
      queryClient.invalidateQueries({ queryKey: keys.myEvents }),
      queryClient.invalidateQueries({ queryKey: keys.dashboard }),
    ]);
  const path = { params: { path: { id: eventId } } };
  // True from a successful Register until Leave: the stamp is pressed on once, not on every render.
  const [landed, setLanded] = useState(false);

  const register = useMutation({
    mutationFn: () => call(api.POST("/api/v1/events/{id}/registrations/me", path)),
    onSuccess: () => {
      setLanded(true);
      toast.success(`You're registered for ${title}. Your copy is in My events.`);
    },
    onSettled: refreshAll,
  });
  const leave = useMutation({
    mutationFn: () => call(api.DELETE("/api/v1/events/{id}/registrations/me", path)),
    onSuccess: () => {
      setLanded(false);
      toast.success(`You've left ${title}. Your seat is free again.`);
    },
    onSettled: refreshAll,
  });
  return { register, leave, landed, busy: register.isPending || leave.isPending };
}
