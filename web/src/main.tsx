import { MutationCache, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider, createRouter } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Toaster, toast } from "sonner";
import { ApiError, problemMessage } from "./lib/api";
import { refresh } from "./lib/session";
import { routeTree } from "./routeTree.gen";
import "./styles.css";

declare module "@tanstack/react-query" {
  interface Register {
    /** `quiet`: the screen shows this mutation's error itself (form fields, inline notices). */
    mutationMeta: { quiet?: boolean };
  }
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      retry: (failures, error) => !(error instanceof ApiError) && failures < 2,
    },
  },
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) => {
      if (!mutation.meta?.quiet) {
        toast.error(problemMessage(error));
      }
    },
  }),
});

const router = createRouter({
  routeTree,
  context: { queryClient },
  defaultPreload: "intent",
  scrollRestoration: true,
});

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

// A reload keeps you signed in: the refresh cookie buys a new access token before the first route
// decides whether to show Sign in.
await refresh();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <Toaster
        position="bottom-right"
        toastOptions={{
          classNames: {
            toast: "!rounded-md !border !border-line !bg-panel !font-sans !text-ink !shadow-[0_2px_4px_rgba(20,32,26,.1),0_12px_28px_rgba(20,32,26,.16)]",
            error: "!border-2 !border-red !bg-pink-row !text-red-ink",
            success: "!border-2 !border-go",
          },
        }}
      />
    </QueryClientProvider>
  </StrictMode>,
);
