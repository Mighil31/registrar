import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { AuthLayout } from "../components/AuthLayout";
import { Button, Field } from "../components/ui";
import { api, call, problemMessage } from "../lib/api";
import { startSession, takeEndReason } from "../lib/session";

export const Route = createFileRoute("/sign-in")({
  validateSearch: (search: Record<string, unknown>): { redirect?: string } =>
    typeof search.redirect === "string" && search.redirect.startsWith("/") ? { redirect: search.redirect } : {},
  component: SignIn,
});

const schema = z.object({
  email: z.string().trim().min(1, "Enter your email"),
  password: z.string().min(1, "Enter your password"),
});

function SignIn() {
  const { redirect } = Route.useSearch();
  const navigate = useNavigate();
  const [notice] = useState(takeEndReason);
  const form = useForm({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } });

  const signIn = useMutation({
    mutationFn: (body: z.infer<typeof schema>) => call(api.POST("/api/v1/auth/signin", { body })),
    meta: { quiet: true },
    onSuccess: async (auth) => {
      startSession(auth);
      await navigate({ to: redirect ?? "/" });
    },
  });

  return (
    <AuthLayout>
      <form noValidate onSubmit={form.handleSubmit((values) => signIn.mutate(values))} className="flex flex-col gap-4.5">
        <h1 className="m-0 text-3xl font-extrabold tracking-[-0.02em]">Sign in</h1>
        {notice === "expired" && !signIn.isError && (
          <p role="status" className="m-0 rounded-md border border-canary-line bg-canary-row px-3 py-2.5 text-sm text-canary-ink">
            Your session ended. Sign in again to carry on where you were.
          </p>
        )}
        {signIn.isError && (
          <p role="alert" className="m-0 rounded-md border-2 border-red bg-pink-row px-3 py-2.5 text-sm font-semibold text-red-ink">
            {problemMessage(signIn.error)}
          </p>
        )}
        <Field label="Email" type="email" autoComplete="email" error={form.formState.errors.email?.message} {...form.register("email")} />
        <Field
          label="Password"
          type="password"
          autoComplete="current-password"
          error={form.formState.errors.password?.message}
          {...form.register("password")}
        />
        <Button type="submit" variant="primary" size="lg" busy={signIn.isPending}>
          Sign in
        </Button>
        <p className="m-0 mt-1.5 text-center text-[15px] text-muted">
          New here? <Link to="/sign-up">Sign up</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
