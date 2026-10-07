import { useState } from "react";
import { useNavigate } from "react-router";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import PasswordRules from "./PasswordRules";
import { allPasswordRulesPass, isPasswordTooLong } from "../utils/passwordRules";
import { useAuthStore } from "../stores/authStore";
import { useAuthModalStore } from "../stores/authModalStore";
import { useListsStore } from "../stores/listsStore";

/**
 * Login form. Mirrors LoginPage.jsx's existing logic, shadcn markup only.
 */
function LoginForm({ onSuccess }) {
  const login = useAuthStore((s) => s.login);
  const [stateInfo, setStateInfo] = useState("");
  const [formData, setFormData] = useState({ username: "", password: "" });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const apiResponse = await login(formData.username, formData.password);
      if (apiResponse.status === 200) {
        setStateInfo("Logged in successfully.");
        onSuccess();
      }
    } catch {
      setStateInfo("Username or password not matching.");
    }
  };

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      {stateInfo && <p className="text-sm text-muted-foreground">{stateInfo}</p>}
      <div className="space-y-2">
        <Label htmlFor="login-username">Username</Label>
        <Input
          id="login-username"
          name="username"
          value={formData.username}
          onChange={handleChange}
          placeholder="PapilioFerox"
          required
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="login-password">Password</Label>
        <Input
          id="login-password"
          name="password"
          type="password"
          value={formData.password}
          onChange={handleChange}
          placeholder="••••••••"
          required
        />
      </div>
      <Button type="submit" className="w-full">
        Log in
      </Button>
    </form>
  );
}

/**
 * Register form. Mirrors RegisterPage.jsx's existing logic, shadcn markup only.
 */
function RegisterForm({ onSuccess }) {
  const register = useAuthStore((s) => s.register);
  const [response, setResponse] = useState("");
  const [passwordFocused, setPasswordFocused] = useState(false);
  const [usernameErrors, setUsernameErrors] = useState([]);
  const [passwordErrors, setPasswordErrors] = useState([]);
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    confirmPassword: "",
  });

  const usernameValid = formData.username.trim().length > 0;
  const canSubmit =
    usernameValid &&
    allPasswordRulesPass(formData.password) &&
    !isPasswordTooLong(formData.password);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (name === "username") setUsernameErrors([]);
    if (name === "password") setPasswordErrors([]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      setPasswordErrors(["Passwords do not match"]);
      return;
    }
    try {
      const apiResponse = await register(formData.username, formData.password);
      if (apiResponse.status === 201) {
        setResponse("Account created. You can log in now.");
        onSuccess();
      }
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;
      if (status === 400 && data?.errors) {
        setUsernameErrors(data.errors.username ?? []);
        setPasswordErrors(data.errors.password ?? []);
        return;
      }
      setResponse(typeof data === "string" ? data : "Got some issue. Try again");
    }
  };

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      {response && <p className="text-sm text-muted-foreground">{response}</p>}
      <div className="space-y-2">
        <Label htmlFor="register-username">Username</Label>
        <Input
          id="register-username"
          name="username"
          value={formData.username}
          onChange={handleChange}
          placeholder="PapilioFerox"
          required
        />
        {usernameErrors.map((m) => (
          <p key={m} className="text-sm text-destructive">{m}</p>
        ))}
      </div>
      <div className="space-y-2">
        <Label htmlFor="register-password">Password</Label>
        <Input
          id="register-password"
          name="password"
          type="password"
          value={formData.password}
          onChange={handleChange}
          onFocus={() => setPasswordFocused(true)}
          onBlur={() => setPasswordFocused(false)}
          placeholder="••••••••"
          required
        />
        <PasswordRules password={formData.password} focused={passwordFocused} />
        {passwordErrors.map((m) => (
          <p key={m} className="text-sm text-destructive">{m}</p>
        ))}
      </div>
      <div className="space-y-2">
        <Label htmlFor="register-confirm">Confirm password</Label>
        <Input
          id="register-confirm"
          name="confirmPassword"
          type="password"
          value={formData.confirmPassword}
          onChange={handleChange}
          placeholder="••••••••"
          required
        />
      </div>
      <Button type="submit" className="w-full" disabled={!canSubmit}>
        Create account
      </Button>
    </form>
  );
}

export default function AuthModal() {
  const isOpen = useAuthModalStore((s) => s.isOpen);
  const mode = useAuthModalStore((s) => s.mode);
  const closeModal = useAuthModalStore((s) => s.closeModal);
  const setMode = useAuthModalStore((s) => s.setMode);
  const navigate = useNavigate();

  const handleLoginSuccess = () => {
    useListsStore.getState().getWatchList();
    setTimeout(() => {
      closeModal();
      navigate("/");
    }, 1200);
  };

  const handleRegisterSuccess = () => {
    setTimeout(() => {
      setMode("login");
    }, 1200);
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && closeModal()}>
      <DialogContent className="bg-card text-card-foreground sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {mode === "login" ? "Log in to your account" : "Create an account"}
          </DialogTitle>
        </DialogHeader>

        {mode === "login" ? (
          <LoginForm onSuccess={handleLoginSuccess} />
        ) : (
          <RegisterForm onSuccess={handleRegisterSuccess} />
        )}

        <p className="text-sm text-muted-foreground">
          {mode === "login" ? "Don't have an account? " : "Already have an account? "}
          <button
            type="button"
            className="font-medium text-primary hover:underline"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? "Register" : "Log in"}
          </button>
        </p>
      </DialogContent>
    </Dialog>
  );
}
