import { expect, test } from "@playwright/test";
import { ADMIN, Api, signIn, unique } from "./support";

test("an admin adds two people to an event with one seat left, past capacity, after a warning", async ({ page, request }) => {
  const api = await Api.asAdmin(request);
  const tag = unique();
  const going = await api.member("Amara Lee", `cap-${tag}-amara@registrar.example`);
  await api.member("Bruno Silva", `cap-${tag}-bruno@registrar.example`);
  await api.member("Chen Wei", `cap-${tag}-chen@registrar.example`);
  const event = await api.event({ mode: "PRIVATE", capacity: 2 });
  await api.add(event.id, [going.id]);

  try {
    await signIn(page, ADMIN.email, ADMIN.password);
    await page.goto(`/admin/events/${event.id}`);

    const add = page.getByRole("region", { name: "Add people" });
    await add.getByLabel("Search members by name or email").fill(`cap-${tag}`);
    await expect(add.getByRole("checkbox", { name: /Amara Lee/ })).toBeDisabled(); // already going
    await add.getByRole("checkbox", { name: /Bruno Silva/ }).check();
    await add.getByRole("checkbox", { name: /Chen Wei/ }).check();

    await expect(add.getByRole("alert")).toContainText("Only 1 seat left.");
    await expect(add.getByRole("alert")).toContainText("makes it 3 / 2");
    await add.getByRole("button", { name: "Add over capacity" }).click();

    await expect(page.getByText("Added 2 people.")).toBeVisible();
    const attendees = page.getByRole("region", { name: "Attendees" }).getByRole("table");
    await expect(attendees.getByRole("row")).toHaveCount(4); // header + 3
    await expect(attendees.getByRole("row").filter({ hasText: "OVER CAPACITY" })).toHaveCount(1);
    await expect(page.getByRole("region", { name: "Event" })).toContainText("3 / 2 registered");
  } finally {
    await api.cleanUp();
  }
});
