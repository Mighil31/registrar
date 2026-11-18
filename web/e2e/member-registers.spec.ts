import { expect, test } from "@playwright/test";
import { Api, PASSWORD, unique } from "./support";

test("a new member signs up, registers for an open event and finds it in My events", async ({ page, request }) => {
  const api = await Api.asAdmin(request);
  const event = await api.event({ mode: "OPEN", capacity: 10 });
  const email = `e2e-${unique()}@registrar.example`;

  try {
    await page.goto("/sign-up");
    await page.getByLabel("Name").fill("Ines Duarte");
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Phone").fill("2125551234");
    await page.getByLabel("Password").fill(PASSWORD);
    await page.getByRole("button", { name: "Sign up" }).click();

    await expect(page).toHaveURL(/\/browse/);
    await page.getByLabel("Search events").fill(event.title);
    await page.getByRole("button", { name: `Register for ${event.title}` }).click();
    await expect(page.getByRole("button", { name: `Leave ${event.title}` })).toBeVisible();

    await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "My events" }).click();
    await expect(page.getByLabel("Upcoming registrations")).toContainText(event.title);
  } finally {
    await api.forget(email);
    await api.cleanUp();
  }
});
