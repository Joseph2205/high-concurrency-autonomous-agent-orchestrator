# Virtual-Thread State in the Dashboard

Owner: Member 3 (Day 17)

## Problem

`ThreadMXBean` only reports platform threads; virtual threads are invisible to
it. To show a meaningful "live virtual threads" gauge we wrap the orchestrator
executor with a counting delegate.

## Implementation

- `VirtualThreadConfig.CountingExecutorService` wraps
  `Executors.newVirtualThreadPerTaskExecutor()` and increments/decrements a
  counter around every task (`onVirtualThreadStarted` / `onVirtualThreadFinished`).
- It implements the marker interface
  `VirtualThreadMonitor.VirtualThreadExecutor` so the monitor can confirm the
  executor really is virtual-thread-based.
- `VirtualThreadMonitor` exposes `liveVirtualThreads`, `platformThreadCount`
  and `peakPlatformThreadCount`.

## Dashboard fields

| Field | Source |
|-------|--------|
| `threads.liveVirtualThreads` | counter in the counting executor |
| `threads.platformThreadCount` | `ThreadMXBean.getThreadCount()` |
| `threads.peakPlatformThreadCount` | `ThreadMXBean.getPeakThreadCount()` |
| `threads.virtualThreadExecutor` | marker-interface check |

## Why this matters (review talking point)

A legacy platform-thread pool would show `platformThreadCount` climbing to
thousands under 10k load. With virtual threads, `platformThreadCount` stays
close to the CPU core count while `liveVirtualThreads` climbs into the
thousands — the core evidence for the project's premise.

## Evidence

- `VirtualThreadMonitorTest` — 3 live virtual threads tracked, then drained to 0.
- `DashboardServiceTest` — the injected executor is confirmed virtual-thread-based.
