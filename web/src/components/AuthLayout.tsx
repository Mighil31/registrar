import type { ReactNode } from "react";
import { Stamp } from "./ui";

/** Sign in and Sign up: the form beside a green panel holding one (sample) member copy. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col-reverse lg:flex-row">
      <section
        aria-label="About Registrar"
        className="flex flex-col justify-between gap-10 bg-rail px-6 py-10 text-white sm:px-12 lg:w-[46%] lg:px-16 lg:py-14"
      >
        <div className="hidden text-[21px] font-extrabold lg:block">Registrar</div>
        <figure className="m-0 flex max-w-[440px] flex-col gap-3 rounded bg-canary px-6 py-5.5 text-ink shadow-[0_2px_4px_rgba(0,0,0,.18),0_12px_28px_rgba(0,0,0,.18)]">
          <span className="text-xs font-bold text-canary-ink">WKS 1123</span>
          <div className="text-2xl font-extrabold">Java 21 in Practice</div>
          <div className="grid grid-cols-2 gap-2.5 border-t border-canary-line pt-3 text-sm">
            <span>
              <span className="block text-xs font-semibold text-canary-ink">When</span>Mon 23 Nov · 10:00
            </span>
            <span>
              <span className="block text-xs font-semibold text-canary-ink">Seats</span>104 / 120
            </span>
          </div>
          <div className="flex items-center justify-between">
            <Stamp kind="registered">REGISTERED</Stamp>
            <figcaption className="text-xs text-canary-ink">Sample</figcaption>
          </div>
        </figure>
        <p className="m-0 max-w-[460px] text-[26px] leading-tight font-bold">Every event, every seat, on record.</p>
      </section>
      <main className="flex flex-1 flex-col items-center justify-center gap-6 px-6 py-12">
        <div className="w-full max-w-[400px] text-[21px] font-extrabold text-rail lg:hidden">Registrar</div>
        <div className="w-full max-w-[400px]">{children}</div>
      </main>
    </div>
  );
}
