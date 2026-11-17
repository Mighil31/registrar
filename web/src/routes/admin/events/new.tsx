import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { toast } from "sonner";
import { EventForm, toRequest } from "../../../components/EventForm";
import { BackLink, PageHeader } from "../../../components/ui";
import { ApiError, type Schemas, api, call } from "../../../lib/api";
import { keys } from "../../../lib/queries";

export const Route = createFileRoute("/admin/events/new")({ component: NewEvent });

function NewEvent() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const create = useMutation({
    mutationFn: (body: Schemas["EventRequest"]) => call(api.POST("/api/v1/events", { body })),
    meta: { quiet: true },
    onSuccess: async (event) => {
      await queryClient.invalidateQueries({ queryKey: keys.events });
      await queryClient.invalidateQueries({ queryKey: keys.dashboard });
      toast.success(`${event.title} created.`);
      await navigate({ to: "/admin/events/$id", params: { id: event.id } });
    },
    onError: (error) => {
      if (!(error instanceof ApiError) || Object.keys(error.fieldErrors).length === 0) {
        toast.error(error.message);
      }
    },
  });

  return (
    <div className="flex max-w-[860px] flex-col gap-4.5">
      <BackLink to="/admin/events">All events</BackLink>
      <PageHeader title="New event" lead="It's scheduled as soon as you save. Open events appear in Browse straight away." />
      <EventForm
        submitLabel="Create event"
        busy={create.isPending}
        error={create.error}
        onSubmit={(values) => create.mutate(toRequest(values))}
        onCancel={() => navigate({ to: "/admin/events" })}
      />
    </div>
  );
}
