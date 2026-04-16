(function(React) {
  "use strict";
  var _a;
  function ExamplePlugin() {
    return /* @__PURE__ */ React.createElement("div", { className: "p-6 max-w-2xl" }, /* @__PURE__ */ React.createElement("h1", { className: "text-2xl font-bold mb-2" }, "Example Element"), /* @__PURE__ */ React.createElement("p", { className: "text-muted-foreground mb-4" }, "This page is served from the Example Element’s user UI content directory."), /* @__PURE__ */ React.createElement("div", { className: "rounded-lg border p-4 text-sm text-muted-foreground" }, "Installed Elements can inject custom user-facing UI by placing a plugin.json and plugin.bundle.js in their ui/user/ content directory."));
  }
  (_a = window.__elementsPlugins) == null ? void 0 : _a.register("example-element", ExamplePlugin);
})(window.React);
