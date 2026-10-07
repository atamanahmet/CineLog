import { useState } from "react";

/**
 * Hold an action until the user confirms they are 18 or older.
 */
export default function useAgeGate() {
  const [pendingAction, setPendingAction] = useState(null);

  const requestConfirmation = (action) => setPendingAction(() => action);

  const confirm = () => {
    pendingAction?.();
    setPendingAction(null);
  };

  const cancel = () => setPendingAction(null);

  return {
    requestConfirmation,
    dialogProps: { open: pendingAction != null, onConfirm: confirm, onCancel: cancel },
  };
}
