import { useState, useRef, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useAuthStore } from "../stores/auth";
import { userApi, uploadApi } from "../api";
import type { UserProfile, FollowUser, FavoriteArticle, HistoryArticle } from "../api/types";
import {
  User,
  Camera,
  Loader2,
  Bookmark,
  Clock,
  Trash2,
} from "lucide-react";

export function ProfilePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, setUser } = useAuthStore();
  const [nickname, setNickname] = useState("");
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const viewingId = searchParams.get("id") ? Number(searchParams.get("id")) : null;
  const isMe = !viewingId || viewingId === user?.id;

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [tab, setTab] = useState<"info" | "following" | "followers" | "favorites" | "history">(
    isMe ? "info" : "following"
  );
  const [followList, setFollowList] = useState<FollowUser[]>([]);
  const [favorites, setFavorites] = useState<FavoriteArticle[]>([]);
  const [history, setHistory] = useState<HistoryArticle[]>([]);
  const [loading, setLoading] = useState(true);

  const targetId = viewingId || user?.id;

  useEffect(() => {
    if (!targetId) return;
    userApi.profile(targetId)
      .then((p) => {
        setProfile(p);
        if (isMe && user) setNickname(p.nickname || "");
      })
      .finally(() => setLoading(false));
  }, [targetId, isMe, user]);

  useEffect(() => {
    if (!user) return;
    if (tab === "following") {
      userApi.following(targetId).then(setFollowList).catch(() => {});
    } else if (tab === "followers") {
      userApi.followers(targetId).then(setFollowList).catch(() => {});
    } else if (tab === "favorites" && isMe) {
      userApi.favorites().then(setFavorites).catch(() => {});
    } else if (tab === "history" && isMe) {
      userApi.history().then(setHistory).catch(() => {});
    }
  }, [tab, targetId, user, isMe]);

  if (loading || !profile) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-6 h-6 text-primary animate-spin" />
      </div>
    );
  }

  const handleSave = async () => {
    if (!user) return;
    setSaving(true);
    try {
      await userApi.updateNickname(nickname);
      setUser({ ...user, nickname });
      setProfile({ ...profile, nickname });
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;
    setUploading(true);
    try {
      const { url } = await uploadApi.avatar(file);
      setUser({ ...user, avatarUrl: url });
      setProfile({ ...profile, avatarUrl: url });
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    } finally {
      setUploading(false);
    }
  };

  const handleFollowToggle = async () => {
    if (!user || !viewingId) return;
    try {
      if (profile.isFollowed) {
        await userApi.unfollow(viewingId);
        setProfile({ ...profile, isFollowed: false, followerCount: profile.followerCount - 1 });
      } else {
        await userApi.follow(viewingId);
        setProfile({ ...profile, isFollowed: true, followerCount: profile.followerCount + 1 });
      }
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    }
  };

  const handleDeleteHistory = async (articleId?: number) => {
    try {
      await userApi.deleteHistory(articleId);
      if (articleId) {
        setHistory(history.filter((h) => h.id !== articleId));
      } else {
        setHistory([]);
      }
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : String(e));
    }
  };

  return (
    <div className="max-w-2xl">
      {/* Profile header */}
      <div className="bg-surface-container-lowest border border-outline-variant p-6 mb-4">
        <div className="flex items-center gap-4">
          <div className="relative">
            {profile.avatarUrl ? (
              <img
                src={profile.avatarUrl}
                alt="avatar"
                className="w-16 h-16 rounded-full object-cover border border-outline-variant"
              />
            ) : (
              <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center border border-outline-variant">
                <User className="w-8 h-8 text-primary" />
              </div>
            )}
            {isMe && (
              <>
                <button
                  onClick={() => fileRef.current?.click()}
                  disabled={uploading}
                  className="absolute -bottom-1 -right-1 w-7 h-7 bg-primary text-on-primary rounded-full flex items-center justify-center hover:opacity-90"
                >
                  {uploading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Camera className="w-3.5 h-3.5" />}
                </button>
                <input ref={fileRef} type="file" accept="image/*" onChange={handleAvatarUpload} className="hidden" />
              </>
            )}
          </div>
          <div className="flex-1">
            <h1 className="text-xl font-bold text-on-surface">
              {profile.nickname || profile.username}
            </h1>
            <p className="text-sm text-on-surface-variant">@{profile.username}</p>
            <div className="flex items-center gap-4 text-xs text-on-surface-variant mt-2">
              <button onClick={() => setTab("following")} className="hover:text-primary">
                <strong>{profile.followingCount || 0}</strong> 关注
              </button>
              <button onClick={() => setTab("followers")} className="hover:text-primary">
                <strong>{profile.followerCount || 0}</strong> 粉丝
              </button>
            </div>
          </div>
          {!isMe && user && (
            <button
              onClick={handleFollowToggle}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                profile.isFollowed
                  ? "bg-surface-container text-on-surface-variant hover:bg-error/10 hover:text-error"
                  : "bg-primary text-on-primary hover:opacity-90"
              }`}
            >
              {profile.isFollowed ? "取消关注" : "关注"}
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-outline-variant mb-4">
        {isMe && (
          <TabBtn active={tab === "info"} onClick={() => setTab("info")}>个人资料</TabBtn>
        )}
        <TabBtn active={tab === "following"} onClick={() => setTab("following")}>关注</TabBtn>
        <TabBtn active={tab === "followers"} onClick={() => setTab("followers")}>粉丝</TabBtn>
        {isMe && (
          <>
            <TabBtn active={tab === "favorites"} onClick={() => setTab("favorites")}>
              <Bookmark className="w-3.5 h-3.5 inline mr-1" />收藏
            </TabBtn>
            <TabBtn active={tab === "history"} onClick={() => setTab("history")}>
              <Clock className="w-3.5 h-3.5 inline mr-1" />浏览记录
            </TabBtn>
          </>
        )}
      </div>

      {/* Info tab */}
      {isMe && tab === "info" && (
        <div className="bg-surface-container-lowest border border-outline-variant p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-on-surface-variant mb-1">用户名</label>
            <input className="w-full px-3 py-2 border border-outline-variant text-sm bg-surface-container" value={profile.username} disabled />
          </div>
          <div>
            <label className="block text-sm font-medium text-on-surface-variant mb-1">邮箱</label>
            <input className="w-full px-3 py-2 border border-outline-variant text-sm bg-surface-container" value={profile.email} disabled />
          </div>
          <div>
            <label className="block text-sm font-medium text-on-surface-variant mb-1">昵称</label>
            <input
              className="w-full px-3 py-2 border border-outline-variant text-sm focus:border-primary outline-none"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSave()}
            />
          </div>
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 bg-primary text-on-primary text-sm font-medium hover:opacity-90 disabled:opacity-50"
          >
            {saving ? "保存中..." : "保存"}
          </button>
        </div>
      )}

      {/* Following / Followers */}
      {(tab === "following" || tab === "followers") && (
        <div className="bg-surface-container-lowest border border-outline-variant divide-y divide-outline-variant/50">
          {followList.length === 0 ? (
            <p className="text-center py-8 text-on-surface-variant text-sm">
              {tab === "following" ? "暂无关注" : "暂无粉丝"}
            </p>
          ) : (
            followList.map((u) => (
              <button
                key={u.id}
                onClick={() => navigate(`/profile?id=${u.id}`)}
                className="w-full text-left flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors"
              >
                <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                  {(u.nickname || u.username || "U").charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-sm font-medium text-on-surface">{u.nickname || u.username}</p>
                  <p className="text-xs text-on-surface-variant">@{u.username}</p>
                </div>
              </button>
            ))
          )}
        </div>
      )}

      {/* Favorites */}
      {isMe && tab === "favorites" && (
        <div className="bg-surface-container-lowest border border-outline-variant divide-y divide-outline-variant/50">
          {favorites.length === 0 ? (
            <p className="text-center py-8 text-on-surface-variant text-sm">暂无收藏</p>
          ) : (
            favorites.map((f) => (
              <button
                key={f.id}
                onClick={() => navigate(`/article/${f.id}`)}
                className="w-full text-left flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors"
              >
                <Bookmark className="w-4 h-4 text-amber-500 fill-amber-500 shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-on-surface truncate">{f.title}</p>
                  <p className="text-xs text-on-surface-variant">{f.updateTime}</p>
                </div>
              </button>
            ))
          )}
        </div>
      )}

      {/* History */}
      {isMe && tab === "history" && (
        <div className="bg-surface-container-lowest border border-outline-variant">
          {history.length > 0 && (
            <div className="flex justify-end px-4 py-2 border-b border-outline-variant/50">
              <button
                onClick={() => handleDeleteHistory()}
                className="text-xs text-error hover:underline"
              >
                清空全部
              </button>
            </div>
          )}
          <div className="divide-y divide-outline-variant/50">
            {history.length === 0 ? (
              <p className="text-center py-8 text-on-surface-variant text-sm">暂无浏览记录</p>
            ) : (
              history.map((h) => (
                <div key={h.id} className="flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors">
                  <Clock className="w-4 h-4 text-on-surface-variant shrink-0" />
                  <button
                    onClick={() => navigate(`/article/${h.id}`)}
                    className="flex-1 text-left min-w-0"
                  >
                    <p className="text-sm font-medium text-on-surface truncate">{h.title}</p>
                    <p className="text-xs text-on-surface-variant">{h.updateTime}</p>
                  </button>
                  <button
                    onClick={() => handleDeleteHistory(h.id)}
                    className="p-1.5 rounded hover:bg-error/10 text-on-surface-variant hover:text-error"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function TabBtn({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
        active ? "text-primary border-primary" : "text-on-surface-variant border-transparent hover:text-on-surface"
      }`}
    >
      {children}
    </button>
  );
}
