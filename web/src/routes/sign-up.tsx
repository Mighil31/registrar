import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { AuthLayout } from "../components/AuthLayout";
import { Button, Field, cx } from "../components/ui";
import { ApiError, api, call, problemMessage } from "../lib/api";
import { email, password, passwordStrength, personName, phoneNumber } from "../lib/rules";
import { startSession } from "../lib/session";

export const Route = createFileRoute("/sign-up")({ component: SignUp });

const schema = z.object({ name: personName, email, phoneNumber, password });
type Values = z.infer<typeof schema>;

const STRENGTH = ["Too short", "Fair", "Good", "Strong"] as const;

function SignUp() {
  const navigate = useNavigate();
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", email: "", phoneNumber: "", password: "" },
  });
  const errors = form.formState.errors;
  const strength = passwordStrength(form.watch("password"));

  const signUp = useMutation({
    mutationFn: (body: Values) => call(api.POST("/api/v1/auth/signup", { body })),
    meta: { quiet: true },
    onSuccess: async (auth) => {
      startSession(auth);
      await navigate({ to: "/browse" });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          form.setError(field as keyof Values, { message });
        }
      }
    },
  });
  const formError = signUp.error instanceof ApiError && Object.keys(signUp.error.fieldErrors).length > 0 ? null : signUp.error;

  return (
    <AuthLayout>
      <form noValidate onSubmit={form.handleSubmit((values) => signUp.mutate(values))} className="flex flex-col gap-4.5">
        <div>
          <h1 className="m-0 text-3xl font-extrabold tracking-[-0.02em]">Sign up</h1>
          <p className="mt-1 mb-0 text-sm text-muted">Create an account to register for events.</p>
        </div>
        {formError && (
          <p role="alert" className="m-0 rounded-md border-2 border-red bg-pink-row px-3 py-2.5 text-sm font-semibold text-red-ink">
            {problemMessage(formError)}
          </p>
        )}
        <Field label="Name" autoComplete="name" error={errors.name?.message} {...form.register("name")} />
        <Field label="Email" type="email" autoComplete="email" error={errors.email?.message} {...form.register("email")} />
        <Field
          label="Phone"
          type="tel"
          inputMode="numeric"
          autoComplete="tel"
          hint="10 to 12 digits"
          error={errors.phoneNumber?.message}
          {...form.register("phoneNumber")}
        />
        <div className="flex flex-col gap-2">
          <Field
            label="Password"
            type="password"
            autoComplete="new-password"
            hint="12 to 64 characters. A few words strung together works well."
            error={errors.password?.message}
            {...form.register("password")}
          />
          <div className="flex items-center gap-3" aria-live="polite">
            <div className="grid flex-1 grid-cols-3 gap-1" aria-hidden="true">
              {[1, 2, 3].map((step) => (
                <span
                  key={step}
                  className={cx(
                    "h-1.5 rounded-sm transition-colors duration-200",
                    strength >= step ? (strength === 1 ? "bg-filling" : "bg-go") : "bg-track",
                  )}
                />
              ))}
            </div>
            <span className="w-20 text-right text-xs font-semibold text-muted">{STRENGTH[strength]}</span>
          </div>
        </div>
        <Button type="submit" variant="primary" size="lg" busy={signUp.isPending}>
          Sign up
        </Button>
        <p className="m-0 mt-1.5 text-center text-[15px] text-muted">
          Already have an account? <Link to="/sign-in">Sign in</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
