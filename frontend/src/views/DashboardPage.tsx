import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { articleApi, searchApi } from "../api";
import type { ArticleVO, KnowledgeBase, SearchUser } from "../api/types";
import { Heart, Search, TrendingUp, Clock } from "lucide-react";

export function DashboardPage() {
  const navigate = useNavigate();
  const [articles, setArticles] = useState<ArticleVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sort, setSort] = useState<"time" | "likes">("time");
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 10;
  const [keyword, setKeyword] = useState("");
  const [searchResults, setSearchResults] = useState<null | { articles: ArticleVO[]; kbs: KnowledgeBase[]; users: SearchUser[] }>(null);
  const [searchTab, setSearchTab] = useState<"articles" | "kbs" | "users">("articles");

  const loadArticles = useCallback(async (s: "time" | "likes" = sort, p: number = page) => {
    setLoading(true);
    setError("");
    try {
      const offset = (p - 1) * PAGE_SIZE;
      const data = await articleApi.publicList(offset, PAGE_SIZE + 1, s);
      setArticles(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [sort, page]);

  useEffect(() => {
    loadArticles();
  }, [loadArticles]);

  const handleSort = (s: "time" | "likes") => {
    setSort(s);
    setPage(1);
    loadArticles(s, 1);
  };

  const handleSearch = async () => {
    if (!keyword.trim()) {
      setSearchResults(null);
      loadArticles();
      return;
    }
    setLoading(true);
    try {
      const [a, k, u] = await Promise.all([
        searchApi.articles(keyword.trim()),
        searchApi.kbs(keyword.trim()),
        searchApi.users(keyword.trim()),
      ]);
      setSearchResults({ articles: a, kbs: k, users: u });
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <h1 className="text-2xl font-bold text-on-surface">公开文章</h1>
        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant" />
            <input
              className="w-full pl-9 pr-3 py-2 border border-outline-variant text-sm focus:border-primary outline-none"
              placeholder="搜索文章、知识库、用户..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            />
          </div>
          <button
            onClick={handleSearch}
            className="px-3 py-2 bg-primary text-on-primary text-sm hover:opacity-90"
          >
            搜索
          </button>
          {searchResults && (
            <button
              onClick={() => {
                setSearchResults(null);
                setKeyword("");
                loadArticles();
              }}
              className="px-3 py-2 text-sm text-on-surface-variant hover:text-primary"
            >
              清除
            </button>
          )}
        </div>
      </div>

      {/* Search results */}
      {searchResults ? (
        <div>
          {/* Tabs */}
          <div className="flex gap-1 mb-4 border-b border-outline-variant">
            {(["articles", "kbs", "users"] as const).map((tab) => (
              <button
                key={tab}
                onClick={() => setSearchTab(tab)}
                className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                  searchTab === tab
                    ? "text-primary border-primary"
                    : "text-on-surface-variant border-transparent"
                }`}
              >
                {tab === "articles"
                  ? `文章 (${searchResults.articles.length})`
                  : tab === "kbs"
                    ? `知识库 (${searchResults.kbs.length})`
                    : `用户 (${searchResults.users.length})`}
              </button>
            ))}
          </div>

          {searchTab === "articles" && (
            <ArticleList
              articles={searchResults.articles}
              onNavigate={(id) => navigate(`/article/${id}`)}
            />
          )}
          {searchTab === "kbs" && (
            <div className="space-y-3">
              {searchResults.kbs.length === 0 ? (
                <p className="text-center py-12 text-on-surface-variant">无匹配知识库</p>
              ) : (
                searchResults.kbs.map((kb) => (
                  <button
                    key={kb.id}
                    onClick={() => navigate(`/kb/${kb.id}`)}
                    className="w-full text-left bg-surface-container-lowest border border-outline-variant p-4 transition-all"
                  >
                    <h3 className="font-medium text-on-surface">{kb.name}</h3>
                    {kb.description && (
                      <p className="text-sm text-on-surface-variant mt-1 line-clamp-2">{kb.description}</p>
                    )}
                  </button>
                ))
              )}
            </div>
          )}
          {searchTab === "users" && (
            <div className="space-y-3">
              {searchResults.users.length === 0 ? (
                <p className="text-center py-12 text-on-surface-variant">无匹配用户</p>
              ) : (
                searchResults.users.map((u) => (
                  <button
                    key={u.id}
                    onClick={() => navigate(`/profile?id=${u.id}`)}
                    className="w-full text-left bg-surface-container-lowest border border-outline-variant p-4 transition-all flex items-center gap-3"
                  >
                    <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                      {(u.nickname || u.username || "U").charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className="font-medium text-on-surface">{u.nickname || u.username}</p>
                      <p className="text-xs text-on-surface-variant">@{u.username}</p>
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </div>
      ) : (
        <>
          {/* Sort controls */}
          <div className="flex items-center gap-2 mb-4">
            <button
              onClick={() => handleSort("time")}
              className={`flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium transition-colors ${
                sort === "time" ? "bg-primary/10 text-primary" : "text-on-surface-variant hover:bg-surface-container"
              }`}
            >
              <Clock className="w-3.5 h-3.5" /> 最新
            </button>
            <button
              onClick={() => handleSort("likes")}
              className={`flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium transition-colors ${
                sort === "likes" ? "bg-primary/10 text-primary" : "text-on-surface-variant hover:bg-surface-container"
              }`}
            >
              <TrendingUp className="w-3.5 h-3.5" /> 最热
            </button>
          </div>

          {error && (
            <div className="bg-error/10 text-error p-3 mb-4 text-sm">{error}</div>
          )}

          {loading ? (
            <div className="flex items-center justify-center py-20">
              <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          ) : articles.length === 0 ? (
            <div className="text-center py-20 text-on-surface-variant">暂无公开文章</div>
          ) : (
            <>
              <ArticleList
                articles={articles.slice(0, PAGE_SIZE)}
                onNavigate={(id) => navigate(`/article/${id}`)}
                showLikes
              />
              <div className="flex items-center justify-center gap-4 mt-6">
                <button
                  disabled={page <= 1}
                  onClick={() => { const p = page - 1; setPage(p); loadArticles(sort, p); }}
                  className="px-4 py-2 text-sm border border-outline-variant disabled:opacity-40 disabled:cursor-not-allowed hover:bg-surface-container transition-colors"
                >
                  上一页
                </button>
                <span className="text-sm text-on-surface-variant">第 {page} 页</span>
                <button
                  disabled={articles.length <= PAGE_SIZE}
                  onClick={() => { const p = page + 1; setPage(p); loadArticles(sort, p); }}
                  className="px-4 py-2 text-sm border border-outline-variant disabled:opacity-40 disabled:cursor-not-allowed hover:bg-surface-container transition-colors"
                >
                  下一页
                </button>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}

function ArticleList({
  articles,
  onNavigate,
  showLikes,
}: {
  articles: ArticleVO[];
  onNavigate: (id: number) => void;
  showLikes?: boolean;
}) {
  return (
    <div className="space-y-3">
      {articles.map((article) => (
        <button
          key={article.id}
          onClick={() => onNavigate(article.id)}
          className="w-full text-left bg-surface-container-lowest border border-outline-variant p-4 transition-all"
        >
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs font-medium text-primary bg-primary/10 px-2 py-0.5 rounded">
              {article.kbName || "知识库"}
            </span>
            <span className="text-xs text-on-surface-variant">{article.updateTime}</span>
            {showLikes && article.likeCount != null && (
              <span className="flex items-center gap-0.5 text-xs text-on-surface-variant ml-auto">
                <Heart className={`w-3 h-3 ${article.liked ? "fill-error text-error" : ""}`} />
                {article.likeCount}
              </span>
            )}
          </div>
          <h2 className="text-lg font-bold text-on-surface mb-1">{article.title}</h2>
          {article.summary && (
            <p className="text-sm text-on-surface-variant line-clamp-2">{article.summary}</p>
          )}
          <div className="flex items-center gap-2 mt-3 text-xs text-on-surface-variant">
            <span>{article.authorName || `用户${article.authorId}`}</span>
          </div>
        </button>
      ))}
    </div>
  );
}
