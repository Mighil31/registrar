import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";
import { EventForm, toRequest } from "../../../components/EventForm";
import { BackLink, Button, LoadError, PageHeader, Skeleton } from "../../../components/ui";
import { ApiError, type Schemas, api, call } from "../../../lib/api";
import { eventQuery, keys } from "../../../lib/queries";

export const Route = createFileRoute("/admin/events/$id/edit")({ component: EditEvent });

function EditEvent() {
  const { id } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const event = useQuery({ ...eventQuery(id), staleTime: Infinity, refetchOnWindowFocus: false });
  // Bumped on "Reload" so the form remounts with the newer version.
  const [generation, setGeneration] = useState(0);

  const save = useMutation({
    mutationFn: (body: Schemas["EventRequest"]) => call(api.PUT("/api/v1/events/{id}", { params: { path: { id } }, body })),
    meta: { quiet: true },
    onSuccess: async (saved) => {
      queryClient.setQueryData(keys.event(id), saved);
      await queryClient.invalidateQueries({ queryKey: keys.events });
      await queryClient.invalidateQueries({ queryKey: keys.myEvents });
      await queryClient.invalidateQueries({ queryKey: keys.dashboard });
      toast.success("Saved.");
      await navigate({ to: "/admin/events/$id", params: { id } });
    },
    onError: (error) => {
      const handled = error instanceof ApiError && (error.kind === "conflict" || Object.keys(error.fieldErrors).length > 0);
      if (!handled) {
        toast.error(error.message);
      }
    },
  });
  const conflict = save.error instanceof ApiError && save.error.kind === "conflict";

  async function reload() {
    await event.refetch();
    save.reset();
    setGeneration((g) => g + 1);
  }

  return (
    <div className="flex max-w-[860px] flex-col gap-4.5">
      <BackLink to="/admin/events/$id" params={{ id }}>Back to the event</BackLink>
      <PageHeader title="Edit event" lead="Title, time and status changes reach everyone's copy of their registration." />
      {event.isError ? (
        <LoadError error={event.error} />
      ) : event.isPending ? (
        <Skeleton className="h-96 w-full" />
      ) : (
        <EventForm
          key={generation}
          event={event.data}
          submitLabel="Save changes"
          busy={save.isPending}
          error={save.error}
          onSubmit={(values) => save.mutate(toRequest(values, event.data.version))}
          onCancel={() => navigate({ to: "/admin/events/$id", params: { id } })}
          conflict={
            conflict && (
              <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-md border-2 border-red bg-pink-row px-4 py-3 text-sm text-red-ink">
                <span>
                  <strong>Someone else saved this event after you opened it.</strong> Reload to see their version; your
                  unsaved changes here will be lost.
                </span>
                <Button variant="danger" onClick={reload}>
                  Reload their version
                </Button>
              </div>
            )
          }
        />
      )}
    </div>
  );
}
