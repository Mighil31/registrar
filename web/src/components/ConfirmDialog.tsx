import * as AlertDialog from "@radix-ui/react-alert-dialog";
import { type ReactNode, useState } from "react";
import { Button } from "./ui";

/**
 * Confirms a destructive action. With `typeToConfirm`, the button stays disabled until that text
 * (the event title or the person's name) is typed exactly.
 */
export function ConfirmDialog({
  trigger,
  title,
  children,
  confirmLabel,
  typeToConfirm,
  busy,
  onConfirm,
}: {
  trigger: ReactNode;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  typeToConfirm?: string;
  busy?: boolean;
  onConfirm: () => Promise<unknown> | void;
}) {
  const [open, setOpen] = useState(false);
  const [typed, setTyped] = useState("");
  const ready = !typeToConfirm || typed.trim() === typeToConfirm;

  return (
    <AlertDialog.Root
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        setTyped("");
      }}
    >
      <AlertDialog.Trigger asChild>{trigger}</AlertDialog.Trigger>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-40 bg-ink/45" />
        <AlertDialog.Content className="fixed top-1/2 left-1/2 z-50 flex w-[min(92vw,460px)] -translate-x-1/2 -translate-y-1/2 flex-col gap-4 rounded-lg border border-line bg-panel p-6 shadow-[0_2px_4px_rgba(20,32,26,.12),0_18px_40px_rgba(20,32,26,.22)]">
          <AlertDialog.Title className="m-0 text-xl font-extrabold">{title}</AlertDialog.Title>
          <AlertDialog.Description asChild>
            <div className="text-sm leading-relaxed text-muted">{children}</div>
          </AlertDialog.Description>
          {typeToConfirm && (
            <label className="flex flex-col gap-1.5 text-sm font-semibold">
              Type “{typeToConfirm}” to confirm
              <input
                value={typed}
                onChange={(e) => setTyped(e.target.value)}
                autoComplete="off"
                className="h-11 rounded-md border border-field px-3 text-base font-normal"
              />
            </label>
          )}
          <div className="flex flex-wrap justify-end gap-2">
            <AlertDialog.Cancel asChild>
              <Button>Keep it</Button>
            </AlertDialog.Cancel>
            <Button
              variant="danger-solid"
              disabled={!ready}
              busy={busy}
              onClick={async () => {
                await onConfirm();
                setOpen(false);
              }}
            >
              {confirmLabel}
            </Button>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
