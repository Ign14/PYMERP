import { FormEvent, MouseEvent, ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import axios from "axios";

import logo from "../../assets/logo.png";
import { useAuth } from "../context/AuthContext";
import {
  AccountRequestPayload,
  AccountRequestResponse,
  LoginPayload,
  submitAccountRequest,
} from "../services/client";
import { isValidRut, normalizeRut } from "../utils/rut";

type PanelState = "none" | "login" | "request" | "success";

type LandingExperienceProps = {
  children: ReactNode;
};

type RequestFormState = {
  rut: string;
  fullName: string;
  address: string;
  email: string;
  companyName: string;
  password: string;
  confirmPassword: string;
};

type CaptchaChallenge = {
  a: number;
  b: number;
};

const SUCCESS_MESSAGE = "¡Muchas gracias! Te contactaremos lo antes posible. PYMERP.cl";
const CAPTCHA_ENABLED = String(import.meta.env.VITE_CAPTCHA_ENABLED ?? "true").toLowerCase() !== "false";

const initialRequestState: RequestFormState = {
  rut: "",
  fullName: "",
  address: "",
  email: "",
  companyName: "",
  password: "",
  confirmPassword: "",
};

function createChallenge(): CaptchaChallenge {
  const a = Math.floor(Math.random() * 7) + 3;
  const b = Math.floor(Math.random() * 6) + 2;
  return { a, b };
}

export default function LandingExperience({ children }: LandingExperienceProps) {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [overlayVisible, setOverlayVisible] = useState(true);
  const [panel, setPanel] = useState<PanelState>("none");
  const [loginForm, setLoginForm] = useState({ email: "", password: "" });
  const [loginError, setLoginError] = useState<string | null>(null);
  const [loginFailures, setLoginFailures] = useState(0);
  const [loginCooldownSeconds, setLoginCooldownSeconds] = useState(0);

  const [requestForm, setRequestForm] = useState<RequestFormState>(initialRequestState);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [confirmationMessage, setConfirmationMessage] = useState<string>(SUCCESS_MESSAGE);
  const [requestCaptcha, setRequestCaptcha] = useState<CaptchaChallenge>(() => createChallenge());
  const [requestCaptchaAnswer, setRequestCaptchaAnswer] = useState<string>("");

  const loginEmailRef = useRef<HTMLInputElement | null>(null);
  const loginPasswordRef = useRef<HTMLInputElement | null>(null);
  const requestRutRef = useRef<HTMLInputElement | null>(null);

  const loginMutation = useMutation<void, unknown, LoginPayload>({
    mutationFn: (payload) => login(payload),
    onSuccess: () => {
      setLoginError(null);
      setLoginFailures(0);
      setLoginCooldownSeconds(0);
      setPanel("none");
      navigate("/app");
    },
    onError: (error) => {
      if (axios.isAxiosError(error)) {
        if (!error.response) {
          setLoginError("No se pudo conectar con el servidor. Intenta nuevamente.");
        } else {
          const detail =
            (error.response.data as { detail?: string; message?: string })?.detail ??
            (error.response.data as { message?: string })?.message ??
            "Credenciales inválidas";
          setLoginError(detail);
        }
      } else {
        setLoginError((error as Error).message);
      }
      setLoginFailures((prev) => {
        const next = prev + 1;
        if (next >= 3) {
          const cooldown = Math.min(30, (next - 2) * 5);
          setLoginCooldownSeconds((current) => Math.max(current, cooldown));
        }
        return next;
      });
    },
  });

  const requestMutation = useMutation<AccountRequestResponse, unknown, AccountRequestPayload>({
    mutationFn: submitAccountRequest,
    onSuccess: (response) => {
      setRequestError(null);
      setConfirmationMessage(response?.message ?? SUCCESS_MESSAGE);
      setPanel("success");
      setRequestForm(initialRequestState);
      setRequestCaptcha(createChallenge());
      setRequestCaptchaAnswer("");
    },
    onError: (error) => {
      setRequestCaptcha(createChallenge());
      setRequestCaptchaAnswer("");
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as { detail?: string; message?: string; error?: string } | undefined;
        const detail = data?.detail ?? data?.message ?? data?.error;
        setRequestError(detail ?? "No se pudo enviar la solicitud. Intenta nuevamente más tarde.");
      } else {
        setRequestError((error as Error).message);
      }
    },
  });

  useEffect(() => {
    if (!isAuthenticated) {
      setOverlayVisible(true);
      setPanel("none");
    } else {
      setLoginForm({ email: "", password: "" });
      setLoginError(null);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (panel === "login") {
      const target = loginEmailRef.current ?? loginPasswordRef.current;
      target?.focus();
    }
    if (panel === "request") {
      requestRutRef.current?.focus();
    }
  }, [panel]);

  useEffect(() => {
    if (panel === "none") {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        if (panel === "request") {
          setPanel("login");
        } else if (panel === "login") {
          setPanel("none");
        } else if (panel === "success") {
          setPanel(isAuthenticated ? "none" : "login");
          if (isAuthenticated) {
            setOverlayVisible(false);
          }
        }
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [panel, isAuthenticated]);

  useEffect(() => {
    if (!CAPTCHA_ENABLED) {
      setRequestCaptchaAnswer(String(requestCaptcha.a + requestCaptcha.b));
    }
  }, [requestCaptcha]);

  useEffect(() => {
    if (loginCooldownSeconds <= 0) {
      return;
    }
    const timer = window.setInterval(() => {
      setLoginCooldownSeconds((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [loginCooldownSeconds]);

  const overlayInstructions = useMemo(() => {
    if (!isAuthenticated) {
      return "Haz clic para iniciar sesión";
    }
    return "Haz clic para entrar al panel principal";
  }, [isAuthenticated]);

  const handleOverlayClick = () => {
    if (panel !== "none") {
      return;
    }
    if (isAuthenticated) {
      setOverlayVisible(false);
    } else {
      setPanel("login");
    }
  };

  const handlePanelContainerClick = (event: MouseEvent<HTMLDivElement>) => {
    if (panel === "none") {
      event.preventDefault();
      handleOverlayClick();
    } else {
      event.stopPropagation();
    }
  };

  const handleLoginSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (loginMutation.isPending || loginCooldownSeconds > 0) {
      return;
    }
    setLoginError(null);
    await loginMutation.mutateAsync({
      email: loginForm.email.trim(),
      password: loginForm.password,
    });
  };

  const handleRequestSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setRequestError(null);

    const trimmedRut = normalizeRut(requestForm.rut);
    if (!isValidRut(trimmedRut)) {
      setRequestError("El RUT ingresado no es válido");
      setRequestCaptcha(createChallenge());
      setRequestCaptchaAnswer("");
      return;
    }
    if (requestForm.password !== requestForm.confirmPassword) {
      setRequestError("Las contraseñas no coinciden");
      return;
    }
    const expected = requestCaptcha.a + requestCaptcha.b;
    const normalizedAnswer = requestCaptchaAnswer.trim();
    if (CAPTCHA_ENABLED) {
      const provided = Number.parseInt(normalizedAnswer, 10);
      if (!normalizedAnswer || Number.isNaN(provided) || provided !== expected) {
        setRequestError("Debes resolver el captcha correctamente");
        setRequestCaptcha(createChallenge());
        setRequestCaptchaAnswer("");
        return;
      }
    }

    requestMutation.mutate({
      rut: trimmedRut,
      fullName: requestForm.fullName.trim(),
      address: requestForm.address.trim(),
      email: requestForm.email.trim().toLowerCase(),
      companyName: requestForm.companyName.trim(),
      password: requestForm.password,
      confirmPassword: requestForm.confirmPassword,
      captcha: {
        a: requestCaptcha.a,
        b: requestCaptcha.b,
        answer: CAPTCHA_ENABLED ? normalizedAnswer : String(expected),
      },
    });
  };

  const openRequestPanel = () => {
    setRequestError(null);
    setRequestForm(initialRequestState);
    setRequestCaptcha(createChallenge());
    setRequestCaptchaAnswer("");
    setPanel("request");
  };

  const closeRequestPanel = () => {
    setPanel("login");
  };

  const closeSuccessPanel = () => {
    setPanel(isAuthenticated ? "none" : "login");
    if (isAuthenticated) {
      setOverlayVisible(false);
    }
  };

  return (
    <>
      {children}
      {overlayVisible && (
        <div className="landing-overlay" onClick={handleOverlayClick} role="presentation">
          <div className="landing-overlay__gradient" />
          <div
            className={`landing-overlay__panel ${panel === "none" ? "landing-overlay__panel--compact" : ""}`}
            onClick={handlePanelContainerClick}
          >
            {panel === "none" && (
              <div className="landing-welcome" aria-live="polite">
                <img src={logo} alt="PYMERP" className="landing-logo" />
                <p className="landing-hint">{overlayInstructions}</p>
              </div>
            )}

            {panel === "login" && (
              <div className="landing-card" role="dialog" aria-modal="true" aria-labelledby="landing-login-title">
                <img src={logo} alt="PYMERP" className="landing-logo landing-logo--small" />
                <h1 id="landing-login-title">Iniciar sesión</h1>
                <p className="landing-subtitle">¡Bienvenid@! Ingresa tus credenciales para acceder.</p>
                <form className="landing-form" onSubmit={handleLoginSubmit}>
                  <label className="landing-label">
                    <span>Email</span>
                    <input
                      ref={loginEmailRef}
                      type="email"
                      autoComplete="email"
                      required
                      value={loginForm.email}
                      onChange={(event) => setLoginForm((prev) => ({ ...prev, email: event.target.value }))}
                    />
                  </label>
                  <label className="landing-label">
                    <span>Contraseña</span>
                    <input
                      ref={loginPasswordRef}
                      type="password"
                      autoComplete="current-password"
                      required
                      value={loginForm.password}
                      onChange={(event) => setLoginForm((prev) => ({ ...prev, password: event.target.value }))}
                    />
                  </label>
                  {loginError && (
                    <p className="landing-error" role="alert" aria-live="assertive">
                      {loginError}
                    </p>
                  )}
                  <button
                    className="landing-button"
                    type="submit"
                    disabled={loginMutation.isPending || loginCooldownSeconds > 0}
                  >
                    {loginMutation.isPending
                      ? "Ingresando..."
                      : loginCooldownSeconds > 0
                        ? `Reintentar en ${loginCooldownSeconds}s`
                        : "Entrar"}
                  </button>
                  {loginCooldownSeconds > 0 && (
                    <p className="landing-hint" role="status" aria-live="polite">
                      Demasiados intentos fallidos. Espera {loginCooldownSeconds}s antes de reintentar.
                    </p>
                  )}
                </form>
                <button type="button" className="landing-link" onClick={openRequestPanel}>
                  ¿No tienes cuenta o tienes problemas para acceder? Haz clic aquí
                </button>
              </div>
            )}

            {panel === "request" && (
              <div className="landing-card" role="dialog" aria-modal="true" aria-labelledby="landing-request-title">
                <div className="landing-card__header">
                  <h1 id="landing-request-title">Solicitud de registro o recuperación de cuenta</h1>
                  <button type="button" className="landing-close" aria-label="Cerrar" onClick={closeRequestPanel}>
                    ×
                  </button>
                </div>
                <p className="landing-subtitle">Completa los datos para registro/recuperación.</p>
                <form className="landing-form" onSubmit={handleRequestSubmit}>
                  <label className="landing-label">
                    <span>RUT</span>
                    <input
                      ref={requestRutRef}
                      value={requestForm.rut}
                      onChange={(event) => setRequestForm((prev) => ({ ...prev, rut: event.target.value }))}
                      required
                    />
                  </label>
                  <label className="landing-label">
                    <span>Nombre completo</span>
                    <input
                      value={requestForm.fullName}
                      onChange={(event) => setRequestForm((prev) => ({ ...prev, fullName: event.target.value }))}
                      required
                    />
                  </label>
                  <label className="landing-label">
                    <span>Dirección</span>
                    <input
                      value={requestForm.address}
                      onChange={(event) => setRequestForm((prev) => ({ ...prev, address: event.target.value }))}
                      required
                    />
                  </label>
                  <label className="landing-label">
                    <span>Email</span>
                    <input
                      type="email"
                      value={requestForm.email}
                      onChange={(event) => setRequestForm((prev) => ({ ...prev, email: event.target.value }))}
                      required
                    />
                  </label>
                  <label className="landing-label">
                    <span>Nombre de la empresa</span>
                    <input
                      value={requestForm.companyName}
                      onChange={(event) => setRequestForm((prev) => ({ ...prev, companyName: event.target.value }))}
                      required
                    />
                  </label>
                  <label className="landing-label">
                    <span>Contraseña</span>
                    <input
                      type="password"
                      value={requestForm.password}
                      onChange={(event) => setRequestForm((prev) => ({ ...prev, password: event.target.value }))}
                      required
                      minLength={8}
                    />
                  </label>
                  <label className="landing-label">
                    <span>Repetir contraseña</span>
                    <input
                      type="password"
                      value={requestForm.confirmPassword}
                      onChange={(event) =>
                        setRequestForm((prev) => ({ ...prev, confirmPassword: event.target.value }))
                      }
                      required
                      minLength={8}
                    />
                  </label>
                  <label className="landing-label landing-captcha">
                    <span className="landing-captcha__label">Captcha: ¿Cuánto es {requestCaptcha.a} + {requestCaptcha.b}?</span>
                    <input
                      inputMode="numeric"

                      pattern="\\d*"
                      value={requestCaptchaAnswer}
                      onChange={(event) => setRequestCaptchaAnswer(event.target.value.replace(/[^0-9]/g, ""))}
                      required={CAPTCHA_ENABLED}
                      aria-required={CAPTCHA_ENABLED}
                      aria-label={`Captcha: ¿Cuánto es ${requestCaptcha.a} + ${requestCaptcha.b}?`}
                    />
                  </label>
                  {!CAPTCHA_ENABLED && (
                    <p className="landing-captcha__label" role="status">
                      Captcha deshabilitado en este entorno (respuesta enviada automáticamente).
                    </p>
                  )}
                  {requestError && (
                    <p className="landing-error" role="alert" aria-live="assertive">
                      {requestError}
                    </p>
                  )}
                  <button className="landing-button" type="submit" disabled={requestMutation.isPending}>
                    {requestMutation.isPending ? "Enviando..." : "Enviar solicitud"}
                  </button>
                </form>
              </div>
            )}

            {panel === "success" && (
              <div className="landing-card" role="dialog" aria-modal="true" aria-labelledby="landing-success-title">
                <img src={logo} alt="PYMERP" className="landing-logo landing-logo--small" />
                <h1 id="landing-success-title">Solicitud enviada</h1>
                <p className="landing-subtitle">{confirmationMessage}</p>
                <button className="landing-button" type="button" onClick={closeSuccessPanel}>
                  {isAuthenticated ? "Ir al panel" : "Volver al inicio de sesión"}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
