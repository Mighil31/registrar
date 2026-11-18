import { type APIRequestContext, type Page, expect } from "@playwright/test";

/** A seeded admin (DEV_SEED=true); override for another stack. */
export const ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL ?? "dana.whitfield@registrar.example",
  password: process.env.E2E_ADMIN_PASSWORD ?? "registrar-demo",
};

export const PASSWORD = "correct-horse-battery";

/** Letters only, so it also fits in a person's name. */
export function unique(): string {
  return Array.from({ length: 8 }, () => String.fromCharCode(97 + Math.floor(Math.random() * 26))).join("");
}

export async function signIn(page: Page, email: string, password: string): Promise<void> {
  await page.goto("/sign-in");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).not.toHaveURL(/sign-in/);
}

/**
 * Setup and cleanup go straight to the API, so each journey only drives the screens it's about.
 * Everything made here is deleted again by {@link Api.cleanUp}.
 */
export class Api {
  private readonly users: string[] = [];
  private readonly events: string[] = [];

  private constructor(
    private readonly request: APIRequestContext,
    private readonly token: string,
  ) {}

  static async asAdmin(request: APIRequestContext): Promise<Api> {
    const response = await request.post("/api/v1/auth/signin", { data: ADMIN });
    expect(response.ok(), `admin sign-in: ${response.status()} ${await response.text()} (is the stack up, with DEV_SEED=true?)`).toBeTruthy();
    return new Api(request, (await response.json()).accessToken);
  }

  private get headers() {
    return { Authorization: `Bearer ${this.token}` };
  }

  /** A new member, signed up through the API. */
  async member(name: string, email: string): Promise<{ id: string; email: string }> {
    const response = await this.request.post("/api/v1/auth/signup", {
      data: { name, email, phoneNumber: "2125551234", password: PASSWORD },
    });
    expect(response.status()).toBe(201);
    const id: string = (await response.json()).user.id;
    this.users.push(id);
    return { id, email };
  }

  /** Someone who signed up through the screens, so they get deleted at the end too. */
  async forget(email: string): Promise<void> {
    const response = await this.request.get("/api/v1/users", { headers: this.headers, params: { q: email } });
    const person = (await response.json()).items.find((p: { email: string }) => p.email === email);
    if (person) {
      this.users.push(person.id);
    }
  }

  async event(fields: Record<string, unknown>): Promise<{ id: string; title: string }> {
    const startsAt = new Date(Date.now() + 3 * 24 * 3600_000);
    startsAt.setUTCHours(10, 0, 0, 0);
    const response = await this.request.post("/api/v1/events", {
      headers: this.headers,
      data: {
        title: `E2E ${unique()}`,
        description: "Made by an end-to-end test.",
        format: "IN_PERSON",
        location: "Room 2.14",
        startsAt: startsAt.toISOString(),
        endsAt: new Date(startsAt.getTime() + 2 * 3600_000).toISOString(),
        mode: "OPEN",
        capacity: 20,
        tags: ["e2e"],
        ...fields,
      },
    });
    expect(response.status()).toBe(201);
    const event = await response.json();
    this.events.push(event.id);
    return event;
  }

  async add(eventId: string, userIds: string[]): Promise<void> {
    const response = await this.request.post(`/api/v1/events/${eventId}/registrations`, {
      headers: this.headers,
      data: { userIds },
    });
    expect(response.ok()).toBeTruthy();
  }

  async cleanUp(): Promise<void> {
    for (const id of this.events) {
      await this.request.delete(`/api/v1/events/${id}`, { headers: this.headers });
    }
    for (const id of this.users) {
      await this.request.delete(`/api/v1/users/${id}`, { headers: this.headers });
    }
  }
}
