import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button, Field, LoadError, PageHeader, Panel, Skeleton, Stamp } from "../components/ui";
import { ApiError, type Schemas, api, call } from "../lib/api";
import { keys } from "../lib/queries";
import { password, personName, phoneNumber } from "../lib/rules";
import { updateUser } from "../lib/session";

export const Route = createFileRoute("/profile")({ component: Profile });

function Profile() {
  const me = useQuery({ queryKey: keys.me, queryFn: () => call(api.GET("/api/v1/me")) });
  return (
    <div className="flex max-w-[760px] flex-col gap-5">
      <PageHeader title="Profile" lead="Your details as admins see them on attendee lists." />
      {me.isError ? (
        <LoadError error={me.error} retry={() => me.refetch()} />
      ) : me.isPending ? (
        <Panel>
          <div className="flex flex-col gap-3 p-6">
            <Skeleton className="h-11 w-full" />
            <Skeleton className="h-11 w-full" />
          </div>
        </Panel>
      ) : (
        <>
          <Details me={me.data} />
          <PasswordForm />
        </>
      )}
    </div>
  );
}

/** Errors that name a field go on the field; anything else is a toast. */
function applyFieldErrors(error: unknown, setError: (field: never, error: { message: string }) => void): boolean {
  if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      setError(field as never, { message });
    }
    return true;
  }
  return false;
}

const detailsSchema = z.object({ name: personName, phoneNumber });

function Details({ me }: { me: Schemas["UserResponse"] }) {
  const queryClient = useQueryClient();
  const form = useForm({
    resolver: zodResolver(detailsSchema),
    defaultValues: { name: me.name, phoneNumber: me.phoneNumber },
  });
  const save = useMutation({
    mutationFn: (body: z.infer<typeof detailsSchema>) => call(api.PATCH("/api/v1/me", { body })),
    meta: { quiet: true },
    onSuccess: (user) => {
      updateUser(user);
      queryClient.setQueryData(keys.me, user);
      form.reset({ name: user.name, phoneNumber: user.phoneNumber });
      toast.success("Saved. Your name is updated on your registrations too.");
    },
    onError: (error) => {
      if (!applyFieldErrors(error, form.setError as never)) {
        toast.error(error.message);
      }
    },
  });

  return (
    <Panel title="Your details" action={<Stamp kind={me.roles.includes("ADMIN") ? "admin" : "member"}>{me.roles.includes("ADMIN") ? "ADMIN" : "MEMBER"}</Stamp>}>
      <form noValidate onSubmit={form.handleSubmit((values) => save.mutate(values))} className="flex flex-col gap-4 px-5 py-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name" autoComplete="name" error={form.formState.errors.name?.message} {...form.register("name")} />
          <Field
            label="Phone"
            type="tel"
            inputMode="numeric"
            autoComplete="tel"
            error={form.formState.errors.phoneNumber?.message}
            {...form.register("phoneNumber")}
          />
        </div>
        <Field label="Email" value={me.email} readOnly disabled hint="Your email is how you sign in; it can't be changed here." />
        <div>
          <Button type="submit" variant="primary" busy={save.isPending} disabled={!form.formState.isDirty}>
            Save changes
          </Button>
        </div>
      </form>
    </Panel>
  );
}

const passwordSchema = z
  .object({ currentPassword: z.string().min(1, "Enter your current password"), newPassword: password, confirm: z.string() })
  .refine((v) => v.newPassword === v.confirm, { path: ["confirm"], message: "The two new passwords don't match" });

function PasswordForm() {
  const form = useForm({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirm: "" },
  });
  const change = useMutation({
    mutationFn: ({ currentPassword, newPassword }: z.infer<typeof passwordSchema>) =>
      call(api.PUT("/api/v1/me/password", { body: { currentPassword, newPassword } })),
    meta: { quiet: true },
    onSuccess: () => {
      form.reset();
      toast.success("Password changed. You've been signed out everywhere else.");
    },
    onError: (error) => {
      if (!applyFieldErrors(error, form.setError as never)) {
        toast.error(error.message);
      }
    },
  });
  const errors = form.formState.errors;

  return (
    <Panel title="Change password">
      <form noValidate onSubmit={form.handleSubmit((values) => change.mutate(values))} className="flex flex-col gap-4 px-5 py-5">
        <Field
          label="Current password"
          type="password"
          autoComplete="current-password"
          error={errors.currentPassword?.message}
          {...form.register("currentPassword")}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="New password"
            type="password"
            autoComplete="new-password"
            hint="12 to 64 characters"
            error={errors.newPassword?.message}
            {...form.register("newPassword")}
          />
          <Field
            label="New password again"
            type="password"
            autoComplete="new-password"
            error={errors.confirm?.message}
            {...form.register("confirm")}
          />
        </div>
        <p className="m-0 text-sm text-muted">Changing it signs you out on every other device; this one stays signed in.</p>
        <div>
          <Button type="submit" variant="primary" busy={change.isPending}>
            Change password
          </Button>
        </div>
      </form>
    </Panel>
  );
}
