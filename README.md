# Sentinel Anti-Xray

**Sentinel Anti-Xray** is a high-performance, professional-grade administrative plugin for Paper and Folia servers. It provides a dual-layer defense against X-Ray users by combining engine-level ray-tracing obfuscation with advanced heuristic mining statistics.

This project is a heavily expanded and modernized fork based on the core ray-tracing logic of [RayTraceAntiXray by stonar96](https://github.com/stonar96/RayTraceAntiXray).

## ✦ Key Features

*   **Ray-Trace Engine**: Intelligent occlusion culling that hides ores and blocks from players behind solid surfaces, making X-Ray texture packs and mods useless.
*   **Heuristic Mining Tracker**: Analyzes mining patterns in real-time. Detects suspicious "valuable-to-common" ratios and alerts staff automatically.
*   **Administrative GUI**: Manage the entire plugin in-game via `/sax gui`.
    *   **Block Manager**: Add or remove protected/tracked materials on the fly.
    *   **World Manager**: Toggle ray-tracing protection for specific worlds without restarting.
*   **Folia Support**: Fully compatible with the Folia region-based multithreading architecture.
*   **High Performance**: Uses lock-free concurrency (Atomic types) and a dedicated asynchronous thread pool for ray-tracing to ensure zero impact on server MSPT.
*   **1.20.4 Native**: Fully mapped and optimized for the latest Paper 1.20.4 builds.

## ✦ Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/sax gui` | Opens the main admin dashboard. | `sentinel.antixray.admin` |
| `/sax reload` | Reloads the configuration and block lists. | `sentinel.antixray.admin` |
| `/sax stats <player>` | View real-time mining heuristics for a player. | `sentinel.antixray.admin` |

*   `sentinel.antixray.alerts`: Receive notifications when a player's mining ratio exceeds the suspicious threshold.
*   `sentinel.antixray.bypass`: Permission to bypass the ray-tracing engine (recommended for staff).

## ✦ Installation

1.  Place the `SentinelAntiXray.jar` into your `plugins` folder.
2.  Ensure you have **ProtocolLib** installed.
3.  Restart your server.
4.  Configure your thresholds and block lists in `config.yml` or via the in-game GUI.

## ✦ Credits

The core ray-tracing and occlusion logic is derived from [RayTraceAntiXray](https://github.com/stonar96/RayTraceAntiXray) by **stonar96**. Sentinel expands upon this foundation with modern administrative tools, heuristic tracking, and performance optimizations.

---
**Developed with ♥ by azreyzaako**
