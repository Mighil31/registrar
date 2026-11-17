import { z } from "zod";

/** The server's rules (the kitchensink port's, plus passwords), so most mistakes show before a round trip. */
export const personName = z
  .string()
  .trim()
  .min(1, "Enter a name")
  .max(25, "Use 25 characters or fewer")
  .regex(/^[\p{L}\p{M} .'’-]+$/u, "Use letters, spaces and . ' - only");

export const phoneNumber = z.string().trim().regex(/^[0-9]{10,12}$/, "Use 10 to 12 digits, no spaces");

export const email = z.string().trim().email("Enter an email like name@example.com");

export const password = z
  .string()
  .min(12, "Use at least 12 characters")
  .max(64, "Use 64 characters or fewer");

/** A rough length-and-variety score for the meter; the server only enforces 12–64 characters. */
export function passwordStrength(value: string): 0 | 1 | 2 | 3 {
  if (value.length < 12) {
    return 0;
  }
  const kinds = [/[a-z]/, /[A-Z]/, /[0-9]/, /[^a-zA-Z0-9]/].filter((r) => r.test(value)).length;
  if (value.length >= 20 || (value.length >= 16 && kinds >= 3)) {
    return 3;
  }
  return value.length >= 14 || kinds >= 3 ? 2 : 1;
}
