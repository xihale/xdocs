import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../stores/auth";
import { authApi } from "../api";
import { Mail, Lock, User } from "lucide-react";
import { Turnstile, type TurnstileInstance } from "@marsidev/react-turnstile";
import React from "react";

export function LoginPage() {
  const navigate = useNavigate();
  const { login, initialized, user } = useAuthStore();

  const [mode, setMode] = useState<"login" | "register" | "forgot">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [turnstileToken, setTurnstileToken] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const turnstileRef = React.useRef<TurnstileInstance>(null);

  useEffect(() => {
    if (initialized && user) navigate("/", { replace: true });
  }, [initialized, user, navigate]);

  const handleSendCode = async () => {
    if (!email) return setError("请输入邮箱");
    if (!turnstileToken) return setError("请先完成人机验证");
    setError("");
    setMessage("");
    try {
      await authApi.sendEmailCode(email, turnstileToken, mode === "register" ? "register" : "reset");
      setMessage("验证码已发送");
      // 发送验证码后重置 Turnstile，因为 Token 是单次使用的
      setTurnstileToken("");
      turnstileRef.current?.reset();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
      setTurnstileToken("");
      turnstileRef.current?.reset();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setMessage("");
    
    if (mode === "login" || mode === "forgot") {
       if (!turnstileToken) return setError("请先完成人机验证");
    }

    setLoading(true);
    try {
      if (mode === "login") {
        await login(username, password, turnstileToken);
        navigate("/");
      } else if (mode === "register") {
        await authApi.register(username, password, email, emailCode);
        navigate("/");
      } else {
        await authApi.resetPassword(email, emailCode, password);
        setMessage("密码已重置，请登录");
        setMode("login");
        setTurnstileToken("");
        turnstileRef.current?.reset();
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
      setTurnstileToken("");
      turnstileRef.current?.reset();
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-surface p-8">
      <div className="w-full max-w-md">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-primary flex items-center justify-center mb-3">
            <span className="text-on-primary font-bold text-lg font-mono">X</span>
          </div>
          <h1 className="text-2xl font-bold text-on-surface font-mono tracking-wider uppercase">XDocs</h1>
        </div>

        <div className="bg-surface-container-lowest border border-outline-variant">
          {/* Tab */}
          {mode !== "forgot" && (
            <div className="flex border-b border-outline-variant">
              {(["login", "register"] as const).map((m) => (
                <button
                  key={m}
                  onClick={() => { setMode(m); setError(""); setMessage(""); setTurnstileToken(""); turnstileRef.current?.reset(); }}
                  className={`flex-1 py-3 text-sm font-medium ${
                    mode === m
                      ? "text-primary border-b-2 border-primary"
                      : "text-on-surface-variant"
                  }`}
                >
                  {m === "login" ? "登录" : "注册"}
                </button>
              ))}
            </div>
          )}

          <form className="p-6 space-y-4" onSubmit={handleSubmit}>
            {error && (
              <div className="bg-error/10 text-error text-sm p-3">{error}</div>
            )}
            {message && (
              <div className="bg-primary/10 text-primary text-sm p-3">{message}</div>
            )}

            {mode !== "forgot" && (
              <Input
                icon={<User className="w-4 h-4" />}
                placeholder="用户名"
                value={username}
                onChange={setUsername}
              />
            )}

            {mode !== "login" && (
              <>
                <Input
                  icon={<Mail className="w-4 h-4" />}
                  placeholder="邮箱"
                  type="email"
                  value={email}
                  onChange={setEmail}
                />
                <div className="flex gap-2">
                  <input
                    className="flex-1 px-3 py-2.5 border border-outline-variant text-sm focus:border-primary focus:ring-2 focus:ring-primary/10 outline-none"
                    placeholder="邮箱验证码"
                    value={emailCode}
                    onChange={(e) => setEmailCode(e.target.value)}
                  />
                  <button
                    type="button"
                    onClick={handleSendCode}
                    className="px-3 py-2.5 border border-outline-variant text-primary text-sm font-medium hover:bg-surface-container whitespace-nowrap"
                  >
                    发送验证码
                  </button>
                </div>
              </>
            )}

            <Input
              icon={<Lock className="w-4 h-4" />}
              placeholder={mode === "forgot" ? "新密码" : "密码"}
              type="password"
              value={password}
              onChange={setPassword}
            />

            <div className="flex justify-center py-2">
              <Turnstile
                ref={turnstileRef}
                siteKey="1x00000000000000000000AA"
                onSuccess={(token) => setTurnstileToken(token)}
                onExpire={() => setTurnstileToken("")}
                onError={() => setTurnstileToken("")}
              />
            </div>

            <button
              disabled={loading}
              className="w-full py-3 bg-primary text-on-primary font-medium hover:opacity-90 transition-all disabled:opacity-50"
              type="submit"
            >
              {loading
                ? "处理中..."
                : mode === "login"
                  ? "登录"
                  : mode === "register"
                    ? "注册"
                    : "重置密码"}
            </button>

            {mode === "login" && (
              <button
                type="button"
                onClick={() => { setMode("forgot"); setError(""); setMessage(""); setTurnstileToken(""); turnstileRef.current?.reset(); }}
                className="text-sm text-primary hover:underline w-full text-center"
              >
                忘记密码？
              </button>
            )}
            {mode === "forgot" && (
              <button
                type="button"
                onClick={() => { setMode("login"); setError(""); setMessage(""); setTurnstileToken(""); turnstileRef.current?.reset(); }}
                className="text-sm text-primary hover:underline w-full text-center"
              >
                返回登录
              </button>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}

function Input({
  icon,
  placeholder,
  type = "text",
  value,
  onChange,
}: {
  icon: React.ReactNode;
  placeholder: string;
  type?: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="relative">
      <div className="absolute left-3 top-1/2 -translate-y-1/2 text-outline">
        {icon}
      </div>
      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full pl-10 pr-3 py-2.5 border border-outline-variant text-sm focus:border-primary focus:ring-2 focus:ring-primary/10 outline-none"
      />
    </div>
  );
}
