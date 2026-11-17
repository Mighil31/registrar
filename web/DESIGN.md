---
name: Registrar
description: An enrolment office for events. Every event is a catalogue section with a code; every registration is a stamped copy of the form.
colors:
  rail: "#17392b"
  rail-deep: "#0e2219"
  rail-line: "#2b5241"
  rail-ink: "#dce7e0"
  rail-muted: "#a9c2b5"
  gold: "#e9b23a"
  ground: "#f3f4f1"
  panel: "#ffffff"
  line: "#d5dbd6"
  rule: "#eef1ee"
  field: "#c2cbc4"
  track: "#e4e8e4"
  quiet: "#eef1ee"
  ink: "#14201a"
  muted: "#46564d"
  faint: "#4f5f56"
  placeholder: "#66756c"
  canary: "#fff1a8"
  canary-row: "#fffbe6"
  canary-line: "#e2cf6e"
  canary-ink: "#6b4b00"
  pink: "#f9cfd9"
  pink-row: "#fdf1f4"
  pink-line: "#e9a3b5"
  red: "#a3123a"
  red-ink: "#5c0a21"
  mint: "#e3eee7"
  go: "#2f6b4f"
  filling: "#b07d0a"
typography:
  display:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "1.875rem"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "-0.02em"
    fontFeature: "tnum"
  figure:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "26px"
    fontWeight: 800
    lineHeight: 1.15
    fontFeature: "tnum"
  headline:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "21px"
    fontWeight: 800
    lineHeight: 1.25
  title:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 700
    lineHeight: 1.5
  body:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.43
    fontFeature: "tnum"
  body-lead:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.625
  code:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 700
    lineHeight: 1.4
    fontFeature: "tnum"
  label:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.33
  stamp:
    fontFamily: "Public Sans Variable, system-ui, sans-serif"
    fontSize: "11px"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "0.08em"
rounded:
  stamp: "3px"
  copy: "4px"
  control: "6px"
  panel: "8px"
  full: "9999px"
spacing:
  hair: "4px"
  tight: "8px"
  row: "10px"
  gap: "12px"
  cell: "16px"
  section: "20px"
  panel: "24px"
  gutter-phone: "16px"
  gutter-desktop: "32px"
  rail-width: "232px"
  touch: "44px"
components:
  button-primary:
    backgroundColor: "{colors.rail}"
    textColor: "{colors.panel}"
    rounded: "{rounded.control}"
    padding: "0 16px"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.rail-deep}"
  button-secondary:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.rail}"
    rounded: "{rounded.control}"
    padding: "0 16px"
    height: "44px"
  button-secondary-hover:
    backgroundColor: "{colors.mint}"
  button-quiet:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0 16px"
    height: "44px"
  button-quiet-hover:
    backgroundColor: "{colors.quiet}"
  button-danger:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.red}"
    rounded: "{rounded.control}"
    padding: "0 16px"
    height: "44px"
  button-danger-solid:
    backgroundColor: "{colors.red}"
    textColor: "{colors.panel}"
    rounded: "{rounded.control}"
    padding: "0 16px"
    height: "44px"
  button-danger-solid-hover:
    backgroundColor: "{colors.red-ink}"
  field:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0 12px"
    height: "44px"
  chip:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.muted}"
    rounded: "{rounded.control}"
    padding: "0 14px"
    height: "44px"
  chip-pressed:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.panel}"
  panel:
    backgroundColor: "{colors.panel}"
    rounded: "{rounded.panel}"
  rail-link:
    textColor: "{colors.rail-ink}"
    rounded: "{rounded.control}"
    padding: "0 10px"
    height: "44px"
  rail-link-hover:
    backgroundColor: "{colors.rail-line}"
    textColor: "{colors.panel}"
  rail-link-active:
    backgroundColor: "{colors.gold}"
    textColor: "{colors.rail}"
  stamp-open:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.go}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "3px 7px"
  stamp-private:
    backgroundColor: "{colors.quiet}"
    textColor: "{colors.ink}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "3px 7px"
  stamp-registered:
    backgroundColor: "{colors.canary}"
    textColor: "{colors.go}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "3px 7px"
  stamp-closed:
    backgroundColor: "{colors.pink}"
    textColor: "{colors.red}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "3px 7px"
  stamp-admin:
    backgroundColor: "{colors.mint}"
    textColor: "{colors.rail}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "3px 7px"
  copy-tag-self:
    backgroundColor: "{colors.canary}"
    textColor: "{colors.canary-ink}"
    rounded: "{rounded.stamp}"
    padding: "1px 6px"
  copy-tag-admin:
    backgroundColor: "{colors.mint}"
    textColor: "{colors.rail}"
    rounded: "{rounded.stamp}"
    padding: "1px 6px"
  copy-tag-closed:
    backgroundColor: "{colors.pink}"
    textColor: "{colors.red}"
    rounded: "{rounded.stamp}"
    padding: "1px 6px"
  member-copy:
    backgroundColor: "{colors.canary}"
    textColor: "{colors.ink}"
    rounded: "{rounded.copy}"
    padding: "18px 20px"
  member-copy-cancelled:
    backgroundColor: "{colors.pink}"
    textColor: "{colors.ink}"
  seat-bar:
    backgroundColor: "{colors.track}"
    rounded: "{rounded.copy}"
    height: "8px"
---

# Design System: Registrar

## Overview

**Creative North Star: "The Enrolment Office"**

Registrar is a registrar's office rendered as software: a course catalogue on one side, carbonless enrolment forms on the other. Every event carries a catalogue code (its first tag's prefix and its start date, like `WKS 1123`), and every registration is a copy of a form, tinted the way multi-part forms are tinted: canary for the member's own copy, pink for a closed or cancelled one, mint for one an admin filed. State is stamped onto those copies in 2px-bordered capitals. The office is calm, dense and official; it reads as records, not as a marketing dashboard.

The frame is a deep registrar-green rail with a gold marker for the current page, a pale ground, and flat white panels with hairline borders. One face does all the work, Public Sans with tabular figures, so columns of counts, times and codes line up across rows. Weight, not a second family, carries hierarchy: extra-bold for titles and figures, bold for codes and names, regular for everything read.

The system refuses the grey KPI-card dashboard with one blue accent. Numbers sit in a plain line, not in cards; colour is spent on state, never on decoration.

**Key Characteristics:**
- Green rail with a gold current-page marker; pale ground; flat white panels with 1px hairlines.
- Form-copy tints carry state: canary (yours), pink (closed, cancelled, full), mint (added by an admin).
- Stamps: 11px extra-bold capitals in a 2px border of their own colour.
- One face, Public Sans, with tabular figures everywhere.
- Columns align across rows; on phones, rows become stacked entries and the selected one expands in place.
- One authored motion: the REGISTERED stamp pressed onto the copy.

## Colors

A forest-green office frame around paper-white records, with state carried by the pastel tints of carbonless form copies.

### Primary
- **Registrar Green** (#17392b): the rail, primary buttons, links, the catalogue code on an event header, and the seat bar while seats are open. The colour of the institution.
- **Deep Registrar** (#0e2219): primary button hover only.
- **Rail Line** (#2b5241): dividers inside the rail and rail-link hover fill.
- **Rail Ink** (#dce7e0) and **Rail Muted** (#a9c2b5): rail link text and the rail's group labels (Admin, Member).

### Secondary
- **Office Gold** (#e9b23a): the current page in the rail, the skip link, and the focus outline on the rail. Nowhere outside the rail's wayfinding.

### Tertiary (form-copy tints)
- **Canary Copy** (#fff1a8): the member's copy. My events cards, REGISTERED stamps and tags, text selection. **Canary Row** (#fffbe6) tints a registered event header and the selected People row; **Canary Line** (#e2cf6e) borders and rules on canary; **Canary Ink** (#6b4b00) for small text on canary.
- **Pink Copy** (#f9cfd9): closed, full, started and cancelled stamps and the cancelled member copy. **Pink Row** (#fdf1f4) tints a full table row, danger hover and the error box; **Pink Line** (#e9a3b5) borders on pink.
- **Mint Copy** (#e3eee7): ADDED BY ADMIN tags, ADMIN stamps, secondary button hover.
- **Docket Red** (#a3123a): danger buttons, error text and borders, the full seat bar, closed-state stamp ink. **Red Ink** (#5c0a21) for error-box body text and solid-danger hover.
- **Go Green** (#2f6b4f): the ink of OPEN and REGISTERED stamps.
- **Filling Amber** (#b07d0a): the seat bar between open and full. Bar fill only, never text.

### Neutral
- **Office Ground** (#f3f4f1): the page behind everything.
- **Panel White** (#ffffff): panels, tables, fields, dialogs.
- **Hairline** (#d5dbd6): panel borders and panel-header dividers.
- **Rule** (#eef1ee): row dividers inside panels; **Quiet** (same value) for quiet-button hover and the PRIVATE stamp fill.
- **Field Stroke** (#c2cbc4): input, chip and quiet-button borders.
- **Track** (#e4e8e4): the seat-bar track and skeletons.
- **Ink** (#14201a): body text, pressed chips, the focus outline.
- **Muted** (#46564d) and **Faint** (#4f5f56): leads and table headers; secondary figures like "/ 120".
- **Placeholder** (#66756c): input placeholder text.

### Named Rules
**The Carbon Copy Rule.** Canary, pink and mint mean exactly one thing each: yours, closed, filed by an admin. They are never decorative, and a tinted surface always carries a word (a stamp or tag) that says the same thing, so colour is never the only signal.

**The Gold Marker Rule.** Gold marks where you are in the rail. It does not appear in the content area.

## Typography

**Display Font:** Public Sans Variable (with system-ui, sans-serif)
**Body Font:** Public Sans Variable
**Label/Mono Font:** Public Sans Variable, tabular figures on

**Character:** One sober civic sans in many weights, like a well-set form. Tabular figures make codes, times and seat counts line up like a printed register.

### Hierarchy
- **Display** (800, 30px, -0.02em): page titles and the event header title. Sign in and Sign up set it at 32px.
- **Figure** (800, 26px): the dashboard's plain line of numbers, followed by a muted 15px word. The event header's seat count uses the display size.
- **Headline** (800, 20–21px): member-copy titles and dialog titles.
- **Title** (700, 16px): panel titles.
- **Body** (400, 14px): tables, lists, forms, panel content. Event description and page leads use **Body Lead** (15px, relaxed leading, max 70ch).
- **Code** (700, 13px): catalogue codes (`WKS 1123`) in tables and above the event title.
- **Label** (600, 12px): table headers, definition terms, rail group labels. Sentence case.
- **Stamp** (800, 11px, 0.08em, capitals): stamps only. Copy tags use 11px 700 at 0.02em.

### Named Rules
**The One Face Rule.** Public Sans is the only family. Hierarchy comes from weight (400 to 800) and size, never from a second face.

**The Capitals Belong To Stamps Rule.** Uppercase is reserved for stamps, copy tags and the copy's registration line. Headings, table headers and labels stay in sentence case.

## Layout

A two-column app shell from the md breakpoint (768px) up: a 232px sticky green rail and a fluid main column with 32px side padding (28px top). Below md the rail becomes a green top bar: the wordmark and Sign out on the first line, the same links wrapping below. Main padding drops to 16px.

Main content stacks with a 20px gap. The dashboard puts the events panel (flex basis 640px, grows 2) beside Recent registrations, wrapping below on narrow widths. My events lays copies in an auto-fill grid with 420px minimum columns and a 16px gap. Pages that hold copies cap at 1180px; reading text caps at 70ch.

Tables align columns across rows: cell padding 16px horizontal, 10px for headers and about 16px for body rows, row dividers in Rule. Below md every admin table becomes a list of stacked entries; on People the selected entry expands in place under the person's name instead of opening a panel below the list. Interactive targets are at least 44px tall throughout (buttons, fields, chips, rail links, back links).

Sign in and Sign up split at lg (1024px): a 46% green panel holding one sample member copy, beside a 400px form column. Below lg the green panel moves under the form.

## Elevation & Depth

Flat by default. Depth comes from tone (green rail, pale ground, white panels) and 1px hairlines, not shadows. Shadows appear only on things that are physically paper or that float above the page.

### Shadow Vocabulary
- **Paper rest** (`box-shadow: 0 1px 2px rgba(20,32,26,.08)`): member copies in My events, a sheet of paper lying on the desk.
- **Sample copy** (`box-shadow: 0 2px 4px rgba(0,0,0,.18), 0 12px 28px rgba(0,0,0,.18)`): the sample copy on the green auth panel.
- **Dialog** (`box-shadow: 0 2px 4px rgba(20,32,26,.12), 0 18px 40px rgba(20,32,26,.22)`): the confirm dialog, over an Ink overlay at 45%.
- **Tab underline** (`box-shadow: inset 0 -3px 0 #17392b`): the selected Upcoming / Past tab. A rule, not depth.

### Named Rules
**The Paper Only Rule.** Panels, tables and buttons never cast shadows. A shadow means a copy of a form or a dialog.

## Shapes

Small, office-supply corners: 3px on stamps and copy tags, 4px on member copies and seat bars, 6px on buttons, fields, chips and rail links, 8px on panels and dialogs. Nothing is pill-shaped except the busy spinner. Borders are 1px hairlines everywhere except stamps (2px in the stamp's own ink) and the error box (2px red). A cancelled copy strikes through its title with a 2px line.

## Components

### Buttons
Solid, square-shouldered, plain.
- **Shape:** gently squared (6px), 44px tall, 16px side padding, 14px semibold. A large size (48px, 20px padding, 16px bold) is used for the Sign in and Sign up submits.
- **Primary:** Registrar Green fill, white text; hover Deep Registrar.
- **Secondary:** white with a green 1px border and green text; hover Mint.
- **Quiet (default):** white, Field Stroke border, Ink text; hover Quiet.
- **Danger / Danger solid:** white with red border and text (hover Pink Row) / red fill with white text (hover Red Ink). Solid danger appears only inside the confirm dialog.
- **Hover / Focus:** 150ms colour transition; focus is a 2px Ink outline offset 2px. Disabled drops to 55% opacity. Busy shows a 14px spinning ring before the label.
- A link that looks like a button takes the button classes directly; a button is never wrapped in a link.

### Stamps (signature)
The world's mark. 11px extra-bold capitals, 0.08em tracking, 3px/7px padding, a 2px border in the text's own colour, 3px corners.
- **Kinds:** OPEN (go green on white), PRIVATE (ink on Quiet), REGISTERED (go green on canary), FULL / STARTED / CLOSED / CANCELLED (red on pink), ADMIN (green on mint), MEMBER (faint on white), OVER capacity (red on white). On a member copy the REGISTERED stamp sits transparent on the canary.
- **Access vs Availability:** an event shows its Access stamp (OPEN or PRIVATE) and, separately, at most one Availability stamp (CANCELLED, then STARTED, then FULL). They are never merged, so a full private event shows PRIVATE and FULL.
- **Landing:** after a successful Register the REGISTERED stamp lands (260ms, scale 1.35 and -6deg to rest, `cubic-bezier(0.16,1,0.3,1)`, no bounce) and the event header's canary tint fades in from white (420ms ease-out). Both are off under `prefers-reduced-motion`. This is the system's only authored motion.

### Copy Tags
Small form-copy labels in feeds and lists: 11px bold, 1px/6px padding, 3px corners, no border. REGISTERED (Canary Ink on canary), ADDED BY ADMIN (green on mint), OVER CAPACITY (red on pink).

### Member Copy (signature)
A registration as a carbon copy: canary card (pink once cancelled) with a Canary Line border, 4px corners, Paper rest shadow, 18px/20px padding. Top line: catalogue code left, "REGISTERED SAT 7 NOV" right, 12px bold in Canary Ink. Title 21px extra-bold. A three-column When / Where / Going grid under a canary rule. Footer: REGISTERED stamp left, actions right.

### Seat Bar
An 8px track in Track grey with 4px corners. Fill is Registrar Green while open, Filling Amber near full, Docket Red when full; unlimited events draw a full bar at 45% green. Width animates over 300ms. Paired with a bold count and a muted "/ capacity" or "no limit"; the bar has a spoken label. A full event's table row is tinted Pink Row and carries a FULL stamp.

### Cards / Containers (Panel)
- **Corner Style:** 8px.
- **Background:** Panel White on the Ground.
- **Shadow Strategy:** none (see The Paper Only Rule).
- **Border:** 1px Hairline; an optional header row (16px bold title, right-hand action link) separated by a Hairline rule, 20px/14px padding.
- **Internal Padding:** content-specific; lists divide with Rule.

### Inputs / Fields
- **Style:** 44px tall, white, 1px Field Stroke, 6px corners, 12px padding, 16px text. Label above in 14px semibold, 6px gap.
- **Focus:** the global 2px Ink outline, offset 2px. The search box carries the outline on its wrapper and a 16px stroked magnifier.
- **Error:** red border plus a 14px semibold red message linked by `aria-describedby`; otherwise a muted hint.

### Chips (filters)
- **Style:** 44px buttons, 6px corners, 13px semibold, Field Stroke border, muted text; hover inks the border and text.
- **State:** pressed is solid Ink with white text and `aria-pressed`.

### Navigation
- **Rail (md and up):** 232px Registrar Green column, sticky. Wordmark "Registrar" 19px extra-bold white over a Rail Line rule; 12px Rail Muted group labels (Admin, Member) for admins; links 44px tall, 14px medium Rail Ink, hover Rail Line fill with white text, current page a solid Gold fill with green bold text. My events shows an upcoming count. The signed-in name and role and a bordered Sign out sit at the foot.
- **Phone:** the same green as a top bar, links wrapping in a row, group labels hidden.
- **Tabs (My events):** text tabs with a 3px green inset underline on the selected one.

### Confirm Dialog
Centred white panel (max 460px, 8px corners, 24px padding, Dialog shadow) over Ink at 45%. Extra-bold 20px title, muted body, "Keep it" quiet button and a solid danger confirm. Destroying an event or a person requires typing its exact title or name first.

### Feedback
Empty states are left-aligned: a bold line, a muted sentence on what to do, an action. Load errors are a 2px red box on Pink Row with the request id to quote and a Try again button. Skeletons pulse in Track grey in the shape of the rows they replace.

## Do's and Don'ts

### Do:
- **Do** give every event its catalogue code (first-tag prefix + MMDD, e.g. `WKS 1123`), set in 13px bold. It is display-only: never store, search or sort by it.
- **Do** show Access (OPEN / PRIVATE) and Availability (FULL / STARTED / CANCELLED) as two separate stamps.
- **Do** put state on form-copy tints with a word on them: canary for the viewer's own registration, pink for closed or cancelled, mint for added by an admin.
- **Do** keep tabular figures on and align numbers across rows.
- **Do** turn admin tables into stacked entries below 768px, and expand the selected entry in place.
- **Do** keep every interactive target at least 44px tall and every focus visible (2px Ink outline, Gold on the rail).
- **Do** keep motion to the REGISTERED stamp landing and the header's canary fade, both off under reduced motion; elsewhere only 150ms colour transitions.

### Don't:
- **Don't** build the grey KPI-card dashboard with one blue accent; key numbers sit in one plain line of figures.
- **Don't** merge Access and Availability into one badge, or let FULL hide PRIVATE.
- **Don't** use canary, pink or mint as decoration, or use colour as the only signal of state.
- **Don't** add a second typeface or a display face; Public Sans carries every role.
- **Don't** put shadows on panels, tables or buttons; shadows belong to paper copies and dialogs.
- **Don't** set headings, table headers or labels in capitals; capitals belong to stamps and copy tags.
- **Don't** use Gold outside the rail's wayfinding.
