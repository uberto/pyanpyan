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

#### 4.1.1 Checklist Structure

Each checklist has:

* **Name** - user-defined title
* **Schedule** - optional time-based activation rules
  * Days of week (e.g., Monday, Wednesday, Friday)
  * Time range (e.g., 06:00-09:00)
  * Default: "Always on" (no schedule restrictions)
* **Items** - list of tasks within the checklist

#### 4.1.2 Checklist Library Screen

The main home screen shows two tabs:

* **Checklists tab** - shows all user checklists
* **Timers tab** - shows all user timers

In the Checklists tab:

* **Active checklists** displayed at top (matching current day/time):
  * Full color/vibrant appearance
  * Alphabetically sorted
* **Inactive checklists** displayed below:
  * Muted/faded appearance
  * Alphabetically sorted
* FAB (Floating Action Button) to create new checklist
* Tap checklist to open detail view
* Swipe/long-press for edit/delete actions

#### 4.1.3 Checklist Items

Each checklist item can be in one of the following states:

* `Pending`
* `Done`
* `IgnoredToday`

Ignored items:

* Are visually softened
* Remain visible
* Automatically reset the next day

#### 4.1.4 Item Interaction - Slider Control

Items use a **slider/toggle control** instead of buttons:

* Slider in center = Pending state
* Drag slider **right** → Done
* Drag slider **left** → Ignore today
* Slider stays in position after action (shows completed state visually)
* Harder to accidentally trigger than tap (ADHD-friendly)
* Tactile and deliberate interaction

#### 4.1.5 Ignore Today

* Users can explicitly choose **Ignore today** by sliding left
* This is a first-class action
* No penalty or warning is shown

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

### 5.1 Screen Structure

#### 5.1.1 Home Screen

The main screen contains two tabs:

* **Checklists** - Library of all checklists (active/inactive)
* **Timers** - Library of all timers

Navigation between tabs is prominent and easy to access.

#### 5.1.2 Navigation Flow

```
Home Screen (Checklists/Timers tabs)
   ↓ tap checklist
Checklist Detail Screen
   ↓ edit button
Checklist Editor Screen → save → Home

   ↓ tap timer
Timer Detail Screen → back → Home
```

### 5.2 Skinnable UI

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

### 5.3 Accessibility & Calm Design

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

* **Checklist Management:**
  * `CreateChecklist` - create new checklist with name and schedule
  * `UpdateChecklist` - modify checklist name/schedule
  * `DeleteChecklist` - remove checklist
  * `AddChecklistItem` - add item to checklist
  * `RemoveChecklistItem` - remove item from checklist
  * `UpdateChecklistItem` - edit item details
* **Item State:**
  * `MarkItemDone` - mark item complete
  * `IgnoreItemToday` - skip item for today
  * `ResetDailyState` - reset all items to pending
* **Timers:**
  * `StartTimer`
  * `CompleteTimer`

---

### 8.3 Queries

Queries read state and never mutate.

Examples:

* **Checklist Queries:**
  * `GetAllChecklists` - retrieve all user checklists
  * `GetActiveChecklists` - get checklists matching current day/time
  * `GetChecklist` - retrieve specific checklist by ID
  * `GetChecklistUIState` - get UI state for checklist screen
* **Item Queries:**
  * `GetIgnoredItems` - get all items ignored today
* **Timer Queries:**
  * `GetTimerProgress` - get current timer state
  * `GetAllTimers` - retrieve all user timers

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

