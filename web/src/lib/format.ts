import { format, isSameYear } from "date-fns";

/** Short catalogue prefixes for common tags; anything else is abbreviated by rule. */
const PREFIXES: Record<string, string> = {
  workshop: "WKS",
  talk: "TLK",
  social: "SOC",
  clinic: "CLN",
  meetup: "MTP",
  onboarding: "ONB",
  planning: "PLN",
  hackathon: "HCK",
  security: "SEC",
  design: "DSN",
  infra: "INF",
  mongodb: "MDB",
  java: "JVA",
};

/** First letter, then consonants, padded from the rest: "observability" → OBS. */
export function tagPrefix(tag: string | undefined): string {
  if (!tag) {
    return "EVT";
  }
  const known = PREFIXES[tag];
  if (known) {
    return known;
  }
  const letters = tag.replace(/[^a-z]/gi, "").toUpperCase();
  const consonants = letters.slice(1).replace(/[AEIOU]/g, "");
  return (letters.charAt(0) + consonants + letters.slice(1)).slice(0, 3).padEnd(3, "X");
}

/**
 * The catalogue code shown beside an event, like "WKS 1123": its first tag and its start date
 * (month, day) in the viewer's time zone. Display only; nothing stores or searches it.
 */
export function eventCode(event: { tags?: string[]; startsAt: string }): string {
  return `${tagPrefix(event.tags?.[0])} ${format(new Date(event.startsAt), "MMdd")}`;
}

/** "Thu 19 Nov · 14:00", with the year when it isn't this one. */
export function when(iso: string): string {
  const date = new Date(iso);
  return format(date, isSameYear(date, new Date()) ? "EEE d MMM · HH:mm" : "EEE d MMM yyyy · HH:mm");
}

/** "14:00–16:00" on one day; otherwise the end in full. */
export function timeRange(startsAt: string, endsAt: string): string {
  const start = new Date(startsAt);
  const end = new Date(endsAt);
  const sameDay = format(start, "yyyyMMdd") === format(end, "yyyyMMdd");
  return sameDay
    ? `${format(start, "EEE d MMM · HH:mm")}–${format(end, "HH:mm")}`
    : `${when(startsAt)} – ${when(endsAt)}`;
}

/** "14:32" today, "Tue 14:32" this week, "12 Nov" before that. */
export function stamp(iso: string): string {
  const date = new Date(iso);
  const ageDays = (Date.now() - date.getTime()) / 86_400_000;
  if (format(date, "yyyyMMdd") === format(new Date(), "yyyyMMdd")) {
    return format(date, "HH:mm");
  }
  return ageDays < 6 ? format(date, "EEE HH:mm") : format(date, "d MMM");
}

export type Fill = "open" | "filling" | "full" | "unlimited";

/** How full an event is; "filling" from 80%, the point where the bar turns amber. */
export function fill(registered: number, capacity: number | null | undefined): Fill {
  if (capacity == null) {
    return "unlimited";
  }
  if (registered >= capacity) {
    return "full";
  }
  return registered / capacity >= 0.8 ? "filling" : "open";
}

export function percent(registered: number, capacity: number | null | undefined): number {
  if (capacity == null || capacity === 0) {
    return 0;
  }
  return Math.min(100, Math.round((registered / capacity) * 100));
}

export function count(n: number): string {
  return new Intl.NumberFormat("en-GB").format(n);
}

export function plural(n: number, one: string, many = `${one}s`): string {
  return `${count(n)} ${n === 1 ? one : many}`;
}

/** Local `datetime-local` input value ⇄ ISO instant. */
export function toLocalInput(iso: string): string {
  return format(new Date(iso), "yyyy-MM-dd'T'HH:mm");
}

export function fromLocalInput(value: string): string {
  return new Date(value).toISOString();
}

/** "2125550013" → "212 555 0013"; 11–12 digits are grouped in threes from the left. */
export function phone(digits: string): string {
  if (/^\d{10}$/.test(digits)) {
    return `${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6)}`;
  }
  return digits.replace(/(\d{3})(?=\d{2})/g, "$1 ");
}
