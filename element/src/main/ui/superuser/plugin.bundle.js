(function(React) {
  "use strict";
  var _a;

  function ExamplePlugin() {
    var _b = React.useState(null), info = _b[0], setInfo = _b[1];
    var _c = React.useState(false), loading = _c[0], setLoading = _c[1];
    var _d = React.useState(null), error = _d[0], setError = _d[1];

    async function fetchVersion() {
      setLoading(true);
      setError(null);
      try {
        var res = await fetch("/api/rest/version");
        if (!res.ok) throw new Error(res.status + " " + res.statusText);
        setInfo(await res.json());
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        setLoading(false);
      }
    }

    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-2xl" },
      /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold mb-2" }, "Example Element"),
      /* @__PURE__ */ React.createElement("p", { className: "text-muted-foreground mb-6" },
        "This page is served from the Example Element\u2019s superuser UI content directory."
      ),
      /* @__PURE__ */ React.createElement("div", { className: "space-y-4" },
        /* @__PURE__ */ React.createElement("div", null,
          /* @__PURE__ */ React.createElement("button", {
            onClick: fetchVersion,
            disabled: loading,
            className: "rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50 transition-opacity"
          }, loading ? "Loading\u2026" : "Get Platform Version")
        ),
        error && /* @__PURE__ */ React.createElement("div", {
          className: "rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive"
        }, error),
        info && /* @__PURE__ */ React.createElement("div", { className: "rounded-lg border p-4 text-sm space-y-1" },
          /* @__PURE__ */ React.createElement("div", { className: "flex gap-2" },
            /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground w-20" }, "Version"),
            /* @__PURE__ */ React.createElement("span", { className: "font-mono" }, info.version)
          ),
          /* @__PURE__ */ React.createElement("div", { className: "flex gap-2" },
            /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground w-20" }, "Revision"),
            /* @__PURE__ */ React.createElement("span", { className: "font-mono" }, info.revision)
          ),
          /* @__PURE__ */ React.createElement("div", { className: "flex gap-2" },
            /* @__PURE__ */ React.createElement("span", { className: "text-muted-foreground w-20" }, "Built"),
            /* @__PURE__ */ React.createElement("span", { className: "font-mono" }, info.timestamp)
          )
        )
      )
    );
  }

  (_a = window.__elementsPlugins) == null ? void 0 : _a.register("example-element", ExamplePlugin);
})(window.React);
