import { expect, test } from "@playwright/test";
import { ADMIN, Api, PASSWORD, signIn, unique } from "./support";

/*
 * The last-admin rule itself (409, and the race when two admins revoke each other) is enforced in
 * the API and tested there (UserIT, LastAdminRaceIT). In the screens it shows up as this: an admin
 * can make someone else an admin, but can never change their own role or delete themselves.
 */
test("an admin makes a member an admin, who then can't demote or delete themselves", async ({ page, browser, request }) => {
  const api = await Api.asAdmin(request);
  const noor = await api.member("Noor Haddad", `roles-${unique()}@registrar.example`);

  try {
    await signIn(page, ADMIN.email, ADMIN.password);
    await page.goto("/admin/people");
    await page.getByLabel("Search people").fill(noor.email);
    const people = page.getByRole("region", { name: "People" }).getByRole("table");
    await people.getByRole("button", { name: "Noor Haddad" }).click();
    await page.getByRole("button", { name: "Make admin" }).click();
    await expect(page.getByText("Noor Haddad is an admin.")).toBeVisible();

    // Noor, in their own browser, signs in to the admin screens.
    const noorsBrowser = await browser.newContext();
    const noorsPage = await noorsBrowser.newPage();
    await signIn(noorsPage, noor.email, PASSWORD);
    await expect(noorsPage).toHaveURL(/\/admin\/dashboard/);

    await noorsPage.goto("/admin/people");
    await noorsPage.getByLabel("Search people").fill(noor.email);
    await noorsPage.getByRole("region", { name: "People" }).getByRole("table").getByRole("button", { name: "Noor Haddad" }).click();
    // The person's panel (the phone layout has a hidden copy in the list).
    const panel = noorsPage.getByLabel("Noor Haddad", { exact: true }).filter({ visible: true });
    await expect(panel.getByText("YOU", { exact: true })).toBeVisible();
    await expect(panel.getByRole("button", { name: "Revoke admin" })).toBeDisabled();
    await expect(panel.getByRole("button", { name: "Delete" })).toBeDisabled();
    await expect(panel).toContainText("Admins can't change their own role or delete themselves");
    await noorsBrowser.close();
  } finally {
    await api.cleanUp();
  }
});
