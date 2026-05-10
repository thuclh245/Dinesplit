# Design System Specification: The Social Ledger

## 1. Overview & Creative North Star: "The Culinary Curator"
This design system rejects the clinical, rigid nature of traditional fintech. Instead, it embraces **The Culinary Curator**—a creative North Star that treats group finances as a social invitation rather than a chore. 

To break the "template" look, we move away from standard grids toward **Intentional Asymmetry**. We use oversized typography scales against tight micro-copy and overlapping "floating" containers that break the bounding box of the screen. The result is a UI that feels like a high-end lifestyle magazine: breathable, editorial, and deeply human.

---

## 2. Color & Surface Philosophy
The palette balances the high-energy "hunger" of orange with the calming "settled" nature of teal. 

### The "No-Line" Rule
**Strict Mandate:** Designers are prohibited from using 1px solid borders for sectioning. 
Structure is defined solely through background color shifts. To separate a "Recent Meals" section from the main feed, transition from `surface` (#f8f6f5) to `surface-container-low` (#f2f0ef). 

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of fine paper. 
- **Base Layer:** `surface` (#f8f6f5)
- **Primary Content Areas:** `surface-container` (#e9e8e7)
- **Floating Cards:** `surface-container-lowest` (#ffffff)
Use these tiers to create "nested" depth. An interactive bill-split card should sit as `surface-container-lowest` atop a `surface-container` background to naturally "pop" without a heavy shadow.

### The "Glass & Gradient" Rule
For floating action buttons or high-priority modals, use **Glassmorphism**. Apply a semi-transparent `surface` color with a 16px–24px backdrop blur. 
**Signature Textures:** Main CTAs must use a subtle linear gradient (45°) from `primary` (#ab2d00) to `primary-container` (#ff7851). This adds "soul" and prevents the vibrant orange from feeling flat or "cheap."

---

## 3. Typography: Editorial Impact
We utilize **Plus Jakarta Sans** for its geometric clarity and friendly apertures.

*   **Display (lg/md):** Reserved for "Wow" moments—total savings or large bill amounts. Use `display-lg` (3.5rem) to make financial data feel like art.
*   **Headline (sm/md):** Used for social headers (e.g., "Dinner with the Crew"). These should feel authoritative and inviting.
*   **Title (md/sm):** The workhorse for card titles and names.
*   **Body (md):** Standardized for all conversational and descriptive text.
*   **Labels (md/sm):** Small-caps or high-tracking variants for metadata (dates, "Paid" status).

**Hierarchy Rule:** Always pair a `display-md` number with a `label-sm` descriptor to create a high-contrast editorial look that emphasizes the "Social" and the "Finance" equally.

---

## 4. Elevation & Depth: Tonal Layering
Traditional material shadows are too heavy for a "friendly" app. We use **Ambient Depth**.

*   **The Layering Principle:** Stack `surface-container-low` on `surface` for a soft, natural lift. No shadow required.
*   **Ambient Shadows:** For high-elevation elements (like a floating receipt scanner), use a shadow with a 40px blur, 0% spread, and an opacity of 6%. The color must be a tint of `on-surface` (#2e2f2e), never pure black.
*   **The "Ghost Border" Fallback:** If accessibility requires a stroke (e.g., in high-glare outdoor settings), use the `outline-variant` (#aeadac) token at **15% opacity**.

---

## 5. Signature Components

### Cards (The Core Unit)
*   **Rule:** Forbid divider lines. 
*   **Styling:** Use `roundedness-lg` (2rem) for main feed cards. Separate content using `spacing-6` (1.5rem) of vertical whitespace. 
*   **Visuals:** Hero cards for "Group Dinners" must use vibrant food imagery with a `surface-tint` overlay at 10% to ensure text legibility.

### Buttons
*   **Primary:** Gradient fill (`primary` to `primary-container`), `roundedness-full`, and `on-primary` text.
*   **Secondary/Teal:** Used only for "Success" or "Settled" states. Use `secondary-container` (#85f6e5) with `on-secondary-container` text.
*   **Tertiary:** No background. Use `primary` text weight 600.

### Financial Input Fields
*   **Style:** No "boxed" inputs. Use a "Bottom-Line-Only" approach with `outline-variant` at 20% opacity. 
*   **Active State:** The line transforms into a 2px `primary` (#ab2d00) stroke with a subtle `primary-fixed-dim` glow.

### Interactive "Settling" Chips
*   For individual diners in a split. Use `roundedness-md` (1.5rem). 
*   **Unpaid:** `surface-container-high` background.
*   **Paid:** `secondary-container` (#85f6e5) with a checkmark icon.

---

## 6. Do’s and Don’ts

### Do
*   **Do** use asymmetrical margins (e.g., 24px left, 16px right) on headline elements to create a bespoke, editorial feel.
*   **Do** lean into `roundedness-xl` (3rem) for large image containers to maintain the "Soft & Friendly" persona.
*   **Do** use Teal (#009688) sparingly as a "reward" color, signifying a completed transaction or a balanced budget.

### Don't
*   **Don't** use 1px dividers to separate list items. Use 12px of vertical `surface-container-low` space instead.
*   **Don't** use pure black (#000000) for text. Use `on-surface` (#2e2f2e) to keep the "Modern/Clean" vibe from feeling harsh.
*   **Don't** use standard Android square corners. Every interactive element must have at least `roundedness-sm` (0.5rem).