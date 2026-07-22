# UX: Tabib.ma
**References**: docs/prd-tabib-ma.md | **Version**: 1.0 | **Date**: 2026-07-21 | **Author**: UX Designer | **Status**: Draft

## 1. Process Level
New product, 4 distinct roles, comprehensive depth requested → **Standard/Full process**: personas + user flows + IA + wireframe-level screen states for all primary journeys, not just a lightweight sketch.

## 2. Personas

```markdown
## Persona: Amina (Primary — Patient)
Who: 34, works in Casablanca, has a 6-year-old with recurring ear infections
Goal: Find a pediatrician who can see her child this week, ideally near her office, without taking a half-day off work
Frustration: Calling 5 clinics to find one with an opening; not knowing which doctors take her insurance/CMI
Context: Uses her phone during work breaks, moderate tech comfort, prefers French UI but reads Arabic too
Quote: "I just want to see who's free tomorrow afternoon without calling anyone."

## Persona: Dr. Bennani (Doctor)
Who: 45, general practitioner, splits time between his own cabinet and a partner clinic two days/week
Goal: Keep one calendar that reflects both locations so he never double-books himself; spend less admin time on scheduling
Frustration: Currently uses a paper agenda at one location and a phone reminder for the other — has double-booked before
Context: Desktop at his cabinet, phone between patients, low patience for complex software

## Persona: Fatima (Edge case — Clinic Admin)
Who: 29, front-desk manager at a multi-doctor clinic in Rabat
Goal: Onboard 6 doctors quickly, see the clinic's full booking calendar at a glance
Frustration: Needs a bird's-eye view, not per-doctor micromanagement
Context: Desktop, power user, will be the most frequent daily user of the admin surface

## Persona: Platform Admin (Edge case — internal ops)
Who: Tabib.ma operations staff
Goal: Clear the doctor-verification queue fast without missing fraudulent credential submissions; resolve disputes
Frustration: Needs enough context per case to decide without opening 5 tabs
```

## 3. Information Architecture

```
Sitemap:
├── Public (unauthenticated)
│   ├── Home / Search
│   ├── Doctor Profile (public view — bio, reviews, fee, available slots)
│   ├── Login / Register (role selection: Patient / Doctor / Clinic sign-up request)
│   └── Help / Support
├── Patient (authenticated)
│   ├── Search & Book (extends public search with booking action)
│   ├── My Appointments (upcoming / past / cancelled)
│   ├── My Prescriptions
│   ├── Video Consultation Room (time-gated — only active during appointment window)
│   └── Account (profile, payment history)
├── Doctor (authenticated)
│   ├── Dashboard (today's appointments)
│   ├── Calendar / Availability Management
│   ├── Patient Consultation View (appointment detail → join video → issue prescription)
│   ├── Earnings
│   └── Account (profile, credentials)
├── Clinic Admin (authenticated)
│   ├── Clinic Dashboard (all doctors' bookings at a glance)
│   ├── Doctor Management (invite, view roster)
│   └── Clinic Reports (booking volume, revenue)
└── Platform Admin (authenticated, MFA-gated)
    ├── Verification Queue
    ├── Disputes
    ├── Platform Health Dashboard
    └── User/Account Management (support actions: refund, unlock account)
```
Navigation depth check: max 3 levels everywhere above (IA rule) ✅. Primary nav per role stays ≤7 items ✅.

**RTL note**: Arabic UI requires full RTL mirroring (nav position, icon direction, form field order) — this is a UI Designer + Frontend Dev implementation concern, flagged here as an IA constraint: the sitemap/nav structure above must work equally well mirrored, so nothing in the nav is inherently "left-anchored" in meaning.

## 4. Core User Flows

### Flow 1 — Patient: Search → Book → Pay
```
Actor: Amina (Patient)

Happy Path:
(Home) → [Search: specialty + city + optional date filter] → [Results list]
  → [Doctor Profile] → [Select an open slot] → <Logged in?>
     ├─ No → [Login/Register modal, preserves selected slot] → back to slot confirm
     └─ Yes → [Booking confirmation screen: slot, fee, cancellation policy shown]
  → [Redirect to CMI payment] → <Payment result?>
     ├─ Success → [Booking confirmed screen] → confirmation SMS/email sent
     └─ Failure → [Payment failed screen] → [Retry payment] or [Choose different slot]
  → (End: appointment in "My Appointments")

Error Paths:
[Select slot] → <Slot taken by someone else between view and select?> →
  [Error: "This slot was just booked — here are other times"] → back to Doctor Profile slots
[CMI redirect] → <User abandons payment (closes tab)?> →
  [Appointment auto-expires from PENDING_PAYMENT after N minutes, slot released] (see System Design async path)

Edge Cases:
- User hits back button after CMI redirect → show current appointment status, don't re-charge
- User is offline mid-search → show cached last results + "reconnecting" banner, not a blank error page
- No doctors match filters → empty state with "broaden your search" suggestion, not a dead end
```

### Flow 2 — Doctor: Manage Availability
```
Actor: Dr. Bennani (Doctor)

Happy Path:
(Dashboard) → [Calendar view] → [Set recurring weekly hours per location]
  → [Add exception (block a specific date/time)] → (Saved, reflected immediately in patient-facing search)

Error Paths:
[Set recurring hours] → <Conflicts with an existing confirmed appointment?> →
  [Warning: "You have 2 confirmed appointments in this window — block anyway?" ] → confirm/cancel

Edge Cases:
- Doctor affiliated with 2 clinics sets overlapping hours at both → system warns, doesn't silently allow (prevents the exact double-booking scenario Dr. Bennani fears)
```

### Flow 3 — Video Consultation + Prescription
```
Actor: Dr. Bennani + Amina

Happy Path:
(Appointment time arrives) → [Both parties see "Join" button active only within ±10min of start time]
  → [Video room] → <Connection quality?>
     ├─ Good → full video
     └─ Poor → [Auto-suggest audio-only mode] (System Design NFR-7 audio-fallback)
  → [Doctor ends consultation] → [Doctor prompted: issue prescription?]
     ├─ Yes → [Prescription form: drug/dosage/instructions] → [Sign & send] → patient notified
     └─ No → [Mark consultation complete without prescription]
  → (Patient can now leave a review)

Error Paths:
[Join video] → <Camera/mic permission denied?> → [Clear instructions to enable + retry], never a silent failure
[Video connection fails entirely] → [Fallback: "Call patient's phone instead?" prompt for doctor] (manual fallback, not automated in v1 — flagged as a support-friendliness feature, not blocking)

Edge Cases:
- Patient never joins → after grace period, doctor can mark as NO_SHOW → triggers cancellation policy
- Doctor's connection drops mid-consult → patient sees "Doctor reconnecting..." not an abrupt disconnect message
```

### Flow 5 — Platform Admin: Doctor Verification
```
Actor: Platform Admin

Happy Path:
(Verification Queue, sorted oldest-first) → [Open submission: credentials, license doc, doctor profile info side by side]
  → <Decision?>
     ├─ Approve → doctor profile goes live in search results
     └─ Reject → [Reason required, sent to doctor] → doctor can resubmit

Edge Cases:
- Duplicate submission (same doctor resubmits) → queue groups by doctor, shows submission history, not duplicate entries
```

## 5. Screen State Checklist (applied to the 3 highest-traffic screens)

```
📱 Search Results
  ├── Empty    → "No doctors match — try a different specialty or city" + broaden-search CTA
  ├── Loading  → skeleton cards (not spinner — perceived-performance win for a list)
  ├── Loaded   → results with fee, next available slot, rating shown
  ├── Error    → "Couldn't load results, retry" (never blank white screen)
  └── Offline  → cached last results + banner

📱 Booking Confirmation
  ├── Loading  → slot re-validation spinner (checking it's still free)
  ├── Loaded   → fee, policy, slot detail, single primary CTA ("Pay & Confirm")
  ├── Error    → slot no longer available → redirect to alternate slots, not a dead-end error
  └── Expired session → re-auth modal preserves the selected slot (per Flow 1 error path)

📱 Video Consultation Room
  ├── Loading    → "Connecting..." with a visible timeout (10s) before offering audio-only fallback
  ├── Loaded     → video + controls (mute, end call, switch to audio)
  ├── Poor conn. → auto-suggested audio-only banner, one-tap accept
  ├── Error      → permission-denied instructions, or "Doctor hasn't joined yet" waiting state (not a failure state)
  └── Ended      → doctor: prescription prompt / patient: "Consultation complete, prescription pending"
```

## 6. Heuristic Review Notes (applied up front, not just retrospectively)
| # | Heuristic | Applied |
|---|---|---|
| 1 | System status visibility | Booking flow always shows current step + payment status explicitly |
| 3 | User control & freedom | Cancellation/reschedule always reachable from "My Appointments", not buried |
| 5 | Error prevention | Slot re-validated before payment redirect; double-booking warning before it happens (Flow 2) |
| 9 | Error recovery | Every error path above pairs the error with a specific next action, never a dead end |

## 7. Accessibility & Localization Constraints (feed into UI doc)
- WCAG 2.1 AA minimum on the entire booking flow (PRD NFR-6) — focus order, keyboard navigation for slot selection, screen-reader labels on all form fields.
- French primary, Arabic secondary — **Arabic requires full RTL layout**, not just translated strings. This affects nav placement, form field order, and icon directionality (e.g., "back" chevron flips).
- Video room controls must be operable via keyboard/screen reader for accessibility compliance even though it's a real-time media context — at minimum, mute/end-call/switch-to-audio must have accessible labels.

## 8. Handoff Points
- **→ UI Designer**: Flows, screen states, and IA above — ready for visual design pass. RTL and WCAG AA are hard constraints, not nice-to-haves.
- **→ Frontend Dev**: Interaction specs per flow (esp. slot re-validation timing, audio-fallback trigger logic).
- **→ Test Architect**: Error paths and edge cases above are the acceptance-test scenario source — especially the slot-taken-during-checkout race and the video-connection-quality fallback.
