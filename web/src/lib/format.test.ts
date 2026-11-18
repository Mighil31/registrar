import { describe, expect, it } from "vitest";
import { eventCode, fill, percent, tagPrefix } from "./format";

describe("tagPrefix", () => {
  it("uses the catalogue prefix for known tags", () => {
    expect(tagPrefix("workshop")).toBe("WKS");
    expect(tagPrefix("clinic")).toBe("CLN");
  });

  it("abbreviates other tags by first letter and consonants", () => {
    expect(tagPrefix("observability")).toBe("OBS");
    expect(tagPrefix("ai")).toBe("AIX");
  });

  it("falls back to EVT without a tag", () => {
    expect(tagPrefix(undefined)).toBe("EVT");
  });
});

describe("eventCode", () => {
  it("joins the first tag's prefix and the start date", () => {
    expect(eventCode({ tags: ["talk", "java"], startsAt: "2026-11-25T12:00:00Z" })).toBe("TLK 1125");
  });
});

describe("fill", () => {
  it("is unlimited without a capacity", () => {
    expect(fill(500, null)).toBe("unlimited");
  });

  it("turns filling at 80% and full at capacity", () => {
    expect(fill(7, 10)).toBe("open");
    expect(fill(8, 10)).toBe("filling");
    expect(fill(10, 10)).toBe("full");
    expect(fill(12, 10)).toBe("full");
  });

  it("caps the bar at 100%", () => {
    expect(percent(12, 10)).toBe(100);
    expect(percent(3, null)).toBe(0);
  });
});
