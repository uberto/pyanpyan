# 🐣 Pyanpyan – Mobile App Specification

A gentle checklist & timer app designed for ADHD brains.

---

## 1. Purpose

Pyanpyan helps users **start**, **continue**, and **finish** small tasks with minimal friction. The app prioritizes forgiveness, calmness, and clarity over productivity pressure.

The goal is **not** to optimize output, but to reduce cognitive load and emotional friction around everyday tasks.

---

## 2. Target Users

* Adults with ADHD
* Children with attention difficulties (with a cute UI skin)
* Parents supporting children with routines
* Users sensitive to shame-based productivity tools

---

## 3. Core Principles

* Starting is more important than finishing
* Skipping is a valid action
* Time awareness should be visual, not stressful
* No punishment, no streaks, no guilt
* Everything is reversible
* Local-first and privacy-respecting

---

## 4. Functional Requirements

### 4.1 Checklists

#### 4.1.1 Checklist Items

Each checklist item can be in one of the following states:

* `Pending`
* `Done`
* `IgnoredToday`

Ignored items:

* Are visually softened
* Remain visible
* Automatically reset the next day

#### 4.1.2 Ignore Today

* Users can explicitly choose **Ignore today**
* This is a first-class action
* No penalty or warning is shown

Suggested gestures:

* Swipe right → Done
* Swipe left → Ignore today

---

### 4.2 Short-Term Context Memory

* When a checklist screen is opened, its UI state is preserved for **15 minutes**

* Preserved state includes:

    * Scroll position
    * Expanded sections
    * Focused item

* After 15 minutes, the screen reopens in a neutral default state

---

### 4.3 Timers

#### 4.3.1 Timer Types

* Short timers (seconds)
* Long timers (minutes)

#### 4.3.2 Time Representation

* Timers do not emphasize clocks or countdown anxiety
* Time is represented as **discrete consumable units**

    * Seconds for short timers
    * Minutes for long timers

Units disappear as time passes

---

### 4.4 Timer Animation

#### 4.4.1 Visual Metaphor

Timer progress is represented as a life cycle:

1. Egg (start)
2. Caterpillar (in progress)
3. Butterfly (completed)

* On completion, the butterfly gently flies away
* No abrupt sounds or alerts by default

#### 4.4.2 Skin Dependency

* Cute skin uses animal metaphor
* Adult skin may replace with abstract or minimal animation

---

## 5. User Interface

### 5.1 Skinnable UI

The UI must support multiple visual skins without changing behavior.

Skins affect:

* Colors
* Typography
* Iconography
* Animations

Skins do **not** affect:

* Domain logic
* State transitions
* Feature availability

---

### 5.2 Accessibility & Calm Design

* No flashing elements
* No red error states
* Soft color palette
* Large tap targets
* Optional sound and animation

---

## 6. Localization

Localization is a first-class requirement.

* All strings externalized
* No text embedded in images
* Pluralization support
* RTL-ready layout
* Cultural neutrality in default UI

---

## 7. Platform & Technology

### 7.1 Platforms

* Android (Kotlin) – initial target
* iOS – future target

### 7.2 Architecture Goals

* Domain logic independent of platform
* Thin UI layers
* Reusable core logic

---

## 8. Architecture

### 8.1 Style

* Command–Query Separation (CQS)
* Functional programming bias
* Deterministic state transitions

---

### 8.2 Commands

Commands mutate state and return no data (or minimal status).

Examples:

* `AddChecklistItem`
* `MarkItemDone`
* `IgnoreItemToday`
* `StartTimer`
* `CompleteTimer`
* `ResetDailyState`

---

### 8.3 Queries

Queries read state and never mutate.

Examples:

* `GetTodayChecklist`
* `GetChecklistUIState`
* `GetTimerProgress`
* `GetIgnoredItems`

---

### 8.4 Time Handling

* Time is injected
* No direct system clock access in domain logic
* Enables deterministic testing

---

## 9. Data & Persistence

### 9.1 Local-First Storage

* All user data stored locally
* Offline-first
* No account required

### 9.2 Optional Analytics

If enabled by the user:

* Only aggregated metrics
* No task content
* No personal profiling

---

## 10. Testing & Validation

* Domain logic fully unit-testable
* Commands and queries independently testable
* Time-travel and replay supported
* AI-assisted validation supported by design

---

## 11. Non-Goals

Pyanpyan will not:

* Enforce streaks
* Gamify productivity aggressively
* Rank or compare users
* Penalize missed tasks
* Require cloud sync

---

## 12. Naming

**Pyanpyan** is a non-semantic, soft name:

* Easy to pronounce
* Language-neutral
* Cute but not childish
* No productivity pressure

---

## 13. Product Statement

> Pyanpyan helps ADHD brains move gently through tasks and time, with kindness, memory support, and zero shame.

