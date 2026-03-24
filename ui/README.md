# Dashboard UI Plugins

Elements can inject custom pages into the Elements dashboard by shipping a small React component bundle alongside the Element's Java code. The dashboard discovers and loads these bundles at runtime - no changes to the dashboard itself are required.

## How it works

When the dashboard starts, it checks each running Element container for a `plugin.json` manifest file. If found, it dynamically loads the declared JavaScript bundle and adds the component to the sidebar navigation.

The manifest and bundle are placed in the Element's static UI content directory:

```
element/src/main/ui/
  superuser/
    plugin.json          <- declares the sidebar entry and bundle location
    plugin.bundle.js     <- self-contained IIFE component bundle
  user/
    plugin.json          <- same structure, for user-facing dashboards (future)
    plugin.bundle.js
```

These files are packaged into the `.elm` artifact at build time and served by the Elements runtime under `/app/ui/{element-prefix}/{segment}/`.

## plugin.json

```json
{
  "schema": "1",
  "entries": [
    {
      "label": "Example Element",
      "icon": "Package",
      "bundlePath": "plugin.bundle.js",
      "route": "example-element"
    }
  ]
}
```

| Field | Description |
|---|---|
| `label` | Text shown in the dashboard sidebar |
| `icon` | A [Lucide](https://lucide.dev/icons/) icon name (e.g. `Package`, `Layers`, `Zap`) |
| `bundlePath` | Path to the bundle, relative to the manifest file |
| `route` | Unique key used in the dashboard URL (`/plugin/{route}`) |

## Bundle format

The bundle must be an IIFE (immediately-invoked function expression) that registers a React component with the dashboard's plugin registry. The host dashboard exposes `window.React` - the bundle must use this same instance to avoid React hook conflicts.

```js
(function () {
  var React = window.React;

  function MyPlugin() {
    return React.createElement('div', { className: 'p-6' },
      React.createElement('h1', { className: 'text-2xl font-bold' }, 'My Plugin')
    );
  }

  window.__elementsPlugins && window.__elementsPlugins.register('my-route', MyPlugin);
})();
```

The component can use Tailwind utility classes - the dashboard's stylesheet is already loaded when the bundle runs.

## User segmentation

The `superuser/` directory serves components shown to administrators. A `user/` directory (same structure) will serve components in user-facing dashboards when that feature is released. Each segment is discovered and loaded independently, so the bundles do not interfere with each other.

## Developing the UI

This module contains a Vite-based TypeScript project for developing plugin components with JSX and full hot-module replacement, while still producing the correct IIFE output for embedding.

### Requirements

- [Node.js](https://nodejs.org/) 18+ (via [nvm](https://github.com/nvm-sh/nvm) recommended)

### One-time setup

```bash
cd ui
npm install
```

### Standalone development (fast iteration)

Run a Vite dev server that renders the component in isolation with live reload:

```bash
npm run dev:superuser   # or: npm run dev:user
```

Open `http://localhost:5173` in a browser. Edit `src/superuser/ExamplePlugin.tsx` (or `src/user/ExamplePlugin.tsx`) and the browser updates instantly.

### Building for integration

When ready to test the component embedded in the actual dashboard, build the bundles:

```bash
npm run build
```

This compiles both segments and writes `plugin.bundle.js` directly into `element/src/main/ui/superuser/` and `element/src/main/ui/user/`. No copy step is needed - the output lands exactly where the Maven build will pick it up.

Then run the `debug` module to repackage the `.elm` and restart the server.

### Source structure

```
ui/src/
  superuser/
    ExamplePlugin.tsx    <- edit this for the superuser UI component
    dev-entry.tsx        <- mounts the component for standalone dev (do not ship)
    plugin-entry.ts      <- registers the component with the dashboard registry
    index.html           <- dev server entry point (do not ship)
  user/
    ExamplePlugin.tsx    <- edit this for the user UI component
    dev-entry.tsx
    plugin-entry.ts
    index.html
  shared/                <- optional: components used by both segments
```

`ExamplePlugin.tsx` is the only file you need to edit for most use cases. The other files wire it into the dev server and the plugin registry respectively.

### Adding your own component

1. Edit `src/superuser/ExamplePlugin.tsx` (and/or the `user/` equivalent)
2. Develop with `npm run dev:superuser` until satisfied
3. Run `npm run build` to write the bundle into `element/src/main/ui/superuser/`
4. Restart the debug server to pick up the new bundle

Shared components (used by both segments) can be placed in `src/shared/` and imported with a relative path.

### CI and release builds

To build the UI as part of a Maven build (e.g. in CI), activate the `build-ui` profile from the project root:

```bash
mvn install -Pbuild-ui
```

This profile is deliberately inactive by default so that normal `mvn install` does not touch `node_modules`.
