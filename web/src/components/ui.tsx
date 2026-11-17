import { Link, type LinkProps } from "@tanstack/react-router";
import { type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, forwardRef, useId } from "react";
import { ApiError } from "../lib/api";
import { type Fill, count, fill as fillOf, percent } from "../lib/format";

export function cx(...classes: (string | false | null | undefined)[]): string {
  return classes.filter(Boolean).join(" ");
}

type Variant = "primary" | "secondary" | "quiet" | "danger" | "danger-solid";

const VARIANTS: Record<Variant, string> = {
  primary: "bg-rail text-white border border-rail hover:bg-rail-deep",
  secondary: "bg-panel text-rail border border-rail hover:bg-mint",
  quiet: "bg-panel text-ink border border-field hover:bg-quiet",
  danger: "bg-panel text-red border border-red hover:bg-pink-row",
  "danger-solid": "bg-red text-white border border-red hover:bg-red-ink",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: "md" | "lg";
  busy?: boolean;
}

/** Button looks for a link (`<Link className={buttonClasses("primary")}>`), never a button in a link. */
export function buttonClasses(variant: Variant = "quiet", size: "md" | "lg" = "md"): string {
  return cx(
    "inline-flex items-center justify-center gap-2 rounded-md font-semibold whitespace-nowrap no-underline transition-colors duration-150",
    "disabled:cursor-not-allowed disabled:opacity-55",
    size === "lg" ? "h-12 px-5 text-base font-bold" : "h-11 px-4 text-sm",
    VARIANTS[variant],
  );
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = "quiet", size = "md", busy, disabled, className, children, type = "button", ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={cx(buttonClasses(variant, size), className)}
      {...rest}
    >
      {busy && <Spinner />}
      {children}
    </button>
  );
});

function Spinner() {
  return (
    <span
      aria-hidden="true"
      className="size-3.5 animate-spin rounded-full border-2 border-current border-r-transparent"
    />
  );
}

/** The world's stamp: caps, 2px border in its own colour. */
export type StampKind =
  | "open"
  | "closed"
  | "full"
  | "private"
  | "registered"
  | "cancelled"
  | "admin"
  | "member"
  | "over";

const STAMPS: Record<StampKind, string> = {
  open: "text-go bg-panel",
  closed: "text-red bg-pink",
  full: "text-red bg-pink",
  private: "text-ink bg-quiet",
  registered: "text-go bg-canary",
  cancelled: "text-red bg-pink",
  admin: "text-rail bg-mint",
  member: "text-faint bg-panel",
  over: "text-red bg-panel",
};

/** `landing`: the stamp is pressed on just now (after Register or Add), the one authored motion. */
export function Stamp({ kind, children, landing }: { kind: StampKind; children: ReactNode; landing?: boolean }) {
  return <span className={cx("stamp", STAMPS[kind], landing && "stamp-land")}>{children}</span>;
}

/** The small form-copy label: REGISTERED (canary), ADDED BY ADMIN (mint), CANCELLED (pink). */
export function CopyTag({ tone, children }: { tone: "self" | "admin" | "closed"; children: ReactNode }) {
  const tones = {
    self: "bg-canary text-canary-ink",
    admin: "bg-mint text-rail",
    closed: "bg-pink text-red",
  };
  return <span className={cx("copy-tag", tones[tone])}>{children}</span>;
}

const BAR: Record<Fill, string> = {
  open: "bg-rail",
  filling: "bg-filling",
  full: "bg-red",
  unlimited: "bg-rail/45",
};

/** Percent-full bar. Unlimited events draw a faint full track instead of a percentage. */
export function SeatBar({
  registered,
  capacity,
  className,
}: {
  registered: number;
  capacity: number | null;
  className?: string;
}) {
  const state = fillOf(registered, capacity);
  const pct = state === "unlimited" ? 100 : percent(registered, capacity);
  const label =
    capacity == null
      ? `${count(registered)} registered, no limit`
      : `${count(registered)} of ${count(capacity)} seats taken, ${percent(registered, capacity)}%`;
  return (
    <div
      role="img"
      aria-label={label}
      className={cx("h-2 w-full min-w-24 overflow-hidden rounded bg-track", className)}
    >
      <div className={cx("h-full rounded transition-[width] duration-300 ease-out", BAR[state])} style={{ width: `${pct}%` }} />
    </div>
  );
}

/** "104 / 120", or "212 no limit". */
export function SeatCount({ registered, capacity }: { registered: number; capacity: number | null }) {
  return (
    <span className="font-bold whitespace-nowrap">
      {count(registered)}{" "}
      <span className="font-medium text-faint">{capacity == null ? "no limit" : `/ ${count(capacity)}`}</span>
    </span>
  );
}

export function Panel({
  title,
  action,
  children,
  className,
  label,
}: {
  title?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  label?: string;
}) {
  return (
    <section aria-label={label} className={cx("min-w-0 rounded-lg border border-line bg-panel", className)}>
      {title && (
        <div className="flex items-center justify-between gap-3 border-b border-line px-5 py-3.5">
          <h2 className="m-0 text-base font-bold">{title}</h2>
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function PageHeader({ title, lead, actions }: { title: ReactNode; lead?: ReactNode; actions?: ReactNode }) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-3.5">
      <div className="min-w-0">
        <h1 className="m-0 text-3xl font-extrabold tracking-[-0.02em]">{title}</h1>
        {lead && <p className="mt-1 mb-0 max-w-[70ch] text-sm text-muted">{lead}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2.5">{actions}</div>}
    </header>
  );
}

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: ReactNode;
}

export const Field = forwardRef<HTMLInputElement, FieldProps>(function Field(
  { label, error, hint, className, id, ...rest },
  ref,
) {
  const generated = useId();
  const inputId = id ?? generated;
  const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;
  return (
    <div className={cx("flex flex-col gap-1.5", className)}>
      <label htmlFor={inputId} className="text-sm font-semibold">
        {label}
      </label>
      <input
        ref={ref}
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cx(
          "h-11 rounded-md border bg-panel px-3 text-base text-ink",
          error ? "border-red" : "border-field",
        )}
        {...rest}
      />
      {error ? (
        <p id={`${inputId}-error`} className="m-0 text-sm font-semibold text-red">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${inputId}-hint`} className="m-0 text-sm text-muted">
            {hint}
          </p>
        )
      )}
    </div>
  );
});

export function SearchBox({
  value,
  onChange,
  label,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  label: string;
  placeholder?: string;
}) {
  return (
    <label className="flex h-11 max-w-sm min-w-60 flex-1 items-center gap-2 rounded-md border border-field bg-panel px-3 text-muted focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-ink">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <path d="M21 21l-4.3-4.3" />
      </svg>
      <input
        type="search"
        aria-label={label}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="min-w-0 flex-1 border-0 bg-transparent text-sm text-ink outline-none"
      />
    </label>
  );
}

/** A row of filter buttons where one is pressed: All / Admins / Members, tag chips. */
export function Chips<T extends string>({
  options,
  value,
  onChange,
  label,
}: {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  label: string;
}) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap gap-2">
      {options.map((option) => {
        const pressed = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={pressed}
            onClick={() => onChange(option.value)}
            className={cx(
              "h-11 rounded-md border px-3.5 text-[13px] font-semibold transition-colors duration-150",
              pressed ? "border-ink bg-ink text-white" : "border-field bg-panel text-muted hover:border-ink hover:text-ink",
            )}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <span aria-hidden="true" className={cx("block animate-pulse rounded bg-track", className)} />;
}

export function SkeletonRows({ rows = 5, columns }: { rows?: number; columns: number }) {
  return (
    <>
      {Array.from({ length: rows }, (_, row) => (
        <tr key={row} className="border-t border-rule">
          {Array.from({ length: columns }, (_, column) => (
            <td key={column} className="px-4 py-4">
              <Skeleton className={column === 1 ? "h-4 w-48" : "h-4 w-16"} />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

/** An empty list that says what to do next. */
export function Empty({ title, children, action }: { title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-start gap-3 px-6 py-10">
      <p className="m-0 text-base font-bold">{title}</p>
      {children && <p className="m-0 max-w-[60ch] text-sm text-muted">{children}</p>}
      {action}
    </div>
  );
}

/** A failed load, with the request id support would need. */
export function LoadError({ error, retry }: { error: unknown; retry?: () => void }) {
  const requestId = error instanceof ApiError ? error.requestId : undefined;
  return (
    <div role="alert" className="flex flex-col items-start gap-2 rounded-md border-2 border-red bg-pink-row px-4 py-3 text-sm text-red-ink">
      <strong>{error instanceof ApiError ? error.message : "Couldn't load this. Check your connection and try again."}</strong>
      {requestId && <span>Request id {requestId}: quote it if you report this.</span>}
      {retry && (
        <Button variant="quiet" onClick={retry}>
          Try again
        </Button>
      )}
    </div>
  );
}

export function Pager({
  page,
  size,
  total,
  onPage,
}: {
  page: number;
  size: number;
  total: number;
  onPage: (page: number) => void;
}) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (pages <= 1) {
    return null;
  }
  return (
    <nav aria-label="Pages" className="flex items-center justify-between gap-3 border-t border-line px-4 py-3 text-sm text-muted">
      <span>
        Page {page + 1} of {pages} · {count(total)} in all
      </span>
      <div className="flex gap-2">
        <Button disabled={page === 0} onClick={() => onPage(page - 1)}>
          Previous
        </Button>
        <Button disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>
          Next
        </Button>
      </div>
    </nav>
  );
}

export function BackLink({ children, ...link }: LinkProps & { children: ReactNode }) {
  return (
    <Link {...link} className="inline-flex min-h-11 w-fit items-center gap-1 text-sm font-semibold">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
        <path d="M15 18l-6-6 6-6" />
      </svg>
      {children}
    </Link>
  );
}

/** Table header cell in the world's small, muted caps-free style. */
export function Th({ children, align, className }: { children?: ReactNode; align?: "right"; className?: string }) {
  return (
    <th className={cx("px-4 py-2.5 text-xs font-semibold text-muted", align === "right" ? "text-right" : "text-left", className)}>
      {children}
    </th>
  );
}
