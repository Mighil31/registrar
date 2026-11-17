import { zodResolver } from "@hookform/resolvers/zod";
import { type ReactNode, useId } from "react";
import { type UseFormRegisterReturn, useForm } from "react-hook-form";
import { z } from "zod";
import { ApiError, type Schemas } from "../lib/api";
import { fromLocalInput, toLocalInput } from "../lib/format";
import { Button, Field, Panel, cx } from "./ui";

const TAG = /^[a-z0-9-]{1,20}$/;

/** The server's rules for an event, checked here first. */
const schema = z
  .object({
    title: z.string().trim().min(1, "Give the event a title").max(100, "Use 100 characters or fewer"),
    description: z.string().max(5000, "Use 5,000 characters or fewer"),
    format: z.enum(["IN_PERSON", "ONLINE"]),
    location: z.string().max(200, "Use 200 characters or fewer"),
    meetingLink: z.string().trim().max(500).refine((v) => !v || /^https:\/\/\S+$/.test(v), "Use an https:// link"),
    startsAt: z.string().min(1, "Choose a start"),
    endsAt: z.string().min(1, "Choose an end"),
    mode: z.enum(["OPEN", "PRIVATE"]),
    capacity: z
      .string()
      .trim()
      .refine((v) => !v || (/^\d+$/.test(v) && Number(v) >= 1 && Number(v) <= 100_000), "Use a whole number from 1, or leave it empty"),
    tags: z.string().refine(
      (v) => splitTags(v).length <= 5 && splitTags(v).every((t) => TAG.test(t)),
      "Up to 5 tags of lower-case letters, digits or -, separated by commas",
    ),
  })
  .refine((v) => !v.startsAt || !v.endsAt || new Date(v.endsAt) > new Date(v.startsAt), {
    path: ["endsAt"],
    message: "End after it starts",
  });

export type EventValues = z.infer<typeof schema>;

function splitTags(value: string): string[] {
  return [...new Set(value.split(",").map((t) => t.trim().toLowerCase()).filter(Boolean))];
}

export function toRequest(values: EventValues, version?: number): Schemas["EventRequest"] {
  return {
    title: values.title.trim(),
    description: values.description || undefined,
    format: values.format,
    location: values.format === "IN_PERSON" ? values.location || undefined : undefined,
    meetingLink: values.format === "ONLINE" ? values.meetingLink || undefined : undefined,
    startsAt: fromLocalInput(values.startsAt),
    endsAt: fromLocalInput(values.endsAt),
    mode: values.mode,
    capacity: values.capacity ? Number(values.capacity) : undefined,
    tags: splitTags(values.tags),
    version,
  };
}

function fromEvent(event?: Schemas["EventResponse"]): EventValues {
  return {
    title: event?.title ?? "",
    description: event?.description ?? "",
    format: event?.format ?? "IN_PERSON",
    location: event?.location ?? "",
    meetingLink: event?.meetingLink ?? "",
    startsAt: event ? toLocalInput(event.startsAt) : "",
    endsAt: event ? toLocalInput(event.endsAt) : "",
    mode: event?.mode ?? "OPEN",
    capacity: event?.capacity != null ? String(event.capacity) : "",
    tags: event?.tags.join(", ") ?? "",
  };
}

/** Two or three exclusive choices as radio cards: Format, Access. */
function Choice({
  legend,
  options,
  register,
  value,
}: {
  legend: string;
  options: { value: string; label: string; hint: string }[];
  register: UseFormRegisterReturn;
  value: string;
}) {
  return (
    <fieldset className="m-0 flex flex-col gap-1.5 border-0 p-0">
      <legend className="mb-1.5 p-0 text-sm font-semibold">{legend}</legend>
      <div className="grid gap-2 sm:grid-cols-2">
        {options.map((option) => (
          <label
            key={option.value}
            className={cx(
              "flex cursor-pointer gap-2.5 rounded-md border px-3 py-2.5 text-sm transition-colors duration-150 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-ink",
              value === option.value ? "border-rail bg-mint" : "border-field bg-panel hover:border-rail",
            )}
          >
            <input type="radio" value={option.value} className="mt-0.5 size-4 accent-rail" {...register} />
            <span>
              <span className="block font-bold">{option.label}</span>
              <span className="text-muted">{option.hint}</span>
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}

function TextArea({ label, hint, error, register }: { label: string; hint: ReactNode; error?: string; register: UseFormRegisterReturn }) {
  const id = useId();
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        {label}
      </label>
      <textarea
        id={id}
        rows={7}
        aria-invalid={error ? true : undefined}
        aria-describedby={`${id}-note`}
        className={cx("rounded-md border bg-panel px-3 py-2.5 text-base leading-relaxed", error ? "border-red" : "border-field")}
        {...register}
      />
      <p id={`${id}-note`} className={cx("m-0 text-sm", error ? "font-semibold text-red" : "text-muted")}>
        {error ?? hint}
      </p>
    </div>
  );
}

export function EventForm({
  event,
  submitLabel,
  busy,
  error,
  onSubmit,
  onCancel,
  conflict,
}: {
  event?: Schemas["EventResponse"];
  submitLabel: string;
  busy: boolean;
  error: unknown;
  onSubmit: (values: EventValues) => void;
  onCancel: () => void;
  conflict?: ReactNode;
}) {
  const form = useForm<EventValues>({ resolver: zodResolver(schema), defaultValues: fromEvent(event) });
  const errors = form.formState.errors;
  const serverErrors = error instanceof ApiError ? error.fieldErrors : {};
  const fieldError = (name: keyof EventValues) => errors[name]?.message ?? serverErrors[name];
  const formatValue = form.watch("format");
  const modeValue = form.watch("mode");

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4.5">
      {conflict}
      <Panel title="What and when">
        <div className="flex flex-col gap-4 px-5 py-5">
          <Field label="Title" error={fieldError("title")} {...form.register("title")} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Starts" type="datetime-local" error={fieldError("startsAt")} {...form.register("startsAt")} />
            <Field label="Ends" type="datetime-local" error={fieldError("endsAt")} {...form.register("endsAt")} />
          </div>
          <TextArea
            label="Description"
            hint="Markdown: **bold**, lists, links. Shown to everyone who can see the event."
            error={fieldError("description")}
            register={form.register("description")}
          />
          <Field
            label="Tags"
            placeholder="workshop, java"
            hint="Up to 5, separated by commas. The first one sets the event's code (WKS, TLK…)."
            error={fieldError("tags")}
            {...form.register("tags")}
          />
        </div>
      </Panel>

      <Panel title="Where">
        <div className="flex flex-col gap-4 px-5 py-5">
          <Choice
            legend="Format"
            value={formatValue}
            register={form.register("format")}
            options={[
              { value: "IN_PERSON", label: "In person", hint: "At a place everyone can see." },
              { value: "ONLINE", label: "Online", hint: "The link is shown only to people going." },
            ]}
          />
          {formatValue === "IN_PERSON" ? (
            <Field label="Location" placeholder="Room 4.02" error={fieldError("location")} {...form.register("location")} />
          ) : (
            <Field
              label="Meeting link"
              type="url"
              placeholder="https://"
              error={fieldError("meetingLink")}
              {...form.register("meetingLink")}
            />
          )}
        </div>
      </Panel>

      <Panel title="Who can come">
        <div className="flex flex-col gap-4 px-5 py-5">
          <Choice
            legend="Access"
            value={modeValue}
            register={form.register("mode")}
            options={[
              { value: "OPEN", label: "Open", hint: "Listed in Browse; members register themselves." },
              { value: "PRIVATE", label: "Private", hint: "Never listed; only admins add people." },
            ]}
          />
          <Field
            label="Capacity"
            inputMode="numeric"
            className="sm:max-w-[50%]"
            hint="Empty means no limit. Members can't go over it; admins can, with a warning."
            error={fieldError("capacity")}
            {...form.register("capacity")}
          />
        </div>
      </Panel>

      <div className="flex flex-wrap gap-2.5">
        <Button type="submit" variant="primary" busy={busy}>
          {submitLabel}
        </Button>
        <Button onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}
