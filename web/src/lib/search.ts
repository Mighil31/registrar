import { useRouter } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";

/**
 * A search box whose text goes into the URL a beat after the last key (shareable; back works).
 *
 * If a link elsewhere is clicked within that beat, this page is already on its way out but still
 * mounted while the next screen's code loads. The search is dropped then; applying it would
 * navigate back here and swallow the click.
 */
export function useUrlSearch(current: string | undefined, apply: (q: string | undefined) => void) {
  const router = useRouter();
  // Where this page is, taken once: the router's own location moves on as soon as a click elsewhere
  // starts navigating, before this page unmounts.
  const [here] = useState(() => router.latestLocation.pathname);
  const [q, setQ] = useState(current ?? "");
  const applyRef = useRef(apply);
  applyRef.current = apply;

  useEffect(() => {
    const id = setTimeout(() => {
      if ((current ?? "") !== q && router.latestLocation.pathname === here) {
        applyRef.current(q || undefined);
      }
    }, 250);
    return () => clearTimeout(id);
  }, [q, current, here, router]);

  return [q, setQ] as const;
}
