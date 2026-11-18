import { expect, test } from "@playwright/test";
import { ADMIN, signIn } from "./support";

/*
 * Regression: search boxes write to the URL a beat after the last key. A link clicked in that beat
 * used to be swallowed: the page stays mounted while the next screen's code loads, the search
 * fired, and it navigated back. Slow script loading makes the window wide enough to hit every time.
 */
test("a link clicked right after typing a search still goes where it says", async ({ page }) => {
  await signIn(page, ADMIN.email, ADMIN.password);
  await page.goto("/browse");
  await page.route(/\/assets\/.*\.js$/, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 600));
    await route.continue();
  });

  await page.getByLabel("Search events").fill("java");
  await page.getByRole("navigation", { name: "Main" }).getByRole("link", { name: "Profile" }).click();

  await expect(page).toHaveURL(/\/profile/);
  await page.waitForTimeout(1000); // past the search's beat: still on Profile
  await expect(page).toHaveURL(/\/profile/);
});
