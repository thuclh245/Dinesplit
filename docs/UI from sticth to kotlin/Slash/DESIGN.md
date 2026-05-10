# Design System Document

## 1. Overview & Creative North Star: "The Social Ledger"
This design system moves away from the sterile, grid-locked nature of traditional fintech. Our Creative North Star is **The Social Ledger**—an editorial-inspired framework that treats a dinner bill with the same aesthetic reverence as a high-end food magazine. 

We break the "template" look by using intentional white space, aggressive typographic scale contrasts, and a "No-Line" philosophy. By layering surfaces rather than boxing them in, we create a fluid, tactile experience that feels like a shared table rather than a spreadsheet.

## 2. Colors & Surface Philosophy
The palette balances the high-energy appetite of "Vibrant Orange" with the calming, mathematical precision of "Soft Teal."

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section content. Boundaries must be defined solely through background color shifts.
*   **Implementation:** A `surface-container-low` section sitting on a `surface` background creates a natural, sophisticated break without the visual clutter of a line.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. Use the `surface-container` tiers to create depth:
*   **Base:** `surface` (#f6f6f6)
*   **Sectioning:** `surface-container-low` (#f0f1f1) for secondary content areas.
*   **Interaction Hubs:** `surface-container-lowest` (#ffffff) for the most important interactive cards.
*   **Nesting:** Place a `surface-container-lowest` card inside a `surface-container-high` zone to create "focal lift."

### The "Glass & Gradient" Rule
To elevate the "Modern" requirement, use Glassmorphism for floating elements (e.g., Bottom Navigation or Floating Action Buttons). Use a semi-transparent `surface` color with a 20px backdrop-blur. 
*   **Signature Textures:** For primary CTAs, use a subtle linear gradient from `primary` (#ab2d00) to `primary_container` (#ff7851) at a 135° angle. This adds "soul" and prevents the app from feeling flat.

## 3. Typography: The Editorial Edge
We pair **Plus Jakarta Sans** (Display/Headlines) with **Inter** (Body/UI) to create a sophisticated, high-contrast hierarchy.

*   **Display (Plus Jakarta Sans):** Used for large "Food Feed" titles and "Total Due" amounts. The exaggerated scale (up to 3.5rem) makes financial data feel bold and transparent.
*   **Headline & Title (Plus Jakarta Sans/Inter):** Used for restaurant names and bill titles. These should feel authoritative.
*   **Body & Labels (Inter):** Used for descriptions and granular expense data. Inter’s high x-height ensures readability even at `body-sm` (0.75rem).

**The Signature Move:** Always pair a `display-lg` financial figure with a `label-md` uppercase descriptor (e.g., "TOTAL BALANCE") to lean into the editorial aesthetic.

## 4. Elevation & Depth
Depth is achieved through **Tonal Layering** and **Ambient Light**, never through heavy, muddy shadows.

*   **The Layering Principle:** Stack `surface-container-lowest` cards on `surface-container-low` backgrounds. This creates a soft, natural lift.
*   **Ambient Shadows:** For floating "Settle Up" buttons, use a shadow with a 24px blur at 6% opacity, using a tinted version of `on-surface` (#2d2f2f) rather than pure black.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility (e.g., in high-contrast mode), use `outline-variant` (#acadad) at **15% opacity**. 100% opaque borders are strictly forbidden.
*   **Glassmorphism:** Apply to top app bars and secondary overlays to allow the vibrant food photography of the feed to bleed through subtly.

## 5. Components

### Cards & Feed Items
*   **Rule:** Forbid divider lines. Use `spacing-6` (1.5rem) or a shift from `surface` to `surface-container` to separate items.
*   **Styling:** Use `xl` (1.5rem/24dp) corner radius for feed cards. Content should be inset with `spacing-4` (1rem).

### Buttons (The Energy Drivers)
*   **Primary:** Gradient fill (`primary` to `primary_container`), `full` roundedness, `title-sm` typography.
*   **Secondary (Settle Up):** `secondary` (#00675f) background with `on_secondary` text. Used exclusively for financial finality.
*   **Tertiary:** Ghost style using `primary` text with no background.

### Status Indicators (The "Finance/Trust" Signal)
*   **Paid:** `secondary_container` background with `on_secondary_container` text.
*   **Unpaid:** `error_container` background with `on_error_container` text.
*   **Shape:** Use `sm` (0.25rem) radius for status chips to distinguish them from the `full` radius buttons.

### Input Fields
*   **Style:** Minimalist. No bottom line. Use a `surface-container-highest` background with an `md` (0.75rem) corner radius. Helper text must use `label-sm` in `on-surface-variant`.

### Social Chips
*   **Action Chips:** Used for "Tag a Friend" or "Split Style." Use `secondary_fixed_dim` with `on_secondary_fixed` text for a soft, premium feel.

## 6. Do's and Don'ts

### Do
*   **Do** use asymmetrical spacing (e.g., more top padding than bottom) in hero sections to create a "magazine" feel.
*   **Do** use `primary_fixed_dim` for "Social" interactions (Likes/Comments) to keep the energy high.
*   **Do** lean on `display-lg` typography for the primary "Amount Owed" to ensure absolute financial clarity.

### Don't
*   **Don't** use 1px dividers to separate friends in a list; use vertical whitespace (`spacing-4`).
*   **Don't** use pure black (#000000) for text. Always use `on_surface` (#2d2f2f) to maintain the "Soft Minimalism" vibe.
*   **Don't** use standard "Drop Shadows." If a card needs to pop, use a tonal background shift or a blurred ambient shadow.